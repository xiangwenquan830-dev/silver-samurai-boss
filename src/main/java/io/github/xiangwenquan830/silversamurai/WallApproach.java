package io.github.xiangwenquan830.silversamurai;

import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Ground approach to a validated wall entry, distinct from an enemy attack-distance goal. */
final class WallApproach {
    private record Entry(WallGeometry.Contact wall, Vec3 ground, Vec3 roof, double score) {}

    private record Rejected(Vec3 at, Vec3 forward, long until) {}

    private static boolean sameEntry(Rejected r, WallGeometry.Contact wall) {
        return r.forward.dot(wall.forward()) > .9
                && Math.abs(r.at.y - wall.face().y) < 4
                && r.at.subtract(wall.face()).horizontalDistanceSqr() < 2.56;
    }

    private record FaceKey(double x, double z, Vec3 forward) {}

    private static final Map<Level, long[]> SCANS = new WeakHashMap<>();
    private final SamuraiEntity boss;
    private final List<Rejected> rejected = new ArrayList<>();
    private Entry entry;
    private boolean routeEntry, searchedNoEntry;
    private UUID target;
    private Vec3 targetPoint = Vec3.ZERO, progress = Vec3.ZERO;
    private long nextScan, started, lastProgress, nextTrace, searchProgressTick, nextExpanded;
    private Vec3 searchOrigin = Vec3.ZERO;
    private String lastTrace = "";
    private String reason = "无上墙路线";

    WallApproach(SamuraiEntity boss) {
        this.boss = boss;
    }

    boolean active() {
        return entry != null;
    }

    boolean needsRoofJump() {
        return entry == null
                && searchedNoEntry
                && now() - searchProgressTick >= 40
                && boss.position().distanceToSqr(searchOrigin) <= .25;
    }

    String status() {
        return reason;
    }

    private long now() {
        return boss.level().getGameTime();
    }

    void clear() {
        if (entry != null) boss.terrainNavigation().cancel();
        entry = null;
        target = null;
        routeEntry = false;
        searchedNoEntry = false;
        reason = "无上墙路线";
    }

    private boolean budget() {
        long[] b = SCANS.computeIfAbsent(boss.level(), l -> new long[] {-1, 0});
        if (b[0] != now()) {
            b[0] = now();
            b[1] = 0;
        }
        return b[1]++ < 2;
    }

    private void trace(String result, Vec3 goal) {
        if (now() < nextTrace || result.equals(lastTrace)) return;
        nextTrace = now() + 100;
        lastTrace = result;
        GeneratedMod.LOG.info(
                "[WallRoute] id={} phase={} pos={} goal={} state={} result={}",
                boss.getId(),
                boss.phase(),
                boss.position(),
                goal,
                boss.state(),
                result);
    }

