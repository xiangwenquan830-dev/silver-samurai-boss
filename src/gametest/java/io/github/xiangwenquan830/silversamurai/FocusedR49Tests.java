package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

@GameTestHolder("silver_samurai_r49")
@PrefixGameTestTemplate(false)
public class FocusedR49Tests {
    private record Setup(SamuraiEntity boss, net.minecraft.server.level.ServerPlayer player) {}

    private static Setup gap(GameTestHelper h, int phase) {
        SamuraiGameTests.floor(h);
        for (int x = 0; x < 24; x++)
            for (int z = 0; z < 24; z++) if (x < 12 || x >= 15) h.setBlock(x, 4, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 10, 12);
        b.setPos(h.absoluteVec(new Vec3(10, 5, 12)));
        b.setOnGround(true);
        var p = SamuraiGameTests.player(h, 21, 5, 12);
        if (phase == 2) b.testCombat(p, 2, false);
        b.testPursuit(p);
        b.facePoint(p.position(), 360);
        return new Setup(b, p);
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_roof")
    public static void lower_roof_not_street(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 5; x < 15; x++)
            for (int z = 6; z < 18; z++) h.setBlock(x, 16, z, Blocks.STONE);
        for (int x = 15; x < 23; x++)
            for (int z = 6; z < 18; z++) h.setBlock(x, 13, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 12, 12);
        b.setPos(h.absoluteVec(new Vec3(12, 17, 12)));
        b.setOnGround(true);
        b.setNoAi(true);
        var p = SamuraiGameTests.player(h, 19, 14, 12);
        b.testPursuit(p);
        h.assertFalse(
                DropIntent.useful(b.position(), h.absoluteVec(new Vec3(15, 1, 12)), p.position()),
                "玩家在屋顶却选择街面");
        h.assertTrue(
                DropIntent.useful(b.position(), h.absoluteVec(new Vec3(17, 14, 12)), p.position()),
                "安全的低屋顶也被禁止");
        b.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_no_roof_drop")
    public static void unreachable_rooftop_no_ground_detour(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 7; x < 15; x++)
            for (int z = 6; z < 18; z++) h.setBlock(x, 16, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 12, 12);
        b.setPos(h.absoluteVec(new Vec3(12, 17, 12)));
        b.setOnGround(true);
        b.setNoAi(true);
        var p = SamuraiGameTests.player(h, 21, 14, 12);
        p.setNoGravity(true);
        b.testPursuit(p);
        h.assertFalse(b.wallTraversal().tryStart(), "玩家在空中/屋顶高度却向街面主动离边");
        b.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 160, batch = "r49_jump_one")
    public static void gap_jump_phase_one(GameTestHelper h) {
        flight(h, 1, false, false);
    }

    @GameTest(template = "arena", timeoutTicks = 160, batch = "r49_jump_two")
    public static void gap_jump_phase_two(GameTestHelper h) {
        flight(h, 2, false, false);
    }

    @GameTest(template = "arena", timeoutTicks = 160, batch = "r49_jump_load")
    public static void midair_reload_lands(GameTestHelper h) {
        flight(h, 1, true, false);
    }

    @GameTest(template = "arena", timeoutTicks = 160, batch = "r49_jump_lost")
    public static void midair_target_lost_lands(GameTestHelper h) {
        flight(h, 2, false, true);
    }

