package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.Projectile;

/** Percentage defenses are alternatives; armor and the final incoming cap remain downstream. */
final class DefensePolicy {
    private static final net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType>
            BULLETS =
                    net.minecraft.tags.TagKey.create(
                            net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                            new net.minecraft.resources.ResourceLocation("tacz", "bullets"));
    private static final java.util.Set<String> BULLET_IDS =
            java.util.Set.of(
                    "tacz:bullet",
                    "tacz:bullet_ignore_armor",
                    "tacz:bullet_void",
                    "tacz:bullet_void_ignore_armor");

    static boolean gunBullet(DamageSource source) {
        return source.is(BULLETS)
                || BULLET_IDS.contains(
                        source.typeHolder()
                                .unwrapKey()
                                .map(k -> k.location().toString())
                                .orElse(""));
    }

    static double reduction(SamuraiEntity boss, DamageSource source) {
        if (CombatRules.absolute(source)) return 0;
        String id = source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("");
        double physical =
                boss.phase() == 1
                                && (CombatRules.damageClass(source).equals("physical")
                                        || gunBullet(source))
                        ? BossConfig.PHYSICAL_REDUCTION.get()
                                * (1
                                        - (WolverineCompat.fullRageClaw(source)
                                                ? BossConfig.CLAW_BYPASS.get()
                                                : 0))
                        : 0;
        boolean ranged = ranged(source);
        boolean periodic = boss.processingEffects() || BossConfig.PERIODIC_TYPES.get().contains(id);
        return Math.max(
                physical,
                Math.max(
                        ranged ? BossConfig.RANGED_REDUCTION.get() : 0,
                        periodic ? BossConfig.PERIODIC_REDUCTION.get() : 0));
    }

    static boolean ranged(DamageSource source) {
        String id = source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("");
        return !CombatRules.absolute(source)
                && (gunBullet(source)
                        || source.is(DamageTypeTags.IS_PROJECTILE)
                        || source.getDirectEntity() instanceof Projectile
                        || BossConfig.RANGED_TYPES.get().contains(id));
    }

    private DefensePolicy() {}
}
