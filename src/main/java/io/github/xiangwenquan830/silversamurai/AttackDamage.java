package io.github.xiangwenquan830.silversamurai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Shared blade damage policy: the configured hard ceiling always wins, including finishers. */
final class AttackDamage {
    static float blade(SamuraiEntity boss, int strike) {
        double multiplier =
                switch (strike) {
                    case 0 -> BossConfig.FIRST.get();
                    case 1 -> BossConfig.SECOND.get();
                    default -> BossConfig.FINISH.get();
                };
        float damage =
                (float)
                        (boss.getAttributeValue(Attributes.ATTACK_DAMAGE)
                                * multiplier
                                * (boss.redReady() ? BossConfig.RED.get() : 1));
        return CombatTiming.cap(damage, BossConfig.OUTGOING_CAP.get());
    }

    static boolean effectiveContact(
            LivingEntity target, float healthBefore, float absorptionBefore) {
        return target.getHealth() < healthBefore || target.getAbsorptionAmount() < absorptionBefore;
    }

    private AttackDamage() {}
}
