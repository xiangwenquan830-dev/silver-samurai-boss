package cn.blockforge.generated.bossmodbossmodbossm;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

/** Entity-only radial shock. No explosions, block edits or red-blade terrain calls. */
public final class StompCombat {
    private static List<LivingEntity> candidates(SamuraiEntity boss, double radius) {
        var area =
                new AABB(
                        boss.getX() - radius,
                        boss.getY() - .6,
                        boss.getZ() - radius,
                        boss.getX() + radius,
                        boss.getY() + 2.4,
                        boss.getZ() + radius);
        return boss.level()
                .getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        e ->
                                eligible(boss, e)
                                        && lowHeight(boss, e)
                                        && e.position()
                                                        .subtract(boss.position())
                                                        .horizontalDistanceSqr()
                                                <= radius * radius);
    }

    static boolean crowded(SamuraiEntity boss, double radius, int required) {
        int count = 0;
        for (var e : candidates(boss, radius))
            if (clear(boss, e) && ++count >= required) return true;
        return false;
    }

    public static List<LivingEntity> enemies(SamuraiEntity boss, double radius) {
        List<LivingEntity> found = candidates(boss, radius);
        found.removeIf(e -> !clear(boss, e));
        found.sort(
                Comparator.<LivingEntity>comparingDouble(boss::distanceToSqr)
                        .thenComparingInt(LivingEntity::getId));
        return found;
    }

    private static boolean eligible(SamuraiEntity boss, LivingEntity e) {
        return CombatRules.enemy(boss, e, e != boss.getTarget());
    }

    private static boolean lowHeight(SamuraiEntity boss, LivingEntity e) {
        double dy = e.getY() - boss.getY();
        return dy >= -1.5 && dy <= .75 && e.getBoundingBox().maxY > boss.getY() + .1;
    }

    static boolean closeReach(SamuraiEntity boss, LivingEntity e) {
        if (e == null || !eligible(boss, e) || !lowHeight(boss, e)) return false;
        double radius =
                Math.min(
                        2.4,
                        CombatSpacing.profile(
                                        boss.phase(),
                                        boss.phase() == 2 ? SamuraiRig.COMBO_TWO : SamuraiRig.A,
                                        0,
                                        e.getBbWidth(),
                                        boss.range(0),
                                        0)
                                .minimum());
        return e.position().subtract(boss.position()).horizontalDistanceSqr() < radius * radius
                && clear(boss, e);
    }

    private static boolean clear(SamuraiEntity boss, LivingEntity e) {
        Vec3 start = boss.position().add(0, .45, 0);
        double y =
                net.minecraft.util.Mth.clamp(
                        start.y, e.getBoundingBox().minY + .1, e.getBoundingBox().maxY - .05);
        Vec3 end = new Vec3(e.getX(), y, e.getZ());
        return boss.level()
                        .clip(
                                new ClipContext(
                                        start,
                                        end,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        boss))
                        .getType()
                == HitResult.Type.MISS;
    }

    public static void impact(SamuraiEntity boss) {
        impact(boss, false);
    }

    public static void impact(SamuraiEntity boss, boolean focused) {
        if (!(boss.level() instanceof ServerLevel level) || !boss.isAlive()) return;
        float damage =
                CombatTiming.cap(
                        (float)
                                (boss.getAttributeValue(Attributes.ATTACK_DAMAGE)
                                        * BossConfig.STOMP_DAMAGE.get()),
                        BossConfig.OUTGOING_CAP.get());
        int count = 0;
        var current = boss.getTarget();
        List<LivingEntity> targets =
                focused
                        ? (closeReach(boss, current) ? List.of(current) : List.of())
                        : enemies(boss, BossConfig.STOMP_RADIUS.get());
        for (var enemy : targets) {
            Vec3 direction = enemy.position().subtract(boss.position());
            float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90;
            if (focused
                    && direction.horizontalDistanceSqr() >= .16
                    && Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - boss.getYRot())) > 65)
                continue;
            if (count++ >= 32) break;
            if (!enemy.hurt(boss.damageSources().mobAttack(boss), damage)) continue;
            Vec3 away = enemy.position().subtract(boss.position()).multiply(1, 0, 1);
            if (away.horizontalDistanceSqr() < .0001) {
                double a = enemy.getId() * 2.399963;
                away = new Vec3(Math.cos(a), 0, Math.sin(a));
            }
            enemy.knockback(BossConfig.STOMP_PUSH.get() * (focused ? .6 : 1), -away.x, -away.z);
            enemy.hurtMarked = true;
        }
        boss.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.6f, .6f);
        for (int i = 0; i < (focused ? 8 : 24); i++) {
            double
                    angle =
                            focused
                                    ? Math.toRadians(boss.getYRot() + 45 + i * 90.0 / 7)
                                    : 2 * Math.PI * i / 24,
                    r = focused ? 1.1 : BossConfig.STOMP_RADIUS.get() * .7;
            level.sendParticles(
                    ParticleTypes.POOF,
                    boss.getX() + Math.cos(angle) * r,
                    boss.getY() + .08,
                    boss.getZ() + Math.sin(angle) * r,
                    1,
                    .04,
                    .02,
                    .04,
                    .015);
        }
    }

    private StompCombat() {}
}
