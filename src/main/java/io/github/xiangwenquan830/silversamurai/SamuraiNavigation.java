package io.github.xiangwenquan830.silversamurai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;

/** 分帧规划与完整体型执行。没有传送，也不使用玩家尺寸替代Boss碰撞箱。 */
public final class SamuraiNavigation implements RoutePlanner.World {
    private static final Map<Level, long[]> BUDGET = new WeakHashMap<>();
    private static final Map<Level, Map<Integer, Long>> REQUESTERS = new WeakHashMap<>();
    private final SamuraiEntity boss;
    private RoutePlanner planner;
    private List<RoutePlanner.Edge> route;
    private int index, segment, tier, failures;
    private long nextPlan, nextTraversal, clearStart;
    private Vec3 destination = Vec3.ZERO, plannedDestination = Vec3.ZERO, progressPoint = Vec3.ZERO;
    private double bestDistance = Double.MAX_VALUE;
    private int noProgress, edgeTicks;
    private final Map<String, Long> failedEdges = new HashMap<>();
    private final Map<BlockPos, Long> deniedBlocks = new HashMap<>();
    private Set<BlockPos> corridor = Set.of();
    private RoutePlanner.Edge activeEdge;
    private String status = "静止";
    private boolean failed, backgroundPlan, routePartial, waypointGoal, allowClearing = true;
    private int movingReplans;

    int movingReplans() {
        return movingReplans;
    }

    private double walkingSpeed, walkMultiplier = 1, remainingStep, walkTravelled;
    private long walkTravelTick = Long.MIN_VALUE, gravityTick = Long.MIN_VALUE;

    boolean controlsGravity() {
        return gravityTick == now() && activeEdge != null;
    }

    /** Actual collision-checked route arc, including vertical stair segments. */
    double walkDistance() {
        return walkTravelTick == now() ? walkTravelled : 0;
    }

    private final PursuitProgress pursuitProgress = new PursuitProgress();

    public double walkingSpeed() {
        return walkingSpeed;
    }

    public boolean terrainMotion() {
        return activeEdge != null
                && activeEdge.kind() == RoutePlanner.Kind.WALK
                && Math.abs(activeEdge.to().y() - activeEdge.from().y()) > .05;
    }

    public void finishTerrainStep() {
        if (!terrainMotion()) return;
        // Keep the same acceleration controller while finishing the safe landing.
        // Previously this froze the first stair edge at its initial 0.035/0.06 speed.
        walkRoute();
    }

    private void updateStep(RoutePlanner.Edge edge) {
        double total = 0, left = 0;
        var points = edge.points();
        for (int i = 1; i < points.size(); i++) total += points.get(i - 1).distance(points.get(i));
        if (segment < points.size()) {
            left = boss.position().distanceTo(vec(points.get(segment)));
            for (int i = segment + 1; i < points.size(); i++)
                left += points.get(i - 1).distance(points.get(i));
        }
        boss.terrainStep(
                (float) Math.max(0, Math.min(1, 1 - left / Math.max(.01, total))),
                (float) (edge.to().y() - edge.from().y()),
                false);
    }

    public void resetSpeed() {
        walkingSpeed = 0;
    }

    /** 短站位动作逐段检查完整碰撞箱、地面和其他实体；拒绝悬崖、窄道及穿人。 */
    public boolean safeStep(Vec3 from, Vec3 to) {
        int samples = Math.max(1, (int) Math.ceil(from.distanceTo(to) / .12));
        for (int i = 1; i <= samples; i++) {
            Vec3 at = from.lerp(to, i / (double) samples);
            var p = point(at);
            if (!fits(p) || !support(p)) return false;
            AABB b = box(p);
            for (var e :
                    boss.level()
                            .getEntitiesOfClass(
                                    net.minecraft.world.entity.LivingEntity.class,
                                    b,
                                    e -> e != boss && e.isAlive() && !e.isSpectator())) {
                // 初始已经贴脸允许向外脱离，但不能进一步挤入碰撞箱。
                if (!e.getBoundingBox().intersects(box(point(from)))
                        || at.distanceToSqr(e.position())
                                < from.distanceToSqr(e.position()) - .0001) return false;
            }
        }
        return true;
    }

    public SamuraiNavigation(SamuraiEntity boss) {
        this.boss = boss;
    }

    private long now() {
        return boss.level().getGameTime();
    }

    public static RoutePlanner.Point point(Vec3 p) {
        return new RoutePlanner.Point(p.x, p.y, p.z);
    }

    public static Vec3 vec(RoutePlanner.Point p) {
        return new Vec3(p.x(), p.y(), p.z());
    }

    public void cancel() {
        allowClearing = true;
        waypointGoal = false;
        routePartial = false;
        backgroundPlan = false;
        planner = null;
        route = null;
        activeEdge = null;
        corridor = Set.of();
        failed = false;
        tier = 0;
        failures = 0;
        noProgress = 0;
        nextPlan = now();
        bestDistance = Double.MAX_VALUE;
        walkingSpeed = 0;
        boss.setNoGravity(false);
        boss.setDeltaMovement(0, Math.min(0, boss.getDeltaMovement().y), 0);
    }

