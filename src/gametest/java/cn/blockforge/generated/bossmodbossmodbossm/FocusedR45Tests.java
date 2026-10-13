package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

@GameTestHolder("silver_samurai_r45")
@PrefixGameTestTemplate(false)
public class FocusedR45Tests {
    @GameTest(template = "arena", timeoutTicks = 500, batch = "r45_three1")
    public static void three_one(GameTestHelper h) {
        wall(h, 3, 1, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 500, batch = "r45_four1")
    public static void four_one(GameTestHelper h) {
        wall(h, 4, 1, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 500, batch = "r45_five1")
    public static void five_one(GameTestHelper h) {
        wall(h, 5, 1, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 500, batch = "r45_three2")
    public static void three_two(GameTestHelper h) {
        wall(h, 3, 2, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 500, batch = "r45_five2")
    public static void five_two(GameTestHelper h) {
        wall(h, 5, 2, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 600, batch = "r45_window")
    public static void window_entry(GameTestHelper h) {
        wall(h, 10, 1, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 550, batch = "r45_setback")
    public static void setback(GameTestHelper h) {
        wall(h, 5, 1, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 550, batch = "r45_route")
    public static void old_route_handoff(GameTestHelper h) {
        wall(h, 4, 1, 3);
    }

    @GameTest(template = "arena", timeoutTicks = 750, batch = "r45_natural_route")
    public static void ground_target_behind_wall(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 0; x < 24; x++)
            for (int z = 9; z <= 12; z++)
                for (int y = 1; y <= 4; y++) h.setBlock(x, y, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 12.5, 6);
        var p = SamuraiGameTests.player(h, 12.5, 1, 16);
        b.testPursuit(p);
        boolean[] climbed = {false};
        h.onEachTick(
                () -> {
                    if (b.isRemoved()) return;
                    if (b.wallTraversal().status().equals("TOP")) climbed[0] = true;
                    h.assertTrue(b.state() != SamuraiEntity.CLIMB, "正常寻路仍使用旧平移攀爬");
                    if (h.getTick() % 80 == 0)
                        GeneratedMod.LOG.info(
                                "[r45 natural route] tick={} pos={} {}",
                                h.getTick(),
                                b.position(),
                                b.report());
                });
        h.runAtTickTime(
                700,
                () -> {
                    h.assertTrue(climbed[0], "隔墙同高度目标未触发攀爬路线: " + b.report());
                    h.assertTrue(b.getZ() > h.absoluteVec(new Vec3(0, 0, 12)).z, "未翻过障碍追击");
                    h.assertTrue(b.getY() < h.absoluteVec(new Vec3(0, 2, 0)).y, "翻墙后没有回到目标地面");
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 550, batch = "r45_snow")
    public static void thin_snow_landing(GameTestHelper h) {
        wall(h, 4, 1, 4);
    }

    private static void wall(GameTestHelper h, int height, int phase, int kind) {
        SamuraiGameTests.floor(h);
        for (int x = 5; x <= 19; x++)
            for (int z = 9; z <= 20; z++)
                for (int y = 1; y <= height; y++) {
                    if (kind == 2 && y > 3 && z == 9) continue;
                    h.setBlock(x, y, z, Blocks.STONE);
                }
        if (kind == 4)
            for (int x = 5; x <= 19; x++)
                for (int z = 9; z <= 20; z++) h.setBlock(x, height + 1, z, Blocks.SNOW);
        if (kind == 1)
            for (int x = 6; x <= 18; x++)
                for (int y = 3; y <= 4; y++) h.setBlock(x, y, 9, Blocks.AIR);
        var b = SamuraiGameTests.boss(h, 12.5, kind == 3 ? 5.5 : 3.5);
        var p =
                SamuraiGameTests.player(
                        h, 12.5, height + 1 + (kind == 4 ? .125 : 0), kind == 2 ? 10.8 : 9.5);
        if (phase == 2 || kind == 3) {
            b.testCombat(p, phase, false);
            b.testPursuit(p);
        }
        if (kind == 3)
            h.assertTrue(b.queueWallRoute(h.absoluteVec(new Vec3(12.5, 5, 10.5))), "旧路线未接受动画交接");
        boolean[] reached = {false};
        java.util.Set<String> stages = new java.util.HashSet<>();
        h.onEachTick(
                () -> {
                    if (b.isRemoved()) return;
                    stages.add(b.wallTraversal().status());
                    h.assertTrue(b.state() != SamuraiEntity.CLIMB, "仍进入无攀爬动画的旧平移分支");
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "碰撞箱穿入矮墙");
                    if (b.onGround()
                            && b.getY() >= h.absoluteVec(new Vec3(0, height + 1, 0)).y - .05
                            && b.getZ() > h.absoluteVec(new Vec3(0, 0, 9)).z) reached[0] = true;
                    if (h.getTick() % 80 == 0)
                        GeneratedMod.LOG.info(
                                "[r45 wall] height={} phase={} kind={} tick={} pos={} stages={} {}",
                                height,
                                phase,
                                kind,
                                h.getTick(),
                                b.position(),
                                stages,
                                b.report());
                });
        h.runAtTickTime(
                kind == 1 ? 560 : 460,
                () -> {
                    h.assertTrue(
                            stages.contains("REACH") && stages.contains("TOP"),
                            "没有完整抓墙翻顶: " + stages + " " + b.report());
                    h.assertTrue(reached[0], "未站上平台: " + b.report());
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }
}
