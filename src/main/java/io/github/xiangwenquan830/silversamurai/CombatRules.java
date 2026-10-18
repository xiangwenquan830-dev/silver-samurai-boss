package io.github.xiangwenquan830.silversamurai;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Set;

public final class CombatRules {
    public static final ResourceKey<DamageType> RED_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, GeneratedMod.id("red_blade"));
    public static final TagKey<DamageType> MAGIC =
            TagKey.create(Registries.DAMAGE_TYPE, GeneratedMod.id("magic"));
    public static final TagKey<DamageType> PHYSICAL =
            TagKey.create(Registries.DAMAGE_TYPE, GeneratedMod.id("physical"));
    public static final TagKey<EntityType<?>> FRIEND =
            TagKey.create(Registries.ENTITY_TYPE, GeneratedMod.id("friends"));
    public static final TagKey<EntityType<?>> HOSTILE =
            TagKey.create(Registries.ENTITY_TYPE, GeneratedMod.id("enemies"));

    public static boolean absolute(DamageSource source) {
        return source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL);
    }

    public static String damageClass(DamageSource source) {
        if (absolute(source)) return "bypass";
        String id = source.typeHolder().unwrapKey().map(k -> k.location().toString()).orElse("");
        for (String value : BossConfig.CLASSIFICATION.get()) {
            String[] pair = value.split("=", 2);
            if (pair.length == 2
                    && pair[0].trim().equals(id)
                    && Set.of("physical", "fire", "magic", "other", "bypass")
                            .contains(pair[1].trim())) return pair[1].trim();
        }
        if (source.is(DamageTypeTags.IS_FIRE)) return "fire";
        if (source.is(MAGIC)
                || source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.DRAGON_BREATH)) return "magic";
        if (source.is(PHYSICAL)
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypeTags.IS_FALL)) return "physical";
        if (source.getEntity() instanceof LivingEntity
                && source.getDirectEntity() == source.getEntity()) return "physical";
        return "other";
    }

    /** 可受攻击与维持观察分别判断，创造免疫不代表附近无人。 */
    public static boolean validPlayer(Player p) {
        return p.isAlive() && !p.isCreative() && !p.isSpectator();
    }

    public static boolean observer(Player p) {
        return p.isAlive()
                && !p.isRemoved()
                && !p.isSpectator()
                && (!p.isCreative() || BossConfig.CREATIVE_ALERT.get());
    }

    public static String entityId(Entity e) {
        return String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(e.getType()));
    }

    public static boolean friend(SamuraiEntity boss, LivingEntity e) {
        return e == boss
                || !e.isAlive()
                || e.isSpectator()
                || e instanceof Player p && !validPlayer(p)
                || boss.isAlliedTo(e)
                || e.getType().is(FRIEND)
                || BossConfig.FRIENDS.get().contains(entityId(e))
                || e instanceof SamuraiEntity && !BossConfig.SAME_KIND.get();
    }

    public static boolean hostileMob(LivingEntity e) {
        return e instanceof Enemy
                || e.getType().is(HOSTILE)
                || BossConfig.ENEMIES.get().contains(entityId(e));
    }

    public static boolean enemy(SamuraiEntity boss, LivingEntity e, boolean acquire) {
        if (friend(boss, e)) return false;
        if (!acquire && e == boss.getTarget()) return true;
        if (e instanceof Player) return BossConfig.PLAYERS.get();
        if (e.getType().is(HOSTILE) || BossConfig.ENEMIES.get().contains(entityId(e)))
            return !acquire || BossConfig.HUNT_MOBS.get();
        if (e instanceof SamuraiEntity) return BossConfig.SAME_KIND.get();
        return hostileMob(e) && (BossConfig.HUNT_MOBS.get() || !acquire);
    }

    public static DamageSource red(SamuraiEntity boss) {
        return new DamageSource(
                boss.level()
                        .registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(RED_TYPE),
                boss) {
            @Override
            public boolean is(TagKey<DamageType> tag) {
                if (tag == DamageTypeTags.BYPASSES_ARMOR || tag == DamageTypeTags.BYPASSES_SHIELD)
                    return true;
                if (tag == DamageTypeTags.BYPASSES_RESISTANCE && BossConfig.BYPASS_RESISTANCE.get())
                    return true;
                if (tag == DamageTypeTags.BYPASSES_ENCHANTMENTS && BossConfig.BYPASS_EFFECTS.get())
                    return true;
                return super.is(tag);
            }
        };
    }

    private CombatRules() {}
}
