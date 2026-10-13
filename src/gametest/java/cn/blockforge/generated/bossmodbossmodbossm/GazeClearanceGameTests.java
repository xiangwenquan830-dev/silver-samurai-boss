package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import org.joml.*;
import java.lang.Math;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GazeClearanceGameTests {
    static float pitch(Vector3f f) {
        return (float) Math.toDegrees(Math.atan2(-f.y, Math.sqrt(f.x * f.x + f.z * f.z)));
    }

    static Vector3f front(Matrix4f m) {
        return m.transformDirection(new Vector3f(0, 0, -1)).normalize();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r38_bow_grip")
    public static void bow_grip(GameTestHelper h) {
        for (int phase : new int[] {1, 2}) {
            var rig = SamuraiRig.get(phase);
            String idle = phase == 1 ? "armed_idle" : SamuraiRig.IDLE_TWO;
            for (float yaw : new float[] {-42, 0, 42}) {
                var base = rig.pose(idle, 0);
                var pose = HeadLookMotion.apply(rig, rig.pose(idle, 0), yaw, 38, 1, 1);
                int torso = rig.bone("腰腹_提身控制");
                Matrix4f delta =
                        new Matrix4f(pose.matrices()[torso])
                                .mul(new Matrix4f(base.matrices()[torso]).invert());
                h.assertTrue(
                        pitch(front(pose.matrices()[torso])) >= 9.99f
                                && pitch(front(pose.matrices()[torso])) <= 10.01f,
                        "waist not slightly bowed");
                for (String name :
                        new String[] {"胸腹", "腕_L", "腕_R", "背挂武士刀_L", "机械夹鞘_L", "机械夹鞘_R"}) {
                    int i = rig.bone(name);
                    h.assertTrue(
                            new Matrix4f(delta)
                                    .mul(base.matrices()[i])
                                    .equals(pose.matrices()[i], .002f),
                            "bow detached grip/back mount: " + name);
                }
                for (String name :
                        phase == 1
                                ? new String[] {"左腿", "右腿", "骨盆"}
                                : new String[] {"左腿", "右腿", "骨盆", "背挂武士刀_R", "脱落_半面罩"}) {
                    int i = rig.bone(name);
                    h.assertTrue(
                            base.matrices()[i].equals(pose.matrices()[i], .00001f),
                            "bow moved legs or detached debris: " + name);
                }
                var before =
                        java.util.Arrays.stream(pose.matrices())
                                .map(Matrix4f::new)
                                .toArray(Matrix4f[]::new);
                HeadLookMotion.apply(rig, pose, yaw, 38, 1, 1);
                for (int i = 0; i < before.length; i++)
                    h.assertTrue(
                            before[i].equals(pose.matrices()[i], .002f),
                            "repeated pose capture accumulated bow: " + rig.bones.get(i).name);
            }
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r38_neck_limit")
    public static void neck_limit(GameTestHelper h) {
        for (int phase : new int[] {1, 2}) {
            var rig = SamuraiRig.get(phase);
            int head = rig.bone("头盔"), torso = rig.bone("腰腹_提身控制");
            for (float yaw = -42; yaw <= 42; yaw += 7)
                for (float pitch = 0; pitch <= 38; pitch += 2) {
                    var pose =
                            HeadLookMotion.apply(
                                    rig,
                                    rig.pose(phase == 1 ? "armed_idle" : SamuraiRig.IDLE_TWO, 0),
                                    yaw,
                                    pitch,
                                    1,
                                    1);
                    Vector3f relative =
                            new Matrix4f(pose.matrices()[torso])
                                    .invert()
                                    .transformDirection(front(pose.matrices()[head]));
                    h.assertTrue(
                            pitch(relative) <= .01, "head exceeds chest-relative downward limit");
                    if (pitch == 38) export(rig, pose, "phase" + phase + "-yaw" + (int) yaw);
                }
            export(
                    rig,
                    rig.pose(phase == 1 ? "armed_idle" : SamuraiRig.IDLE_TWO, 0),
                    "phase" + phase + "-neutral");
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r38_bow_continuity")
    public static void bow_continuity(GameTestHelper h) {
        for (int phase : new int[] {1, 2}) {
            var rig = SamuraiRig.get(phase);
            String idle = phase == 1 ? "armed_idle" : SamuraiRig.IDLE_TWO;
            SamuraiRig.Pose previous = null;
            for (int frame = 0; frame <= 152; frame++) {
                float angle = frame <= 76 ? frame * .5f : (152 - frame) * .5f;
                var pose = HeadLookMotion.apply(rig, rig.pose(idle, 0), 0, angle, 1, 1);
                if (previous != null)
                    for (String name : new String[] {"头盔", "腕_L", "腕_R", "背挂武士刀_L"}) {
                        int i = rig.bone(name);
                        Vector3f pivot = rig.bones.get(i).origin;
                        h.assertTrue(
                                pose.matrices()[i]
                                                .transformPosition(new Vector3f(pivot))
                                                .distance(
                                                        previous.matrices()[i].transformPosition(
                                                                new Vector3f(pivot)))
                                        < .12,
                                "gaze interpolation pops: " + name);
                    }
                previous = pose;
                if (frame % 19 == 0) export(rig, pose, "phase" + phase + "-frame" + frame);
            }
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r38_authored_actions")
    public static void authored_actions(GameTestHelper h) {
        for (int state :
                new int[] {
                    SamuraiEntity.DRAW,
                    SamuraiEntity.SHEATHE,
                    SamuraiEntity.ATTACK_A,
                    SamuraiEntity.ATTACK_B,
                    SamuraiEntity.STOMP,
                    SamuraiEntity.CLIMB,
                    SamuraiEntity.TRANSITION,
                    SamuraiEntity.DEAD
                })
            h.assertFalse(HeadLookMotion.bowAllowed(state), "bow overwrites authored action");
        for (int phase : new int[] {1, 2}) {
            var rig = SamuraiRig.get(phase);
            String clip = phase == 1 ? SamuraiRig.A : SamuraiRig.COMBO_TWO;
            int head = rig.bone("头盔");
            for (float t = 0; t < rig.length(clip); t += .05f) {
                var base = rig.pose(clip, t);
                var pose = HeadLookMotion.apply(rig, rig.pose(clip, t), 20, 38, 1, 0);
                for (int i = 0; i < rig.bones.size(); i++) {
                    boolean child = false;
                    for (int j = i; j >= 0; j = rig.bones.get(j).parent)
                        if (j == head) {
                            child = true;
                            break;
                        }
                    if (!child)
                        h.assertTrue(
                                base.matrices()[i].equals(pose.matrices()[i], .00001f),
                                "gaze altered attack sword/body path");
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r38_face_clearance")
    public static void face_clearance(GameTestHelper h) {
        for (int phase : new int[] {1, 2}) {
            var rig = SamuraiRig.get(phase);
            int head = rig.bone("头盔"), chest = rig.bone("胸腹");
            for (float yaw = -42; yaw <= 42; yaw += 7)
                for (float pitch = 0; pitch <= 38; pitch += 2) {
                    var pose =
                            HeadLookMotion.apply(
                                    rig,
                                    rig.pose(phase == 1 ? "armed_idle" : SamuraiRig.IDLE_TWO, 0),
                                    yaw,
                                    pitch,
                                    1,
                                    1);
                    var neck =
                            rig.cubes.stream()
                                    .filter(c -> c.name.equals("neck"))
                                    .findFirst()
                                    .orElseThrow();
                    var neckBox = corners(rig, pose, neck);
                    boolean connected = false;
                    for (var a : rig.cubes) {
                        if (a.bone != head || a.debris) continue;
                        var box = corners(rig, pose, a);
                        connected |= depth(box, neckBox) > .001f;
                        if (!(a.name.equals("helmet_main")
                                || a.name.equals("helmet_faceplate")
                                || a.name.startsWith("mask_retained")
                                || a.name.startsWith("helmet_shell"))) continue;
                        for (var b : rig.cubes)
                            if (b.bone == chest && !b.name.equals("neck"))
                                h.assertTrue(
                                        depth(box, corners(rig, pose, b)) < .015f,
                                        "face/chest penetration phase="
                                                + phase
                                                + " yaw="
                                                + yaw
                                                + " pitch="
                                                + pitch
                                                + " "
                                                + a.name
                                                + " / "
                                                + b.name);
                    }
                    h.assertTrue(
                            connected,
                            "helmet lost neck contact phase="
                                    + phase
                                    + " yaw="
                                    + yaw
                                    + " pitch="
                                    + pitch);
                }
        }
        h.succeed();
    }

    private static Vector3f[] corners(SamuraiRig rig, SamuraiRig.Pose pose, SamuraiRig.Cube c) {
        var m = rig.cubeMatrix(c, pose);
        Vector3f[] v = new Vector3f[8];
        for (int i = 0; i < 8; i++)
            v[i] =
                    m.transformPosition(
                            new Vector3f(
                                    (i & 1) == 0 ? c.from.x : c.to.x,
                                    (i & 2) == 0 ? c.from.y : c.to.y,
                                    (i & 4) == 0 ? c.from.z : c.to.z));
        return v;
    }

    private static float depth(Vector3f[] a, Vector3f[] b) {
        var axes = new java.util.ArrayList<Vector3f>();
        Vector3f[] aa = new Vector3f[3], bb = new Vector3f[3];
        int[] index = {1, 2, 4};
        for (int i = 0; i < 3; i++) {
            aa[i] = new Vector3f(a[index[i]]).sub(a[0]).normalize();
            bb[i] = new Vector3f(b[index[i]]).sub(b[0]).normalize();
            axes.add(aa[i]);
            axes.add(bb[i]);
        }
        for (var x : aa) for (var y : bb) axes.add(new Vector3f(x).cross(y));
        float depth = Float.POSITIVE_INFINITY;
        for (var axis : axes) {
            if (axis.lengthSquared() < 1e-8f) continue;
            axis.normalize();
            float amin = Float.POSITIVE_INFINITY,
                    bmin = amin,
                    amax = Float.NEGATIVE_INFINITY,
                    bmax = amax;
            for (var v : a) {
                float p = v.dot(axis);
                amin = Math.min(amin, p);
                amax = Math.max(amax, p);
            }
            for (var v : b) {
                float p = v.dot(axis);
                bmin = Math.min(bmin, p);
                bmax = Math.max(bmax, p);
            }
            float overlap = Math.min(amax, bmax) - Math.max(amin, bmin);
            if (overlap <= 0) return 0;
            depth = Math.min(depth, overlap);
        }
        return depth;
    }

    private static void export(SamuraiRig rig, SamuraiRig.Pose pose, String name) {
        var cubes = new com.google.gson.JsonArray();
        for (var c : rig.cubes) {
            if (c.debris || (c.bone >= 0 && !pose.visible()[c.bone])) continue;
            var item = new com.google.gson.JsonObject();
            item.addProperty("name", c.name);
            item.addProperty("bone", c.bone < 0 ? "" : rig.bones.get(c.bone).name);
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
            var dir = java.nio.file.Path.of("gaze-preview");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(dir.resolve(name + ".json"), cubes.toString());
        } catch (java.io.IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