    public boolean failed() {
        return failed;
    }

    void targetChanged() {
        pursuitProgress.reset();
    }

    int partialRoutes() {
        return partialRoutes;
    }

    private int partialRoutes;

    private record Approach(Vec3 from, Vec3 to, long expires, int quality) {}

    private final Map<Integer, Approach> approaches = new HashMap<>();
    private long probeTick = -1;
    private int probes;

    /**
     * Cheap, bounded full-body corridor check. Unknown/blocked direct routes still permit A*
     * detours.
     */
    int approachQuality(net.minecraft.world.entity.LivingEntity target) {
        Vec3 to = target.position(), from = boss.position();
        Approach cached = approaches.get(target.getId());
        if (cached != null
                && cached.expires > now()
                && cached.from.distanceToSqr(from) < 1
                && cached.to.distanceToSqr(to) < 1) return cached.quality;
        if (probeTick != now()) {
            probeTick = now();
            probes = 0;
        }
        if (probes >= 4 || from.distanceToSqr(to) > 144) return 0;
        probes++;
        if (approaches.size() >= 64)
            approaches.entrySet().removeIf(e -> e.getValue().expires <= now());
        if (approaches.size() >= 64) approaches.clear();
        double distance = from.distanceTo(to), travel = Math.max(0, distance - 3.0);
        int quality = 0;
        if (Math.abs(to.y - from.y) < .4 && travel > .05) {
            quality = 1;
            int steps = Math.max(1, (int) Math.ceil(travel / .25));
            for (int i = 1; i <= steps; i++) {
                Vec3 at = from.lerp(to, travel / distance * i / steps);
                if (!fits(point(at)) || !support(point(at))) {
                    quality = -1;
                    break;
                }
            }
        }
        approaches.put(target.getId(), new Approach(from, to, now() + 30, quality));
        return quality;
    }

    private record FailedGoal(Vec3 at, long expires) {}

    private final List<FailedGoal> failedGoals = new ArrayList<>();

    private void rememberGoal() {
        failedGoals.removeIf(e -> e.expires <= now() || e.at.distanceToSqr(destination) < 4);
        if (failedGoals.size() >= 32) failedGoals.remove(0);
        failedGoals.add(new FailedGoal(destination, now() + BossConfig.FAILED_RETRY.get()));
    }

    boolean failedDestination(Vec3 target) {
        failedGoals.removeIf(e -> e.expires <= now());
        return failedGoals.stream().anyMatch(e -> e.at.distanceToSqr(target) < 4);
    }

    static long spentThisTick(Level level) {
        long[] b = BUDGET.get(level);
        return b != null && b[0] == level.getGameTime() ? b[1] : 0;
    }

    public String report() {
        return status
                + "，搜索层级="
                + tier
                + "，停滞="
                + noProgress
                + "，失败="
                + failures
                + "，往返停滞="
                + pursuitProgress.stalledWindows();
    }

    public boolean planning() {
        return planner != null || tier > 0;
    }

    public boolean hasRoute() {
        return route != null && index < route.size();
    }

    public void chase(Vec3 target, double speed) {
        allowClearing = true;
        waypointGoal = false;
        chaseInternal(target, speed);
    }

    void reposition(Vec3 target, double speed) {
        allowClearing = false;
        waypointGoal = false;
        chaseInternal(target, speed);
    }

    void chaseWaypoint(Vec3 point, double speed) {
        allowClearing = true;
        waypointGoal = true;
        chaseInternal(point, speed);
    }

