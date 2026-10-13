package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Short observed-motion lead for preparation only. Never moves a target or enlarges a hitbox. */
final class AttackTracking {
    private LivingEntity observed;
    private long tick = Long.MIN_VALUE;
    private Vec3 last = Vec3.ZERO, velocity = Vec3.ZERO;

    void sample(LivingEntity target, long now) {
        if (target == null || !target.isAlive()) {
            observed = null;
            tick = now;
            velocity = Vec3.ZERO;
            return;
        }
        if (target != observed || now - tick != 1) {
            if (target == observed && now == tick) return;
            observed = target;
            last = target.position();
            tick = now;
            velocity = Vec3.ZERO;
            return;
        }
        Vec3 travel = target.position().subtract(last).multiply(1, 0, 1);
        last = target.position();
        tick = now;
        if (travel.horizontalDistanceSqr() > 2.25) {
            velocity = Vec3.ZERO;
            return;
        } // teleport/discontinuity
        if (travel.horizontalDistanceSqr() < .0001) {
            velocity = Vec3.ZERO;
            return;
        }
        velocity = velocity.dot(travel) < 0 ? travel : velocity.scale(.35).add(travel.scale(.65));
    }

    boolean fast(LivingEntity target) {
        return target != null && target == observed && velocity.horizontalDistanceSqr() > .22 * .22;
    }

    Vec3 aimPoint(LivingEntity target) {
        if (!fast(target)) return target.position();
        Vec3 lead = velocity.scale(2);
        double length = lead.length();
        if (length > 1.1) lead = lead.scale(1.1 / length);
        return target.position().add(lead);
    }
}
