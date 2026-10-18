package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import org.joml.Vector3f;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArmBlendGameTests {
    @GameTest(template = "arena", timeoutTicks = 30, batch = "idle_grip_lock")
    public static void idle_grip(GameTestHelper h) {
        var r = SamuraiRig.get(2);
        var rest = r.pose(SamuraiRig.IDLE_TWO, 0);
        int sword = r.bone("背挂武士刀_L");
        for (int f = 0; f < 240; f++) {
            var pose = r.pose(SamuraiRig.IDLE_TWO, f / 60f);
            FootworkMotion.reaction(r, pose, (float) Math.sin(f * .1f), 1);
            FootworkMotion.look(r, pose, (float) Math.sin(f * .08f) * 16);
            pose.matrices()[r.bone("腕_R")].rotateY(.7f); // unwanted independent overlay
            StableGrip.idle(r, pose);
            for (String side : new String[] {"L", "R"}) {
                int w = r.bone("腕_" + side);
                var expected =
                        new org.joml.Matrix4f(rest.matrices()[sword])
                                .invert()
                                .mul(rest.matrices()[w]);
                var actual =
                        new org.joml.Matrix4f(pose.matrices()[sword])
                                .invert()
                                .mul(pose.matrices()[w]);
                h.assertTrue(actual.equals(expected, .0001f), "idle hand moved relative to hilt");
            }
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "arm_authored_roll")
    public static void roll(GameTestHelper h) {
        Vector3f a = new Vector3f(), b = new Vector3f(0, -5, 0);
        org.joml.Matrix4f previous = null;
        for (int i = -100; i <= 100; i++) {
            var authored =
                    new org.joml.Matrix4f().rotateZ((float) Math.PI + i * .0001f).rotateY(.9f);
            var x = authored.transformPosition(new Vector3f(a));
            var y = authored.transformPosition(new Vector3f(b));
            var actual = DeathMotion.aimPreservingRoll(authored, a, b, x, y);
            h.assertTrue(
                    actual.equals(authored, .0001f), "same target lost authored wrist/arm roll");
            if (previous != null)
                h.assertTrue(
                        actual.transformDirection(new Vector3f(1, 0, 0))
                                        .distance(
                                                previous.transformDirection(new Vector3f(1, 0, 0)))
                                < .002f,
                        "roll flipped near inverted arm");
            previous = actual;
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "arms_dense_transitions")
    public static void dense(GameTestHelper h) {
        floor(h);
        var r = SamuraiRig.get(2);
        var idle = r.pose(SamuraiRig.IDLE_TWO, 0);
        SamuraiRig.Pose[] poses = {
            idle,
            r.pose(SamuraiRig.WALK_TWO, .4f),
            r.pose(SamuraiRig.COMBO_TWO, .4f),
            r.pose(SamuraiRig.COMBO_TWO, 1.3f),
            r.pose(SamuraiRig.COMBO_TWO, 2.5f),
            StompMotion.pose(r, idle, StompMotion.IMPACT, SamuraiRig.IDLE_TWO)
        };
        for (int a = 0; a < poses.length; a++)
            for (int b = 0; b < poses.length; b++) {
                var previous = FootworkMotion.join(r, poses[a], poses[b], 0);
                for (int frame = 1; frame <= 200; frame++) {
                    var now = FootworkMotion.join(r, poses[a], poses[b], frame / 200f);
                    for (String name : new String[] {"左肩臂", "右肩臂", "肘_L", "肘_R", "腕_L", "腕_R"}) {
                        int i = r.bone(name);
                        var point = new Vector3f(r.bones.get(i).origin).add(1, 2, 1);
                        var p = now.matrices()[i].transformPosition(new Vector3f(point));
                        float distance =
                                p.distance(
                                                previous.matrices()[i].transformPosition(
                                                        new Vector3f(point)))
                                        * SamuraiRig.SCALE;
                        h.assertTrue(
                                p.isFinite() && distance < .15,
                                "arm discontinuity "
                                        + a
                                        + " -> "
                                        + b
                                        + " bone="
                                        + name
                                        + " frame="
                                        + frame
                                        + " gap="
                                        + distance);
                    }
                    previous = now;
                }
            }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "arms_endpoints")
    public static void endpoints(GameTestHelper h) {
        floor(h);
        var r = SamuraiRig.get(2);
        var from = r.pose(SamuraiRig.IDLE_TWO, 0);
        var to = r.pose(SamuraiRig.WALK_TWO, .4f);
        for (float t : new float[] {.00001f, .99999f}) {
            var pose = FootworkMotion.join(r, from, to, t);
            var end = t < .5 ? from : to;
            for (String name : new String[] {"左肩臂", "右肩臂", "肘_L", "肘_R", "腕_L", "腕_R"}) {
                int i = r.bone(name);
                var p = r.bones.get(i).origin;
                for (var offset :
                        new Vector3f[] {
                            new Vector3f(), new Vector3f(0, 2, 0), new Vector3f(1, 0, 1)
                        }) {
                    var probe = new Vector3f(p).add(offset);
                    float d =
                            pose.matrices()[i]
                                            .transformPosition(new Vector3f(probe))
                                            .distance(
                                                    end.matrices()[i].transformPosition(
                                                            new Vector3f(probe)))
                                    * SamuraiRig.SCALE;
                    h.assertTrue(
                            d < .005,
                            "arm blend endpoint snap " + name + " at " + t + " distance=" + d);
                }
            }
        }
        h.succeed();
    }
}
