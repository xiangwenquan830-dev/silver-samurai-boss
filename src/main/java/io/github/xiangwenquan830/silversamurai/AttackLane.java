package io.github.xiangwenquan830.silversamurai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.function.BiPredicate;

/** Shared geometry for live attacks, hypothetical navigation goals and diagnostics. */
final class AttackLane {
    static final double MAX_RISE = 5.5, ATTACK_DROP = 1.4, NAVIGATION_DROP = 1.25, RAISED = 1.4;

    static boolean heightAllowed(
            LivingEntity target, double feetY, double targetY, double maxDrop) {
        return target != null && TargetGeometry.large(target)
                ? TargetGeometry.overlapsBladeHeight(target, feetY)
                : targetY - feetY <= MAX_RISE && targetY - feetY >= -maxDrop;
    }

    static double[] heights(LivingEntity target, double feetY, double targetY, double eyeHeight) {
        return (target == null || !TargetGeometry.large(target)) && targetY - feetY > RAISED
                ? new double[] {3.4, eyeHeight}
                : new double[] {1.15, 2.65};
    }

    static Vec3 end(LivingEntity target, Vec3 from, Vec3 point, double height) {
        return target != null && TargetGeometry.large(target)
                ? TargetGeometry.bodyPoint(target, from)
                : point.add(0, height * .55, 0);
    }

    static boolean clear(
            LivingEntity target,
            Vec3 origin,
            Vec3 point,
            double height,
            double eyeHeight,
            BiPredicate<Vec3, Vec3> ray) {
        for (double y : heights(target, origin.y, point.y, eyeHeight)) {
            Vec3 from = origin.add(0, y, 0);
            if (!ray.test(from, end(target, from, point, height))) return false;
        }
        return true;
    }

    private AttackLane() {}
}
