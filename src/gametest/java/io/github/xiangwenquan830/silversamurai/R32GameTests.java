package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class R32GameTests {
    @GameTest(template = "arena", timeoutTicks = 30, batch = "r32_damage")
    public static void damage_policy(GameTestHelper h) {
        floor(h);
        var b = boss(h, 5.5, 5.5);
        b.setNoAi(true);
        var p = player(h, 5.5, 1, 8.6);
        for (int phase = 1; phase <= 2; phase++)
            for (boolean alt : new boolean[] {false, true}) {
                b.testCombat(p, phase, alt);
                b.setNoAi(true);
                for (int strike = 0; strike < 3; strike++) {
                    p.invulnerableTime = 0;
                    float before = p.getHealth();
                    BladeCombat.damage(b, p, strike);
                    float expected = (strike == 2 ? 30 : 12) * (phase == 2 ? 2 : 1);
                    h.assertTrue(
                            Math.abs(AttackDamage.blade(b, strike) - expected) < .01,
                            "raw damage policy changed");
                    if (phase == 1)
                        expected =
                                switch (h.getLevel().getDifficulty()) {
                                    case PEACEFUL -> 0;
                                    case EASY -> Math.min(expected / 2 + 1, expected);
                                    case NORMAL -> expected;
                                    case HARD -> expected * 1.5f;
                                };
                    h.assertTrue(
                            Math.abs(before - p.getHealth() - expected) < .01,
                            "phase/strike damage "
                                    + phase
                                    + "/"
                                    + strike
                                    + " actual="
                                    + (before - p.getHealth()));
                }
            }
        double cap = BossConfig.OUTGOING_CAP.get();
        try {
            BossConfig.OUTGOING_CAP.set(17d);
            p.invulnerableTime = 0;
            float before = p.getHealth();
            BladeCombat.damage(b, p, 2);
            h.assertTrue(
                    Math.abs(before - p.getHealth() - 17) < .01,
                    "custom hard cap bypassed by finisher");
        } finally {
            BossConfig.OUTGOING_CAP.set(cap);
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r32_heal")
    public static void healing_paths(GameTestHelper h) {
        floor(h);
        var b = boss(h, 5.5, 5.5);
        b.setNoAi(true);
        var p = player(h, 5.5, 1, 8.6);
        b.testCombat(p, 2, false);
        b.setNoAi(true);
        p.setHealth(500);
        BladeCombat.damage(b, p, 0);
        h.assertTrue(p.hasEffect(RedBladeWound.EFFECT.get()), "red hit missing wound");
        float before = p.getHealth();
        p.heal(20);
        h.assertTrue(Math.abs(p.getHealth() - before - 5) < .001, "heal did not retain 25%");
        before = p.getHealth();
        MobEffects.HEAL.applyInstantenousEffect(null, null, p, 0, 1);
        h.assertTrue(Math.abs(p.getHealth() - before - 1) < .001, "instant health not suppressed");
        before = p.getHealth();
        MobEffects.REGENERATION.applyEffectTick(p, 0);
        h.assertTrue(Math.abs(p.getHealth() - before - .25) < .001, "regeneration not suppressed");
        RedBladeWound.apply(p);
        RedBladeWound.apply(p);
        before = p.getHealth();
        p.heal(20);
        h.assertTrue(
                Math.abs(p.getHealth() - before - 5) < .001,
                "multiple hits compounded suppression");
        boolean enabled = BossConfig.RED_WOUND.get();
        double reduction = BossConfig.RED_WOUND_REDUCTION.get();
        try {
            BossConfig.RED_WOUND.set(false);
            before = p.getHealth();
            p.heal(20);
            h.assertTrue(
                    p.getHealth() - before == 20, "disable did not immediately restore healing");
            BossConfig.RED_WOUND.set(true);
            BossConfig.RED_WOUND_REDUCTION.set(1d);
            before = p.getHealth();
            p.heal(20);
            h.assertTrue(p.getHealth() == before, "100% setting still heals");
        } finally {
            BossConfig.RED_WOUND.set(enabled);
            BossConfig.RED_WOUND_REDUCTION.set(reduction);
        }
        p.removeEffect(RedBladeWound.EFFECT.get());
        before = p.getHealth();
        p.heal(20);
        h.assertTrue(p.getHealth() - before == 20, "cleansing did not restore healing");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 170, batch = "r32_wound_expiry")
    public static void wound_lifecycle(GameTestHelper h) {
        floor(h);
        var p = player(h, 5.5, 1, 8.6);
        p.setHealth(400);
        RedBladeWound.apply(p);
        h.runAtTickTime(
                20,
                () -> {
                    RedBladeWound.apply(p);
                    h.assertTrue(
                            p.getEffect(RedBladeWound.EFFECT.get()).getDuration() == 120,
                            "hit did not refresh to 120 ticks");
                    var copy = h.spawn(EntityType.COW, new BlockPos(10, 1, 10));
                    CompoundTag tag = new CompoundTag();
                    p.saveWithoutId(tag);
                    // Effect serialization uses the vanilla remaining-duration contract,
                    // independent of entity type.
                    var loaded =
                            MobEffectInstance.load(tag.getList("ActiveEffects", 10).getCompound(0));
                    copy.addEffect(loaded);
                    h.assertTrue(
                            copy.getEffect(RedBladeWound.EFFECT.get()).getDuration() == 120,
                            "effect reload extended or lost timer");
                });
        h.runAtTickTime(
                143,
                () -> {
                    h.assertFalse(p.hasEffect(RedBladeWound.EFFECT.get()), "wound never expired");
                    float before = p.getHealth();
                    p.heal(20);
                    h.assertTrue(
                            p.getHealth() - before == 20, "healing stayed suppressed after expiry");
                    h.succeed();
                });
    }

    public static final class Reject {
        final LivingEntity target;
        boolean zero;

        Reject(LivingEntity target) {
            this.target = target;
        }

        @SubscribeEvent
        public void damage(LivingHurtEvent e) {
            if (e.getEntity() == target) {
                if (zero) e.setAmount(0);
                else e.setCanceled(true);
            }
        }
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r32_hit_commit")
    public static void accepted_contact(GameTestHelper h) {
        floor(h);
        var b = boss(h, 5.5, 5.5);
        b.setNoAi(true);
        var p = player(h, 5.5, 1, 8.6);
        b.testCombat(p, 2, false);
        b.setNoAi(true);
        var reject = new Reject(p);
        MinecraftForge.EVENT_BUS.register(reject);
        try {
            BladeCombat.damage(b, p, 0);
            h.assertFalse(p.hasEffect(RedBladeWound.EFFECT.get()), "canceled hit applied wound");
            reject.zero = true;
            p.invulnerableTime = 0;
            BladeCombat.damage(b, p, 0);
            h.assertFalse(p.hasEffect(RedBladeWound.EFFECT.get()), "zero damage applied wound");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(reject);
        }
        p.invulnerableTime = 0;
        p.setAbsorptionAmount(50);
        float health = p.getHealth();
        BladeCombat.damage(b, p, 0);
        h.assertTrue(
                p.getHealth() == health
                        && p.getAbsorptionAmount() < 50
                        && p.hasEffect(RedBladeWound.EFFECT.get()),
                "real absorption contact lost wound");
        p.removeEffect(RedBladeWound.EFFECT.get());
        p.invulnerableTime = 0;
        b.testCombat(p, 1, false);
        b.setNoAi(true);
        BladeCombat.damage(b, p, 0);
        h.assertFalse(p.hasEffect(RedBladeWound.EFFECT.get()), "silver blade applied red effect");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r32_kick_exit")
    public static void kick_then_blade(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        var p = player(h, 10.5, 1, 12);
        p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        b.testCombat(p, 2, false);
        b.testPursuit(p);
        int[] last = {-1}, starts = {0};
        long[] lastKick = {-1000};
        boolean[] moved = {false}, blade = {false};
        Vec3 origin = b.position();
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    if (b.state() == SamuraiEntity.STOMP && last[0] != SamuraiEntity.STOMP) {
                        h.assertTrue(h.getTick() - lastKick[0] >= 80, "shared cooldown bypassed");
                        lastKick[0] = h.getTick();
                        starts[0]++;
                    }
                    if (starts[0] > 0) {
                        moved[0] |= b.position().distanceTo(origin) > .3;
                        blade[0] |= java.util.Arrays.stream(b.hitCounts()).sum() > 0;
                    }
                    last[0] = b.state();
                });
        h.runAtTickTime(
                215,
                () -> {
                    h.assertTrue(starts[0] >= 1, "no near defense");
                    h.assertTrue(
                            moved[0] && blade[0],
                            "kick cooldown starved reposition/blade: " + b.report());
                    h.assertTrue(starts[0] <= 2, "repeated kick instead of blade pressure");
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r32_shared_save")
    public static void shared_cooldown_reload(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        var p = player(h, 10.5, 1, 12);
        b.testCombat(p, 2, false);
        b.testPursuit(p);
        b.setNoAi(true);
        var policy = new CloseDefense(b);
        h.assertTrue(
                policy.choose(false, 0) == CloseDefense.Action.FOCUSED, "blind spot not eligible");
        h.assertTrue(
                policy.choose(true, 0) == CloseDefense.Action.NONE, "kick preempted valid blade");
        policy.started(CloseDefense.Action.FOCUSED);
        CompoundTag tag = new CompoundTag();
        policy.save(tag);
        var loaded = new CloseDefense(b);
        loaded.load(tag, 0);
        h.assertTrue(
                loaded.sharedRemaining() == 80
                        && loaded.choose(false, 0) == CloseDefense.Action.NONE,
                "reload bypassed shared timer");
        // Crowd appearing during single kick cooldown must not start an independent second action.
        for (int i = 0; i < 4; i++) {
            var e = h.spawn(EntityType.HUSK, new BlockPos(9 + i % 2, 1, 10 + i / 2));
            e.setNoAi(true);
        }
        h.assertTrue(
                loaded.choose(false, 0) == CloseDefense.Action.NONE,
                "crowd bypassed focused cooldown");
        tag.putLong("SamuraiCloseSharedCooldown", Long.MAX_VALUE);
        loaded.load(tag, Long.MAX_VALUE);
        h.assertTrue(loaded.sharedRemaining() == 80, "corrupt NBT made permanent wait");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 80, batch = "r32_wolverine_heal")
    public static void wolverine_regeneration(GameTestHelper h) throws Exception {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("minec_minecraft_forge_1")) {
            GeneratedMod.LOG.info(
                    "[COMPAT_SKIP] real Wolverine healing test requires installed jar");
            h.succeed();
            return;
        }
        floor(h);
        var p = player(h, 8.5, 1, 8.5);
        Class<?> type = Class.forName("cn.blockforge.generated.minecminecraftforge1.AbilityData");
        Object data =
                type.getMethod("get", net.minecraft.world.entity.player.Player.class)
                        .invoke(null, p);
        var tick =
                Class.forName("cn.blockforge.generated.minecminecraftforge1.HealingEngine")
                        .getMethod("tick", net.minecraft.server.level.ServerPlayer.class, type);
        for (int mode = 0; mode < 4; mode++) {
            float[] gain = new float[2];
            for (int wound = 0; wound < 2; wound++) {
                p.removeEffect(RedBladeWound.EFFECT.get());
                p.setHealth(mode == 2 ? 200 : 500);
                set(type, data, "enabled", true);
                set(type, data, "combat", mode == 1 ? 0 : 200);
                set(type, data, "reserve", mode == 3 ? 0 : 100);
                set(type, data, "wounds", 0);
                set(type, data, "woundedTime", 0);
                set(type, data, "burstActive", mode == 2 ? 40 : 0);
                set(type, data, "burstCooldown", 2000);
                set(type, data, "healCarryBase", 0);
                set(type, data, "healCarryFast", 0);
                set(type, data, "lastHealingTick", -1);
                if (wound == 1) RedBladeWound.apply(p);
                float before = p.getHealth();
                tick.invoke(null, p, data);
                gain[wound] = p.getHealth() - before;
            }
            h.assertTrue(gain[0] > .0001, "real Wolverine did not regenerate in mode " + mode);
            h.assertTrue(
                    Math.abs(gain[1] - gain[0] * .25) < .0002,
                    "Wolverine mode "
                            + mode
                            + " not 75% suppressed: "
                            + gain[0]
                            + " -> "
                            + gain[1]);
            GeneratedMod.LOG.info(
                    "[COMPAT_PASS] actual Wolverine HealingEngine mode={} normal={} wounded={} ratio={}",
                    mode,
                    gain[0],
                    gain[1],
                    gain[1] / gain[0]);
        }
        p.removeEffect(RedBladeWound.EFFECT.get());
        set(type, data, "lastHealingTick", -1);
        float before = p.getHealth();
        tick.invoke(null, p, data);
        h.assertTrue(p.getHealth() > before, "Wolverine failed to recover after effect removal");
        h.succeed();
    }

    private static void set(Class<?> type, Object data, String name, Object value)
            throws ReflectiveOperationException {
        var f = type.getField(name);
        Class<?> t = f.getType();
        if (t == boolean.class) f.setBoolean(data, (Boolean) value);
        else if (t == int.class) f.setInt(data, ((Number) value).intValue());
        else if (t == long.class) f.setLong(data, ((Number) value).longValue());
        else if (t == float.class) f.setFloat(data, ((Number) value).floatValue());
        else if (t == double.class) f.setDouble(data, ((Number) value).doubleValue());
        else throw new IllegalArgumentException(name);
    }
}
