package io.github.xiangwenquan830.silversamurai;

import com.google.gson.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** 原格式读取交接包；客户端画面和服务端刀路共用同一套骨骼采样，不另造命中圆。 */
public final class SamuraiRig {
    public static final float SCALE = .1125f; // 用户r4：约4.5格，不回退到r3尺寸
    public static final String DRAW_STILL = "拔刀_提身上抽_双刀";
    public static final String DRAW = "行进拔刀_双刀低展压迫", SHEATHE = "行进收刀_低展回插_双刀";
    public static final String WALK_ONE = "行走_背刀负重_循环", A = "战斗_A_后摆拧腰滑步上挑", B = "战斗_B_合手蓄力双刀砸地";
    public static final String TRANSITION = "二阶段_踉跄单手_站直换侧双手立刀", IDLE_TWO = "二阶段_侧身双手立刀_待机";
    public static final String WALK_TWO = "二阶段_双手立刀_负重行走", COMBO_TWO = "二阶段_双手三连_斜劈横斩突进重劈";
    private static SamuraiRig one, two;

    public static synchronized SamuraiRig get(int phase) {
        if (phase == 1) {
            if (one == null) one = new SamuraiRig(1);
            return one;
        }
        if (two == null) two = new SamuraiRig(2);
        return two;
    }

    /** Called during common setup; no world, renderer or config access is required. */
    static void preload() {
        get(1);
        get(2);
    }

    public record Key(float time, Vector3f value, boolean step) {}

    public static final class Track {
        final List<Key> position = new ArrayList<>(),
                rotation = new ArrayList<>(),
                scale = new ArrayList<>();

        List<Key> channel(String name) {
            return name.equals("position") ? position : name.equals("rotation") ? rotation : scale;
        }

        Vector3f sample(List<Key> keys, float time, float fallback) {
            if (keys.isEmpty()) return new Vector3f(fallback);
            if (time <= keys.get(0).time) return new Vector3f(keys.get(0).value);
            int lo = 0, hi = keys.size() - 1;
            if (time >= keys.get(hi).time) return new Vector3f(keys.get(hi).value);
            while (hi - lo > 1) {
                int m = (lo + hi) / 2;
                if (keys.get(m).time <= time) lo = m;
                else hi = m;
            }
            Key x = keys.get(lo), y = keys.get(hi);
            return new Vector3f(x.value)
                    .lerp(
                            y.value,
                            x.step ? 0 : (time - x.time) / Math.max(.000001f, y.time - x.time));
        }
    }

    public static final class Clip {
        public final String name;
        public final float length;
        final boolean loop;
        final Map<String, Track> tracks = new HashMap<>();

        Clip(String name, float length, boolean loop, Map<String, Track> tracks) {
            this.name = name;
            this.length = length;
            this.loop = loop;
            this.tracks.putAll(tracks);
        }

        Clip(JsonObject a) {
            name = a.get("name").getAsString();
            length = a.get("length").getAsFloat();
            loop = "loop".equals(a.get("loop").getAsString());
            for (var entry : a.getAsJsonObject("animators").entrySet()) {
                JsonObject v = entry.getValue().getAsJsonObject();
                if (!v.has("keyframes")) continue;
                Track t = new Track();
                for (JsonElement e : v.getAsJsonArray("keyframes")) {
                    JsonObject k = e.getAsJsonObject();
                    String ch = k.get("channel").getAsString();
                    if (!Set.of("rotation", "position", "scale").contains(ch)) continue;
                    JsonObject data = k.getAsJsonArray("data_points").get(0).getAsJsonObject();
                    t.channel(ch)
                            .add(
                                    new Key(
                                            k.get("time").getAsFloat(),
                                            new Vector3f(
                                                    data.get("x").getAsFloat(),
                                                    data.get("y").getAsFloat(),
                                                    data.get("z").getAsFloat()),
                                            k.has("interpolation")
                                                    && "step"
                                                            .equals(
                                                                    k.get("interpolation")
                                                                            .getAsString())));
                }
                for (List<Key> list : List.of(t.position, t.rotation, t.scale))
                    list.sort(Comparator.comparingDouble(Key::time));
                tracks.put(entry.getKey(), t);
            }
        }
    }

    public static final class Bone {
        public final String uuid, name;
        public final Vector3f origin, rotation;
        public int parent = -1;

        Bone(JsonObject b) {
            uuid = b.get("uuid").getAsString();
            name = b.get("name").getAsString();
            origin = vec(b, "origin", 0);
            rotation = vec(b, "rotation", 0);
        }
    }

    public record Face(String side, float[] uv, int texture, int rotation) {}