    private void chaseInternal(Vec3 target, double speed) {
        failed = false;
        walkMultiplier = speed;
        if (activeEdge == null && !boss.onGround() && !support(point(boss.position()))) {
            planner = null;
            boss.haltMotion();
            status = "先正常落地，再从落脚点寻路";
            return;
        }
        if (failedDestination(target)) {
            failed = true;
            boss.haltMotion();
            walkingSpeed = 0;
            status = "暂避已失败的接近位置";
            return;
        }
        if (target.distanceToSqr(plannedDestination) > 4 && now() >= nextPlan) {
            destination = target;
            plannedDestination = target;
            planner = null;
            tier = 0;
            noProgress = 0;
            backgroundPlan = hasRoute();
            if (!backgroundPlan) route = null;
            nextPlan = now();
        } else destination = target;
        // Start the next segment while still moving; never brake as if a waypoint were the enemy.
        if (routePartial
                && hasRoute()
                && route.size() - index <= 8
                && !terrainMotion()
                && planner == null
                && now() >= nextPlan) backgroundPlan = true;
        if (backgroundPlan || planner != null || !hasRoute()) plan();
        // Finish a wall waypoint precisely; the normal A* goal allows a sub-block grid tolerance.
        // Reuse walking acceleration and collision checks instead of snapping the model to the
        // wall.
        if (waypointGoal
                && !hasRoute()
                && boss.position().distanceToSqr(target) > .000025
                && boss.position().distanceToSqr(target) < 1.5
                && walkConnection(boss.position(), target)) {
            route =
                    List.of(
                            new RoutePlanner.Edge(
                                    point(boss.position()),
                                    point(target),
                                    RoutePlanner.Kind.WALK,
                                    target.y));
            index = 0;
            segment = 1;
        }
        if (route != null && index < route.size()) {
            RoutePlanner.Edge edge = route.get(index);
            if (boss.weaponsDrawn()) {
                var progress = pursuitProgress.sample(now(), boss.position(), target);
                if (progress != PursuitProgress.Result.ADVANCING) {
                    reject(edge, "重复回到障碍附近，禁用本段并换接近方向");
                    if (progress == PursuitProgress.Result.SWITCH_TARGET) {
                        failed = true;
                        rememberGoal();
                        status = "连续往返无进展，暂避此目标并选择附近敌人";
                    }
                    return;
                }
            }
            if (edge.kind() != RoutePlanner.Kind.WALK) {
                if (!boss.weaponsDrawn()) {
                    boss.haltMotion();
                    status = "等待拔刀完成";
                    return;
                }
                if (edge.kind() == RoutePlanner.Kind.CLEAR) {
                    if (beginClear(edge)) return;
                    reject(edge, "清障入口受限");
                    return;
                }
                if (edge.kind() == RoutePlanner.Kind.CLIMB
                        && edge.to().y() - edge.from().y() >= 2.5
                        && BossConfig.WALL_CLIMB.get()) {
                    if (boss.queueWallRoute(vec(edge.to()))) return;
                    reject(edge, "抓墙入口不足，改选可站稳的接近路线");
                    return;
                }
                if (now() < nextTraversal) {
                    boss.haltMotion();
                    status = "等待机械动作冷却";
                    return;
                }
                activeEdge = edge;
                segment = 1;
                edgeTicks = 0;
                boss.beginTraversal(edge.kind() == RoutePlanner.Kind.CLIMB);
                status = edge.kind() == RoutePlanner.Kind.CLIMB ? "机械攀爬" : "低障碍翻越";
                return;
            }
            walkRoute();
            return;
        }
        boss.haltMotion();
        boss.setNoGravity(false);
        walkingSpeed = 0;
        if (boss.position().subtract(target).horizontalDistanceSqr() > .01)
            boss.facePoint(target, 7);
    }

    private boolean walkConnection(Vec3 from, Vec3 to) {
        return Math.abs(from.y - to.y) <= 1.25
                && RoutePlanner.sweep(
                        this,
                        new RoutePlanner.Edge(
                                point(from),
                                point(to),
                                RoutePlanner.Kind.WALK,
                                Math.max(from.y, to.y)),
                        false);
    }

    boolean directWalkable(Vec3 goal) {
        Vec3 from = boss.position(), flat = goal.subtract(from).multiply(1, 0, 1);
        double distance = flat.length();
        if (distance < 1 || distance > 24 || !support(point(from))) return false;
        Vec3 direction = flat.scale(1 / distance);
        double travel = Math.max(.5, distance - 2);
        for (double d = .5; d <= travel + .001; d += .5) {
            Vec3 at = boss.position().add(direction.scale(d));
            var floor = floor(at.x, at.z, from.y, 1.25, 1.25);
            if (floor == null) return false;
            Vec3 to = vec(floor);
            if (!walkConnection(from, to)) return false;
            from = to;
        }
        return Math.abs(from.y - goal.y) <= 1.25;
    }