    private List<Entry> search(Vec3 goal, boolean expanded) {
        List<Entry> found = new ArrayList<>();
        Set<FaceKey> checked = new HashSet<>();
        Vec3 delta = goal.subtract(boss.position()).multiply(1, 0, 1),
                side = new Vec3(delta.z, 0, -delta.x).normalize();
        if (boss.terrainNavigation().directWalkable(goal)) return found;
        if (delta.lengthSqr() < .01) side = new Vec3(1, 0, 0);
        // Fan toward the confirmed target, then check the actual hit face normal (including
        // diagonal approaches).
        for (double offset : new double[] {0, -3, 3, -6, 6, -10, 10})
            for (double probe : new double[] {3.2, 2.2, 1.2}) {
                Vec3 from = boss.position().add(0, probe, 0),
                        to = new Vec3(goal.x, from.y, goal.z).add(side.scale(offset));
                Vec3 ray = to.subtract(from);
                if (ray.length() > 48) to = from.add(ray.normalize().scale(48));
                var hit = WallGeometry.ray(boss, from, to);
                consider(found, checked, goal, hit);
            }
        // Sloped eaves can sit between the old fixed-height rays. Probe near the
        // confirmed roof level, including the lower steps on either side.
        if (found.isEmpty() && goal.y > boss.getY() + 2.5 && delta.lengthSqr() <= 24 * 24) {
            for (double offset : new double[] {0, -1.5, 1.5, -3, 3})
                for (double below : new double[] {.25, 1.25, 2.25}) {
                    double y = goal.y - below;
                    if (y < boss.getY() + 1.2) continue;
                    Vec3 from = new Vec3(boss.getX(), y, boss.getZ());
                    Vec3 to = new Vec3(goal.x, y, goal.z).add(side.scale(offset));
                    consider(found, checked, goal, WallGeometry.ray(boss, from, to));
                }
        }
        // A narrow column or a facade projection may block the face seen from below.
        // Check the other sides near the already-confirmed target, then walk to a valid entry.
        if (delta.lengthSqr() <= 12 * 12 && goal.y > boss.getY() + 2.5) {
            Vec3 centre = new Vec3(Math.floor(goal.x) + .5, boss.getY(), Math.floor(goal.z) + .5);
            for (Direction face : Direction.Plane.HORIZONTAL)
                for (double height : new double[] {3.2, 6.2}) {
                    Vec3 outward = Vec3.atLowerCornerOf(face.getNormal()),
                            end = centre.add(0, height, 0);
                    consider(
                            found,
                            checked,
                            goal,
                            WallGeometry.ray(boss, end.add(outward.scale(4)), end));
                }
        }
        if (found.isEmpty() && expanded) {
            // Parallel probes do not converge back on the same projecting pillar.
            for (double offset : new double[] {-3, 3, -6, 6, -9, 9})
                for (double height : new double[] {3.2, 4.2, 1.2, 6.2}) {
                    Vec3 from = boss.position().add(side.scale(offset)).add(0, height, 0);
                    Vec3 to = new Vec3(goal.x, from.y, goal.z).add(side.scale(offset));
                    Vec3 ray = to.subtract(from);
                    if (ray.length() > 32) to = from.add(ray.normalize().scale(32));
                    consider(found, checked, goal, WallGeometry.ray(boss, from, to));
                }
            // Probe from OUTSIDE a nearby crown/overhang. A trunk ray from underneath
            // cannot discover a safe leaf edge or the far side of a projecting ledge.
            if (delta.lengthSqr() <= 24 * 24) {
                Vec3 centre =
                        new Vec3(Math.floor(goal.x) + .5, boss.getY(), Math.floor(goal.z) + .5);
                for (double radius : new double[] {6, 12})
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        Vec3 out = Vec3.atLowerCornerOf(direction.getNormal()),
                                tangent = new Vec3(out.z, 0, -out.x);
                        for (double offset : new double[] {0, -3, 3, -6, 6})
                            for (double height : new double[] {3.2, 4.2, 1.2, 6.2}) {
                                Vec3 to = centre.add(tangent.scale(offset)).add(0, height, 0);
                                consider(
                                        found,
                                        checked,
                                        goal,
                                        WallGeometry.ray(boss, to.add(out.scale(radius)), to));
                            }
                    }
            }
        }
        found.sort(Comparator.comparingDouble(Entry::score));
        return found;
    }

    private void consider(
            List<Entry> found,
            Set<FaceKey> checked,
            Vec3 goal,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (found.size() >= 24 || hit == null || hit.getDirection().getAxis() == Direction.Axis.Y)
            return;
        Vec3 forward = Vec3.atLowerCornerOf(hit.getDirection().getOpposite().getNormal());
        Vec3 face = hit.getLocation();
        // Aim at the middle of the actual face, including one-block-wide columns.
        face =
                forward.x == 0
                        ? new Vec3(Math.floor(face.x) + .5, face.y, face.z)
                        : new Vec3(face.x, face.y, Math.floor(face.z) + .5);
        if (!checked.add(new FaceKey(face.x, face.z, forward))) return;
        var wall = new WallGeometry.Contact(face, forward);
        if (blocked(wall)) return;
        Vec3 ground = wall.face().subtract(forward.scale(boss.weaponsDrawn() ? 3.4 : 2.1));
        // The centre ray alone may land below the uphill side of this wide body.
        var standing = boss.terrainNavigation().floor(ground.x, ground.z, boss.getY(), 3.1, 5.1);
        if (standing == null) return;
        ground = SamuraiNavigation.vec(standing);
        if (!WallGeometry.fits(boss, ground)
                || !boss.terrainNavigation().support(SamuraiNavigation.point(ground))
                || !WallGeometry.grips(boss, wall, ground.y)) return;
        Vec3 point = ground;
        if (boss.terrainNavigation().failedDestination(ground)
                || found.stream().anyMatch(e -> e.ground.distanceToSqr(point) < 4)) return;
        Vec3 roof = WallGeometry.roof(boss, wall, ground.y);
        if (roof == null
                || roof.y > Math.max(goal.y + 3, boss.getY() + 6)
                || roof.y < ground.y + 2.5) return;
        found.add(
                new Entry(
                        wall,
                        ground,
                        roof,
                        boss.position().distanceTo(ground) + roof.distanceTo(goal) * .6));
    }

    boolean acceptRoute(Vec3 destination) {
        if (!BossConfig.WALL_CLIMB.get() || boss.getTarget() == null) return false;
        var wall = WallGeometry.find(boss, destination, 5);
        if (wall == null || blocked(wall)) return false;
        Vec3 ground =
                new Vec3(wall.face().x, boss.getY(), wall.face().z)
                        .subtract(wall.forward().scale(boss.weaponsDrawn() ? 3.4 : 2.1));
        var standing = boss.terrainNavigation().floor(ground.x, ground.z, boss.getY(), 2.1, 3.1);
        if (standing == null) return false;
        ground = SamuraiNavigation.vec(standing);
        if (!WallGeometry.fits(boss, ground)
                || !boss.terrainNavigation().support(SamuraiNavigation.point(ground))
                || destination.y < ground.y + 2.5
                || destination.y > ground.y + 6) return false;
        if (!WallGeometry.fits(boss, destination)
                || !boss.terrainNavigation().support(SamuraiNavigation.point(destination)))
            return false;
        if (!WallGeometry.routeClear(boss, wall, ground.y, destination)) return false;
        entry = new Entry(wall, ground, destination, 0);
        routeEntry = true;
        target = boss.getTarget().getUUID();
        targetPoint = boss.getTarget().position();
        started = lastProgress = now();
        progress = boss.position();
        boss.terrainNavigation().cancel();
        reason = "短墙路线接入抓墙翻顶";
        return true;
    }

    boolean blocked(WallGeometry.Contact wall) {
        return rejected.stream().anyMatch(r -> r.until > now() && sameEntry(r, wall));
    }

    void failed(WallGeometry.Contact wall) {
        if (wall == null) return;
        rejected.removeIf(r -> r.until <= now() || sameEntry(r, wall));
        if (rejected.size() >= 12) rejected.remove(0);
        rejected.add(new Rejected(wall.face(), wall.forward(), now() + 160));
        nextScan = now() + 4;
    }

    private void reject() {
        if (entry != null) failed(entry.wall);
        clear();
        nextScan = now() + 4;
        reason = "上墙点失效，改找入口";
    }

    boolean tick(LivingEntity enemy, Vec3 confirmed, boolean seen, int unseen) {
        boolean elevated = confirmed.y >= boss.getY() + 2.5;
        if (elevated
                && seen
                && enemy != null
                && enemy.position().subtract(boss.position()).horizontalDistanceSqr() < 49
                && CombatSpacing.assess(
                                boss,
                                enemy,
                                boss.phase() == 2 ? SamuraiRig.COMBO_TWO : SamuraiRig.A,
                                0,
                                false)
                        .attackable()) {
            clear();
            return false;
        }
        boolean blocked = !seen && unseen > 8 && unseen < BossConfig.memoryTicks(enemy);
        if (!BossConfig.WALL_CLIMB.get()
                || enemy == null
                || !routeEntry && !elevated && (entry == null ? !blocked : seen)) {
            clear();
            return false;
        }
        if (!ActionRules.canBeginAttack(boss.state())) return false;
        if (entry != null
                && (!enemy.getUUID().equals(target) || confirmed.distanceToSqr(targetPoint) > 64)) {
            clear();
            nextScan = 0;
        }
        rejected.removeIf(r -> r.until <= now());
        if (entry == null) {
            if (boss.position().distanceToSqr(searchOrigin) > .25) {
                searchOrigin = boss.position();
                searchProgressTick = now();
            }
            boolean expanded =
                    now() >= nextExpanded
                            && (boss.terrainNavigation().failed()
                                    || now() - searchProgressTick >= 40);
            if (now() < nextScan && !expanded
                    || !seen && unseen >= BossConfig.memoryTicks(enemy)
                    || !budget()) return false;
            nextScan = now() + 30;
            if (expanded) nextExpanded = now() + 30;
            var options = search(confirmed, expanded);
            searchedNoEntry = options.isEmpty();
            if (options.isEmpty()) {
                reason = "未找到有净空与落脚点的墙面";
                trace(reason, confirmed);
                return false;
            }
            entry = options.get(0);
            target = enemy.getUUID();
            targetPoint = confirmed;
            started = lastProgress = now();
            progress = boss.position();
            boss.terrainNavigation().cancel();
            reason = "接近上墙点";
            trace(reason, entry.ground);
        }
        if (boss.position().distanceToSqr(progress) > .25) {
            progress = boss.position();
            lastProgress = now();
        }
        if (now() - started > 600
                || now() - lastProgress > 100
                || boss.terrainNavigation().failed()) {
            reject();
            return false;
        }
        if (now() % 10 == 0
                && (!WallGeometry.grips(boss, entry.wall, entry.ground.y)
                        || !WallGeometry.fits(boss, entry.ground)
                        || !boss.terrainNavigation()
                                .support(SamuraiNavigation.point(entry.ground)))) {
            reject();
            return false;
        }
        Vec3 difference = entry.ground.subtract(boss.position());
        if (difference.horizontalDistanceSqr() <= .0001 && Math.abs(difference.y) < .12) {
            // Face every tick while stopped; ordinary pursuit no longer rotates against this turn.
            boss.haltMotion();
            Vec3 f = entry.wall.forward();
            boss.facePoint(boss.position().add(f), 18);
            float yaw = (float) Math.toDegrees(Math.atan2(f.z, f.x)) - 90;
            reason = "对正墙面，准备抓墙";
            if (Math.abs(Mth.wrapDegrees(yaw - boss.getYRot())) <= 8
                    && boss.wallTraversal().ready()) {
                var local =
                        new WallGeometry.Contact(
                                boss.position()
                                        .add(
                                                f.scale(
                                                        entry.wall
                                                                .face()
                                                                .subtract(boss.position())
                                                                .dot(f))),
                                f);
                if (!WallGeometry.grips(boss, local, boss.getY())
                        || !WallGeometry.fits(boss, local.stand(boss.getY()))
                        || !WallGeometry.routeClear(boss, local, boss.getY(), entry.roof)) {
                    reject();
                    return false;
                }
                if (boss.wallTraversal().startAt(local, entry.roof)) {
                    entry = null;
                    target = null;
                    reason = "已接入攀爬";
                    trace(reason, local.face());
                    boss.wallTraversal().tick();
                }
            }
            return true;
        }
        boss.wallApproachWalk(entry.ground);
        return true;
    }
}
