package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

@GameTestHolder("silver_samurai_r51")
@PrefixGameTestTemplate(false)
public class FocusedR51Tests {
    @GameTest(template = "arena", timeoutTicks = 500, batch = "r51_four")
    public static void four_high(GameTestHelper h) {
        ledge(h, 4, 1, false);
    }

    @GameTest(template = "arena", timeoutTicks = 500, batch = "r51_five")
    public static void five_high(GameTestHelper h) {
        ledge(h, 5, 2, false);
    }

    @GameTest(template = "arena", timeoutTicks = 500, batch = "r51_eave")
    public static void half_roof(GameTestHelper h) {
        ledge(h, 4, 1, true);
    }

    @GameTest(template = "arena", timeoutTicks = 500, batch = "r51_five_one")
    public static void five_high_phase_one(GameTestHelper h) {
        ledge(h, 5, 1, false);
    }

    private static void ledge(GameTestHelper h, int height, int phase, boolean half) {
        SamuraiGameTests.floor(h);
        for (int x = 12; x < 24; x++)
            for (int z = 0; z < 24; z++)
                for (int y = 1; y <= height; y++) h.setBlock(x, y, z, Blocks.STONE);
        if (half)
            for (int x = 12; x < 24; x++)
                for (int z = 0; z < 24; z++) h.setBlock(x, height + 1, z, Blocks.STONE_SLAB);
        var b = SamuraiGameTests.boss(h, 9, 12);
        var p = SamuraiGameTests.player(h, 14, 1 + height + (half ? .5 : 0), 12);
        if (phase == 2) b.testCombat(p, 2, false);
        b.testPursuit(p);
        b.facePoint(p.position(), 360);
        boolean[] moved = {false}, hit = {false};
        Vec3 start = b.position();
        float health = p.getHealth();
        h.onEachTick(
                () -> {
                    moved[0] |= b.position().distanceTo(start) > 1;
                    hit[0] |= p.getHealth() < health;
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)), "高差接近穿墙");
                });
        h.runAtTickTime(
                450,
                () -> {
                    h.assertTrue(
                            moved[0] && hit[0],
                            "高处目标未接近并命中: " + b.report() + " wall=" + b.wallTraversal().status());
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r51_exposed")
    public static void exposed_high_blade_solution(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 12);
        b.setNoAi(true);
        var p = SamuraiGameTests.player(h, 8, 5, 12);
        p.setNoGravity(true);
        b.testPursuit(p);
        h.assertTrue(CombatSpacing.lane(b, p), "胸头高度的可见目标仍被脚底高差排除");
        var d = CombatSpacing.assess(b, p, SamuraiRig.A, 0, false);
        h.assertTrue(d.attackable(), "四格高目标没有可用刀路: " + d);
        b.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r51_floor")
    public static void solid_floor_still_blocks(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 12);
        b.setNoAi(true);
        var p = SamuraiGameTests.player(h, 8, 6, 12);
        p.setNoGravity(true);
        for (int x = 2; x < 14; x++) for (int z = 7; z < 18; z++) h.setBlock(x, 5, z, Blocks.STONE);
        h.assertFalse(CombatSpacing.lane(b, p), "透过完整屋顶启动近战");
        b.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 160, batch = "r51_forward")
    public static void forward_jump_and_reload(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 0; x < 24; x++)
            for (int z = 0; z < 24; z++) if (x < 12 || x >= 15) h.setBlock(x, 4, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 10, 12);
        b.setPos(h.absoluteVec(new Vec3(10, 5, 12)));
        b.setOnGround(true);
        var p = SamuraiGameTests.player(h, 22, 5, 12);
        b.testPursuit(p);
        b.facePoint(p.position(), 360);
        boolean[] air = {false}, land = {false};
        h.onEachTick(
                () -> {
                    if (b.pursuitJump().airborne() && !air[0]) {
                        air[0] = true;
                        h.assertTrue(b.getDeltaMovement().horizontalDistance() >= .26, "前冲不足");
                        var n = new net.minecraft.nbt.CompoundTag();
                        b.addAdditionalSaveData(n);
                        b.readAdditionalSaveData(n);
                        h.assertTrue(b.pursuitJump().active(), "新跳速存档校验拒绝");
                    }
                    land[0] |= air[0] && b.onGround() && h.relativeVec(b.position()).x >= 15.9;
                });
        h.runAtTickTime(
                120,
                () -> {
                    h.assertTrue(air[0] && land[0], "未完成前冲落地: " + b.report());
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }
}
