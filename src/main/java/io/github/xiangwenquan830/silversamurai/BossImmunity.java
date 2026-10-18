package io.github.xiangwenquan830.silversamurai;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.registries.ForgeRegistries;

/** Optional registry-ID compatibility: no dependency on The Hordes being installed. */
public final class BossImmunity {
    public static boolean blocked(MobEffect effect) {
        var id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        return id != null && BossConfig.IMMUNE_EFFECTS.get().contains(id.toString());
    }

    public static boolean blocked(DamageSource source) {
        if (CombatRules.absolute(source)) return false;
        return source.typeHolder()
                .unwrapKey()
                .map(k -> BossConfig.IMMUNE_DAMAGE.get().contains(k.location().toString()))
                .orElse(false);
    }

    private BossImmunity() {}
}
