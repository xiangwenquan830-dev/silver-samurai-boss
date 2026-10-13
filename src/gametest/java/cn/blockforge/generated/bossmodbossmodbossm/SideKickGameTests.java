package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import org.joml.*;
import java.lang.Math;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SideKickGameTests {
    @GameTest(template = "arena", timeoutTicks = 30, batch = "sidekick_sweep")
    public static void sidekick_sweep(GameTestHelper h) {
        for (int phase : new int[] {1, 2}) {
            var rig = SamuraiRig.get(phase);
            String idle = phase == 1 ? "armed_idle" : SamuraiRig.IDLE_TWO;
            var base = rig.pose(idle, 0);
            int support = rig.bone("行走_踝_L"),
                    foot = rig.bone("行走_踝_R"),
                    torso = rig.bone("腰腹_提身控制"),
                    sword = rig.bone("背挂武士刀_L");
            var load = StompMotion.pose(rig, base, .34f, idle);
            var strike = StompMotion.pose(rig, base, StompMotion.IMPACT, idle);
            float yawA = yaw(load, base, torso), yawB = yaw(strike, base, torso);
            int pelvis = rig.bone("骨盆");
            float hipA = yaw(load, base, pelvis), hipB = yaw(strike, base, pelvis);
            h.assertTrue(
                    yawA > 15 && yawA < 21 && yawB < -25 && yawB > -31,
                    "chest must counter-turn away from the right kick");
            h.assertTrue(
                    hipA < -8 && hipB > 10 && yawB * hipB < 0 && Math.abs(yawB - hipB) > 36,
                    "hip and chest turn together instead of opposing");
            h.assertTrue(
                    Math.abs(yawB - yawA) > 40, "chest lacks counter-rotation through release");
            float early =
                    Math.abs(
                                    yaw(StompMotion.pose(rig, base, .20f, idle), base, torso)
                                            - yaw(
                                                    StompMotion.pose(rig, base, .10f, idle),
                                                    base,
                                                    torso))
                            / .10f;
            float burst =
                    Math.abs(
                                    yaw(StompMotion.pose(rig, base, .50f, idle), base, torso)
                                            - yaw(
                                                    StompMotion.pose(rig, base, .43f, idle),
                                                    base,
                                                    torso))
                            / .07f;
            float recover =
                    Math.abs(
                                    yaw(StompMotion.pose(rig, base, 1.0f, idle), base, torso)
                                            - yaw(
                                                    StompMotion.pose(rig, base, .8f, idle),
                                                    base,
                                                    torso))
                            / .20f;
            h.assertTrue(
                    burst > early * 2 && burst > recover * 4,
                    "release is not faster than load and recovery");
            Vector3f previousFoot = null;
            Vector3f[] previousHand = new Vector3f[2];
            for (int frame = 0; frame <= 150; frame++) {
                float time = frame / 120f;
                var pose = StompMotion.pose(rig, base, time, idle);
                if (time >= .49f && time <= .95f)
                    h.assertTrue(
                            yaw(pose, base, torso) < 0 && yaw(pose, base, pelvis) > 0,
                            "counter-rotation reverses during kick or recovery");
                h.assertTrue(
                        pose.matrices()[support].equals(base.matrices()[support], .0001f),
                        "support foot slides or spins");
                var at =
                        pose.matrices()[foot].transformPosition(
                                new Vector3f(rig.bones.get(foot).origin));
                if (previousFoot != null)
                    h.assertTrue(
                            at.distance(previousFoot) * SamuraiRig.SCALE < .15,
                            "kick foot jumps between render subframes");
                previousFoot = at;
                for (int side = 0; side < 2; side++) {
                    int wrist = rig.bone(side == 0 ? "腕_L" : "腕_R");
                    var hand =
                            pose.matrices()[wrist].transformPosition(
                                    new Vector3f(rig.bones.get(wrist).origin));
                    if (previousHand[side] != null)
                        h.assertTrue(
                                hand.distance(previousHand[side]) * SamuraiRig.SCALE < .22,
                                "waist/hand snaps during kick");
                    previousHand[side] = hand;
                    if (phase == 2) {
                        var relation =
                                new Matrix4f(pose.matrices()[sword])
                                        .invert()
                                        .mul(pose.matrices()[wrist]);
                        var expected =
                                new Matrix4f(base.matrices()[sword])
                                        .invert()
                                        .mul(base.matrices()[wrist]);
                        h.assertTrue(
                                relation.equals(expected, .001f),
                                "two-handed grip opens during waist turn");
                    }
                }
                if (frame % 2 == 0) export(rig, pose, phase, frame / 2);
            }
            var end = StompMotion.pose(rig, base, StompMotion.DURATION, idle);
            for (int i = 0; i < rig.bones.size(); i++)
                h.assertTrue(
                        end.matrices()[i].equals(base.matrices()[i], .001f),
                        "kick does not return to original stance: " + rig.bones.get(i).name);
            GeneratedMod.LOG.info(
                    "[r31 counterkick] phase={} chestLoad={} chestImpact={} hipLoad={} hipImpact={} burstDegPerSec={} recoverDegPerSec={}",
                    phase,
                    yawA,
                    yawB,
                    hipA,
                    hipB,
                    burst,
                    recover);
        }
        h.succeed();
    }

    private static float yaw(SamuraiRig.Pose pose, SamuraiRig.Pose base, int bone) {
        return (float)
                Math.toDegrees(
                        new Matrix4f(pose.matrices()[bone])
                                .mul(new Matrix4f(base.matrices()[bone]).invert())
                                .getEulerAnglesXYZ(new Vector3f())
                                .y);
    }

    private static void export(SamuraiRig rig, SamuraiRig.Pose pose, int phase, int frame) {
        var cubes = new com.google.gson.JsonArray();
        for (var c : rig.cubes) {
            if (c.debris || (c.bone >= 0 && !pose.visible()[c.bone])) continue;
            var item = new com.google.gson.JsonObject();
            item.addProperty("name", c.name);
            var vertices = new com.google.gson.JsonArray();
            var matrix = rig.cubeMatrix(c, pose);
            for (int i = 0; i < 8; i++) {
                var point =
                        matrix.transformPosition(
                                new Vector3f(
                                        (i & 1) == 0 ? c.from.x : c.to.x,
                                        (i & 2) == 0 ? c.from.y : c.to.y,
                                        (i & 4) == 0 ? c.from.z : c.to.z));
                var v = new com.google.gson.JsonArray();
                v.add(point.x);
                v.add(point.y);
                v.add(point.z);
                vertices.add(v);
            }
            item.add("vertices", vertices);
            cubes.add(item);
        }
        try {
            var dir = java.nio.file.Path.of("sidekick-preview");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(
                    dir.resolve(
                            String.format(
                                    java.util.Locale.ROOT, "phase%d-%03d.json", phase, frame)),
                    cubes.toString());
        } catch (java.io.IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
