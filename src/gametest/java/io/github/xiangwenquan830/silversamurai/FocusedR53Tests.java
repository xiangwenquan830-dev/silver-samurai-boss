package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

@GameTestHolder("silver_samurai_r53")
@PrefixGameTestTemplate(false)
public class FocusedR53Tests {
    @GameTest(template = "arena", timeoutTicks = 1050, batch = "r53_projection")
    public static void bypass_projection(GameTestHelper h) {
        scene(h, false, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 1050, batch = "r53_canopy")
    public static void climb_canopy_edge(GameTestHelper h) {
        scene(h, true, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 1050, batch = "r53_canopy_two")
    public static void canopy_phase_two(GameTestHelper h) {
        scene(h, true, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 1050, batch = "r53_above")
    public static void target_directly_above(GameTestHelper h) {
        scene(h, true, 3);
    }

    @GameTest(template = "arena", timeoutTicks = 1300, batch = "r53_changed")
    public static void obstruction_added_during_climb(GameTestHelper h) {
        scene(h, false, 4);
    }

    @GameTest(template = "arena", timeoutTicks = 220, batch = "r53_failure_memory")
    public static void failed_entry_is_shared_and_expires(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 4; x <= 21; x++)
            for (int z = 10; z <= 21; z++)
                for (int y = 1; y <= 10; y++) h.setBlock(x, y, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 12.5, 6.6);
        var p = SamuraiGameTests.player(h, 12.5, 11, 14);
        b.testPursuit(p);
        b.setNoAi(true);
        b.setOnGround(true);
        b.facePoint(p.position(), 360);
        var c = WallGeometry.find(b, p.position(), 5);
        h.assertTrue(c != null, "缺少测试墙面");
        b.wallApproach().failed(c);
        h.assertFalse(b.wallTraversal().tryStart(), "直接攀爬绕过失败入口缓存");
        var neighbor = new WallGeometry.Contact(c.face().add(3, 0, 0), c.forward());
        h.assertFalse(b.wallApproach().blocked(neighbor), "失败入口误封整面墙");
        h.runAtTickTime(
                170,
                () -> {
                    h.assertFalse(b.wallApproach().blocked(c), "入口失败缓存不恢复");
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r53_rail")
    public static void overhang_rejects_entire_motion_path(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 4; x <= 21; x++)
            for (int z = 10; z <= 21; z++)
                for (int y = 1; y <= 10; y++) h.setBlock(x, y, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 12.5, 6.6);
        var p = SamuraiGameTests.player(h, 12.5, 11, 14);
        b.testPursuit(p);
        b.setNoAi(true);
        b.setOnGround(true);
        var c = WallGeometry.find(b, p.position(), 5);
        var roof = WallGeometry.roof(b, c, b.getY());
        h.assertTrue(roof != null, "开放墙面被拒绝");
        for (int x = 10; x <= 14; x++)
            for (int z = 8; z <= 9; z++) h.setBlock(x, 7, z, Blocks.STONE);
        h.assertFalse(WallGeometry.routeClear(b, c, b.getY(), roof), "漏检头顶凸出物");
        h.assertFalse(b.wallTraversal().startAt(c, roof), "手动路线绕过中途净空检查");
        b.discard();
        p.discard();
        h.succeed();
    }

    private static void scene(GameTestHelper h, boolean tree, int phase) {
        SamuraiGameTests.floor(h);
        if (tree) {
            for (int y = 1; y <= 8; y++) h.setBlock(12, y, 14, Blocks.OAK_LOG);
            for (int x = 7; x <= 18; x++)
                for (int z = 9; z <= 20; z++)
                    for (int y = phase == 3 ? 6 : 5; y <= 8; y++)
                        h.setBlock(
                                new BlockPos(x, y, z),
                                Blocks.OAK_LEAVES
                                        .defaultBlockState()
                                        .setValue(LeavesBlock.PERSISTENT, true));
        } else {
            for (int x = 4; x <= 21; x++)
                for (int z = 10; z <= 21; z++)
                    for (int y = 1; y <= 10; y++) h.setBlock(x, y, z, Blocks.STONE);
            if (phase != 4)
                for (int x = 10; x <= 14; x++)
                    for (int z = 7; z <= 9; z++) h.setBlock(x, 6, z, Blocks.STONE);
        }
        if (phase == 3) for (int y = 1; y < 6; y++) h.setBlock(7, y, 12, Blocks.OAK_LOG);
        var b = SamuraiGameTests.boss(h, 12.5, 5);
        var p = SamuraiGameTests.player(h, 12.5, tree ? 9 : 11, 14);
        if (phase == 3) {
            b.setPos(h.absoluteVec(new Vec3(12.5, 1, 12.5)));
            b.setOnGround(true);
            p.setPos(h.absoluteVec(new Vec3(12.5, 9, 12.5)));
        }
        if (phase == 2) b.testCombat(p, 2, false);
        b.testPursuit(p);
        boolean[] climbed = {false}, reached = {false}, changed = {false};
        h.onEachTick(
                () -> {
                    if (b.isRemoved()) return;
                    if (phase == 4 && !changed[0] && b.wallTraversal().status().equals("FIRST")) {
                        changed[0] = true;
                        for (int x = 11; x <= 13; x++)
                            for (int z = 8; z <= 9; z++) h.setBlock(x, 7, z, Blocks.STONE);
                    }
                    climbed[0] |= b.wallTraversal().status().equals("TOP");
                    reached[0] |= b.onGround() && b.getY() >= p.getY() - .1;
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "绕开凸出物时穿墙");
                    if (h.getTick() % 100 == 0)
                        GeneratedMod.LOG.info(
                                "[r53 terrain] tree={} tick={} pos={} climbed={} reached={} {}",
                                tree,
                                h.getTick(),
                                h.relativeVec(b.position()),
                                climbed[0],
                                reached[0],
                                b.report());
                });
        h.runAtTickTime(
                phase == 4 ? 1200 : 980,
                () -> {
                    if (phase == 4) h.assertTrue(changed[0], "未覆盖途中改变地形");
                    h.assertTrue(climbed[0] && reached[0], "未改找有效入口并登顶: " + b.report());
                    h.assertTrue(p.getHealth() < 1000, "登顶后未接近并攻击目标");
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }
}