    public static final class Cube {
        public final String name, uuid;
        public final Vector3f from, to, origin, rotation;
        public final List<Face> faces = new ArrayList<>();
        public int bone = -1;
        public boolean debris;

        Cube(JsonObject c) {
            name = c.get("name").getAsString();
            uuid = c.get("uuid").getAsString();
            from = vec(c, "from", 0);
            to = vec(c, "to", 0);
            origin = vec(c, "origin", 0);
            rotation = vec(c, "rotation", 0);
            for (var e : c.getAsJsonObject("faces").entrySet()) {
                JsonObject f = e.getValue().getAsJsonObject();
                if (!f.has("texture") || f.get("texture").isJsonNull()) continue;
                JsonArray uv = f.getAsJsonArray("uv");
                faces.add(
                        new Face(
                                e.getKey(),
                                new float[] {
                                    uv.get(0).getAsFloat(),
                                    uv.get(1).getAsFloat(),
                                    uv.get(2).getAsFloat(),
                                    uv.get(3).getAsFloat()
                                },
                                f.get("texture").getAsInt(),
                                f.has("rotation") ? f.get("rotation").getAsInt() : 0));
            }
        }

        public boolean blade() {
            return name.startsWith("katana_")
                    && (name.contains("blade")
                            || name.contains("edge")
                            || name.contains("tip")
                            || name.contains("kissaki"));
        }
    }

    public record Pose(Matrix4f[] matrices, boolean[] visible) {}

    public record Blade(Vec3 base, Vec3 tip) {}

    public final int phase;
    public final List<Bone> bones = new ArrayList<>();
    public final List<Cube> cubes = new ArrayList<>();
    public final Map<String, Clip> clips = new LinkedHashMap<>();
    final Map<String, Integer> boneIds = new HashMap<>();
    final Map<String, Cube> cubeIds = new HashMap<>();
    private final Map<String, Matrix4f> sideGrips = new HashMap<>();

    private SamuraiRig(int phase) {
        this(phase, RuntimeRigReader.read(phase));
    }

    // Kept as an independent full-tree reference for authoring/runtime equivalence tests.
    SamuraiRig(int phase, JsonObject root) {
        this(phase, RuntimeRigReader.reference(root));
    }

    private SamuraiRig(int phase, RuntimeRigReader.Data data) {
        this.phase = phase;
        JsonObject root = data.geometry();
        for (JsonElement e : root.getAsJsonArray("groups")) {
            Bone b = new Bone(e.getAsJsonObject());
            boneIds.put(b.uuid, bones.size());
            bones.add(b);
        }
        for (JsonElement e : root.getAsJsonArray("elements")) {
            Cube c = new Cube(e.getAsJsonObject());
            cubes.add(c);
            cubeIds.put(c.uuid, c);
        }
        for (JsonElement e : root.getAsJsonArray("outliner")) link(e, -1, false);
        for (Clip c : data.clips()) clips.put(c.name, c);
        Pose rest = pose(phase == 1 ? A : IDLE_TWO, 0);
        for (String hand : phase == 1 ? List.of("L", "R") : List.of("L")) {
            sideGrips.put(
                    hand,
                    new Matrix4f(rest.matrices[bone("腕_" + hand)])
                            .invert()
                            .mul(rest.matrices[bone("背挂武士刀_" + hand)]));
        }
    }

    private void link(JsonElement element, int parent, boolean debris) {
        if (element.isJsonPrimitive()) {
            Cube c = cubeIds.get(element.getAsString());
            if (c != null) {
                c.bone = parent;
                c.debris = debris;
            }
            return;
        }
        JsonObject n = element.getAsJsonObject();
        int index = boneIds.get(n.get("uuid").getAsString());
        Bone b = bones.get(index);
        b.parent = parent;
        boolean detached =
                debris
                        || b.name.contains("碎片")
                        || b.name.contains("脱落")
                        || (phase == 2 && b.name.equals("背挂武士刀_R"));
        for (JsonElement child : n.getAsJsonArray("children")) link(child, index, detached);
    }

    Vec3 rootMotion(String name, float time) {
        Clip c = clips.get(name);
        int i = bone("行走_重心");
        if (c == null || i < 0) return Vec3.ZERO;
        Vector3f v =
                value(c.tracks.get(bones.get(i).uuid), 0, Math.max(0, Math.min(time, c.length)));
        return new Vec3(v.x, v.y, v.z);
    }

    static Vec3 local(Vec3 world, float yaw) {
        double r = Math.toRadians(180 - yaw), c = Math.cos(r), s = Math.sin(r);
        return new Vec3(world.x * c - world.z * s, world.y, world.x * s + world.z * c);
    }

    public float length(String clip) {
        Clip c = clips.get(clip);
        return c == null ? 0 : c.length;
    }

