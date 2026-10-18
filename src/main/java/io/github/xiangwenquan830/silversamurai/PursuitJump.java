package io.github.xiangwenquan830.silversamurai;

import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/** Short obstacle/gap jump. Real gravity and collision own flight, never a teleport. */
final class PursuitJump {
    static final String PREP = "跳跃_压身蹬地", AIR = "跳跃_腾空收腿", LAND = "跳跃_落地缓冲";

    private enum Stage {
        PREP,
        AIR,
        LAND
    }

    private record Plan(Vec3 landing, Vec3 velocity, int ticks) {}

    private final SamuraiEntity boss;
    private Stage stage;
    private Plan plan;
    private long nextTry, started;
    private Vec3 origin;
    private int airTicks, probeBudget;
    private static final java.util.Map<net.minecraft.world.level.Level, long[]> BUDGET =
            new java.util.WeakHashMap<>();

    private boolean scanSlot() {
        long[] b = BUDGET.computeIfAbsent(boss.level(), k -> new long[] {-1, 0});
        if (b[0] != now()) {
            b[0] = now();
            b[1] = 0;
        }
        return b[1]++ < 2;
    }

    PursuitJump(SamuraiEntity boss) {
        this.boss = boss;
    }

    boolean active() {
        return stage != null;
    }

    boolean airborne() {
        return stage == Stage.AIR;
    }

    String status() {
        return stage == null ? "无" : stage.name();
    }

    static boolean clip(String s) {
        return s.equals(PREP) || s.equals(AIR) || s.equals(LAND) || s.equals("跳跃_完整审阅");
    }

    private long now() {
        return boss.level().getGameTime();
    }

    boolean tryStart() {
        if (active()
                || now() < nextTry
                || !boss.onGround()
                || !boss.weaponsDrawn()
                || !ActionRules.canBeginAttack(boss.state())
                || !boss.targetValid(boss.getTarget())) return false;
        nextTry = now() + 10;
        Vec3 target = boss.getTarget().position();
        double horizontal = target.subtract(boss.position()).horizontalDistance();
        if (horizontal < 3.5
                || horizontal > 18
                || target.y < boss.getY() - 2
                || target.y > boss.getY() + (boss.wallApproach().needsRoofJump() ? 6 : 3)
                || boss.terrainNavigation().directWalkable(target)) return false;
        if (!scanSlot()) return false;
        probeBudget = 640;
        Plan candidate = find(target);
        if (candidate == null) return false;
        float yaw =
                (float)
                                Math.toDegrees(
                                        Math.atan2(
                                                candidate.landing.z - boss.getZ(),
                                                candidate.landing.x - boss.getX()))
                        - 90;
        boss.facePoint(candidate.landing, 18);
        if (Math.abs(Mth.wrapDegrees(yaw - boss.getYRot())) > 15) return false;
        plan = candidate;
        origin = boss.position();
        started = now();
        airTicks = 0;
        boss.terrainNavigation().cancel();
        enter(Stage.PREP, PREP);
        return true;
    }

    private Plan find(Vec3 target) {
        Vec3 dir = target.subtract(boss.position()).multiply(1, 0, 1).normalize();
        for (int angle : new int[] {0, 20, -20})
            for (double distance : new double[] {6, 6.5, 7, 7.5, 8, 5.5, 5, 4.5, 4, 3.5, 3}) {
                Vec3 at =
                        boss.position()
                                .add(dir.yRot((float) Math.toRadians(angle)).scale(distance));
                var floor =
                        boss.terrainNavigation()
                                .floor(
                                        at.x,
                                        at.z,
                                        boss.getY(),
                                        boss.wallApproach().needsRoofJump() ? 6 : 2.5,
                                        1.5);
                if (floor == null) continue;
                Vec3 landing = SamuraiNavigation.vec(floor);
                if (landing.y < target.y - 1.5
                        || landing.distanceTo(target) >= boss.position().distanceTo(target) - 1.5)
                    continue;
                // Do not replace a perfectly walkable slope with repeated hopping.
                if (groundClear(landing)) continue;
                for (int ticks : new int[] {20, 24, 28}) {
                    double sum = (1 - Math.pow(.98, ticks)) / .02;
                    double vy = (landing.y - boss.getY() + 3.92 * (ticks - sum)) / sum;
                    if (vy < .42 || vy > (boss.wallApproach().needsRoofJump() ? 1.18 : .92))
                        continue;
                    Vec3 v =
                            new Vec3(
                                    (landing.x - boss.getX()) / ticks,
                                    vy,
                                    (landing.z - boss.getZ()) / ticks);
                    Plan p = new Plan(landing, v, ticks);
                    if (clearArc(boss.position(), p)) return p;
                }
            }
        return null;
    }

    private boolean groundClear(Vec3 landing) {
        int n = Math.max(1, (int) Math.ceil(landing.distanceTo(boss.position()) / .25));
        for (int i = 1; i <= n; i++) {
            var at = SamuraiNavigation.point(boss.position().lerp(landing, i / (double) n));
            if (!boss.terrainNavigation().fits(at) || !boss.terrainNavigation().support(at))
                return false;
        }
        return true;
    }

    private boolean safeLanding(Vec3 p) {
        return boss.terrainNavigation().fits(SamuraiNavigation.point(p))
                && boss.terrainNavigation().support(SamuraiNavigation.point(p));
    }