    private void plan() {
        if (planner == null && now() >= nextPlan) {
            failedEdges.entrySet().removeIf(e -> e.getValue() <= now());
            deniedBlocks.entrySet().removeIf(e -> e.getValue() <= now());
            var settings =
                    new RoutePlanner.Settings(
                            BossConfig.SAFE_DROP.get(),
                            BossConfig.VAULT.get(),
                            BossConfig.CLIMB.get(),
                            canClear() ? BossConfig.CLEAR_RANGE.get() : 0,
                            Math.min(
                                    BossConfig.NAV_TOTAL.get(),
                                    tier == 0
                                            ? 180
                                            : tier == 1
                                                    ? 180
                                                    : tier == 2 ? 240 : BossConfig.NAV_TOTAL.get()),
                            tier);
            planner = new RoutePlanner(this, point(boss.position()), point(destination), settings);
            status = "分帧规划";
            nextPlan = now() + BossConfig.REPATH.get();
        }
        if (planner != null) {
            long[] budget = BUDGET.computeIfAbsent(boss.level(), l -> new long[] {-1, 0});
            if (budget[0] != now()) {
                budget[0] = now();
                budget[1] = 0;
            }
            int available =
                    (int)
                            Math.min(
                                    BossConfig.NAV_NODES.get(),
                                    BossConfig.NAV_WORLD.get() - budget[1]);
            // 按本维度近期请求者轮转，避免实体tick顺序使后面的Boss一直拿不到预算。
            Map<Integer, Long> requests =
                    REQUESTERS.computeIfAbsent(boss.level(), l -> new HashMap<>());
            requests.put(boss.getId(), now());
            requests.entrySet().removeIf(e -> now() - e.getValue() > 3);
            List<Integer> order = new ArrayList<>(requests.keySet());
            Collections.sort(order);
            int rank = order.indexOf(boss.getId()), count = order.size();
            int slots = Math.max(1, BossConfig.NAV_WORLD.get() / BossConfig.NAV_NODES.get());
            boolean turn =
                    count <= slots || Math.floorMod(rank - (int) (now() % count), count) < slots;
            if (available > 0 && turn) budget[1] += planner.step(available);
            if (planner.done()) {
                List<RoutePlanner.Edge> result = planner.result();
                boolean partial = planner.partial();
                if (backgroundPlan) {
                    List<RoutePlanner.Edge> joined =
                            result == null ? null : joinMovingRoute(result, planner);
                    planner = null;
                    backgroundPlan = false;
                    if (joined == null) {
                        status = "新路线尚不能安全衔接，继续已验证路线";
                        return;
                    }
                    route = joined;
                    routePartial = partial;
                    if (partial) partialRoutes++;
                    index = 0;
                    activeEdge = null;
                    noProgress = 0;
                    bestDistance = Double.MAX_VALUE;
                    movingReplans++;
                    status = "行进中接续新路线，保留当前速度";
                    return;
                }
                route = result;
                routePartial = partial;
                if (partial) partialRoutes++;
                planner = null;
                index = 0;
                activeEdge = null;
                if (route == null) {
                    tier++;
                    status = "正常路径失败，升级障碍处理";
                    if (tier > 3) {
                        tier = 0;
                        failures++;
                        failed = true;
                        rememberGoal();
                        status = "暂时不可达";
                        nextPlan = now() + BossConfig.FAILED_RETRY.get();
                    }
                } else if (route.isEmpty()) {
                    route = null;
                    status = "已到可攻击接近位";
                } else status = "按完整体型路线追击";
            }
        }
    }

    /** Rebase a bounded prefix at the current feet; never snap back to the old search origin. */
    private List<RoutePlanner.Edge> joinMovingRoute(
            List<RoutePlanner.Edge> result, RoutePlanner search) {
        if (result.isEmpty())
            return goal(point(boss.position()), point(destination)) ? result : null;
        Vec3 here = boss.position();
        RoutePlanner.Point from = point(here);
        if (!support(from)) return null;
        Vec3 forward =
                new Vec3(
                        -Math.sin(Math.toRadians(boss.getYRot())),
                        0,
                        Math.cos(Math.toRadians(boss.getYRot())));
        int best = -1;
        double cost = Double.MAX_VALUE;
        RoutePlanner.Edge connector = null;
        for (int i = 0; i < Math.min(8, result.size()); i++) {
            var e = result.get(i);
            if (e.kind() != RoutePlanner.Kind.WALK) break;
            Vec3 to = vec(e.to()), offset = to.subtract(here);
            double distance = offset.length();
            if (distance < .0001
                    || distance > 2.5
                    || offset.dot(forward) < -.05
                    || Math.abs(offset.y) > 1.001) continue;
            var link =
                    new RoutePlanner.Edge(
                            from, e.to(), RoutePlanner.Kind.WALK, Math.max(from.y(), e.to().y()));
            if (!search.sweep(link, false)) continue;
            if (Math.abs(offset.y) < .05 && !safeStep(here, to)) continue;
            if (Math.abs(offset.y) >= .05 && offset.horizontalDistanceSqr() > 2.01) continue;
            if (distance < cost) {
                cost = distance;
                best = i;
                connector = link;
            }
        }
        if (best < 0) return null;
        List<RoutePlanner.Edge> joined = new ArrayList<>();
        joined.add(connector);
        joined.addAll(result.subList(best + 1, result.size()));
        return joined;
    }

    private void beginWalkEdge(RoutePlanner.Edge edge) {
        if (activeEdge == edge) return;
        activeEdge = edge;
        segment = 1;
        edgeTicks = 0;
        boss.terrainStep(
                0,
                (float) (edge.to().y() - edge.from().y()),
                Math.abs(edge.to().y() - edge.from().y()) > .05);
    }

