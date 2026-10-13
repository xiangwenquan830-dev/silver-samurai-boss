package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.minecraft.util.Mth;

/** Distances to a real body, without expanding the damage box or aiming through terrain. */
final class TargetGeometry {
    static double extraRadius(LivingEntity target) {
        return Math.max(0, target.getBbWidth() - .6) * .5;
    }

    static boolean large(LivingEntity target) {
        return target.getBbWidth() > 1.8 || target.getBbHeight() > 4;
    }

    static Vec3 closest(AABB box, Vec3 point) {
        return new Vec3(
                Mth.clamp(point.x, box.minX, box.maxX),
                Mth.clamp(point.y, box.minY, box.maxY),
                Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    static double distanceSqr(LivingEntity boss, LivingEntity target) {
        if (!large(target)) return boss.distanceToSqr(target);
        return closest(target.getBoundingBox(), boss.getBoundingBox().getCenter())
                .distanceToSqr(boss.getBoundingBox().getCenter());
    }

    static Vec3 bodyPoint(LivingEntity target, Vec3 from) {
        return closest(target.getBoundingBox().deflate(.01), from);
    }

    static boolean overlapsBladeHeight(LivingEntity target, double feet) {
        AABB box = target.getBoundingBox();
        return box.maxY > feet + .15 && box.minY < feet + 4.0;
    }

    private TargetGeometry() {}
}