    public Pose pose(String clip, float time) {
        return pose(clip, time, null, 0, 1, 0);
    }

    public Pose pose(
            String name, float time, String from, float fromTime, float blend, float death) {
        return pose(name, time, from, fromTime, blend, death, 0);
    }

    public Pose pose(
            String name,
            float time,
            String from,
            float fromTime,
            float blend,
            float death,
            float aimPitch) {
        Clip c = clips.get(name), old = clips.get(from);
        time = name.equals("armed_walk") ? time : clampTime(c, time);
        fromTime = "armed_walk".equals(from) ? fromTime : clampTime(old, fromTime);
        Matrix4f[] matrices = new Matrix4f[bones.size()];
        boolean[] visible = new boolean[bones.size()];
        Vector3f[] p = new Vector3f[bones.size()], s = new Vector3f[bones.size()];
        Quaternionf[] q = new Quaternionf[bones.size()];
        for (int i = 0; i < bones.size(); i++) {
            Bone b = bones.get(i);
            p[i] = boneValue(b, name, time, 0);
            s[i] = boneValue(b, name, time, 2);
            q[i] = rotation(boneValue(b, name, time, 1).add(b.rotation));
            if (from != null && blend < 1) {
                p[i] = boneValue(b, from, fromTime, 0).lerp(p[i], blend);
                q[i] = rotation(boneValue(b, from, fromTime, 1).add(b.rotation)).slerp(q[i], blend);
                s[i] = boneValue(b, from, fromTime, 2).lerp(s[i], blend);
            }
        }
        Vector3f rootMotion = new Vector3f();
        for (int i = 0; i < bones.size(); i++)
            if (bones.get(i).name.equals("行走_重心")) rootMotion.set(p[i]);
        for (int i = 0; i < bones.size(); i++)
            if (bones.get(i).parent < 0) {
                p[i].x -= rootMotion.x;
                p[i].z -= rootMotion.z;
            }
        for (int i = 0; i < bones.size(); i++) matrix(i, matrices, visible, p, q, s);
        float finisherYaw =
                B.equals(name)
                        ? 12 * smooth((time - 1.7f) / .4f) * (1 - smooth((time - 2.65f) / .75f))
                        : 0;
        if (Math.abs(aimPitch) > .0001f || Math.abs(finisherYaw) > .0001f) {
            int torso = bone("腰腹_提身控制");
            if (torso >= 0) {
                Bone b = bones.get(torso);
                Matrix4f pivot = b.parent < 0 ? new Matrix4f() : new Matrix4f(matrices[b.parent]);
                pivot.translate(p[torso]).translate(b.origin);
                Matrix4f delta =
                        new Matrix4f(pivot)
                                .rotateY(rad(finisherYaw))
                                .rotateX(rad(aimPitch))
                                .mul(new Matrix4f(pivot).invert());
                for (int i = 0; i < bones.size(); i++) {
                    boolean upper = false;
                    for (int j = i; j >= 0; j = bones.get(j).parent)
                        if (j == torso || bones.get(j).name.startsWith("背挂武士刀_")) {
                            upper = true;
                            break;
                        }
                    if (upper) matrices[i] = new Matrix4f(delta).mul(matrices[i]);
                }
            }
        }
        Pose result = new Pose(matrices, visible);
        return SideIntercept.clip(name) ? lockSideGrip(result) : result;
    }

    /** Do not interpolate the sword independently from its holding hand during side cuts. */
    Pose lockSideGrip(Pose pose) {
        for (var entry : sideGrips.entrySet()) {
            int wrist = bone("腕_" + entry.getKey()), sword = bone("背挂武士刀_" + entry.getKey());
            Matrix4f wanted = new Matrix4f(pose.matrices[wrist]).mul(entry.getValue());
            Matrix4f delta = new Matrix4f(wanted).mul(new Matrix4f(pose.matrices[sword]).invert());
            for (int i = 0; i < bones.size(); i++)
                for (int j = i; j >= 0; j = bones.get(j).parent)
                    if (j == sword) {
                        pose.matrices[i] = new Matrix4f(delta).mul(pose.matrices[i]);
                        break;
                    }
        }
        return pose;
    }

    private Vector3f boneValue(Bone bone, String name, float time, int channel) {
        if ("armed_idle".equals(name)) {
            Clip source = clips.get(phase == 2 ? IDLE_TWO : A);
            return value(source == null ? null : source.tracks.get(bone.uuid), channel, 0);
        }
        if ("armed_walk".equals(name)) {
            boolean leg =
                    bone.name.startsWith("行走_")
                            || bone.name.contains("腿")
                            || bone.name.contains("膝")
                            || bone.name.contains("踝")
                            || bone.name.contains("裙")
                            || bone.parent < 0;
            Clip source =
                    clips.get(phase == 2 ? (leg ? WALK_TWO : IDLE_TWO) : (leg ? WALK_ONE : A));
            return value(
                    source == null ? null : source.tracks.get(bone.uuid),
                    channel,
                    leg ? clampTime(source, time) : 0);
        }
        Clip source = clips.get(name);
        return value(source == null ? null : source.tracks.get(bone.uuid), channel, time);
    }