    /**
     * Consume one tick's distance budget across compatible nodes without changing action ownership.
     */
    private void walkRoute() {
        if (route == null || index >= route.size()) {
            activeEdge = null;
            boss.setNoGravity(false);
            return;
        }
        var edge = route.get(index);
        if (edge.kind() != RoutePlanner.Kind.WALK) return;
        beginWalkEdge(edge);
        Vec3 heading = walkHeading(edge);
        double yaw =
                Math.toDegrees(Math.atan2(heading.z - boss.getZ(), heading.x - boss.getX())) - 90;
        float error = net.minecraft.util.Mth.wrapDegrees((float) yaw - boss.getYRot());
        if (Math.abs(error) > 70 && !terrainMotion()) {
            walkingSpeed = 0;
            boss.haltMotion();
            boss.facePoint(heading, 7);
            edgeTicks++;
            status = "沿路径连续转向";
            if (edgeTicks > 100) reject(edge, "路径转向超时，重新规划");
            return;
        }
        boss.facePoint(heading, 7);
        double top =
                Math.min(
                        .38,
                        boss.getAttributeValue(Attributes.MOVEMENT_SPEED) * walkMultiplier * .8);
        double wanted = top * Math.max(.3, Math.cos(Math.toRadians(error)));
        double remaining = boss.position().distanceTo(vec(route.get(route.size() - 1).to()));
        if (!routePartial) wanted *= Math.min(1, Math.max(.18, remaining / 1.6));
        walkingSpeed = net.minecraft.util.Mth.approach((float) walkingSpeed, (float) wanted, .035f);
        double budget = walkingSpeed;
        for (int nodes = 0; nodes < 4 && budget > .00001; nodes++) {
            edge = route.get(index);
            if (!support(edge.to())) {
                reject(edge, "落脚地面发生变化，重新规划");
                return;
            }
            beginWalkEdge(edge);
            boolean complete = moveEdge(edge, budget);
            updateStep(edge);
            budget = remainingStep;
            if (!complete) {
                progress(vec(edge.to()));
                if (noProgress >= 16 || edgeTicks > 100) reject(edge, "路径点无法继续接近");
                return;
            }
            index++;
            activeEdge = null;
            failures = 0;
            noProgress = 0;
            bestDistance = Double.MAX_VALUE;
            if (index >= route.size()) return;
            var next = route.get(index);
            if (next.kind() != RoutePlanner.Kind.WALK)
                return; // traversal/clearing keeps its own entry sequence
            Vec3 direction = walkHeading(next).subtract(boss.position());
            float nextYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
            if (Math.abs(net.minecraft.util.Mth.wrapDegrees(nextYaw - boss.getYRot())) > 20) return;
        }
    }

    private Vec3 walkHeading(RoutePlanner.Edge edge) {
        Vec3 direction = vec(edge.to()).subtract(boss.position());
        // At a descending edge's vertical leg, atan2(0,0) invents a new facing.
        // Preserve the tread direction until the feet have landed.
        if (direction.horizontalDistanceSqr() < .0001)
            direction = vec(edge.to()).subtract(vec(edge.from()));
        return boss.position().add(direction.x, 0, direction.z);
    }

    private void progress(Vec3 next) {
        if (next.distanceToSqr(progressPoint) > .01) {
            progressPoint = next;
            bestDistance = boss.position().distanceTo(next);
            noProgress = 0;
        }
        double d = boss.position().distanceTo(next);
        if (d < bestDistance - .035) {
            bestDistance = d;
            noProgress = 0;
        } else noProgress++;
    }

    private boolean moveEdge(RoutePlanner.Edge edge, double step) {
        remainingStep = 0;
        gravityTick = now();
        boss.haltMotion();
        boss.setNoGravity(true);
        boss.setDeltaMovement(Vec3.ZERO);
        edgeTicks++;
        List<RoutePlanner.Point> points = edge.points();
        for (int attempts = 0;
                attempts < 4 && segment < points.size() && step > .00001;
                attempts++) {
            Vec3 goal = vec(points.get(segment)), delta = goal.subtract(boss.position());
            double distance = delta.length();
            if (distance < .00001) {
                segment++;
                continue;
            }
            Vec3 movement = delta.scale(Math.min(step, distance) / distance);
            int samples = Math.max(1, (int) Math.ceil(movement.length() / .12));
            for (int i = 1; i <= samples; i++)
                if (!fits(point(boss.position().add(movement.scale(i / (double) samples))))) {
                    if (boss.debug() && edgeTicks == 2)
                        GeneratedMod.LOG.info(
                                "[nav blocked] from={} step={} edge={} box={}",
                                boss.position(),
                                movement,
                                edge,
                                box(point(boss.position().add(movement))));
                    return false;
                }
            Vec3 before = boss.position();
            boss.move(MoverType.SELF, movement);
            double travelled = before.distanceTo(boss.position());
            step -= travelled;
            if (edge.kind() == RoutePlanner.Kind.WALK) {
                if (walkTravelTick != now()) {
                    walkTravelTick = now();
                    walkTravelled = 0;
                }
                walkTravelled += travelled;
            }
            if (boss.position().distanceToSqr(before) < .000001) {
                if (boss.debug() && edgeTicks == 2)
                    GeneratedMod.LOG.info(
                            "[nav immobile] pos={} step={} entityCollision={}",
                            before,
                            movement,
                            boss.level().getEntities(boss, boss.getBoundingBox().inflate(.3)));
                return false;
            }
            if (boss.position().distanceTo(goal) < .00001) segment++;
        }
        if (segment >= points.size()) {
            remainingStep = Math.max(0, step);
            boss.setNoGravity(false);
            boss.setDeltaMovement(Vec3.ZERO);
            return true;
        }
        return false;
    }

