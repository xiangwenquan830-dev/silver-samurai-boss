package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

@GameTestHolder("silver_samurai_r48")
@PrefixGameTestTemplate(false)
public class FocusedR48Tests {
    private record Setup(SamuraiEntity boss, net.minecraft.server.level.ServerPlayer player) {}

    private static Setup setup(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 8; x <= 15; x++)
            for (int z = 8; z <= 15; z++) h.setBlock(x, 16, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 12, 12);
        b.setPos(h.absoluteVec(new Vec3(12, 17, 12)));
        b.setNoAi(true);
        b.setOnGround(true);
        var p = SamuraiGameTests.player(h, 20, 1, 12);
        b.testPursuit(p);
        b.facePoint(p.position(), 360);
        return new Setup(b, p);
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_creative")
    public static void creative_no_drop(GameTestHelper h) {
        invalid(h, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_dead")
    public static void dead_no_drop(GameTestHelper h) {
        invalid(h, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_removed")
    public static void removed_no_drop(GameTestHelper h) {
        invalid(h, 2);
    }

    private static void invalid(GameTestHelper h, int kind) {
        var s = setup(h);
        var b = s.boss();
        var p = s.player();
        if (kind == 0) p.setGameMode(GameType.CREATIVE);
        if (kind == 1) p.setHealth(0);
        if (kind == 2) p.discard();
        h.assertFalse(b.wallTraversal().tryStart(), "无效目标启动下跳: " + kind);
        b.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_turn")
    public static void drop_owns_turn(GameTestHelper h) {
        var s = setup(h);
        var b = s.boss();
        b.setYRot(90);
        b.yBodyRot = 90;
        h.assertTrue(b.wallTraversal().tryStart(), "安全出口未接管转身，仍会被普通追击覆盖");
        Vec3 before = b.position();
        b.wallTraversal().tick();
        h.assertTrue(b.position().distanceTo(before) < .001, "尚未对正出口就侧滑");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_cancel")
    public static void cancel_departure_when_creative(GameTestHelper h) {
        var s = setup(h);
        var b = s.boss();
        h.assertTrue(b.wallTraversal().tryStart(), "未启动测试下跳");
        s.player().setGameMode(GameType.CREATIVE);
        Vec3 before = b.position();
        b.wallTraversal().tick();
        h.assertFalse(b.wallTraversal().active(), "尚在屋顶时仍向无效目标走出悬崖");
        h.assertTrue(b.position().distanceTo(before) < .001, "取消后仍移动");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_air")
    public static void finish_fall_after_target_lost(GameTestHelper h) {
        var s = setup(h);
        var b = s.boss();
        h.assertTrue(b.wallTraversal().tryStart(), "未启动测试下跳");
        b.setPos(b.position().add(5, -4, 0));
        b.setOnGround(false);
        b.setDeltaMovement(0, -.5, 0);
        s.player().discard();
        b.wallTraversal().tick();
        h.assertTrue(b.wallTraversal().status().equals("DROP") && !b.isNoGravity(), "半空丢目标中断安全落地");
        h.assertTrue(b.getDeltaMovement().y < 0, "下落速度被清零");
        b.discard();
        h.succeed();
    }

    private static net.minecraft.nbt.CompoundTag activeTag(Setup s, GameTestHelper h) {
        h.assertTrue(s.boss().wallTraversal().tryStart(), "未建立测试动作");
        var n = new net.minecraft.nbt.CompoundTag();
        s.boss().addAdditionalSaveData(n);
        return n;
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_reach_load")
    public static void reach_load_stays_sheathed(GameTestHelper h) {
        var s = setup(h);
        var b = s.boss();
        var n = activeTag(s, h);
        n.getCompound("SamuraiWall").putString("Stage", "REACH");
        b.readAdditionalSaveData(n);
        h.assertTrue(b.wallTraversal().status().equals("REACH"), "有效动作未恢复");
        h.assertFalse(b.weaponsDrawn(), "抓墙阶段重载错误变成持刀");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_stale_load")
    public static void missing_wall_tag_clears_old_stage(GameTestHelper h) {
        var s = setup(h);
        var b = s.boss();
        var n = activeTag(s, h);
        n.remove("SamuraiWall");
        b.readAdditionalSaveData(n);
        h.assertFalse(b.wallTraversal().active() || b.isNoGravity(), "没有攀爬存档却继承旧动作/重力锁");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_missing_vector")
    public static void incomplete_wall_vector_rejected(GameTestHelper h) {
        var s = setup(h);
        var b = s.boss();
        var n = activeTag(s, h);
        n.getCompound("SamuraiWall").remove("StartX");
        b.readAdditionalSaveData(n);
        h.assertFalse(b.wallTraversal().active() || b.isNoGravity(), "缺失坐标被当成零坐标恢复");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_far_origin")
    public static void distant_wall_origin_rejected(GameTestHelper h) {
        var s = setup(h);
        var b = s.boss();
        var n = activeTag(s, h);
        n.getCompound("SamuraiWall").putDouble("StartX", b.getX() + 1000000);
        b.readAdditionalSaveData(n);
        h.assertFalse(b.wallTraversal().active() || b.isNoGravity(), "远端动作原点被接受");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_top_pose")
    public static void top_reload_preserves_visual_offset(GameTestHelper h) {
        var s = setup(h);
        var b = s.boss();
        var n = activeTag(s, h);
        n.getCompound("SamuraiWall").putString("Stage", "TOP");
        b.wallTraversal().load(n);
        b.wallResume(.7f);
        b.wallOffset(new Vec3(0, 2, 0));
        var before = b.currentPose(0).matrices()[0];
        var saved = new net.minecraft.nbt.CompoundTag();
        b.addAdditionalSaveData(saved);
        b.readAdditionalSaveData(saved);
        h.assertTrue(before.equals(b.currentPose(0).matrices()[0], .001f), "翻顶重载丢失视觉偏移，首帧抽动");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_red_time")
    public static void red_wall_reload_keeps_animation_time(GameTestHelper h) {
        var s = setup(h);
        var b = s.boss();
        var n = activeTag(s, h);
        var wall = n.getCompound("SamuraiWall");
        wall.putString("Stage", "STAB");
        wall.putBoolean("Red", true);
        float t = SamuraiRig.get(b.phase()).length(WallClips.RED_STAB) - .05f;
        wall.putFloat("Time", t);
        b.wallTraversal().load(n);
        h.assertTrue(
                b.clip().equals(WallClips.RED_STAB) && Math.abs(b.stateAge(0) - t) < .026,
                "红刀恢复按普通刀动作截断时间");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r48_save_reuse")
    public static void saving_finished_action_removes_stale_tag(GameTestHelper h) {
        var s = setup(h);
        var b = s.boss();
        var n = activeTag(s, h);
        b.wallTraversal().cancel();
        b.wallTraversal().save(n);
        h.assertFalse(n.contains("SamuraiWall"), "复用存档标签保留已结束动作");
        b.discard();
        s.player().discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 330, batch = "r48_high_reload")
    public static void high_fall_reload_then_attack(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 7; x <= 15; x++)
            for (int z = 7; z <= 15; z++) h.setBlock(x, 96, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 12, 12);
        b.setPos(h.absoluteVec(new Vec3(12, 97, 12)));
        var p = SamuraiGameTests.player(h, 19, 1, 12);
        b.testPursuit(p);
        boolean[] loaded = {false}, landed = {false}, hit = {false};
        h.onEachTick(
                () -> {
                    if (!loaded[0]
                            && b.wallTraversal().status().equals("DROP")
                            && h.relativeVec(b.position()).y < 70) {
                        var n = new net.minecraft.nbt.CompoundTag();
                        b.addAdditionalSaveData(n);
                        Vec3 before = b.position();
                        double vy = b.getDeltaMovement().y;
                        b.readAdditionalSaveData(n);
                        loaded[0] = true;
                        h.assertTrue(
                                b.wallTraversal().status().equals("DROP") && !b.isNoGravity(),
                                "高空重载丢失下跳状态");
                        h.assertTrue(
                                b.position().distanceTo(before) < .001
                                        && b.getDeltaMovement().y <= vy + .001,
                                "高空重载位移/下落速度突变");
                    }
                    landed[0] |= loaded[0] && b.onGround() && h.relativeVec(b.position()).y < 2;
                    hit[0] |=
                            p.getLastDamageSource() != null
                                    && p.getLastDamageSource().getEntity() == b;
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "重载后下跳穿模");
                });
        h.runAtTickTime(
                290,
                () -> {
                    h.assertTrue(loaded[0] && landed[0] && hit[0], "高空重载未完成落地续攻: " + b.report());
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }
}
