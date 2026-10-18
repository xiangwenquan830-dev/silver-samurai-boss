package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RedBreakGameTests {
    public static final class InterfaceFixture {
        public boolean requested;
        public String action;
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "pain_duration")
    public static void pain_duration(GameTestHelper h) {
        floor(h);
        var p = player(h, 5.5, 1, 5.5);
        var speed =
                p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double before = speed.getValue();
        ClawPain.begin(p);
        long deadline = p.getPersistentData().getLong(ClawPain.UNTIL);
        h.assertTrue(
                ClawPain.remaining(p) == 20 && speed.getValue() == 0,
                "pain did not immobilize immediately");
        var dummy =
                h.spawn(
                        net.minecraft.world.entity.EntityType.ZOMBIE,
                        new net.minecraft.core.BlockPos(8, 1, 5));
        dummy.setNoAi(true);
        var attack = new net.minecraftforge.event.entity.player.AttackEntityEvent(p, dummy);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(attack);
        h.assertTrue(attack.isCanceled(), "pain allowed attack");
        var use =
                new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start(
                        p, new ItemStack(Items.APPLE), 32);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(use);
        h.assertTrue(use.isCanceled(), "pain allowed item use");
        h.onEachTick(
                () -> {
                    ClawPain.tick(p);
                    long now = ClawSeverance.now(p);
                    if (now < deadline)
                        h.assertTrue(
                                ClawPain.active(p) && speed.getValue() == 0, "pain ended early");
                    else {
                        h.assertTrue(
                                !ClawPain.active(p) && Math.abs(speed.getValue() - before) < 1e-8,
                                "speed not restored");
                        dummy.discard();
                        p.discard();
                        h.succeed();
                    }
                });
    }

    @GameTest(template = "arena", timeoutTicks = 5, batch = "compat_contract")
    public static void compat_contract(GameTestHelper h) throws Exception {
        h.assertTrue(
                WolverineCompat.required(InterfaceFixture.class, "requested", boolean.class)
                        != null,
                "core contract rejected");
        h.assertTrue(
                WolverineCompat.optional(InterfaceFixture.class, "action", int.class) == null,
                "wrong optional type accepted");
        h.assertTrue(
                WolverineCompat.optional(InterfaceFixture.class, "actionRage", double.class)
                        == null,
                "missing optional field accepted");
        boolean rejected = false;
        try {
            WolverineCompat.required(InterfaceFixture.class, "requested", int.class);
        } catch (ReflectiveOperationException expected) {
            rejected = true;
        }
        h.assertTrue(rejected, "wrong core type accepted");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "red_destroy_shield")
    public static void shield(GameTestHelper h) {
        shieldCase(h, true, true);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "red_held_shield")
    public static void held(GameTestHelper h) {
        shieldCase(h, false, true);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "red_back_shield")
    public static void rear(GameTestHelper h) {
        shieldCase(h, true, false);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "red_cancelled_shield")
    public static void cancelled(GameTestHelper h) {
        shieldCase(h, true, true, true, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "ordinary_shield")
    public static void ordinary(GameTestHelper h) {
        shieldCase(h, true, true, false, 1);
    }

    private static void shieldCase(GameTestHelper h, boolean guard, boolean facing) {
        shieldCase(h, guard, facing, false, 2);
    }

    private static void shieldCase(
            GameTestHelper h, boolean guard, boolean facing, boolean cancel, int phase) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        b.testCombat(p, phase, false);
        b.setNoAi(true);
        ItemStack shield = new ItemStack(Items.SHIELD);
        shield.getOrCreateTag().putBoolean("Unbreakable", true);
        p.setItemInHand(InteractionHand.OFF_HAND, shield);
        p.getInventory().setItem(9, new ItemStack(Items.SHIELD));
        p.setYRot(facing ? 180 : 0);
        if (guard) p.startUsingItem(InteractionHand.OFF_HAND);
        h.runAtTickTime(
                75,
                () -> {
                    h.assertTrue(!guard || p.isBlocking(), "fixture failed to raise shield");
                    java.util.function.Consumer<
                                    net.minecraftforge.event.entity.living.LivingAttackEvent>
                            deny =
                                    e -> {
                                        if (e.getEntity() == p && e.getSource().getEntity() == b)
                                            e.setCanceled(true);
                                    };
                    if (cancel)
                        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(deny);
                    try {
                        BladeCombat.damage(b, p, 0);
                    } finally {
                        if (cancel)
                            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(deny);
                    }
                    h.assertTrue(
                            p.getOffhandItem().isEmpty()
                                    == (guard && facing && !cancel && phase == 2),
                            "wrong shield destruction condition");
                    h.assertTrue(
                            p.getInventory().getItem(9).is(Items.SHIELD), "spare shield destroyed");
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "red_regrow_timer")
    public static void regrow_timer(GameTestHelper h) {
        floor(h);
        var p = player(h, 5.5, 1, 5.5);
        ClawSeverance.begin(p, true);
        long deadline = p.getPersistentData().getLong(ClawSeverance.UNTIL);
        h.assertTrue(deadline - ClawSeverance.now(p) == 600, "not thirty seconds");
        boolean[] last = {false};
        h.onEachTick(
                () -> {
                    long time = ClawSeverance.now(p);
                    if (time == deadline - 1) {
                        h.assertTrue(ClawSeverance.remaining(p) == 1, "expired early");
                        last[0] = true;
                    }
                    if (time >= deadline) {
                        h.assertTrue(
                                last[0] && !ClawSeverance.active(p),
                                "timer did not expire exactly");
                        p.discard();
                        h.succeed();
                    }
                });
    }

    @GameTest(template = "arena", timeoutTicks = 780, batch = "red_wolverine_r69")
    public static void wolverine(GameTestHelper h) throws Exception {
        wolverineCase(h, false);
    }

    @GameTest(template = "arena", timeoutTicks = 780, batch = "red_wolverine_side")
    public static void wolverine_side(GameTestHelper h) throws Exception {
        wolverineCase(h, true);
    }

    private static void wolverineCase(GameTestHelper h, boolean side) throws Exception {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("minec_minecraft_forge_1")) {
            GeneratedMod.LOG.info("[COMPAT_SKIP] r69 not installed in this test run");
            h.succeed();
            return;
        }
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        b.testCombat(p, 2, false);
        b.setNoAi(true);
        p.setYRot(180);
        Class<?> type = Class.forName("cn.blockforge.generated.minecminecraftforge1.AbilityData");
        Object data =
                type.getMethod("get", net.minecraft.world.entity.player.Player.class)
                        .invoke(null, p);
        long[] deadline = {Long.MAX_VALUE};
        h.runAtTickTime(
                70,
                () -> {
                    try {
                        for (String name :
                                new String[] {"enabled", "requested", "mainClaw", "offClaw"})
                            type.getField(name).setBoolean(data, true);
                        Object guard = type.getField("guard").get(data);
                        guard.getClass().getField("active").setBoolean(guard, true);
                        h.assertTrue(WolverineCompat.guarding(p), "real r69 guard not detected");
                        if (side) b.testSideSweep(p, 2, true, -24);
                        BladeCombat.damage(b, p, 0);
                        h.assertTrue(
                                ClawSeverance.remaining(p) == 600, "real r69 did not sever claws");
                        h.assertTrue(
                                ClawPain.remaining(p) == 20, "sever did not start one-second pain");
                        h.assertTrue(
                                !type.getField("mainClaw").getBoolean(data)
                                        && !type.getField("offClaw").getBoolean(data),
                                "real claws stayed visible");
                        h.assertTrue(
                                type.getField("enabled").getBoolean(data),
                                "Wolverine ability removed");
                        deadline[0] = p.getPersistentData().getLong(ClawSeverance.UNTIL);
                    } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException(ex);
                    }
                });
        h.onEachTick(
                () -> {
                    if (deadline[0] == Long.MAX_VALUE) return;
                    try {
                        if (ClawSeverance.now(p) < deadline[0]) {
                            if (ClawSeverance.now(p) >= deadline[0] - 580)
                                h.assertTrue(!ClawPain.active(p), "pain outlasted one second");
                            type.getField("requested").setBoolean(data, true);
                            WolverineCompat.tickBroken(p);
                            h.assertTrue(
                                    !type.getField("requested").getBoolean(data)
                                            && !type.getField("mainClaw").getBoolean(data),
                                    "extension bypassed broken timer");
                        } else {
                            WolverineCompat.tickBroken(p);
                            long elapsed = ClawSeverance.now(p) - deadline[0];
                            if (elapsed < 5) {
                                h.assertTrue(
                                        !type.getField("requested").getBoolean(data)
                                                && !type.getField("mainClaw").getBoolean(data),
                                        "自动伸爪，未等待再次按键");
                            }
                            if (elapsed == 5 || elapsed == 12 || elapsed == 20) {
                                type.getField("requested").setBoolean(data, true);
                                WolverineCompat.tickBroken(p);
                                h.assertTrue(ClawSeverance.growing(p) == 48, "按键未启动半速伸爪");
                            }
                            if (elapsed == 10) {
                                type.getField("requested").setBoolean(data, false);
                                WolverineCompat.tickBroken(p);
                                h.assertTrue(ClawSeverance.growing(p) == 0, "取消伸爪后仍在长出");
                            }
                            if (elapsed == 18) {
                                WolverineCompat.interruptGrowth(p);
                                h.assertTrue(
                                        ClawSeverance.growing(p) == 0
                                                && !type.getField("requested").getBoolean(data),
                                        "重进游戏中断后仍自动生长");
                            }
                            if (elapsed > 12 && elapsed < 68)
                                h.assertTrue(ClawSeverance.locked(p), "伸爪中提前解除攻击锁");
                            if (elapsed >= 68) {
                                WolverineCompat.tickBroken(p);
                                h.assertTrue(!ClawSeverance.locked(p), "手动生长后锁未解除");
                                h.assertTrue(
                                        type.getField("requested").getBoolean(data)
                                                && type.getField("mainClaw").getBoolean(data)
                                                && type.getField("offClaw").getBoolean(data),
                                        "手动生长完成未交回正常爪子");
                                GeneratedMod.LOG.info(
                                        "[COMPAT_PASS_R55] actual Wolverine: waits for key, 48-tick growth, cancel/retry, final handoff");
                                p.discard();
                                b.discard();
                                h.succeed();
                            }
                        }
                    } catch (ReflectiveOperationException ex) {
                        throw new RuntimeException(ex);
                    }
                });
    }
}