    public void traversalTick() {
        if (activeEdge == null) {
            boss.endTraversal();
            return;
        }
        var edge = activeEdge;
        if (!support(edge.to())
                || !wall(
                        edge.from(),
                        Integer.signum((int) Math.round(edge.to().x() - edge.from().x())),
                        Integer.signum((int) Math.round(edge.to().z() - edge.from().z())),
                        edge.kind() == RoutePlanner.Kind.CLIMB
                                ? edge.to().y() - edge.from().y()
                                : 1)) {
            reject(edge, "墙面或落点已失效");
            boss.endTraversal();
            return;
        }
        boss.facePoint(vec(edge.to()), 5);
        // 起手先沉重心，收尾稳脚；位移与姿态共用实际路线进度。
        if (edgeTicks < 5) {
            boss.haltMotion();
            edgeTicks++;
            return;
        }
        boolean complete = moveEdge(edge, edge.kind() == RoutePlanner.Kind.CLIMB ? .12 : .18);
        List<RoutePlanner.Point> p = edge.points();
        double total = 0, remaining = 0;
        for (int i = 1; i < p.size(); i++) total += p.get(i - 1).distance(p.get(i));
        if (segment < p.size()) {
            remaining = boss.position().distanceTo(vec(p.get(segment)));
            for (int i = segment + 1; i < p.size(); i++)
                remaining += p.get(i - 1).distance(p.get(i));
        }
        boss.traversalProgress(
                (float) Math.max(0, Math.min(1, 1 - remaining / Math.max(.01, total))));
        progress(segment < p.size() ? vec(p.get(segment)) : vec(edge.to()));
        if (complete) {
            index++;
            activeEdge = null;
            nextTraversal = now() + BossConfig.TRAVERSE_COOLDOWN.get();
            boss.endTraversal();
            noProgress = 0;
        } else if (noProgress > 24 || edgeTicks > 180) {
            reject(edge, "机械移动遇到新碰撞");
            boss.endTraversal();
        }
    }

    private void reject(RoutePlanner.Edge edge, String reason) {
        failedEdges.put(edgeKey(edge), now() + BossConfig.FAILED_RETRY.get());
        route = null;
        routePartial = false;
        planner = null;
        backgroundPlan = false;
        activeEdge = null;
        corridor = Set.of();
        noProgress = 0;
        tier = Math.min(3, tier + 1);
        nextPlan = now() + 2;
        boss.setNoGravity(false);
        boss.haltMotion();
        status = reason;
        if (++failures >= 4) {
            failed = true;
            rememberGoal();
            failures = 0;
        }
    }

    private String edgeKey(RoutePlanner.Edge e) {
        return BlockPos.containing(vec(e.from())) + "/" + BlockPos.containing(vec(e.to()));
    }

    @Override
    public boolean banned(RoutePlanner.Edge e) {
        return failedEdges.getOrDefault(edgeKey(e), 0L) > now();
    }

    private AABB box(RoutePlanner.Point p) {
        double r = boss.getBbWidth() / 2.0;
        return new AABB(
                p.x() - r + .001,
                p.y() + .002,
                p.z() - r + .001,
                p.x() + r - .001,
                p.y() + boss.getBbHeight() - .002,
                p.z() + r - .001);
    }

