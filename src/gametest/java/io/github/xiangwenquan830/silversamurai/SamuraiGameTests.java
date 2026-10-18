package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.*;
import java.util.*;

/** 仅Forge GameTest模式发现；普通游戏不生成测试实体、不运行这些测试。 */
@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SamuraiGameTests {
    static void floor(GameTestHelper h) {
        TestIsolation.begin(h);
        for (int x = 0; x < 24; x++)
            for (int z = 0; z < 24; z++) {
                h.setBlock(x, 0, z, Blocks.STONE);
                for (int y = 1; y <= 12; y++) h.setBlock(x, y, z, Blocks.AIR);
            }
        h.getLevel().setDayTime(18000);
        h.getLevel()
                .getGameRules()
                .getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)
                .set(false, h.getLevel().getServer());
        h.getLevel()
                .getGameRules()
                .getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION)
                .set(false, h.getLevel().getServer());
    }

    static SamuraiEntity boss(GameTestHelper h, double x, double z) {
        SamuraiEntity b = GeneratedMod.SAMURAI.get().create(h.getLevel());
        Vec3 p = h.absoluteVec(new Vec3(x, 1, z));
        b.moveTo(p.x, p.y, p.z, 0, 0);
        b.applyConfig(false);
        h.getLevel().addFreshEntity(b);
        return b;
    }

    static ServerPlayer player(GameTestHelper h, double x, double y, double z) {
        var level = h.getLevel();
        ServerPlayer p =
                new ServerPlayer(
                        level.getServer(),
                        level,
                        new com.mojang.authlib.GameProfile(UUID.randomUUID(), "samurai-test"));
        p.connection =
                new net.minecraft.server.network.ServerGamePacketListenerImpl(
                        level.getServer(),
                        new net.minecraft.network.Connection(
                                net.minecraft.network.protocol.PacketFlow.SERVERBOUND),
                        p) {
                    @Override
                    public void send(net.minecraft.network.protocol.Packet<?> packet) {}

                    @Override
                    public void send(
                            net.minecraft.network.protocol.Packet<?> packet,
                            net.minecraft.network.PacketSendListener listener) {}
                };
        Vec3 point = h.absoluteVec(new Vec3(x, y, z));
        p.moveTo(point.x, point.y, point.z, 0, 0);
        p.setGameMode(GameType.SURVIVAL);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        p.setHealth(1000);
        level.addNewPlayer(p);
        TestIsolation.readyPlayer(p);
        h.onEachTick(
                () -> {
                    if (!p.isRemoved()) p.doTick();
                });
        return p;
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "damage")
    public static void damage_caps(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 5.5, 5.5);
        b.setNoAi(true);
        float health = b.getHealth();
        b.hurt(b.damageSources().magic(), 10000);
        h.assertTrue(Math.abs(health - b.getHealth() - 30) < .001, "超高魔法伤害没有封顶30");
        b.invulnerableTime = 0;
        health = b.getHealth();
        var attacker = (Zombie) h.spawn(EntityType.ZOMBIE, new BlockPos(8, 1, 5));
        attacker.setNoAi(true);
        b.hurt(b.damageSources().mobAttack(attacker), 20);
        h.assertTrue(Math.abs(health - b.getHealth() - .2f) < .002, "普通20物理经99%减伤后不是0.2");
        b.testCombat(attacker, 2, false);
        b.applyConfig(true);
        b.invulnerableTime = 0;
        health = b.getHealth();
        b.hurt(b.damageSources().mobAttack(attacker), 20);
        h.assertTrue(Math.abs(health - b.getHealth() - 4) < .01, "40护甲后的4点伤害被上限放大或顺序错误");
        b.invulnerableTime = 0;
        health = b.getHealth();
        b.hurt(b.damageSources().mobAttack(attacker), 100000);
        h.assertTrue(Math.abs(health - b.getHealth() - 30) < .01, "二阶段护甲后没有最终封顶30");
        b.invulnerableTime = 0;
        b.hurt(b.damageSources().genericKill(), 100000);
        h.assertTrue(!b.isAlive(), "管理员清除被上限拦截");
        GeneratedMod.LOG.info("[实测通过] 最终事件上限30，小伤害0.2，管理员清除不受阻");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "red_rules")
    public static void red_shield_armor_cap(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 10.5, 7.5);
        b.setNoAi(true);
        ServerPlayer p = player(h, 10.5, 1, 10.6);
        p.getAttribute(Attributes.ARMOR).setBaseValue(30);
        p.setItemInHand(
                net.minecraft.world.InteractionHand.OFF_HAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHIELD));
        h.runAtTickTime(
                65,
                () -> {
                    p.setYRot(180);
                    p.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
                });
        h.runAtTickTime(
                75,
                () -> {
                    h.assertTrue(p.isBlocking(), "测试目标未举盾");
                    b.testCombat(p, 2, false);
                    b.setNoAi(true);
                    b.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1000);
                    float before = p.getHealth();
                    BladeCombat.damage(b, p, 0);
                    h.assertTrue(
                            Math.abs(before - p.getHealth() - BossConfig.OUTGOING_CAP.get()) < .01,
                            "红刀护甲/盾牌/配置输出上限结算不符");
                    h.assertTrue(
                            !p.isBlocking()
                                    && p.getPersistentData().getLong("SilverSamuraiShieldUntil")
                                            > h.getLevel().getGameTime(),
                            "红刀未禁盾");
                    GeneratedMod.LOG.info(
                            "[实测通过] 30护甲并举盾目标实际扣{}；红刀绕甲破盾且受独立输出上限约束", before - p.getHealth());
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "combo_a")
    public static void combo_a_static(GameTestHelper h) {
        combo(h, 1, false, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "combo_b")
    public static void combo_b_static(GameTestHelper h) {
        combo(h, 1, true, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "combo_red")
    public static void combo_red_static(GameTestHelper h) {
        combo(h, 2, false, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "combo_a_move")
    public static void combo_a_moving(GameTestHelper h) {
        combo(h, 1, false, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "combo_b_move")
    public static void combo_b_moving(GameTestHelper h) {
        combo(h, 1, true, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "combo_a_push")
    public static void combo_a_knockback(GameTestHelper h) {
        combo(h, 1, false, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "combo_b_push")
    public static void combo_b_knockback(GameTestHelper h) {
        combo(h, 1, true, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "combo_move")
    public static void combo_red_moving(GameTestHelper h) {
        combo(h, 2, false, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "combo_push")
    public static void combo_red_knockback(GameTestHelper h) {
        combo(h, 2, false, 2);
    }

    private static void combo(GameTestHelper h, int phase, boolean alt, int movement) {
        floor(h);
        SamuraiEntity b = boss(h, 10.5, 7.5);
        b.setNoAi(true);
        ServerPlayer p = player(h, 10.5, 1, 10.6);
        b.debug(true);
        // Static fixture must stay static even when damage applies knockback before the callback.
        if (movement == 0) p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        h.runAtTickTime(
                65,
                () -> {
                    b.setNoAi(false);
                    b.testCombat(p, phase, alt);
                    GeneratedMod.LOG.info(
                            "[combo fixture] phase={} movement={} boss={} target={} alive={} removed={} lane={}",
                            phase,
                            movement,
                            b.position(),
                            p.position(),
                            p.isAlive(),
                            p.isRemoved(),
                            CombatSpacing.lane(b, p));
                });
        h.onEachTick(
                () -> {
                    if (movement == 0) p.setDeltaMovement(Vec3.ZERO);
                    if (movement == 1 && h.getTick() > 65 && h.getTick() < 125)
                        p.move(MoverType.SELF, new Vec3(.035, 0, 0));
                    if (movement == 2 && (h.getTick() == 79 || h.getTick() == 95))
                        p.move(MoverType.SELF, new Vec3(0, 0, .6));
                    if (h.getTick() > 65 && b.state() == SamuraiEntity.RECOVER) b.setNoAi(true);
                });
        h.runAtTickTime(
                155,
                () -> {
                    int[] counts = b.hitCounts();
                    GeneratedMod.LOG.info(
                            "[实测连招] phase={} alt={} movement={} 命中={} 生命={} {}",
                            phase,
                            alt,
                            movement,
                            Arrays.toString(counts),
                            p.getHealth(),
                            b.report());
                    if (movement == 0)
                        h.assertTrue(
                                counts[0] == 1 && counts[1] == 1 && counts[2] == 1,
                                "静止目标三刀缺少实际扣血：" + b.report());
                    h.assertTrue(p.getHealth() < 1000, "整套连招没有实际扣血");
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 320, batch = "hostility")
    public static void bilateral_sleep(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 10.5, 10.5);
        ServerPlayer p = player(h, 10.5, 49, 10.5);
        p.setNoGravity(true);
        Zombie z = (Zombie) h.spawn(EntityType.ZOMBIE, new BlockPos(10, 1, 17));
        Skeleton s = (Skeleton) h.spawn(EntityType.SKELETON, new BlockPos(17, 1, 10));
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        z.setHealth(1000);
        s.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        s.setHealth(1000);
        boolean[] found = {false, false, false};
        h.onEachTick(
                () -> {
                    if (h.getTick() < 140) {
                        found[0] |= b.getTarget() == z || b.getTarget() == s;
                        found[1] |= z.getTarget() == b;
                        found[2] |= s.getTarget() == b;
                    }
                });
        h.runAtTickTime(
                140,
                () -> {
                    h.assertTrue(
                            found[0] && found[1] && found[2],
                            "无人手动攻击时双向索敌未建立：" + Arrays.toString(found));
                    p.setGameMode(GameType.CREATIVE);
                });
        h.runAtTickTime(
                215,
                () -> {
                    h.assertTrue(b.active() && b.weaponsDrawn(), "创造观察者或有效怪物存在时提前休眠收刀");
                    p.setGameMode(GameType.SURVIVAL);
                });
        h.runAtTickTime(
                290,
                () -> {
                    h.assertTrue(b.active() && b.getTarget() != null, "重新激活后未恢复索敌");
                    GeneratedMod.LOG.info("[实测通过] 僵尸、骷髅和Boss双向主动索敌；无人激活休眠与再次激活");
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "navigation")
    public static void real_collision_routes(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 3.5, 10.5);
        b.setNoAi(true);
        for (int x = 8; x < 20; x++)
            for (int z = 0; z < 24; z++)
                for (int y = 1; y <= 3; y++) h.setBlock(x, y, z, Blocks.STONE);
        Vec3 target = h.absoluteVec(new Vec3(14.5, 4, 10.5));
        RoutePlanner route =
                new RoutePlanner(
                        b.terrainNavigation(),
                        SamuraiNavigation.point(b.position()),
                        SamuraiNavigation.point(target),
                        new RoutePlanner.Settings(3, 3, 4, 0, 900, 2));
        while (!route.done()) route.step(12);
        h.assertTrue(
                route.result() != null
                        && route.result().stream()
                                .anyMatch(e -> e.kind() == RoutePlanner.Kind.CLIMB),
                "真实方块世界中未找到3格平台攀爬路线");
        h.assertTrue(
                !b.terrainNavigation()
                        .fits(SamuraiNavigation.point(h.absoluteVec(new Vec3(10.5, 1, 10.5)))),
                "实体完整碰撞箱没有挡住实心墙");
        GeneratedMod.LOG.info("[实测通过] 真实方块碰撞规划3格平台，完整碰撞箱拒绝实心墙，展开={}", route.expanded());
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 600, batch = "climb_motion")
    public static void climb_movement(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 3.5, 10.5);
        ServerPlayer p = player(h, 15.5, 4, 10.5);
        for (int x = 8; x < 24; x++)
            for (int z = 0; z < 24; z++)
                for (int y = 1; y <= 3; y++) h.setBlock(x, y, z, Blocks.STONE);
        boolean[] climbed = {false};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    climbed[0] |=
                            b.state() == SamuraiEntity.CLIMB || b.state() == SamuraiEntity.WALL;
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "攀爬执行时进入实体方块");
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            climbed[0] && b.getY() >= p.getY() - .1 && b.distanceToSqr(p) < 20,
                            "未完成真实3格平台登顶：" + b.report() + "，位置=" + b.position());
                    GeneratedMod.LOG.info("[实测通过] 完整AI从低处规划并机械攀爬3格平台，途中未穿墙");
                    p.discard();
                    b.discard();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 850, batch = "clear_motion")
    public static void red_clear_corridor(GameTestHelper h) {
        floor(h);
        for (int i = 0; i < 24; i++)
            for (int y = 1; y <= 8; y++) {
                h.setBlock(0, y, i, Blocks.BEDROCK);
                h.setBlock(23, y, i, Blocks.BEDROCK);
                h.setBlock(i, y, 0, Blocks.BEDROCK);
                h.setBlock(i, y, 23, Blocks.BEDROCK);
            }
        SamuraiEntity b = boss(h, 3.5, 10.5);
        ServerPlayer p = player(h, 14.5, 1, 10.5);
        b.testCombat(p, 2, false);
        b.setNoAi(true);
        h.runAtTickTime(
                5,
                () -> {
                    b.testPursuit(p);
                    for (int x = 8; x <= 9; x++)
                        for (int z = 0; z < 24; z++)
                            for (int y = 1; y <= 7; y++) h.setBlock(x, y, z, Blocks.STONE);
                    b.setNoAi(false);
                });
        boolean[] cleared = {false};
        b.debug(true);
        h.onEachTick(
                () -> {
                    if (h.getTick() % 40 == 0)
                        GeneratedMod.LOG.info("[清障实测进度] {} 位置={}", b.report(), b.position());
                    p.setDeltaMovement(Vec3.ZERO);
                    cleared[0] |= b.state() == SamuraiEntity.CLEAR;
                    for (int x = 0; x < 24; x++)
                        for (int z = 0; z < 24; z++)
                            h.assertTrue(
                                    h.getBlockState(new BlockPos(x, 0, z)).is(Blocks.STONE),
                                    "清障破坏了必要地板");
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            cleared[0] && b.getX() > h.absoluteVec(new Vec3(11, 1, 10.5)).x,
                            "尚未清出通道：" + b.report() + "，位置=" + b.position());
                    GeneratedMod.LOG.info("[实测通过] 红刀逐刀清出完整体型通道并通过，地板保留");
                    p.discard();
                    b.discard();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 80, batch = "blocked")
    public static void protected_low_narrow(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 3.5, 10.5);
        b.setNoAi(true);
        h.setBlock(7, 1, 10, Blocks.BEDROCK);
        h.setBlock(8, 1, 10, Blocks.CHEST);
        h.assertFalse(
                BladeCombat.breakable(b, h.absolutePos(new BlockPos(7, 1, 10)), true), "基岩允许破坏");
        h.assertFalse(
                BladeCombat.breakable(b, h.absolutePos(new BlockPos(8, 1, 10)), true), "容器允许破坏");
        for (int z = 4; z < 18; z++) {
            h.setBlock(10, 1, z, Blocks.STONE);
            h.setBlock(12, 1, z, Blocks.STONE);
        }
        h.assertFalse(
                b.terrainNavigation()
                        .fits(SamuraiNavigation.point(h.absoluteVec(new Vec3(11.5, 1, 10.5)))),
                "穿过1格宽通道");
        for (int x = 2; x < 6; x++) for (int z = 2; z < 6; z++) h.setBlock(x, 4, z, Blocks.STONE);
        h.assertFalse(
                b.terrainNavigation()
                        .fits(SamuraiNavigation.point(h.absoluteVec(new Vec3(3.5, 1, 3.5)))),
                "穿过3格高通道");
        h.assertFalse(
                b.terrainNavigation()
                        .support(SamuraiNavigation.point(h.absoluteVec(new Vec3(3.5, 7, 3.5)))),
                "悬空被视为落点");
        GeneratedMod.LOG.info("[实测通过] 真实碰撞拒绝窄道、低顶与悬空落点；基岩和容器禁止破坏");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "protected_event")
    public static void protection_event(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 3.5, 10.5);
        b.setNoAi(true);
        BlockPos pos = h.absolutePos(new BlockPos(6, 1, 10));
        h.setBlock(6, 1, 10, Blocks.STONE);
        java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> deny =
                e -> {
                    if (e.getPos().equals(pos)) e.setCanceled(true);
                };
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(deny);
        try {
            h.assertFalse(BladeCombat.breakable(b, pos, true), "未服从保护事件取消");
            h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.STONE), "保护方块被删除");
        } finally {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(deny);
        }
        GeneratedMod.LOG.info("[实测通过] Forge保护事件取消时清障拒绝破坏");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "two_players")
    public static void reachable_target(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 5.5, 10.5);
        ServerPlayer high = player(h, 8.5, 10, 10.5);
        high.setNoGravity(true);
        ServerPlayer reachable = player(h, 17.5, 1, 10.5);
        h.succeedWhen(
                () -> {
                    h.assertTrue(b.getTarget() == reachable, "未优先选择同层可接近目标：" + b.report());
                    GeneratedMod.LOG.info("[实测通过] 两名玩家中选择同层可达者而不是高处悬空者");
                    high.discard();
                    reachable.discard();
                    b.discard();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 150, batch = "budget")
    public static void multi_boss_budget(GameTestHelper h) {
        floor(h);
        List<SamuraiEntity> bosses = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            SamuraiEntity b = boss(h, 2.5 + (i % 4) * 2, 2.5 + (i / 4) * 2);
            b.setNoAi(true);
            bosses.add(b);
        }
        long[] maxNanos = {0}, total = {0}, maxNodes = {0};
        h.onEachTick(
                () -> {
                    long start = System.nanoTime();
                    for (SamuraiEntity b : bosses)
                        b.terrainNavigation().chase(h.absoluteVec(new Vec3(20.5, 1, 20.5)), 1);
                    long time = System.nanoTime() - start;
                    maxNanos[0] = Math.max(maxNanos[0], time);
                    total[0] += time;
                    maxNodes[0] =
                            Math.max(maxNodes[0], SamuraiNavigation.spentThisTick(h.getLevel()));
                    h.assertTrue(
                            SamuraiNavigation.spentThisTick(h.getLevel())
                                    <= BossConfig.NAV_WORLD.get(),
                            "16只Boss超出共享预算");
                });
        h.runAtTickTime(
                100,
                () -> {
                    GeneratedMod.LOG.info(
                            "[实测预算] 16只Boss 100刻，路线处理平均毫秒={}，最高毫秒={}，最大展开节点={}",
                            total[0] / 100000000.0,
                            maxNanos[0] / 1000000.0,
                            maxNodes[0]);
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 180, batch = "platforms")
    public static void platform_heights(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 3.5, 10.5);
        b.setNoAi(true);
        for (int height = 1; height <= 5; height++) {
            for (int x = 8; x < 24; x++)
                for (int z = 0; z < 24; z++)
                    for (int y = 1; y <= 5; y++)
                        h.setBlock(x, y, z, y <= height ? Blocks.STONE : Blocks.AIR);
            Vec3 destination = h.absoluteVec(new Vec3(15.5, height + 1, 10.5));
            RoutePlanner route =
                    new RoutePlanner(
                            b.terrainNavigation(),
                            SamuraiNavigation.point(b.position()),
                            SamuraiNavigation.point(destination),
                            new RoutePlanner.Settings(3, 3, 4, 0, 900, 2));
            while (!route.done()) route.step(12);
            h.assertTrue((route.result() != null) == (height <= 4), "真实世界平台高度判断错误：" + height);
        }
        GeneratedMod.LOG.info("[实测通过] 真实方块世界1～4格平台可规划，默认4格攀爬上限下5格平台暂不可达");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 600, batch = "vault")
    public static void fence_vault(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 3.5, 10.5);
        ServerPlayer p = player(h, 15.5, 1, 10.5);
        for (int z = 0; z < 24; z++) h.setBlock(8, 1, z, Blocks.OAK_FENCE);
        boolean[] vaulted = {false};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    vaulted[0] |= b.state() == SamuraiEntity.VAULT || b.pursuitJump().airborne();
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "翻越期间进入方块");
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            vaulted[0] && b.getX() > h.absoluteVec(new Vec3(10, 1, 10.5)).x,
                            "未翻过栏杆：" + b.report());
                    GeneratedMod.LOG.info("[实测通过] 真实AI翻越1.5格栏杆，整段位移未穿方块");
                    p.discard();
                    b.discard();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 80, batch = "switches")
    public static void clearing_switches(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 3.5, 10.5);
        b.setNoAi(true);
        var target = (Zombie) h.spawn(EntityType.ZOMBIE, new BlockPos(15, 1, 10));
        target.setNoAi(true);
        boolean active = BossConfig.CLEAR.get(),
                terrain = BossConfig.TERRAIN.get(),
                grief = BossConfig.GRIEFING.get();
        var rule =
                h.getLevel()
                        .getGameRules()
                        .getRule(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        boolean original = rule.get();
        try {
            h.assertFalse(b.terrainNavigation().canClear(), "一阶段获得主动清障");
            b.testCombat(target, 2, false);
            BossConfig.CLEAR.set(false);
            h.assertFalse(b.terrainNavigation().canClear(), "单独关闭清障无效");
            BossConfig.CLEAR.set(true);
            BossConfig.TERRAIN.set(false);
            h.assertFalse(b.terrainNavigation().canClear(), "总地形破坏关闭无效");
            BossConfig.TERRAIN.set(true);
            BossConfig.GRIEFING.set(true);
            rule.set(false, h.getLevel().getServer());
            h.assertFalse(b.terrainNavigation().canClear(), "不遵守mobGriefing");
        } finally {
            BossConfig.CLEAR.set(active);
            BossConfig.TERRAIN.set(terrain);
            BossConfig.GRIEFING.set(grief);
            rule.set(original, h.getLevel().getServer());
        }
        GeneratedMod.LOG.info("[实测通过] 一阶段无清障，独立开关、总开关、mobGriefing分别关闭均停止主动破坏");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "stairs")
    public static void stairs_up_down(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 3.5, 10.5);
        b.setNoAi(true);
        for (int x = 8; x < 24; x++)
            for (int z = 0; z < 24; z++)
                for (int y = 1; y <= Math.min(4, x - 7); y++) h.setBlock(x, y, z, Blocks.STONE);
        Vec3 top = h.absoluteVec(new Vec3(16.5, 5, 10.5));
        var settings = new RoutePlanner.Settings(3, 3, 4, 0, 900, 0);
        var up =
                new RoutePlanner(
                        b.terrainNavigation(),
                        SamuraiNavigation.point(b.position()),
                        SamuraiNavigation.point(top),
                        settings);
        while (!up.done()) up.step(12);
        h.assertTrue(up.result() != null, "真实4格楼梯无法正常上行");
        var down =
                new RoutePlanner(
                        b.terrainNavigation(),
                        SamuraiNavigation.point(top),
                        SamuraiNavigation.point(b.position()),
                        settings);
        while (!down.done()) down.step(12);
        h.assertTrue(down.result() != null, "真实4格楼梯无法安全下行");
        var high = (Zombie) h.spawn(EntityType.ZOMBIE, new BlockPos(3, 7, 10));
        high.setNoAi(true);
        high.setNoGravity(true);
        h.assertFalse(b.canStartAttack(high), "隔高差仍允许原地空挥");
        GeneratedMod.LOG.info("[实测通过] 真实4格楼梯上行下行，禁止对头顶6格目标发起原地连招");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 500, batch = "spacing_close")
    public static void spacing_close(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 10.5, 10.5);
        ServerPlayer p = player(h, 10.5, 1, 11.9);
        b.testPursuit(p);
        boolean[] stepped = {false}, attacked = {false};
        double[] previous = {b.getZ()};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    stepped[0] |=
                            b.state() == SamuraiEntity.FOOTWORK
                                    || (b.state() == SamuraiEntity.STOMP && b.focusedKick());
                    attacked[0] |= java.util.Arrays.stream(b.hitCounts()).sum() > 0;
                    h.assertTrue(Math.abs(b.getZ() - previous[0]) < .4, "贴脸后撤出现大段倒滑");
                    previous[0] = b.getZ();
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)), "后撤进入墙体");
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(stepped[0] && attacked[0], "贴脸尚未通过短踢或后撤腾出刀路并实际斩中：" + b.report());
                    GeneratedMod.LOG.info("[本轮实测通过] 贴脸腾出距离后实际刀路命中，最大每刻位移受限");
                    b.discard();
                    p.discard();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 280, batch = "turn_behind")
    public static void turn_behind(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 12.5, 12.5);
        ServerPlayer p = player(h, 12.5, 1, 9.4);
        b.testPursuit(p);
        float[] yaw = {b.getYRot()};
        boolean[] step = {false};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    float delta =
                            Math.abs(net.minecraft.util.Mth.wrapDegrees(b.getYRot() - yaw[0]));
                    h.assertTrue(delta <= 7.1, "侧后方目标导致瞬间大角度转身：" + delta);
                    yaw[0] = b.getYRot();
                    step[0] |= b.state() == SamuraiEntity.FOOTWORK;
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            step[0]
                                    && (b.state() == SamuraiEntity.ATTACK_A
                                            || b.state() == SamuraiEntity.ATTACK_B),
                            "尚未完成脚步转身再出刀：" + b.report());
                    GeneratedMod.LOG.info("[本轮实测通过] 背后目标先分段转身步，每刻朝向不超过7.1度，再起手");
                    p.discard();
                    b.discard();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "spacing_safety")
    public static void spacing_safety(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        Vec3 from = b.position();
        for (int x = 8; x < 13; x++) for (int y = 1; y < 6; y++) h.setBlock(x, y, 8, Blocks.STONE);
        h.assertFalse(b.terrainNavigation().safeStep(from, from.add(0, 0, -2)), "短后撤穿墙");
        for (int x = 8; x < 13; x++) for (int z = 12; z < 16; z++) h.setBlock(x, 0, z, Blocks.AIR);
        h.assertFalse(b.terrainNavigation().safeStep(from, from.add(0, 0, 3)), "短步退下悬崖");
        var z = (Zombie) h.spawn(EntityType.ZOMBIE, new BlockPos(13, 1, 10));
        z.setNoAi(true);
        var high = (Zombie) h.spawn(EntityType.ZOMBIE, new BlockPos(10, 6, 10));
        high.setNoAi(true);
        high.setNoGravity(true);
        // Require a live, indexed obstacle before testing entity avoidance; newly generated
        // arena chunks need not expose entities to world queries during the spawn callback.
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            z.isAlive()
                                    && h.getLevel()
                                            .getEntitiesOfClass(
                                                    LivingEntity.class,
                                                    b.getBoundingBox().inflate(4))
                                            .contains(z),
                            "实体避让夹具尚未进入世界查询列表");
                    h.assertFalse(
                            b.terrainNavigation().safeStep(from, from.add(3, 0, 0)), "短步穿过其他实体");
                    h.assertFalse(b.canStartAttack(high), "对楼板上方目标空挥");
                    GeneratedMod.LOG.info("[本轮实测通过] 逐段短步拒绝墙体、悬崖和实体；高差攻击拒绝");
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 210, batch = "target_loss")
    public static void target_loss(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 10.5, 7.5);
        b.setNoAi(true);
        ServerPlayer p = player(h, 10.5, 1, 10.6), other = player(h, 10.5, 1, 3.8);
        boolean[] lost = {false}, recovered = {false};
        float[] locked = {0};
        h.runAtTickTime(
                65,
                () -> {
                    b.setNoAi(false);
                    b.testCombat(p, 2, false);
                });
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    other.setDeltaMovement(Vec3.ZERO);
                    if (!lost[0]
                            && b.state() == SamuraiEntity.ATTACK_A
                            && b.age(0) >= .44
                            && b.age(0) < .62) {
                        locked[0] = b.getYRot();
                        p.setGameMode(GameType.CREATIVE);
                        lost[0] = true;
                    }
                    if (lost[0] && b.state() == SamuraiEntity.ATTACK_A) {
                        h.assertTrue(b.getTarget() != other, "有效挥砍中切换背后目标");
                        h.assertTrue(
                                Math.abs(
                                                net.minecraft.util.Mth.wrapDegrees(
                                                        b.getYRot() - locked[0]))
                                        < .01,
                                "目标失效后当前刀突然转向");
                    }
                    if (lost[0] && b.state() == SamuraiEntity.RECOVER) {
                        recovered[0] = true;
                        b.setNoAi(true);
                    }
                });
        h.runAtTickTime(
                150,
                () -> {
                    h.assertTrue(lost[0] && recovered[0], "目标失效未安全收势");
                    h.assertTrue(other.getHealth() == 1000, "原刀误伤背后新目标");
                    GeneratedMod.LOG.info("[本轮实测通过] 目标失效时锁方向结束当前窗口，再平滑收势；背后另一目标未被切刀");
                    p.discard();
                    other.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 180, batch = "dodge_lock")
    public static void dodge_lock(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 10.5, 7.5);
        b.setNoAi(true);
        ServerPlayer p = player(h, 10.5, 1, 10.6);
        float[] yaw = {Float.NaN};
        boolean[] checked = {false};
        h.runAtTickTime(
                65,
                () -> {
                    b.setNoAi(false);
                    b.testCombat(p, 2, false);
                });
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    if (b.state() == SamuraiEntity.ATTACK_A
                            && b.age(0) >= .424
                            && b.age(0) <= .625) {
                        if (Float.isNaN(yaw[0])) {
                            yaw[0] = b.getYRot();
                            p.move(MoverType.SELF, new Vec3(1.7, 0, 0));
                        } else {
                            h.assertTrue(
                                    Math.abs(
                                                    net.minecraft.util.Mth.wrapDegrees(
                                                            b.getYRot() - yaw[0]))
                                            < .01,
                                    "有效刀路中追着横闪转向");
                            checked[0] = true;
                        }
                    }
                    // End at this strike's boundary: faster cadence can start a second combo before
                    // tick 145.
                    if (!Float.isNaN(yaw[0]) && b.age(0) > .625) {
                        h.assertTrue(checked[0], "未采到出刀锁方向窗口");
                        GeneratedMod.LOG.info("[本轮实测通过] 出刀后目标横闪1.7格，方向不追踪；允许真实躲刀");
                        p.discard();
                        b.discard();
                        h.succeed();
                    }
                });
    }

    @GameTest(template = "arena", timeoutTicks = 80, batch = "reload_safe")
    public static void reload_safe(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 10.5, 7.5);
        b.setNoAi(true);
        ServerPlayer p = player(h, 10.5, 1, 10.6);
        b.testCombat(p, 2, false);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        b.addAdditionalSaveData(tag);
        SamuraiEntity copy = boss(h, 4.5, 4.5);
        copy.setNoAi(true);
        copy.readAdditionalSaveData(tag);
        h.assertTrue(
                copy.phase() == 2
                        && copy.getTarget() == null
                        && copy.state() == SamuraiEntity.RECOVER,
                "活体重载保留旧攻击或失效目标");
        for (int i = 0; i < 8; i++) h.assertTrue(copy.shed(i), "重载恢复掉落零件");
        h.assertTrue(Arrays.equals(copy.hitCounts(), new int[3]), "重载继承旧命中记录");
        GeneratedMod.LOG.info("[本轮实测通过] 攻击中保存后新实体读取，二阶段安全待机、无旧目标和旧刀伤害，零件不恢复");
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "reaction")
    public static void reaction(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        ServerPlayer p = player(h, 15.5, 1, 10.5);
        float before = b.getHealth();
        b.hurt(b.damageSources().magic(), 10);
        h.assertTrue(b.reactionActive(), "有效伤害未产生反馈");
        b.testCombat(p, 2, false);
        b.invulnerableTime = 0;
        b.hurt(b.damageSources().magic(), 10);
        h.assertFalse(b.reactionActive(), "攻击状态被受击动作接管");
        h.assertTrue(b.state() == SamuraiEntity.ATTACK_A, "受击取消连招");
        GeneratedMod.LOG.info(
                "[本轮实测通过] 实际扣血触发局部受力；进入连招清除反馈，高优先级不被打断，初次扣血={}", before - b.getHealth() - 10);
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "preview_one")
    public static void preview_one(GameTestHelper h) {
        movingPreview(h, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "preview_two")
    public static void preview_two(GameTestHelper h) {
        movingPreview(h, 2);
    }

    private static void movingPreview(GameTestHelper h, int phase) {
        floor(h);
        SamuraiEntity b = boss(h, 10.5, 9.5);
        b.setNoAi(true);
        ServerPlayer p = player(h, 10.5, 1, 10.9);
        b.testCombat(p, phase, false);
        b.testPursuit(p);
        b.setNoAi(true);
        CombatTrace trace =
                new CombatTrace("phase" + phase, h.absoluteVec(new Vec3(10.5, 1, 10.5)));
        boolean[] turning = {false}, attack = {false}, walk = {false};
        double[] maxSpeed = {0};
        float[] oldYaw = {b.getYRot()};
        h.runAtTickTime(65, () -> b.setNoAi(false));
        h.onEachTick(
                () -> {
                    long t = h.getTick();
                    p.setDeltaMovement(Vec3.ZERO);
                    if (t >= 65 && t < 145) {
                        /* 初始贴近，目标保持位置供短后撤。 */
                    } else if (t >= 145 && t < 205) {
                        double a = (t - 145) / 60.0 * Math.PI;
                        Vec3 center = h.absoluteVec(new Vec3(10.5, 1, 9.5));
                        p.setPos(
                                center.x + Math.sin(a) * 3.1,
                                center.y,
                                center.z + Math.cos(a) * 3.1);
                    } else if (t == 230) p.setPos(h.absoluteVec(new Vec3(20.5, 1, 5.5)));
                    else if (t > 265 && t < 310) p.move(MoverType.SELF, new Vec3(-.075, 0, .045));
                    else if (t == 355) p.setPos(h.absoluteVec(new Vec3(12.5, 1, 19.5)));
                    else if (t > 400 && t < 445) p.move(MoverType.SELF, new Vec3(.045, 0, -.035));
                    if (t >= 65) {
                        turning[0] |= b.state() == SamuraiEntity.FOOTWORK;
                        attack[0] |=
                                b.state() == SamuraiEntity.ATTACK_A
                                        || b.state() == SamuraiEntity.ATTACK_B;
                        walk[0] |= b.walkingSpeed() > .1;
                        maxSpeed[0] = Math.max(maxSpeed[0], b.walkingSpeed());
                        h.assertTrue(
                                Math.abs(
                                                net.minecraft.util.Mth.wrapDegrees(
                                                        b.getYRot() - oldYaw[0]))
                                        < 8.1,
                                "移动实测突然转向 tick="
                                        + t
                                        + " before="
                                        + oldYaw[0]
                                        + " after="
                                        + b.getYRot()
                                        + " state="
                                        + b.state());
                        trace.frame(b, p, (int) t - 65);
                    }
                    oldYaw[0] = b.getYRot();
                });
        h.runAtTickTime(
                580,
                () -> {
                    trace.close();
                    h.assertTrue(
                            turning[0] && attack[0] && walk[0], "移动目标场景缺少站位步/出刀/追击：" + b.report());
                    GeneratedMod.LOG.info(
                            "[本轮移动目标实测] 阶段={}，站位步={}，攻击={}，追击={}，最高追击速度={}，{}",
                            phase,
                            turning[0],
                            attack[0],
                            walk[0],
                            maxSpeed[0],
                            b.report());
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 150, batch = "reaction_cooldown")
    public static void reaction_cooldown(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        var z = (Zombie) h.spawn(EntityType.ZOMBIE, new BlockPos(14, 1, 10));
        z.setNoAi(true);
        b.hurt(b.damageSources().mobAttack(z), 20);
        h.assertTrue(b.reactionPower() <= .11f, "99%减伤的小伤害触发了强受力");
        long stamp = b.reactionStarted();
        h.runAtTickTime(
                2,
                () -> {
                    b.invulnerableTime = 0;
                    b.hurt(b.damageSources().magic(), 25);
                    h.assertTrue(b.reactionStarted() == stamp, "反馈冷却内重启造成抽搐");
                });
        h.runAtTickTime(
                30,
                () -> {
                    b.invulnerableTime = 0;
                    b.hurt(b.damageSources().magic(), 10);
                    h.assertTrue(
                            b.reactionStarted() > stamp && b.reactionPower() > .5, "冷却后有效重击未反馈");
                    var before = b.currentPose(0);
                    b.hurt(b.damageSources().genericKill(), 100000);
                    h.assertTrue(
                            b.state() == SamuraiEntity.DEAD && b.getTarget() == null,
                            "死亡未立即终止主动行为");
                    var after = b.currentPose(0);
                    for (int i = 0; i < before.matrices().length; i++)
                        h.assertTrue(
                                before.matrices()[i].equals(after.matrices()[i], .0001f),
                                "受力中死亡首帧跳变");
                    GeneratedMod.LOG.info("[本轮实测通过] 按最终扣血分轻重、24刻反馈冷却；受力中死亡保留首帧并立即停止主动行为");
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 190, batch = "stable_targets")
    public static void stable_targets(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 12.5, 12.5);
        ServerPlayer a = player(h, 12.5, 1, 16), c = player(h, 9, 1, 12.5);
        b.testPursuit(a);
        int[] switches = {0};
        LivingEntity[] last = {a};
        h.onEachTick(
                () -> {
                    a.setDeltaMovement(Vec3.ZERO);
                    c.setDeltaMovement(Vec3.ZERO);
                    if (h.getTick() % 8 == 0)
                        c.move(MoverType.SELF, new Vec3(0, 0, h.getTick() % 16 == 0 ? .1 : -.1));
                    if (b.getTarget() != null && b.getTarget() != last[0]) {
                        switches[0]++;
                        last[0] = b.getTarget();
                    }
                });
        h.runAtTickTime(
                150,
                () -> {
                    h.assertTrue(switches[0] == 0, "两方向目标距离微变造成频繁换目标");
                    GeneratedMod.LOG.info("[本轮实测通过] 两名无网络连接测试玩家不同方向，150刻轻微跑位，保持原目标0次切换");
                    a.discard();
                    c.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 220, batch = "acceleration")
    public static void acceleration(GameTestHelper h) {
        floor(h);
        SamuraiEntity b = boss(h, 3.5, 10.5);
        ServerPlayer p = player(h, 20.5, 1, 10.5);
        b.setYRot(-90);
        b.testPursuit(p);
        double[] last = {0}, peak = {0}, first = {0};
        int[] pauses = {0};
        boolean[] started = {false};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    double speed = b.walkingSpeed();
                    if (speed > 0 && !started[0]) {
                        first[0] = speed;
                        started[0] = true;
                    }
                    if (speed > last[0]) h.assertTrue(speed - last[0] <= .036, "起步瞬间达到最大速度");
                    if (started[0]
                            && b.state() == SamuraiEntity.CHASE
                            && b.distanceToSqr(p) > 36
                            && speed == 0) pauses[0]++;
                    peak[0] = Math.max(peak[0], speed);
                    last[0] = speed;
                });
        h.runAtTickTime(
                180,
                () -> {
                    h.assertTrue(started[0] && first[0] < .04 && peak[0] > .25, "未实际验证渐进起步");
                    h.assertTrue(pauses[0] < 5, "路径节点频繁停顿：" + pauses[0]);
                    GeneratedMod.LOG.info(
                            "[本轮实测通过] 平地起步首刻={}，最高={}，途中归零刻数={}", first[0], peak[0], pauses[0]);
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    private SamuraiGameTests() {}
}