    private boolean clearArc(Vec3 from, Plan p) {
        if (!safeLanding(p.landing)) return false;
        Vec3 at = from;
        double vy = p.velocity.y;
        for (int tick = 1; tick <= p.ticks; tick++) {
            Vec3 to = at.add(p.velocity.x, vy, p.velocity.z);
            int n = Math.max(1, (int) Math.ceil(to.distanceTo(at) / .1));
            for (int i = 1; i <= n; i++)
                if (--probeBudget < 0
                        || !boss.terrainNavigation()
                                .fits(
                                        SamuraiNavigation.point(
                                                at.lerp(to, i / (double) n).add(0, .015, 0))))
                    return false;
            at = to;
            vy = (vy - .08) * .98;
        }
        return at.distanceTo(p.landing) < .04;
    }

    private void enter(Stage s, String clip) {
        stage = s;
        boss.wallEnter(clip);
        boss.wallDrawn(true);
        boss.setNoGravity(false);
    }

    void tick() {
        if (stage == null) return;
        boss.setNoGravity(false);
        if (stage == Stage.PREP) {
            boss.jumpVelocity(new Vec3(0, Math.min(0, boss.getDeltaMovement().y), 0));
            if (!boss.onGround()
                    || !boss.targetValid(boss.getTarget())
                    || !safeLanding(plan.landing)
                    || plan.landing.y < boss.getTarget().getY() - 1.5
                    || plan.landing.distanceTo(boss.getTarget().position())
                            >= boss.position().distanceTo(boss.getTarget().position()) - 1
                    || boss.position().distanceTo(origin) > .35) {
                finish();
                return;
            }
            boss.facePoint(plan.landing, 18);
            if (boss.stateAge(0) < .32f) return;
            probeBudget = 640;
            if (!clearArc(boss.position(), plan)) {
                finish();
                return;
            }
            enter(Stage.AIR, AIR);
            airTicks = 0;
            boss.setOnGround(false);
            boss.jumpVelocity(plan.velocity);
            boss.hasImpulse = true;
            return;
        }
        if (stage == Stage.AIR) {
            airTicks++;
            if (boss.onGround() && airTicks > 1) {
                enter(Stage.LAND, LAND);
                boss.jumpVelocity(Vec3.ZERO);
                return;
            }
            Vec3 v = boss.getDeltaMovement();
            // Keep the checked horizontal launch speed; leave vertical velocity to real gravity.
            if (airTicks <= plan.ticks && safeLanding(plan.landing) && !boss.horizontalCollision)
                boss.jumpVelocity(new Vec3(plan.velocity.x, v.y, plan.velocity.z));
            else boss.jumpVelocity(new Vec3(0, v.y, 0));
            if (now() - started > 160) {
                finish();
                return;
            }
            return;
        }
        boss.jumpVelocity(new Vec3(0, Math.min(0, boss.getDeltaMovement().y), 0));
        if (!boss.onGround() || boss.stateAge(0) >= .43f) finish();
    }

    private void finish() {
        cancel();
        nextTry = now() + 40;
        boss.wallFinish();
    }

    void cancel() {
        stage = null;
        plan = null;
        boss.setNoGravity(false);
    }

    void save(CompoundTag tag) {
        tag.remove("SamuraiJump");
        if (!active()) return;
        CompoundTag j = new CompoundTag();
        j.putString("Stage", stage.name());
        j.putFloat("Time", boss.stateAge(0));
        j.putInt("AirTicks", airTicks);
        j.putInt("Ticks", plan.ticks);
        put(j, "Motion", boss.getDeltaMovement());
        put(j, "Landing", plan.landing);
        put(j, "Velocity", plan.velocity);
        put(j, "Origin", origin);
        tag.put("SamuraiJump", j);
    }

    void load(CompoundTag tag) {
        cancel();
        if (!tag.contains("SamuraiJump", 10) || !boss.isAlive()) return;
        try {
            var j = tag.getCompound("SamuraiJump");
            Stage s = Stage.valueOf(j.getString("Stage"));
            Vec3 landing = get(j, "Landing"),
                    velocity = get(j, "Velocity"),
                    o = get(j, "Origin"),
                    motion = get(j, "Motion");
            int ticks = j.getInt("Ticks");
            float time = j.getFloat("Time");
            if (motion.horizontalDistance() > .41
                    || motion.y > 1.18
                    || motion.y < -4
                    || ticks < 20
                    || ticks > 28
                    || velocity.horizontalDistance() > .41
                    || velocity.y < .42
                    || velocity.y > 1.18
                    || o.distanceTo(boss.position()) > 20
                    || landing.distanceTo(o) > 10
                    || !Float.isFinite(time)) throw new IllegalArgumentException();
            if (s == Stage.PREP && !safeLanding(landing)) throw new IllegalArgumentException();
            plan = new Plan(landing, velocity, ticks);
            origin = o;
            airTicks = Mth.clamp(j.getInt("AirTicks"), 0, 160);
            started = now() - airTicks;
            enter(s, s == Stage.PREP ? PREP : s == Stage.AIR ? AIR : LAND);
            boss.wallResume(Mth.clamp(time, 0, s == Stage.AIR ? 8 : s == Stage.PREP ? .32f : .43f));
            if (s == Stage.AIR) boss.jumpVelocity(motion);
        } catch (RuntimeException invalid) {
            cancel();
            boss.wallFinish();
        }
    }

    private static void put(CompoundTag t, String k, Vec3 v) {
        t.putDouble(k + "X", v.x);
        t.putDouble(k + "Y", v.y);
        t.putDouble(k + "Z", v.z);
    }

    private static Vec3 get(CompoundTag t, String k) {
        for (String a : new String[] {"X", "Y", "Z"})
            if (!t.contains(k + a, 99) || !Double.isFinite(t.getDouble(k + a)))
                throw new IllegalArgumentException();
        return new Vec3(t.getDouble(k + "X"), t.getDouble(k + "Y"), t.getDouble(k + "Z"));
    }
}