    private boolean loaded(AABB box) {
        return boss.level().hasChunkAt(BlockPos.containing(box.minX, box.minY, box.minZ))
                && boss.level().hasChunkAt(BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                && boss.level().getWorldBorder().isWithinBounds(box);
    }

    @Override
    public boolean fits(RoutePlanner.Point p) {
        AABB box = box(p);
        if (!loaded(box)) return false;
        for (var shape : boss.level().getBlockCollisions(boss, box))
            if (!shape.isEmpty()) return false;
        for (BlockPos pos :
                BlockPos.betweenClosed(
                        BlockPos.containing(box.minX, box.minY, box.minZ),
                        BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var state = boss.level().getBlockState(pos);
            if (net.minecraft.world.level.pathfinder.WalkNodeEvaluator.isBurningBlock(state))
                return false;
            var fluid = state.getFluidState();
            if (!fluid.isEmpty()
                    && (!fluid.is(net.minecraft.tags.FluidTags.WATER)
                            || pos.getY() + fluid.getHeight(boss.level(), pos) > p.y() + 1.05))
                return false;
        }
        BlockPos feet = BlockPos.containing(p.x(), p.y() + .02, p.z());
        if (boss.level().getFluidState(feet).is(net.minecraft.tags.FluidTags.WATER)) {
            Vec3 from = new Vec3(p.x(), p.y() + .03, p.z());
            var bed =
                    boss.level()
                            .clip(
                                    new ClipContext(
                                            from,
                                            from.add(0, -1.08, 0),
                                            ClipContext.Block.COLLIDER,
                                            ClipContext.Fluid.NONE,
                                            boss));
            if (bed.getType() == HitResult.Type.MISS
                    || bed.getDirection() != net.minecraft.core.Direction.UP) return false;
        }
        return true;
    }

    @Override
    public RoutePlanner.Point floor(double x, double z, double y, double up, double down) {
        double r = boss.getBbWidth() / 2 - .002;
        AABB probe = new AABB(x - r, y - down - .01, z - r, x + r, y + up + .01, z + r);
        if (!loaded(probe)) return null;
        var heights = new TreeSet<Double>(Comparator.reverseOrder());
        for (var shape : boss.level().getBlockCollisions(boss, probe))
            for (var bounds : shape.toAabbs()) {
                double top = bounds.maxY;
                if (top >= y - down - .001 && top <= y + up + .001) heights.add(top);
            }
        for (double top : heights) {
            var p = new RoutePlanner.Point(x, top, z);
            if (fits(p) && support(p)) return p;
        }
        return null;
    }

    @Override
    public boolean support(RoutePlanner.Point p) {
        if (!loaded(box(p))) return false;
        AABB body = box(p),
                feet =
                        new AABB(
                                body.minX,
                                p.y() - .08,
                                body.minZ,
                                body.maxX,
                                p.y() + .001,
                                body.maxZ);
        // Use the same solid shapes that stop the entity falling. Requiring several
        // probes at exactly the same Y rejects most uneven snow and hillside edges.
        double area = 0;
        for (var shape : boss.level().getBlockCollisions(boss, feet))
            for (var part : shape.toAabbs()) {
                if (Math.abs(part.maxY - p.y()) > .08) continue;
                double x0 = Math.max(body.minX, part.minX), x1 = Math.min(body.maxX, part.maxX);
                double z0 = Math.max(body.minZ, part.minZ), z1 = Math.min(body.maxZ, part.maxZ);
                if (x1 <= x0 || z1 <= z0) continue;
                BlockPos pos = BlockPos.containing((x0 + x1) * .5, part.maxY - .01, (z0 + z1) * .5);
                var state = boss.level().getBlockState(pos);
                if (net.minecraft.world.level.pathfinder.WalkNodeEvaluator.isBurningBlock(state)
                        || state.is(net.minecraft.world.level.block.Blocks.CACTUS)
                        || state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK))
                    return false;
                area += (x1 - x0) * (z1 - z0);
            }
        return area >= .06;
    }

    @Override
    public boolean wall(RoutePlanner.Point p, int dx, int dz, double height) {
        if (dx == 0 && dz == 0) return false;
        for (double h = .5; h <= Math.max(.5, height - .35); h += .65) {
            Vec3 a = new Vec3(p.x(), p.y() + h, p.z()),
                    b =
                            a.add(
                                    dx * (boss.getBbWidth() / 2 + 1.1),
                                    0,
                                    dz * (boss.getBbWidth() / 2 + 1.1));
            if (boss.level()
                            .clip(
                                    new ClipContext(
                                            a,
                                            b,
                                            ClipContext.Block.COLLIDER,
                                            ClipContext.Fluid.NONE,
                                            boss))
                            .getType()
                    == HitResult.Type.MISS) return false;
        }
        return true;
    }

    @Override
    public boolean clearable(RoutePlanner.Point p, double supportY) {
        AABB box = box(p);
        if (!loaded(box)) return false;
        for (BlockPos pos :
                BlockPos.betweenClosed(
                        BlockPos.containing(box.minX, box.minY, box.minZ),
                        BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var state = boss.level().getBlockState(pos);
            if (state.isAir()) continue;
            if (!state.getFluidState().isEmpty()) return false;
            if (state.getCollisionShape(boss.level(), pos).isEmpty()) continue;
            if (pos.getY() < Math.floor(supportY + .01)
                    || deniedBlocks.getOrDefault(pos, 0L) > now()
                    || !BladeCombat.breakable(boss, pos, false)) return false;
        }
        return true;
    }

    @Override
    public boolean goal(RoutePlanner.Point p, RoutePlanner.Point target) {
        double horizontal = Math.hypot(p.x() - target.x(), p.z() - target.z());
        if (waypointGoal) return horizontal <= .8 && Math.abs(p.y() - target.y()) <= .55;
        var enemy = boss.getTarget();
        double height = enemy == null ? 1.8 : enemy.getBbHeight(),
                width = enemy == null ? .6 : enemy.getBbWidth();
        String clip = boss.phase() == 2 ? SamuraiRig.COMBO_TWO : SamuraiRig.A;
        var profile =
                CombatSpacing.profile(
                        boss.phase(), clip, 0, width, boss.range(0), BossConfig.DASH.get());
        if (enemy != null && !Targeting.visible(boss, enemy)) {
            if (horizontal > 1.25 || Math.abs(p.y() - target.y()) > 1.25) return false;
            Vec3 end = vec(target).add(0, height * .55, 0);
            return boss.level()
                            .clip(
                                    new ClipContext(
                                            vec(p).add(0, 1.15, 0),
                                            end,
                                            ClipContext.Block.COLLIDER,
                                            ClipContext.Fluid.NONE,
                                            boss))
                            .getType()
                    == HitResult.Type.MISS;
        }
        double extra = enemy == null ? 0 : TargetGeometry.extraRadius(enemy);
        boolean large = enemy != null && TargetGeometry.large(enemy);
        if (horizontal > Math.min(3.6 + extra, profile.maximum())
                || horizontal < profile.minimum()
                || !AttackLane.heightAllowed(enemy, p.y(), target.y(), AttackLane.NAVIGATION_DROP))
            return false;
        if (!AttackLane.clear(
                enemy,
                vec(p),
                vec(target),
                height,
                boss.getEyeHeight(),
                (from, end) ->
                        boss.level()
                                        .clip(
                                                new ClipContext(
                                                        from,
                                                        end,
                                                        ClipContext.Block.COLLIDER,
                                                        ClipContext.Fluid.NONE,
                                                        boss))
                                        .getType()
                                == HitResult.Type.MISS)) return false;
        return BladeAim.solve(boss.phase(), clip, 0, horizontal, target.y() - p.y(), height, width)
                .contact();
    }

    public boolean canClear() {
        return allowClearing
                && boss.redReady()
                && BossConfig.CLEAR.get()
                && BossConfig.TERRAIN.get()
                && !BossConfig.DENY_UNCERTAIN.get()
                && (!BossConfig.GRIEFING.get()
                        || net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(
                                boss.level(), boss));
    }

    private boolean beginClear(RoutePlanner.Edge edge) {
        if (!canClear()) return false;
        activeEdge = edge;
        clearStart = now();
        Set<BlockPos> positions = new LinkedHashSet<>();
        int samples = (int) Math.ceil(edge.from().distance(edge.to()) / .25);
        for (int i = 1; i <= samples; i++) {
            AABB b = box(edge.from().mix(edge.to(), i / (double) samples));
            for (BlockPos p :
                    BlockPos.betweenClosed(
                            BlockPos.containing(b.minX, b.minY, b.minZ),
                            BlockPos.containing(b.maxX, b.maxY, b.maxZ)))
                if (p.getY() >= Math.floor(edge.from().y() + .01)) positions.add(p.immutable());
        }
        corridor = positions;
        boss.beginClearing();
        status = "红刀逐段清障";
        return true;
    }

    public boolean clearAllowed(BlockPos pos) {
        return corridor.contains(pos)
                && activeEdge != null
                && pos.getY() >= Math.floor(activeEdge.from().y() + .01);
    }

    public void denied(BlockPos pos) {
        deniedBlocks.put(pos.immutable(), now() + BossConfig.FAILED_RETRY.get());
    }

    public BlockPos nextObstruction() {
        return corridor.stream()
                .filter(
                        p ->
                                !boss.level()
                                        .getBlockState(p)
                                        .getCollisionShape(boss.level(), p)
                                        .isEmpty())
                .min(Comparator.comparingDouble(p -> p.distToCenterSqr(boss.position())))
                .orElse(null);
    }

    public boolean clearingTick() {
        if (activeEdge == null) return false;
        if (!canClear()
                || now() - clearStart > BossConfig.CLEAR_TIMEOUT.get()
                || !support(activeEdge.to())) {
            reject(activeEdge, "清障关闭、超时或落点失效");
            return false;
        }
        BlockPos next = nextObstruction();
        if (next == null) {
            route = null;
            planner = null;
            activeEdge = null;
            corridor = Set.of();
            tier = 0;
            nextPlan = now();
            status = "通道已清空，停止破坏";
            return false;
        }
        if (deniedBlocks.getOrDefault(next, 0L) > now()
                || !BladeCombat.breakable(boss, next, true)) {
            denied(next);
            reject(activeEdge, "保护方块，暂避失败路线");
            return false;
        }
        return true;
    }

    public void clearMiss() {
        if (activeEdge != null) reject(activeEdge, "刀路无法接触，换路线");
    }

    public void obstacleChanged(BlockPos pos) {
        deniedBlocks.remove(pos);
        if (pos.distToCenterSqr(boss.position()) < 64) {
            failedEdges.clear();
            failedGoals.clear();
            approaches.clear();
            pursuitProgress.reset();
            nextPlan = 0;
        }
    }
}