    private static float clampTime(Clip c, float t) {
        return c == null
                ? 0
                : c.loop
                        ? (t % c.length + c.length) % c.length
                        : Math.max(0, Math.min(t, c.length));
    }

    private static Vector3f value(Track t, int ch, float time) {
        if (t == null) return new Vector3f(ch == 2 ? 1 : 0);
        return t.sample(
                ch == 0 ? t.position : ch == 1 ? t.rotation : t.scale, time, ch == 2 ? 1 : 0);
    }

    private void matrix(
            int i, Matrix4f[] out, boolean[] visible, Vector3f[] p, Quaternionf[] r, Vector3f[] s) {
        if (out[i] != null) return;
        Bone b = bones.get(i);
        Matrix4f m = new Matrix4f();
        boolean show = true;
        if (b.parent >= 0) {
            matrix(b.parent, out, visible, p, r, s);
            m.set(out[b.parent]);
            show = visible[b.parent];
        }
        m.translate(p[i])
                .translate(b.origin)
                .rotate(r[i])
                .scale(s[i])
                .translate(-b.origin.x, -b.origin.y, -b.origin.z);
        out[i] = m;
        visible[i] = show && Math.abs(s[i].x * s[i].y * s[i].z) > .000001f;
    }

    public Matrix4f cubeMatrix(Cube c, Pose pose) {
        Matrix4f m = c.bone < 0 ? new Matrix4f() : new Matrix4f(pose.matrices[c.bone]);
        return m.translate(c.origin)
                .rotateZ(rad(c.rotation.z))
                .rotateY(rad(c.rotation.y))
                .rotateX(rad(c.rotation.x))
                .translate(-c.origin.x, -c.origin.y, -c.origin.z);
    }

    public List<Blade> blades(String clip, float t) {
        return blades(clip, t, 0);
    }

    public List<Blade> blades(String clip, float t, float aim) {
        return blades(
                pose(clip, t, null, 0, 1, 0, aim),
                SideIntercept.clip(clip) ? SideIntercept.hand(clip) : null);
    }

    public List<Blade> blades(Pose pose) {
        return blades(pose, null);
    }

    public List<Blade> blades(Pose pose, String hand) {
        List<Blade> result = new ArrayList<>();
        for (Cube c : cubes) {
            if (hand != null && !c.name.startsWith("katana_" + hand + "_")) continue;
            if (!c.blade() || c.debris || c.bone >= 0 && !pose.visible[c.bone]) continue;
            Vector3f a = new Vector3f(c.from).add(c.to).mul(.5f),
                    b = new Vector3f(a),
                    d = new Vector3f(c.to).sub(c.from);
            int axis = d.x > d.y ? (d.x > d.z ? 0 : 2) : (d.y > d.z ? 1 : 2);
            a.setComponent(axis, c.from.get(axis));
            b.setComponent(axis, c.to.get(axis));
            Matrix4f m = cubeMatrix(c, pose);
            m.transformPosition(a);
            m.transformPosition(b);
            result.add(
                    new Blade(
                            new Vec3(a.x * SCALE, a.y * SCALE, a.z * SCALE),
                            new Vec3(b.x * SCALE, b.y * SCALE, b.z * SCALE)));
        }
        return result;
    }

    public static Vec3 world(Vec3 local, Vec3 origin, float yaw) {
        double r = Math.toRadians(180 - yaw), c = Math.cos(r), s = Math.sin(r);
        return origin.add(local.x * c + local.z * s, local.y, -local.x * s + local.z * c);
    }

    public static Vector3f vec(JsonObject object, String name, float fallback) {
        if (!object.has(name)) return new Vector3f(fallback);
        JsonArray a = object.getAsJsonArray(name);
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }

    public int bone(String name) {
        for (int i = 0; i < bones.size(); i++) if (bones.get(i).name.equals(name)) return i;
        return -1;
    }

    public static Quaternionf rotation(Vector3f r) {
        return new Quaternionf().rotationZYX(rad(r.z), rad(r.y), rad(r.x));
    }

    public static float smooth(float f) {
        f = Math.max(0, Math.min(1, f));
        return f * f * (3 - 2 * f);
    }

    private static float rad(float degrees) {
        return degrees * (float) Math.PI / 180;
    }
}
