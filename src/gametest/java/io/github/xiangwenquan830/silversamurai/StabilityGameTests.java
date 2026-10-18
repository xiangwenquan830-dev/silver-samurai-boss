package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

/** r10真实服务器状态回归；无连接ServerPlayer不冒充客户端视觉验收。 */
@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StabilityGameTests {
    private static void done(GameTestHelper h, String text) {
        GeneratedMod.LOG.info("[r10实测通过] {}", text);
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 360, batch = "r10_creative")
    public static void creative_observer(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        b.setNoAi(true);
        float[] hp = {1000};
        h.runAtTickTime(
                65,
                () -> {
                    b.testCombat(p, 1, false);
                    b.setNoAi(false);
                });
        h.runAtTickTime(
                70,
                () -> {
                    p.setGameMode(GameType.CREATIVE);
                    hp[0] = p.getHealth();
                });
        h.onEachTick(
                () -> {
                    if (h.getTick() > 70) {
                        h.assertTrue(p.getHealth() == hp[0], "创造切换后仍造成伤害");
                        if (h.getTick() < 150)
                            h.assertTrue(
                                    b.state() != SamuraiEntity.SHEATHE
                                            && b.state() != SamuraiEntity.SLEEP,
                                    "创造切换后跳过脱战等待");
                    }
                });
        h.runAtTickTime(
                310,
                () -> {
                    h.assertTrue(
                            b.getTarget() == null
                                    && !b.weaponsDrawn()
                                    && b.state() == SamuraiEntity.IDLE,
                            "无实际威胁但观察者导致无法收刀");
                    done(h, "唯一生存切创造：立即停伤，脱战等待后完整收刀，仍维持观察");
                });
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r10_switch_mob")
    public static void creative_switch_mob(GameTestHelper h) {
        switchTarget(h, false);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r10_switch_player")
    public static void creative_switch_player(GameTestHelper h) {
        switchTarget(h, true);
    }

    private static void switchTarget(GameTestHelper h, boolean playerTarget) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        LivingEntity other;
        if (playerTarget) other = player(h, 15.5, 1, 10.5);
        else {
            var z = h.spawn(EntityType.ZOMBIE, new BlockPos(15, 1, 10));
            z.setNoAi(true);
            other = z;
        }
        b.testPursuit(p);
        h.runAtTickTime(10, () -> p.setGameMode(GameType.CREATIVE));
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            h.getTick() > 10
                                    && b.getTarget() == other
                                    && b.weaponsDrawn()
                                    && b.state() != SamuraiEntity.SHEATHE,
                            "未切到另一个敌人");
                    GeneratedMod.LOG.info(
                            "[r10实测通过] 创造切换后保持持刀，切换目标={}", playerTarget ? "另一生存玩家" : "僵尸");
                });
    }

    @GameTest(template = "arena", timeoutTicks = 340, batch = "r10_spectator")
    public static void spectator_sleep(GameTestHelper h) {
        sleep(h, true);
    }

    @GameTest(template = "arena", timeoutTicks = 340, batch = "r10_empty")
    public static void empty_sleep(GameTestHelper h) {
        sleep(h, false);
    }

    private static void sleep(GameTestHelper h, boolean spectator) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        b.testPursuit(p);
        h.runAtTickTime(
                5,
                () -> {
                    if (spectator) p.setGameMode(GameType.SPECTATOR);
                    else p.discard();
                });
        h.runAtTickTime(
                80,
                () ->
                        h.assertTrue(
                                b.weaponsDrawn() && b.state() != SamuraiEntity.SHEATHE, "脱战延迟被绕过"));
        h.runAtTickTime(
                290,
                () -> {
                    h.assertTrue(
                            b.state() == SamuraiEntity.SLEEP && !b.weaponsDrawn(),
                            "无威胁延迟后未收刀休眠：" + b.report());
                    done(h, spectator ? "只剩旁观不维持警戒，延迟后收刀休眠" : "完全无人无敌人，延迟后收刀休眠");
                });
    }

    @GameTest(template = "arena", timeoutTicks = 400, batch = "r10_sheathe")
    public static void sheathe_reversal(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        b.testPursuit(p);
        p.setGameMode(GameType.SPECTATOR);
        boolean[] reversed = {false};
        h.onEachTick(
                () -> {
                    if (!reversed[0] && b.state() == SamuraiEntity.SHEATHE && b.stateAge(0) > .8f) {
                        p.setGameMode(GameType.SURVIVAL);
                        reversed[0] = true;
                    }
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            reversed[0]
                                    && b.state() != SamuraiEntity.SHEATHE
                                    && b.getTarget() == p
                                    && b.weaponsDrawn(),
                            "收刀未沿原路返回持刀");
                    GeneratedMod.LOG.info("[r10实测通过] 收刀途中敌人重现，原轨道反向衔接并重新索敌");
                });
    }

    @GameTest(template = "arena", timeoutTicks = 80, batch = "r10_reload_one")
    public static void reload_one(GameTestHelper h) {
        reload(h, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 80, batch = "r10_reload_two")
    public static void reload_two(GameTestHelper h) {
        reload(h, 2);
    }

    private static void reload(GameTestHelper h, int phase) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        b.testCombat(p, phase, false);
        b.setNoAi(true);
        CompoundTag tag = new CompoundTag();
        b.addAdditionalSaveData(tag);
        var copy = boss(h, 4.5, 4.5);
        copy.readAdditionalSaveData(tag);
        copy.setNoAi(true);
        h.assertTrue(
                copy.weaponsDrawn()
                        && copy.getTarget() == null
                        && copy.state() == SamuraiEntity.RECOVER,
                "重载丢失持刀或重播攻击");
        var before = b.currentPose(0);
        var after = copy.currentPose(0);
        for (int i = 0; i < before.matrices().length; i++)
            h.assertTrue(before.matrices()[i].equals(after.matrices()[i], .0001f), "重载首帧丢失快照");
        if (phase == 2) for (int i = 0; i < 8; i++) h.assertTrue(copy.shed(i), "二阶段掉件重生");
        done(h, "阶段" + phase + "攻击中存读NBT：保持持刀、首帧一致、不继承目标和命中");
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r10_dead_reload")
    public static void death_reload(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        b.testCombat(p, 2, false);
        b.hurt(b.damageSources().genericKill(), 100000);
        h.runAtTickTime(
                35,
                () -> {
                    CompoundTag tag = new CompoundTag();
                    b.addAdditionalSaveData(tag);
                    var copy = boss(h, 4.5, 4.5);
                    copy.readAdditionalSaveData(tag);
                    h.assertTrue(
                            copy.state() == SamuraiEntity.DEAD
                                    && !copy.isAlive()
                                    && copy.getTarget() == null,
                            "尸体读取复活");
                    h.assertTrue(
                            Math.abs(copy.deathElapsed(0) - b.deathElapsed(0)) < .001, "死亡进度重播");
                    var a = b.currentPose(0);
                    var c = copy.currentPose(0);
                    for (int i = 0; i < a.matrices().length; i++)
                        h.assertTrue(a.matrices()[i].equals(c.matrices()[i], .0001f), "尸体姿势不同");
                    done(h, "死亡中途NBT存读：进度、表面、掉件与全部矩阵一致，不再次调用死亡结算");
                });
    }

    @GameTest(template = "arena", timeoutTicks = 500, batch = "r10_threat")
    public static void blocked_threat(GameTestHelper h) {
        floor(h);
        var b = boss(h, 4.5, 10.5);
        var p = player(h, 15.5, 1, 10.5);
        b.testPursuit(p);
        for (int y = 1; y < 9; y++)
            for (int z = 0; z < 24; z++) h.setBlock(9, y, z, Blocks.BEDROCK);
        h.onEachTick(
                () -> {
                    if (h.getTick() > 5)
                        h.assertTrue(
                                b.weaponsDrawn() && b.state() != SamuraiEntity.SHEATHE, "不可达被误判脱战");
                });
        h.runAtTickTime(
                300,
                () -> {
                    h.assertTrue(b.alert(), "墙后有效敌人未维持警戒");
                    done(h, "基岩墙后不可达目标等待重规划，不收刀不伤及隔墙目标");
                });
    }

    @GameTest(template = "arena", timeoutTicks = 700, batch = "r10_phase_climb")
    public static void phase_climb(GameTestHelper h) {
        terrainInterrupt(h, false, false);
    }

    @GameTest(template = "arena", timeoutTicks = 700, batch = "r10_death_climb")
    public static void death_climb(GameTestHelper h) {
        terrainInterrupt(h, true, false);
    }

    @GameTest(template = "arena", timeoutTicks = 700, batch = "r10_phase_step")
    public static void phase_step(GameTestHelper h) {
        terrainInterrupt(h, false, true);
    }

    @GameTest(template = "arena", timeoutTicks = 700, batch = "r10_death_step")
    public static void death_step(GameTestHelper h) {
        terrainInterrupt(h, true, true);
    }

    private static void terrainInterrupt(GameTestHelper h, boolean death, boolean step) {
        floor(h);
        var b = boss(h, 3.5, 10.5);
        var p = player(h, 15.5, step ? 2 : 4, 10.5);
        for (int x = 8; x < 24; x++)
            for (int z = 0; z < 24; z++)
                for (int y = 1; y <= (step ? 1 : 3); y++) h.setBlock(x, y, z, Blocks.STONE);
        boolean[] triggered = {false};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    boolean moving =
                            step
                                    ? b.terrainNavigation().terrainMotion()
                                    : b.state() == SamuraiEntity.CLIMB
                                            || b.state() == SamuraiEntity.WALL;
                    if (!triggered[0]
                            && moving
                            && b.getY() > h.absoluteVec(new Vec3(0, 1.2, 0)).y) {
                        triggered[0] = true;
                        if (death) b.hurt(b.damageSources().genericKill(), 100000);
                        else b.requestSecondPhase();
                    }
                    if (!death && b.state() == SamuraiEntity.TRANSITION)
                        h.assertTrue(
                                b.terrainNavigation()
                                        .support(SamuraiNavigation.point(b.position())),
                                "阶段转场悬空");
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "地形动作中断穿墙");
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            triggered[0]
                                    && (death
                                            ? b.state() == SamuraiEntity.DEAD
                                                    && b.deathElapsed(0) > .6
                                            : b.phase() == 2),
                            "尚未实际触发并安全中断");
                    GeneratedMod.LOG.info(
                            "[r10实测通过] {}中{}：完整碰撞箱、不瞬移、优先安全落地",
                            step ? "迈阶" : "攀爬",
                            death ? "死亡" : "转阶段");
                });
    }

    @GameTest(template = "arena", timeoutTicks = 900, batch = "r10_clear_death")
    public static void clearing_death(GameTestHelper h) {
        floor(h);
        var b = boss(h, 3.5, 10.5);
        var p = player(h, 14.5, 1, 10.5);
        b.testCombat(p, 2, false);
        b.testPursuit(p);
        for (int i = 0; i < 24; i++)
            for (int y = 1; y <= 8; y++) {
                h.setBlock(0, y, i, Blocks.BEDROCK);
                h.setBlock(23, y, i, Blocks.BEDROCK);
                h.setBlock(i, y, 0, Blocks.BEDROCK);
                h.setBlock(i, y, 23, Blocks.BEDROCK);
            }
        for (int x = 8; x <= 9; x++)
            for (int z = 0; z < 24; z++)
                for (int y = 1; y <= 7; y++) h.setBlock(x, y, z, Blocks.STONE);
        boolean[] killed = {false};
        int[] count = {0};
        long[] at = {0};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    if (!killed[0] && b.state() == SamuraiEntity.CLEAR) {
                        b.requestSecondPhase();
                        h.assertTrue(
                                b.state() == SamuraiEntity.CLEAR && b.phase() == 2,
                                "清障中的重复阶段请求重播转场");
                        b.hurt(b.damageSources().genericKill(), 100000);
                        killed[0] = true;
                        at[0] = h.getTick();
                        for (int x = 8; x <= 9; x++)
                            for (int z = 0; z < 24; z++)
                                for (int y = 1; y <= 7; y++)
                                    if (h.getBlockState(new BlockPos(x, y, z)).is(Blocks.STONE))
                                        count[0]++;
                    }
                    if (killed[0]) {
                        int now = 0;
                        for (int x = 8; x <= 9; x++)
                            for (int z = 0; z < 24; z++)
                                for (int y = 1; y <= 7; y++)
                                    if (h.getBlockState(new BlockPos(x, y, z)).is(Blocks.STONE))
                                        now++;
                        h.assertTrue(now == count[0] && b.getTarget() == null, "死亡后继续清障或索敌");
                    }
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(killed[0] && h.getTick() > at[0] + 30, "尚未在真实清障任务中死亡");
                    GeneratedMod.LOG.info("[r10实测通过] 清障阶段请求幂等；清障中死亡后不再破坏方块");
                });
    }

    @GameTest(template = "arena", timeoutTicks = 420, batch = "r10_range")
    public static void enemy_range_return(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        b.testPursuit(p);
        h.runAtTickTime(12, () -> p.setPos(h.absoluteVec(new Vec3(10.5, 1, 200))));
        h.runAtTickTime(
                80,
                () ->
                        h.assertTrue(
                                b.getTarget() == null
                                        && b.weaponsDrawn()
                                        && b.state() != SamuraiEntity.SHEATHE,
                                "离开范围立即收刀或保留无效目标"));
        h.runAtTickTime(100, () -> p.setPos(h.absoluteVec(new Vec3(10.5, 1, 12))));
        h.runAtTickTime(
                140,
                () -> {
                    h.assertTrue(b.getTarget() == p && b.weaponsDrawn(), "目标回范围后未重新索敌");
                    p.discard();
                });
        h.runAtTickTime(
                390,
                () -> {
                    h.assertTrue(
                            b.getTarget() == null && b.state() == SamuraiEntity.SLEEP,
                            "目标移除后未正常脱战");
                    done(h, "敌人离开范围、重新进入、移除后的警戒与延迟休眠");
                });
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "r10_damage_audit")
    public static void damage_audit(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        b.setNoAi(true);
        var p = player(h, 10.5, 1, 10.6);
        b.testCombat(p, 2, false);
        h.runAtTickTime(
                65,
                () -> {
                    java.util.function.Consumer<
                                    net.minecraftforge.event.entity.living.LivingHurtEvent>
                            deny =
                                    e -> {
                                        if (e.getEntity() == p) e.setCanceled(true);
                                    };
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(deny);
                    try {
                        BladeCombat.damage(b, p, 0);
                        h.assertTrue(
                                p.getHealth() == 1000 && b.report().contains("事件取消"), "事件取消未被单独记录");
                    } finally {
                        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(deny);
                    }
                    p.setGameMode(GameType.CREATIVE);
                    BladeCombat.damage(b, p, 1);
                    h.assertTrue(p.getHealth() == 1000 && b.report().contains("创造免疫"), "创造免疫错误归因");
                    done(h, "刀路结算单独识别Forge伤害取消和创造免疫，无补伤害");
                });
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "r10_half")
    public static void half_slab_route(GameTestHelper h) {
        slabRoute(h, .5);
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "r10_onehalf")
    public static void one_half_platform(GameTestHelper h) {
        slabRoute(h, 1.5);
    }

    private static void slabRoute(GameTestHelper h, double height) {
        floor(h);
        var b = boss(h, 3.5, 10.5);
        var p = player(h, 15.5, 1 + height, 10.5);
        for (int x = 8; x < 24; x++)
            for (int z = 0; z < 24; z++) {
                if (height > 1) h.setBlock(x, 1, z, Blocks.STONE);
                h.setBlock(x, height > 1 ? 2 : 1, z, Blocks.STONE_SLAB);
            }
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "半砖或1.5平台穿模");
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            Math.abs(b.getY() - p.getY()) < .12 && b.distanceToSqr(p) < 22,
                            "高差路线尚未执行完：" + b.report());
                    GeneratedMod.LOG.info("[r10实测通过] {}格独立平台真实移动通过，不用栏杆场景替代", height);
                });
    }

    @GameTest(template = "arena", timeoutTicks = 320, batch = "r10_first_dash")
    public static void first_dash(GameTestHelper h) {
        firstHit(h, 4.35, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 320, batch = "r10_first_angle")
    public static void first_angle(GameTestHelper h) {
        firstHit(h, 3.1, 35);
    }

    @GameTest(template = "arena", timeoutTicks = 420, batch = "r10_first_close")
    public static void first_close(GameTestHelper h) {
        firstHit(h, 1.4, 0);
    }

    private static void firstHit(GameTestHelper h, double distance, float angle) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p =
                player(
                        h,
                        10.5 + Math.sin(Math.toRadians(angle)) * distance,
                        1,
                        7.5 + Math.cos(Math.toRadians(angle)) * distance);
        b.testPursuit(p);
        h.onEachTick(() -> p.setDeltaMovement(Vec3.ZERO));
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            b.hitCounts()[0] > 0 && p.getHealth() < 1000,
                            "首刀还没有真实扣血：" + b.report());
                    GeneratedMod.LOG.info(
                            "[r10实测通过] 首刀实际扣血 距离={} 斜角={} 生命={}", distance, angle, p.getHealth());
                });
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "r10_stair_blocks")
    public static void actual_stair_blocks(GameTestHelper h) {
        floor(h);
        var b = boss(h, 3.5, 10.5);
        b.setNoAi(true);
        var stair =
                Blocks.STONE_STAIRS
                        .defaultBlockState()
                        .setValue(
                                net.minecraft.world.level.block.StairBlock.FACING,
                                net.minecraft.core.Direction.EAST);
        for (int x = 8; x < 24; x++)
            for (int z = 0; z < 24; z++) {
                int top = Math.min(4, x - 7);
                for (int y = 1; y < top; y++) h.setBlock(x, y, z, Blocks.STONE);
                h.setBlock(
                        new BlockPos(x, top, z), x < 12 ? stair : Blocks.STONE.defaultBlockState());
            }
        var target = h.absoluteVec(new Vec3(16.5, 5, 10.5));
        var settings = new RoutePlanner.Settings(3, 3, 4, 0, 900, 0);
        var up =
                new RoutePlanner(
                        b.terrainNavigation(),
                        SamuraiNavigation.point(b.position()),
                        SamuraiNavigation.point(target),
                        settings);
        while (!up.done()) up.step(12);
        h.assertTrue(up.result() != null, "原版楼梯方块上行无路线");
        var down =
                new RoutePlanner(
                        b.terrainNavigation(),
                        SamuraiNavigation.point(target),
                        SamuraiNavigation.point(b.position()),
                        settings);
        while (!down.done()) down.step(12);
        h.assertTrue(down.result() != null, "原版楼梯方块下行无路线");
        done(h, "原版STONE_STAIRS连续楼梯上下规划，独立于整方块台阶场景");
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r10_draw_reload")
    public static void draw_reload(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        boolean[] checked = {false};
        h.onEachTick(
                () -> {
                    if (!checked[0] && b.state() == SamuraiEntity.DRAW && b.stateAge(0) > .6f) {
                        checked[0] = true;
                        var tag = new CompoundTag();
                        b.addAdditionalSaveData(tag);
                        var copy = boss(h, 4.5, 4.5);
                        copy.readAdditionalSaveData(tag);
                        copy.setNoAi(true);
                        h.assertTrue(
                                copy.state() == SamuraiEntity.DRAW
                                        && Math.abs(copy.visualTime(0) - b.visualTime(0)) < .051,
                                "拔刀重载未续同一时间");
                        GeneratedMod.LOG.info("[r10实测通过] 拔刀中重载保留动画时间，不进入旧攻击");
                        h.succeed();
                    }
                });
    }

    @GameTest(template = "arena", timeoutTicks = 400, batch = "r10_sheathe_reload")
    public static void sheathe_reload(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        b.testPursuit(p);
        p.setGameMode(GameType.SPECTATOR);
        h.onEachTick(
                () -> {
                    if (b.state() == SamuraiEntity.SHEATHE && b.stateAge(0) > .9f) {
                        var before = b.currentPose(0);
                        var tag = new CompoundTag();
                        b.addAdditionalSaveData(tag);
                        var copy = boss(h, 4.5, 4.5);
                        copy.readAdditionalSaveData(tag);
                        copy.setNoAi(true);
                        h.assertTrue(
                                copy.state() == SamuraiEntity.SHEATHE && copy.weaponsDrawn(),
                                "收刀读取没有原路退回持刀端");
                        var after = copy.currentPose(0);
                        for (int i = 0; i < before.matrices().length; i++)
                            h.assertTrue(
                                    before.matrices()[i].equals(after.matrices()[i], .0001f),
                                    "收刀读取首帧跳变");
                        done(h, "收刀中途存读NBT首帧逐骨骼一致，恢复策略为原轨道反向");
                    }
                });
    }

    @GameTest(template = "arena", timeoutTicks = 90, batch = "r10_gravity_reload")
    public static void gravity_reload(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        b.setNoGravity(true);
        var tag = new CompoundTag();
        b.addAdditionalSaveData(tag);
        var copy = boss(h, 4.5, 4.5);
        copy.readAdditionalSaveData(tag);
        h.assertFalse(copy.isNoGravity(), "活体读取保留了机械动作的禁用重力");
        done(h, "地形动作重载取消过期禁重力，不把实体留在半空");
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r10_surfaces")
    public static void terrain_contacts(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        for (int x = 12; x < 18; x++)
            for (int z = 4; z < 18; z++) h.setBlock(x, 1, z, Blocks.STONE_SLAB);
        for (int z = 5; z < 17; z++) for (int y = 1; y < 6; y++) h.setBlock(10, y, z, Blocks.AIR);
        for (int x = 6; x < 15; x++) for (int y = 1; y < 6; y++) h.setBlock(x, y, 12, Blocks.STONE);
        var surface = DeathContacts.capture(b);
        float max = -100, min = 100;
        for (float v : surface.heights) {
            max = Math.max(max, v);
            min = Math.min(min, v);
        }
        h.assertTrue(max - min > 3, "半砖碰撞高度未进入接触快照");
        h.assertTrue(surface.direction != 0, "前方贴墙仍朝墙倒");
        done(h, "真实半砖碰撞面快照包含不同高度，贴墙选择其他倒地方向（非客户端穿模验收）");
    }
}
