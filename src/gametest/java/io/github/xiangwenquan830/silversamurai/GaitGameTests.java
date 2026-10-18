package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GaitGameTests {
    @GameTest(template = "arena", timeoutTicks = 120, batch = "gait_flat")
    public static void flat_node_cadence(GameTestHelper h) {
        exercise(h, false, false);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "gait_stair_start")
    public static void uphill_start_cadence(GameTestHelper h) {
        exercise(h, true, false);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "gait_stair_down")
    public static void downhill_cadence(GameTestHelper h) {
        exercise(h, true, true);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "gait_flat_two")
    public static void phase_two_flat(GameTestHelper h) {
        exercise(h, false, false, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "gait_up_two")
    public static void phase_two_up(GameTestHelper h) {
        exercise(h, true, false, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "gait_down_two")
    public static void phase_two_down(GameTestHelper h) {
        exercise(h, true, true, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 20, batch = "gait_overlay")
    public static void stair_overlay_join(GameTestHelper h) {
        for (int phase : new int[] {1, 2}) {
            var rig = SamuraiRig.get(phase);
            String clip = phase == 2 ? SamuraiRig.WALK_TWO : "armed_walk";
            var a = new StepMotion.Frame(.88f, 1, 1);
            var b = new StepMotion.Frame(.15f, .5f, -1);
            for (String side : new String[] {"L", "R"}) {
                int ankle = rig.bone("行走_踝_" + side);
                var origin = rig.bones.get(ankle).origin;
                org.joml.Vector3f previous = null;
                for (int i = 0; i <= 20; i++) {
                    var pose = StepMotion.interpolate(rig, rig.pose(clip, .3f), a, b, i / 20f);
                    var foot =
                            pose.matrices()[ankle].transformPosition(new org.joml.Vector3f(origin));
                    h.assertTrue(Float.isFinite(foot.length()), "invalid terrain pose");
                    if (previous != null)
                        h.assertTrue(
                                previous.distance(foot) * SamuraiRig.SCALE < .035,
                                "stair join snaps at render subframe: "
                                        + previous.distance(foot) * SamuraiRig.SCALE);
                    previous = foot;
                }
                for (int i : new int[] {0, 1}) {
                    var f = i == 0 ? a : b;
                    var direct =
                            StepMotion.pose(
                                    rig, rig.pose(clip, .3f), f.progress(), f.height(), f.side());
                    var mixed = StepMotion.interpolate(rig, rig.pose(clip, .3f), a, b, i);
                    h.assertTrue(
                            direct.matrices()[ankle].equals(mixed.matrices()[ankle], .0001f),
                            "stair interpolation endpoint changed pose");
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 80, batch = "gait_stop")
    public static void halted_pursuit_does_not_march(GameTestHelper h) {
        floor(h);
        var b = boss(h, 3.25, 10.5);
        var p = player(h, 22.25, 1, 10.5);
        b.facePoint(p.position(), 360);
        b.testPursuit(p);
        float[] time = {0};
        Vec3[] at = {Vec3.ZERO};
        h.runAtTickTime(
                24,
                () -> {
                    b.setNoAi(true);
                    b.terrainNavigation().cancel();
                    b.haltMotion();
                    time[0] = b.visualTime(0);
                    at[0] = b.position();
                });
        h.runAtTickTime(
                36,
                () -> {
                    h.assertTrue(
                            Math.abs(b.visualTime(0) - time[0]) < .0001,
                            "halted boss still marches");
                    h.assertTrue(b.position().distanceTo(at[0]) < .001, "halted boss slides");
                    b.setNoAi(false);
                });
        h.runAtTickTime(
                48,
                () -> {
                    h.assertTrue(b.visualTime(0) > time[0], "walk does not resume after halt");
                    h.succeed();
                });
    }

    static void exercise(GameTestHelper h, boolean stairs, boolean down) {
        exercise(h, stairs, down, 1);
    }

    static void exercise(GameTestHelper h, boolean stairs, boolean down, int phase) {
        floor(h);
        if (stairs) ReacquisitionGameTests.stairs(h);
        var b = boss(h, down ? 12.25 : stairs ? 7.05 : 2.25, 10.5);
        if (down) b.setPos(h.absoluteVec(new Vec3(12.25, 5, 10.5)));
        var p = player(h, down ? 2.25 : 22.25, stairs && !down ? 5 : 1, 10.5);
        if (phase == 2) b.testCombat(p, 2, false);
        b.facePoint(p.position(), 360);
        b.testPursuit(p);
        Vec3[] previous = {b.position()};
        float[] cycle = {b.visualTime(0)};
        int[] moving = {0}, frozen = {0}, slow = {0}, vertical = {0};
        double[] minimum = {1}, maximum = {0};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    Vec3 delta = b.position().subtract(previous[0]);
                    double moved = delta.length();
                    if (b.state() == SamuraiEntity.CHASE && moved > .001 && b.distanceTo(p) > 6) {
                        moving[0]++;
                        minimum[0] = Math.min(minimum[0], moved);
                        maximum[0] = Math.max(maximum[0], moved);
                        if (Math.abs(delta.y) > .005) vertical[0]++;
                        if (b.visualTime(0) - cycle[0] < .00001) frozen[0]++;
                        if (moving[0] > 8 && moved < .14) {
                            slow[0]++;
                            GeneratedMod.LOG.info(
                                    "[r28 slow] phase={} tick={} delta={} speed={} yaw={} status={}",
                                    phase,
                                    h.getTick(),
                                    delta,
                                    b.walkingSpeed(),
                                    b.getYRot(),
                                    b.report());
                        }
                    }
                    previous[0] = b.position();
                    cycle[0] = b.visualTime(0);
                    if (!h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02))) {
                        GeneratedMod.LOG.error(
                                "[r33 gait collision] box={} nearby={}",
                                b.getBoundingBox(),
                                h.getLevel().getEntities(b, b.getBoundingBox().inflate(.02)));
                        for (var pos :
                                net.minecraft.core.BlockPos.betweenClosed(
                                        net.minecraft.core.BlockPos.containing(
                                                b.getBoundingBox().minX,
                                                b.getBoundingBox().minY,
                                                b.getBoundingBox().minZ),
                                        net.minecraft.core.BlockPos.containing(
                                                b.getBoundingBox().maxX,
                                                b.getBoundingBox().maxY,
                                                b.getBoundingBox().maxZ))) {
                            var block = h.getLevel().getBlockState(pos);
                            var shape = block.getCollisionShape(h.getLevel(), pos);
                            if (!shape.isEmpty()
                                    && shape.bounds()
                                            .move(pos)
                                            .intersects(b.getBoundingBox().deflate(.02)))
                                GeneratedMod.LOG.error(
                                        "[r33 gait block] {} {}", h.relativePos(pos), block);
                        }
                    }
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "gait fix clipped terrain: "
                                    + b.position()
                                    + " tick="
                                    + h.getTick()
                                    + " target="
                                    + p.position()
                                    + " "
                                    + b.report());
                });
        h.runAtTickTime(
                75,
                () -> {
                    GeneratedMod.LOG.info(
                            "[r28 gait] phase={} stairs={} down={} moving={} frozen={} slow={} vertical={} min={} max={}",
                            phase,
                            stairs,
                            down,
                            moving[0],
                            frozen[0],
                            slow[0],
                            vertical[0],
                            minimum[0],
                            maximum[0]);
                    h.assertTrue(moving[0] >= 12, "insufficient pursuit samples: " + b.report());
                    if (stairs) h.assertTrue(vertical[0] > 3, "did not cross stairs");
                    h.assertTrue(
                            frozen[0] == 0, "walk animation froze while body moved: " + frozen[0]);
                    h.assertTrue(
                            slow[0] <= 1, "repeated low-speed route-node/stair ticks: " + slow[0]);
                    h.succeed();
                });
    }
}
