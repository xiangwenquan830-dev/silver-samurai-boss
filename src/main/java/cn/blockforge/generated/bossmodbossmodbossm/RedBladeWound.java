package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.*;

/**
 * Suppress healing before its owner commits health/reserve changes; no health rollback or polling.
 */
public final class RedBladeWound extends MobEffect {
    static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, GeneratedMod.MOD_ID);
    static final RegistryObject<MobEffect> EFFECT =
            EFFECTS.register("red_blade_wound", RedBladeWound::new);

    private RedBladeWound() {
        super(MobEffectCategory.HARMFUL, 0xAD383B);
    }

    static void apply(LivingEntity target) {
        if (target.level().isClientSide
                || !target.isAlive()
                || !BossConfig.RED_WOUND.get()
                || BossConfig.RED_WOUND_REDUCTION.get() <= 0) return;
        target.addEffect(
                new MobEffectInstance(
                        EFFECT.get(), BossConfig.RED_WOUND_TICKS.get(), 0, false, false, true));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void healing(LivingHealEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide
                || !target.isAlive()
                || !BossConfig.RED_WOUND.get()
                || !target.hasEffect(EFFECT.get())
                || event.getAmount() <= 0) return;
        // Also receives Wolverine r69 HealingEngine's explicit ForgeEventFactory.onLivingHeal call.
        // One effect instance and one hook: repeated hits or multiple bosses cannot multiply the
        // reduction.
        float accepted = (float) (event.getAmount() * (1 - BossConfig.RED_WOUND_REDUCTION.get()));
        event.setAmount(accepted);
        if (accepted <= 0) event.setCanceled(true);
    }
}
