package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

@GameTestHolder("silver_samurai_r44")
@PrefixGameTestTemplate(false)
public class FocusedR44Tests {
    @GameTest(template = "arena", timeoutTicks = 650, batch = "r44_far")
    public static void far_natural(GameTestHelper h) {
        approach(h, 1, false, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "r44_diagonal")
    public static void diagonal_two(GameTestHelper h) {
        approach(h, 2, true, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 700, batch = "r44_obstacle")
    public static void obstructed_entry(GameTestHelper h) {
        approach(h, 1, false, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 700, batch = "r44_armed")
    public static void armed_one(GameTestHelper h) {
        approach(h, 1, false, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 700, batch = "r44_change")
    public static void target_comes_down(GameTestHelper h) {
        approach(h, 1, false, 3);
    }

    @GameTest(template = "arena", timeoutTicks = 700, batch = "r44_near")
    public static void nearer_enemy(GameTestHelper h) {
        approach(h, 1, false, 4);
    }

    @GameTest(template = "arena", timeoutTicks = 700, batch = "r44_tall")
    public static void tall_building(GameTestHelper h) {
        approach(h, 1, false, 5);
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "r44_bounds")
    public static void height_range_policy(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 10, 3);
        b.setNoAi(true);
        var p = SamuraiGameTests.player(h, 10, 1, 8);
        var targeting = new Targeting(b);
        boolean old = BossConfig.WALL_CLIMB.get();
        double acquire = BossConfig.PLAYER_ACQUIRE.get(), retain = BossConfig.PLAYER_RETAIN.get();
        try {
            BossConfig.PLAYER_ACQUIRE.set(28d);
            BossConfig.PLAYER_RETAIN.set(40d);
            p.setPos(b.position().add(0, 48, 4));
            h.assertTrue(targeting.valid(p), "高处目标被普通球形保留距离排除");
            BossConfig.WALL_CLIMB.set(false);
            h.assertFalse(targeting.valid(p), "关闭攀爬仍扩大高处索敌");
            BossConfig.WALL_CLIMB.set(true);
            p.setPos(b.position().add(0, 49, 4));
            h.assertFalse(targeting.valid(p), "超出攀爬高度仍保留目标");
            p.setPos(b.position().add(50, 45, 0));
            h.assertFalse(targeting.valid(p), "扩大了水平保留距离");
        } finally {
            BossConfig.WALL_CLIMB.set(old);
            BossConfig.PLAYER_ACQUIRE.set(acquire);
            BossConfig.PLAYER_RETAIN.set(retain);
        }
        b.discard();
        p.discard();
        h.succeed();
    }

    private static void approach(GameTestHelper h, int phase, boolean diagonal, int scenario) {
        SamuraiGameTests.floor(h);
        int height = scenario == 5 ? 24 : 12;
        for (int x = 8; x <= 21; x++)
            for (int z = 18; z <= 23; z++)
                for (int y = 1; y <= height; y++) h.setBlock(x, y, z, Blocks.STONE);
        if (scenario == 1)
            for (int x = 12; x <= 16; x++)
                for (int z = 13; z <= 16; z++)
                    for (int y = 1; y <= 4; y++) h.setBlock(x, y, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, diagonal ? 3.5 : 14.5, 2.5);
        var p = SamuraiGameTests.player(h, diagonal ? 19.5 : 14.5, height + 1, 18.9);
        if (phase == 2 || scenario == 2) {
            b.testCombat(p, phase, false);
            b.testPursuit(p);
            b.setTarget(null);
        }
        b.setYRot(180);
        b.yBodyRot = 180;
        net.minecraft.server.level.ServerPlayer[] nearby = {null};
        if (scenario == 3) h.runAtTickTime(16, () -> p.setPos(h.absoluteVec(new Vec3(14.5, 1, 8))));
        if (scenario == 4) {
            nearby[0] = SamuraiGameTests.player(h, 14.5, 1, 8);
            nearby[0].setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            h.runAtTickTime(
                    16, () -> nearby[0].setGameMode(net.minecraft.world.level.GameType.SURVIVAL));
        }
        Vec3 initial = b.position();
        boolean[] acquired = {false}, wall = {false}, top = {false}, completed = {false};
        h.onEachTick(
                () -> {
                    if (b.isRemoved()) return;
                    acquired[0] |= b.getTarget() == p;
                    wall[0] |= b.wallTraversal().active();
                    top[0] |=
                            b.onGround()
                                    && b.getY() >= initial.y + height - .05
                                    && b.getZ() > h.absoluteVec(new Vec3(0, 0, 18)).z;
                    completed[0] |= top[0] && !b.wallTraversal().active();
                    String stage = b.wallTraversal().status();
                    if (stage.equals("FIRST") || stage.equals("CLIMB")) {
                        Vec3 f =
                                new Vec3(
                                        -Math.sin(Math.toRadians(b.getYRot())),
                                        0,
                                        Math.cos(Math.toRadians(b.getYRot())));
                        h.assertTrue(
                                WallGeometry.find(b, b.position().add(f), 1.7) != null,
                                "起爬时离墙过远，手部悬空");
                    }
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)), "攀爬穿墙");
                    if (h.getTick() % 80 == 0)
                        GeneratedMod.LOG.info(
                                "[r44 approach] {} tick={} pos={} {}",
                                phase,
                                h.getTick(),
                                b.position(),
                                b.report());
                });
        h.runAtTickTime(
                scenario == 5 ? 680 : 610,
                () -> {
                    if (scenario == 3 || scenario == 4) {
                        h.assertFalse(wall[0], "目标离开/出现近敌后仍爬旧墙");
                        h.assertTrue(
                                (scenario == 3 ? p : nearby[0]).getHealth() < 1000, "没有切回地面攻击");
                        b.discard();
                        p.discard();
                        if (nearby[0] != null) nearby[0].discard();
                        h.succeed();
                        return;
                    }
                    h.assertTrue(acquired[0], "远处未自然索敌");
                    h.assertTrue(wall[0], "已索敌但没有走向墙并攀爬: " + b.report());
                    h.assertTrue(top[0], "未到楼顶: " + b.report());
                    h.assertTrue(completed[0], "登顶没有结束攀爬: " + b.report());
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }
}