    private static void flight(GameTestHelper h, int phase, boolean reload, boolean lost) {
        var s = gap(h, phase);
        var b = s.boss();
        var p = s.player();
        boolean[] air = {false}, land = {false}, changed = {false};
        double[] high = {b.getY()};
        h.onEachTick(
                () -> {
                    high[0] = Math.max(high[0], b.getY());
                    air[0] |= b.pursuitJump().status().equals("AIR");
                    if (air[0]
                            && b.pursuitJump().status().equals("AIR")
                            && b.stateAge(0) > .2
                            && !changed[0]) {
                        changed[0] = true;
                        if (reload) {
                            var n = new net.minecraft.nbt.CompoundTag();
                            Vec3 velocity = b.getDeltaMovement();
                            b.addAdditionalSaveData(n);
                            b.readAdditionalSaveData(n);
                            h.assertTrue(
                                    b.getDeltaMovement().distanceTo(velocity) < .001, "重载打断跳跃速度");
                            h.assertTrue(b.pursuitJump().active(), "空中重载丢失跳跃");
                        }
                        if (lost) p.discard();
                        Vec3 owned = b.getDeltaMovement();
                        b.setDeltaMovement(new Vec3(4, 3, 4));
                        b.push(5, 2, 5);
                        b.knockback(5, 1, 1);
                        h.assertTrue(b.getDeltaMovement().distanceTo(owned) < .001, "外部击退干扰主动跳跃");
                    }
                    land[0] |=
                            air[0]
                                    && b.onGround()
                                    && h.relativeVec(b.position()).x > 14
                                    && Math.abs(h.relativeVec(b.position()).y - 5) < .1;
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "跳跃穿过实体方块");
                    if (b.pursuitJump().active()) h.assertFalse(b.isNoGravity(), "跳跃错误锁定重力");
                });
        h.runAtTickTime(
                115,
                () -> {
                    h.assertTrue(
                            air[0] && land[0] && high[0] > h.absoluteVec(new Vec3(0, 6.2, 0)).y,
                            "未完成有高度的跳跃并着地: " + b.report() + " jump=" + b.pursuitJump().status());
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_ceiling")
    public static void low_ceiling_rejects_jump(GameTestHelper h) {
        var s = gap(h, 1);
        s.boss().setNoAi(true);
        for (int x = 7; x < 20; x++)
            for (int z = 8; z < 17; z++) h.setBlock(x, 10, z, Blocks.STONE);
        h.assertFalse(s.boss().pursuitJump().tryStart(), "头顶空间不足仍起跳");
        s.boss().discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_void")
    public static void missing_landing_rejects_jump(GameTestHelper h) {
        var s = gap(h, 1);
        s.boss().setNoAi(true);
        for (int x = 12; x < 24; x++) for (int z = 0; z < 24; z++) h.setBlock(x, 4, z, Blocks.AIR);
        h.assertFalse(s.boss().pursuitJump().tryStart(), "没有同层落点仍跳出平台");
        s.boss().discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_flat")
    public static void flat_ground_keeps_walking(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 12);
        var p = SamuraiGameTests.player(h, 15, 1, 12);
        b.testPursuit(p);
        b.setOnGround(true);
        b.setNoAi(true);
        h.assertFalse(b.pursuitJump().tryStart(), "平地追击反复乱跳");
        b.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_removed_floor")
    public static void removed_landing_cancels_windup(GameTestHelper h) {
        var s = gap(h, 1);
        var b = s.boss();
        b.setNoAi(true);
        h.assertTrue(b.pursuitJump().tryStart(), "夹具未触发跳跃");
        for (int x = 12; x < 24; x++) for (int z = 0; z < 24; z++) h.setBlock(x, 4, z, Blocks.AIR);
        b.pursuitJump().tick();
        h.assertFalse(b.pursuitJump().active(), "蓄力中落点被拆仍起跳");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_anim")
    public static void grip_and_clip_continuity(GameTestHelper h) {
        for (int phase = 1; phase <= 2; phase++) {
            var rig = SamuraiRig.get(phase);
            String[] clips = {PursuitJump.PREP, PursuitJump.AIR, PursuitJump.LAND};
            var base = rig.pose(clips[0], 0);
            for (String side : phase == 1 ? new String[] {"L", "R"} : new String[] {"L"}) {
                int wrist = rig.bone("腕_" + side), sword = rig.bone("背挂武士刀_" + side);
                var grip =
                        new Matrix4f(base.matrices()[wrist]).invert().mul(base.matrices()[sword]);
                for (String clip : clips)
                    for (float t = 0; t <= rig.length(clip); t += 1f / 120) {
                        var pose = rig.pose(clip, t);
                        var now =
                                new Matrix4f(pose.matrices()[wrist])
                                        .invert()
                                        .mul(pose.matrices()[sword]);
                        h.assertTrue(
                                grip.equals(now, .008f), "刀柄脱手: " + phase + "/" + clip + "/" + t);
                    }
            }
            for (int j = 0; j < 2; j++) {
                var a = rig.pose(clips[j], rig.length(clips[j]));
                var b = rig.pose(clips[j + 1], 0);
                for (int i = 0; i < a.matrices().length; i++)
                    h.assertTrue(a.matrices()[i].equals(b.matrices()[i], .006f), "跳跃分段边界姿势不连续");
            }
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 160, batch = "r49_ledge")
    public static void two_block_ledge_jump(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 12; x < 24; x++)
            for (int z = 0; z < 24; z++)
                for (int y = 1; y <= 2; y++) h.setBlock(x, y, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 10, 12);
        var p = SamuraiGameTests.player(h, 20, 3, 12);
        b.testPursuit(p);
        b.setOnGround(true);
        b.facePoint(p.position(), 360);
        boolean[] air = {false}, land = {false};
        h.onEachTick(
                () -> {
                    air[0] |= b.pursuitJump().airborne();
                    land[0] |=
                            air[0]
                                    && b.onGround()
                                    && h.relativeVec(b.position()).x >= 12
                                    && h.relativeVec(b.position()).y >= 2.99;
                });
        h.runAtTickTime(
                120,
                () -> {
                    h.assertTrue(air[0] && land[0], "两格台阶未跳上平台: " + b.report());
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_fire")
    public static void burning_landing_rejected(GameTestHelper h) {
        var s = gap(h, 1);
        s.boss().setNoAi(true);
        for (int x = 15; x < 24; x++) for (int z = 0; z < 24; z++) h.setBlock(x, 5, z, Blocks.FIRE);
        h.assertFalse(s.boss().pursuitJump().tryStart(), "跳向着火平台");
        s.boss().discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_water")
    public static void deep_water_landing_rejected(GameTestHelper h) {
        var s = gap(h, 1);
        s.boss().setNoAi(true);
        for (int x = 15; x < 24; x++)
            for (int z = 0; z < 24; z++)
                for (int y = 5; y < 8; y++) h.setBlock(x, y, z, Blocks.WATER);
        h.assertFalse(s.boss().pursuitJump().tryStart(), "跳向深水落点");
        s.boss().discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_rising_target")
    public static void raised_target_cancels_windup(GameTestHelper h) {
        var s = gap(h, 1);
        var b = s.boss();
        b.setNoAi(true);
        h.assertTrue(b.pursuitJump().tryStart(), "未进入蓄力");
        s.player().setPos(s.player().position().add(0, 8, 0));
        b.pursuitJump().tick();
        h.assertFalse(b.pursuitJump().active(), "目标升高仍跳向过低平台");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_creative")
    public static void creative_cancels_jump_windup(GameTestHelper h) {
        var s = gap(h, 1);
        var b = s.boss();
        b.setNoAi(true);
        h.assertTrue(b.pursuitJump().tryStart(), "未进入蓄力");
        s.player().setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        b.pursuitJump().tick();
        h.assertFalse(b.pursuitJump().active(), "目标无效仍起跳");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_bad_save")
    public static void invalid_jump_save_rejected(GameTestHelper h) {
        var s = gap(h, 1);
        var b = s.boss();
        b.setNoAi(true);
        h.assertTrue(b.pursuitJump().tryStart(), "未进入蓄力");
        var n = new net.minecraft.nbt.CompoundTag();
        b.addAdditionalSaveData(n);
        n.getCompound("SamuraiJump").putDouble("MotionY", Double.NaN);
        b.readAdditionalSaveData(n);
        h.assertFalse(b.pursuitJump().active() || b.isNoGravity(), "非法跳跃存档仍被接受");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r49_two_hand")
    public static void single_sword_two_hand_grip_stable(GameTestHelper h) {
        var rig = SamuraiRig.get(2);
        var base = rig.pose(PursuitJump.PREP, 0);
        int l = rig.bone("腕_L"), r = rig.bone("腕_R");
        var grip = new Matrix4f(base.matrices()[l]).invert().mul(base.matrices()[r]);
        for (String clip : new String[] {PursuitJump.PREP, PursuitJump.AIR, PursuitJump.LAND})
            for (float t = 0; t < rig.length(clip); t += 1f / 120) {
                var pose = rig.pose(clip, t);
                h.assertTrue(
                        grip.equals(
                                new Matrix4f(pose.matrices()[l]).invert().mul(pose.matrices()[r]),
                                .005f),
                        "单刀双手之间握距发生变化");
            }
        h.succeed();
    }
}
