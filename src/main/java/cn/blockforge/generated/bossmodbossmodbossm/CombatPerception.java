package cn.blockforge.generated.bossmodbossmodbossm;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Exact-pose, single-tick observations shared by selection, preparation and blade sweeps. */
final class CombatPerception {
    private final SamuraiEntity boss;
    private final Map<LivingEntity, Observation> observations = new IdentityHashMap<>();
    private long tick = Long.MIN_VALUE, rays, hits;
    private Vec3 origin = Vec3.ZERO;

    private record Observation(AABB box, Vec3 eye, boolean visible, Boolean lane) {}

    CombatPerception(SamuraiEntity boss) {
        this.boss = boss;
    }

    void invalidate() {
        observations.clear();
        tick = Long.MIN_VALUE;
    }

    long rayCount() {
        return rays;
    }

    long cacheHits() {
        return hits;
    }

    private boolean clear(Vec3 from, Vec3 to) {
        rays++;
        return boss.level()
                        .clip(
                                new ClipContext(
                                        from,
                                        to,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        boss))
                        .getType()
                == HitResult.Type.MISS;
    }

    private Observation observe(LivingEntity target) {
        long now = boss.level().getGameTime();
        if (tick != now || !origin.equals(boss.position())) {
            observations.clear();
            tick = now;
            origin = boss.position();
        }
        AABB box = target.getBoundingBox();
        Vec3 eye = target.getEyePosition();
        Observation old = observations.get(target);
        if (old != null && old.box.equals(box) && old.eye.equals(eye)) {
            hits++;
            return old;
        }
        boolean visible = clear(boss.getEyePosition(), eye);
        if (!visible && TargetGeometry.large(target)) {
            for (double height : new double[] {2.65, 1.15}) {
                Vec3 from = origin.add(0, height, 0);
                if (clear(from, TargetGeometry.bodyPoint(target, from))) {
                    visible = true;
                    break;
                }
            }
        }
        if (!visible) {
            outer:
            for (double height : new double[] {2.65, 1.15}) {
                Vec3 from = origin.add(0, Math.min(height, boss.getBbHeight() * .8), 0);
                for (Vec3 to :
                        new Vec3[] {eye, target.position().add(0, target.getBbHeight() * .55, 0)})
                    if (clear(from, to)) {
                        visible = true;
                        break outer;
                    }
            }
        }
        Observation result = new Observation(box, eye, visible, null);
        // Candidate selection is bounded, but damage sweeps may encounter a larger crowd.
        if (observations.size() >= 128) observations.clear();
        observations.put(target, result);
        return result;
    }

    boolean visible(LivingEntity target) {
        return valid(target) && observe(target).visible;
    }

    boolean lane(LivingEntity target) {
        if (!valid(target)) return false;
        if (!AttackLane.heightAllowed(target, boss.getY(), target.getY(), AttackLane.ATTACK_DROP))
            return false;
        Observation value = observe(target);
        if (!value.visible) return false;
        if (value.lane != null) return value.lane;
        boolean lane =
                AttackLane.clear(
                        target,
                        origin,
                        target.position(),
                        target.getBbHeight(),
                        boss.getEyeHeight(),
                        this::clear);
        observations.put(target, new Observation(value.box, value.eye, value.visible, lane));
        return lane;
    }

    String laneFailure(LivingEntity target) {
        if (!valid(target)) return "目标已死亡/移除/切换维度";
        double dy = target.getY() - boss.getY();
        if (!AttackLane.heightAllowed(target, boss.getY(), target.getY(), AttackLane.ATTACK_DROP))
            return "高度差=" + dy;
        StringBuilder out =
                new StringBuilder(
                        "boss="
                                + boss.position()
                                + " target="
                                + target.position()
                                + " cachedLane="
                                + lane(target));
        Vec3 end = target.position().add(0, target.getBbHeight() * .55, 0);
        for (double height :
                AttackLane.heights(target, boss.getY(), target.getY(), boss.getEyeHeight())) {
            var ray =
                    boss.level()
                            .clip(
                                    new ClipContext(
                                            boss.position().add(0, height, 0),
                                            AttackLane.end(
                                                    target,
                                                    boss.position().add(0, height, 0),
                                                    target.position(),
                                                    target.getBbHeight()),
                                            ClipContext.Block.COLLIDER,
                                            ClipContext.Fluid.NONE,
                                            boss));
            if (ray.getType() != HitResult.Type.MISS)
                out.append(" ray@")
                        .append(height)
                        .append(" block=")
                        .append(ray.getBlockPos())
                        .append(' ')
                        .append(boss.level().getBlockState(ray.getBlockPos()));
        }
        return out.toString();
    }

    private boolean valid(LivingEntity target) {
        return target != null
                && target.isAlive()
                && !target.isRemoved()
                && target.level() == boss.level();
    }
}
