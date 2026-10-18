package io.github.xiangwenquan830.silversamurai;

import net.minecraft.nbt.CompoundTag;
import org.joml.*;
import java.lang.Math;

/** 有控制的分部接触死亡：膝锚定→骨盆失撑→胸肩落地→头部停机；不改原攻击轨道。 */
public final class DeathMotion {
    public static final int DURATION_TICKS = 76;
    public static final float DURATION = 3.8f;

    private static float ramp(float t, float a, float b) {
        return SamuraiRig.smooth((t - a) / (b - a));
    }

    private static float rad(float x) {
        return x * (float) Math.PI / 180;
    }

    public static CompoundTag encode(SamuraiRig.Pose pose, float red) {
        CompoundTag tag = new CompoundTag();
        int[] values = new int[pose.matrices().length * 17];
        for (int i = 0; i < pose.matrices().length; i++) {
            float[] m = new float[16];
            pose.matrices()[i].get(m);
            for (int j = 0; j < 16; j++) values[i * 17 + j] = Float.floatToIntBits(m[j]);
            values[i * 17 + 16] = pose.visible()[i] ? 1 : 0;
        }
        tag.putIntArray("Matrices", values);
        tag.putFloat("Red", red);
        return tag;
    }

    public static SamuraiRig.Pose decode(SamuraiRig rig, CompoundTag tag) {
        int[] values = tag.getIntArray("Matrices");
        if (values.length != rig.bones.size() * 17)
            return rig.pose(rig.phase == 2 ? SamuraiRig.IDLE_TWO : "armed_idle", 0);
        Matrix4f[] m = new Matrix4f[rig.bones.size()];
        boolean[] v = new boolean[m.length];
        for (int i = 0; i < m.length; i++) {
            float[] f = new float[16];
            for (int j = 0; j < 16; j++) {
                f[j] = Float.intBitsToFloat(values[i * 17 + j]);
                if (!Float.isFinite(f[j]))
                    return rig.pose(rig.phase == 2 ? SamuraiRig.IDLE_TWO : "armed_idle", 0);
            }
            m[i] = new Matrix4f().set(f);
            v[i] = values[i * 17 + 16] != 0;
        }
        return new SamuraiRig.Pose(m, v);
    }

    public static SamuraiRig.Pose pose(SamuraiRig rig, SamuraiRig.Pose source, float elapsed) {
        return pose(rig, source, elapsed, DeathSurface.flat());
    }

    private static Vector3f point(DeathSurface surface, float x, float height, float z) {
        Vector3f p = new Vector3f(x, 0, z).rotateY(rad(surface.direction));
        p.y = surface.height(p.x, p.z) + height;
        return p;
    }

    public static SamuraiRig.Pose pose(
            SamuraiRig rig, SamuraiRig.Pose source, float elapsed, DeathSurface surface) {
        if (source == null)
            source = rig.pose(rig.phase == 2 ? SamuraiRig.IDLE_TWO : "armed_idle", 0);
        float t = Math.max(0, Math.min(DURATION, elapsed));
        if (t <= 0) return source;
        float loss = ramp(t, .08f, .44f),
                kneel = ramp(t, .27f, 1.13f),
                second = ramp(t, .70f, 1.52f);
        // 支撑丢失先加速，接触前再减速；各部位不共用一个倒下系数。
        float pelvisFall = ramp(t, 1.65f, 2.83f),
                chestFall = ramp(t, 1.88f, 3.16f),
                headFall = ramp(t, 2.24f, 3.53f),
                settle = ramp(t, 3.22f, 3.8f);
        SamuraiRig.Pose rest = rig.pose("", 0);
        Matrix4f[] target = new Matrix4f[rig.bones.size()];
        boolean[] show = source.visible().clone();
        for (int i = 0; i < target.length; i++) target[i] = new Matrix4f(rest.matrices()[i]);
        float compact = surface.compact ? .5f : 1;
        float pelvisZ = -1.7f * kneel - 10.5f * pelvisFall * compact;
        float kneeY = point(surface, 3.4f, 4.05f, -3.7f).y;
        float supportedHip =
                kneeY
                        + 1.25f
                        + (float) Math.sqrt(Math.max(4, 81 - (pelvisZ + 3.7f) * (pelvisZ + 3.7f)));
        Vector3f pelvis = point(surface, 0, 0, pelvisZ);
        pelvis.y = (22.5f * (1 - kneel) + supportedHip * kneel) - .10f * settle;
        Matrix4f root =
                new Matrix4f()
                        .translate(pelvis)
                        .rotateY(rad(surface.direction * ramp(t, .3f, 1.3f)))
                        .translate(0, -22.5f, 0);
        for (int i = 0; i < target.length; i++) target[i] = new Matrix4f(root).mul(target[i]);
        SamuraiRig.Pose goal = new SamuraiRig.Pose(target, show);
        FootworkMotion.rotate(
                rig,
                goal,
                "腰腹_提身控制",
                -5 * loss - 14 * kneel - (surface.compact ? 40 : 78) * chestFall,
                0,
                12 * chestFall,
                true);
        FootworkMotion.rotate(
                rig,
                goal,
                "头盔",
                -4 * loss - 12 * ramp(t, .62f, 1.55f) - 10 * headFall,
                0,
                -7 * headFall,
                false);
        // 裙甲随骨盆而非拖着膝盖一起平移。
        for (int i = 0; i < rig.bones.size(); i++)
            if (rig.bones.get(i).name.startsWith("行走_随动_front"))
                FootworkMotion.rotate(rig, goal, rig.bones.get(i).name, 32 * kneel, 0, 0, false);
        Matrix4f[] result = new Matrix4f[target.length];
        for (int i = 0; i < result.length; i++) {
            String n = rig.bones.get(i).name;
            float join = ramp(t, .02f, .62f);
            if (n.equals("头盔")) join = ramp(t, .13f, .86f);
            result[i] = mix(source.matrices()[i], target[i], rig.bones.get(i).origin, join);
        }
        SamuraiRig.Pose out = new SamuraiRig.Pose(result, show);
        for (int side = 0; side < 2; side++) {
            String s = side == 0 ? "L" : "R";
            int hip = rig.bone(side == 0 ? "左腿" : "右腿"),
                    knee = rig.bone("行走_膝_" + s),
                    ankle = rig.bone("行走_踝_" + s);
            Vector3f a = rig.bones.get(hip).origin,
                    b = rig.bones.get(knee).origin,
                    c = rig.bones.get(ankle).origin;
            float contact = side == 0 ? kneel : second;
            Vector3f fixedKnee = point(surface, side == 0 ? 3.4f : -3.4f, 4.05f, -3.7f);
            Vector3f k =
                    source.matrices()[knee]
                            .transformPosition(new Vector3f(b))
                            .lerp(fixedKnee, contact);
            Vector3f foot =
                    source.matrices()[ankle]
                            .transformPosition(new Vector3f(c))
                            .lerp(point(surface, side == 0 ? 3.4f : -3.4f, 5.15f, 5.5f), contact);
            Vector3f h = result[hip].transformPosition(new Vector3f(a));
            branch(rig, out, hip, between(a, b, h, k));
            branch(rig, out, knee, between(b, c, k, foot));
            Matrix4f footTarget =
                    new Matrix4f()
                            .translate(foot)
                            .rotateY(rad(surface.direction))
                            .rotateX(rad(-58 * contact))
                            .translate(new Vector3f(c).negate());
            branch(rig, out, ankle, mix(source.matrices()[ankle], footTarget, c, contact));
        }
        // 胸肩接触采用自身几何，不允许长刀最低点把全身抬起。
        int waist = rig.bone("腰腹_提身控制");
        float upperLift = clearance(rig, out, surface, waist, false);
        float contactShift =
                upperLift > 0
                        ? upperLift * ramp(t, .65f, 1.2f)
                        : Math.max(-3, upperLift) * ramp(t, 2.65f, 3.45f);
        if (Math.abs(contactShift) > .0001f) {
            Matrix4f adjust = new Matrix4f().translate(0, contactShift, 0);
            branch(rig, out, waist, adjust.mul(result[waist]));
        }
        boolean[] constrained = {false, false};
        for (int side = 0; side < 2; side++) {
            String s = side == 0 ? "L" : "R";
            int sword = rig.bone("背挂武士刀_" + s),
                    wrist = rig.bone("腕_" + s),
                    spine = rig.bone("背部脊柱");
            if (sword < 0 || !show[sword]) continue;
            Vector3f grip = rig.bones.get(sword).origin, hand = rig.bones.get(wrist).origin;
            Vector3f oldGrip = source.matrices()[sword].transformPosition(new Vector3f(grip)),
                    oldHand = source.matrices()[wrist].transformPosition(new Vector3f(hand));
            if (oldGrip.distance(oldHand) > 5.5f) {
                result[sword] =
                        new Matrix4f(result[spine])
                                .mul(new Matrix4f(source.matrices()[spine]).invert())
                                .mul(source.matrices()[sword]);
                continue;
            }
            // 刀不松手：刀柄/刀尖先落稳，两手从致命姿态的柄内相对矩阵求解。
            int shoulder = rig.bone(side == 0 ? "左肩臂" : "右肩臂");
            Vector3f anchor =
                    result[shoulder].transformPosition(
                            new Vector3f(rig.bones.get(shoulder).origin));
            if (rig.phase == 2) {
                int other = rig.bone("右肩臂");
                anchor.lerp(
                        result[other].transformPosition(new Vector3f(rig.bones.get(other).origin)),
                        .5f);
            }
            Vector3f g =
                    anchor.add(new Vector3f(0, -8, -4).rotateY(rad(surface.direction)))
                            .lerp(
                                    point(
                                            surface,
                                            rig.phase == 2 ? 0 : side == 0 ? 8 : -8,
                                            3.6f,
                                            -12 * compact),
                                    ramp(t, 2.5f, 3.18f));
            g.y = Math.max(surface.height(g.x, g.z) + 3.6f, g.y);
            Vector3f tip =
                    new Vector3f(g)
                            .add(
                                    new Vector3f(side == 0 ? 1 : -1, 0, -29.6f)
                                            .rotateY(rad(surface.direction)));
            tip.y = surface.height(tip.x, tip.z) + .5f;
            Vector3f axis = new Vector3f(tip).sub(g).normalize();
            Matrix4f blade =
                    new Matrix4f()
                            .translate(g)
                            .rotate(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), axis))
                            .translate(new Vector3f(grip).negate());
            result[sword] = mix(source.matrices()[sword], blade, grip, ramp(t, .10f, 1.48f));
            float lift = clearance(rig, out, surface, sword, true);
            if (lift > 0) result[sword].m31(result[sword].m31() + lift * ramp(t, .12f, .65f));
            for (int j = 0; j < 2; j++) {
                String hs = j == 0 ? "L" : "R";
                int w = rig.bone("腕_" + hs);
                Vector3f wp =
                        source.matrices()[w].transformPosition(
                                new Vector3f(rig.bones.get(w).origin));
                if (wp.distance(oldGrip) > 5.5f || constrained[j]) continue;
                Matrix4f desired =
                        new Matrix4f(result[sword])
                                .mul(new Matrix4f(source.matrices()[sword]).invert())
                                .mul(source.matrices()[w]);
                solveDeathArm(rig, result, source, hs, desired, ramp(t, 0, .38f));
                constrained[j] = true;
            }
        }
        for (int side = 0; side < 2; side++)
            if (!constrained[side]) {
                String s = side == 0 ? "L" : "R";
                int wrist = rig.bone("腕_" + s);
                Vector3f c = rig.bones.get(wrist).origin;
                Vector3f palm = point(surface, side == 0 ? 9 : -9, 1.5f, -14 * compact);
                Matrix4f hand =
                        new Matrix4f()
                                .translate(palm)
                                .rotateX(rad(-80))
                                .translate(new Vector3f(c).negate());
                hand = mix(source.matrices()[wrist], hand, c, ramp(t, .55f, 1.85f));
                solveDeathArm(rig, result, source, s, hand, ramp(t, .2f, 1));
            }
        // 局部前臂触地修正：只升刀柄/手臂，不把胸膝等已落地部位一并抬起。
        for (int pass = 0; pass < 3; pass++)
            for (int side = 0; side < 2; side++)
                if (constrained[side] && (rig.phase == 1 || side == 0)) {
                    int sword = rig.bone("背挂武士刀_" + (side == 0 ? "L" : "R"));
                    float lift = 0;
                    for (int handSide = 0; handSide < 2; handSide++)
                        if (constrained[handSide] && (rig.phase == 2 || handSide == side))
                            lift =
                                    Math.max(
                                            lift,
                                            clearance(
                                                    rig,
                                                    out,
                                                    surface,
                                                    rig.bone("肘_" + (handSide == 0 ? "L" : "R")),
                                                    true));
                    if (lift > .001f) {
                        result[sword].m31(result[sword].m31() + lift * ramp(t, .8f, 1.55f));
                        for (int handSide = 0; handSide < 2; handSide++)
                            if (constrained[handSide] && (rig.phase == 2 || handSide == side)) {
                                String hs = handSide == 0 ? "L" : "R";
                                int w = rig.bone("腕_" + hs);
                                Matrix4f hand =
                                        new Matrix4f(result[sword])
                                                .mul(
                                                        new Matrix4f(source.matrices()[sword])
                                                                .invert())
                                                .mul(source.matrices()[w]);
                                solveDeathArm(rig, result, source, hs, hand, ramp(t, 0, .38f));
                            }
                    }
                }
        // 肩甲跟随最终求解后的上臂，避免手臂撑地而肩甲停在站姿。
        for (String s : new String[] {"L", "R"}) {
            int arm = rig.bone(s.equals("L") ? "左肩臂" : "右肩臂"), plate = rig.bone("肩甲避让_" + s);
            Matrix4f m =
                    new Matrix4f(result[arm])
                            .mul(new Matrix4f(rest.matrices()[arm]).invert())
                            .mul(rest.matrices()[plate]);
            branch(rig, out, plate, m);
        }
        if (rig.phase == 2) {
            String[] anchors = {"右肩臂", "头盔", "肩甲避让_L", "胸腹", "行走_膝_R", "左腿", "前侧裙甲", "前侧裙甲"};
            for (int part = 1; part < 8; part++) {
                int i = rig.bone(DebrisGeometry.NAMES[part]), a = rig.bone(anchors[part]);
                if (show[i])
                    result[i] =
                            new Matrix4f(result[a])
                                    .mul(new Matrix4f(source.matrices()[a]).invert())
                                    .mul(source.matrices()[i]);
            }
        }
        for (int i = 0; i < result.length; i++)
            if (!show[i]) result[i] = new Matrix4f(source.matrices()[i]);
        if (t < .38f) {
            float blend = ramp(t, 0, .38f);
            SamuraiRig.Pose joined = transition(rig, source, out, blend);
            for (int side = 0; side < 2; side++)
                if (constrained[side]) {
                    String s = side == 0 ? "L" : "R";
                    int w = rig.bone("腕_" + s),
                            sword = rig.bone("背挂武士刀_" + (rig.phase == 2 ? "L" : s));
                    Matrix4f hand =
                            new Matrix4f(joined.matrices()[sword])
                                    .mul(new Matrix4f(source.matrices()[sword]).invert())
                                    .mul(source.matrices()[w]);
                    solveDeathArm(rig, joined.matrices(), source, s, hand, blend);
                }
            return joined;
        }
        return out;
    }

    private static boolean child(SamuraiRig rig, int bone, int root) {
        for (int j = bone; j >= 0; j = rig.bones.get(j).parent) if (j == root) return true;
        return false;
    }

    private static void branch(
            SamuraiRig rig, SamuraiRig.Pose pose, int root, Matrix4f replacement) {
        Matrix4f delta =
                new Matrix4f(replacement).mul(new Matrix4f(pose.matrices()[root]).invert());
        for (int i = 0; i < rig.bones.size(); i++)
            if (child(rig, i, root))
                pose.matrices()[i] = new Matrix4f(delta).mul(pose.matrices()[i]);
    }

    private static float clearance(
            SamuraiRig rig, SamuraiRig.Pose pose, DeathSurface surface, int root, boolean sword) {
        float lift = Float.NEGATIVE_INFINITY;
        for (var c : rig.cubes) {
            if (c.debris || c.bone < 0 || !pose.visible()[c.bone] || !child(rig, c.bone, root))
                continue;
            if (!sword
                    && (c.name.startsWith("katana_")
                            || child(rig, c.bone, rig.bone("左肩臂"))
                            || child(rig, c.bone, rig.bone("右肩臂")))) continue;
            Matrix4f m = rig.cubeMatrix(c, pose);
            for (int n = 0; n < 8; n++) {
                Vector3f v =
                        m.transformPosition(
                                new Vector3f(
                                        (n & 1) == 0 ? c.from.x : c.to.x,
                                        (n & 2) == 0 ? c.from.y : c.to.y,
                                        (n & 4) == 0 ? c.from.z : c.to.z));
                lift = Math.max(lift, surface.height(v.x, v.z) + .08f - v.y);
            }
        }
        return Float.isFinite(lift) ? lift : 0;
    }

    public static SamuraiRig.Pose transition(
            SamuraiRig rig, SamuraiRig.Pose from, SamuraiRig.Pose target, float blend) {
        Matrix4f[] out = new Matrix4f[rig.bones.size()];
        for (int i = 0; i < out.length; i++)
            out[i] = mix(from.matrices()[i], target.matrices()[i], rig.bones.get(i).origin, blend);
        return new SamuraiRig.Pose(out, target.visible().clone());
    }

    static void solveArm(
            SamuraiRig rig,
            Matrix4f[] result,
            SamuraiRig.Pose source,
            String side,
            Matrix4f hand,
            float blend) {
        solveArm(rig, result, source, side, hand, blend, false);
    }

    private static void solveDeathArm(
            SamuraiRig rig,
            Matrix4f[] result,
            SamuraiRig.Pose source,
            String side,
            Matrix4f hand,
            float blend) {
        solveArm(rig, result, source, side, hand, blend, true);
    }

    private static void solveArm(
            SamuraiRig rig,
            Matrix4f[] result,
            SamuraiRig.Pose source,
            String side,
            Matrix4f hand,
            float blend,
            boolean death) {
        int shoulder = rig.bone(side.equals("L") ? "左肩臂" : "右肩臂"),
                elbow = rig.bone("肘_" + side),
                wrist = rig.bone("腕_" + side);
        Vector3f a = rig.bones.get(shoulder).origin,
                b = rig.bones.get(elbow).origin,
                c = rig.bones.get(wrist).origin;
        Vector3f start = result[shoulder].transformPosition(new Vector3f(a)),
                end = hand.transformPosition(new Vector3f(c));
        Vector3f axis = new Vector3f(end).sub(start);
        float distance = axis.length();
        if (distance < .001f) return;
        axis.div(distance);
        float l1 = a.distance(b),
                l2 = b.distance(c),
                d = Math.min(distance, l1 + l2 - .001f),
                along = (l1 * l1 - l2 * l2 + d * d) / (2 * d),
                height = (float) Math.sqrt(Math.max(0, l1 * l1 - along * along));
        // 背侧肘平面不与典型向下/横向肩腕轴重合；避免相反向量混合或180度运输翻转。
        Vector3f pref =
                death
                        ? new Vector3f(0, 0, 1)
                        : source.matrices()[elbow].transformPosition(new Vector3f(b)).sub(start);
        pref.sub(new Vector3f(axis).mul(pref.dot(axis)));
        // Keep the authored elbow side. Near a straight/ambiguous arm, fade the correction
        // instead of snapping between opposite pole directions.
        if (!death) blend *= SamuraiRig.smooth(pref.length() / .75f);
        if (pref.lengthSquared() < .0001f) {
            pref.set(side.equals("L") ? 1 : -1, 0, 0);
            pref.sub(new Vector3f(axis).mul(pref.dot(axis)));
        }
        if (pref.lengthSquared() < .0001f) {
            pref.set(0, 0, 1);
            pref.sub(new Vector3f(axis).mul(pref.dot(axis)));
        }
        pref.normalize();
        Vector3f bend =
                new Vector3f(start).add(new Vector3f(axis).mul(along)).add(pref.mul(height));
        Matrix4f
                upper =
                        mix(
                                source.matrices()[shoulder],
                                (death
                                        ? between(a, b, start, bend)
                                        : aimPreservingRoll(
                                                source.matrices()[shoulder], a, b, start, bend)),
                                a,
                                blend),
                lower =
                        mix(
                                source.matrices()[elbow],
                                (death
                                        ? between(b, c, bend, end)
                                        : aimPreservingRoll(
                                                source.matrices()[elbow], b, c, bend, end)),
                                b,
                                blend);
        if (death) {
            result[shoulder] = upper;
            result[elbow] = lower;
            result[wrist] = hand;
        } else {
            SamuraiRig.Pose pose = new SamuraiRig.Pose(result, source.visible());
            FootworkMotion.applyBranch(rig, pose, shoulder, upper);
            FootworkMotion.applyBranch(rig, pose, elbow, lower);
            FootworkMotion.applyBranch(rig, pose, wrist, hand);
        }
    }

    // Swing the authored arm toward its target; do not rebuild its roll from the rest axis.
    static Matrix4f aimPreservingRoll(
            Matrix4f authored, Vector3f a, Vector3f b, Vector3f x, Vector3f y) {
        Vector3f origin = authored.transformPosition(new Vector3f(a));
        Vector3f from = authored.transformDirection(new Vector3f(b).sub(a));
        Vector3f to = new Vector3f(y).sub(x);
        if (from.lengthSquared() < 1e-8f || to.lengthSquared() < 1e-8f)
            return new Matrix4f(authored);
        from.normalize();
        to.normalize();
        Quaternionf swing = new Quaternionf();
        if (from.dot(to) < -.9999f) {
            Vector3f pole = authored.transformDirection(new Vector3f(0, 0, 1));
            pole.sub(new Vector3f(from).mul(pole.dot(from)));
            if (pole.lengthSquared() < 1e-6f) {
                pole = authored.transformDirection(new Vector3f(1, 0, 0));
                pole.sub(new Vector3f(from).mul(pole.dot(from)));
            }
            swing.rotationAxis((float) Math.PI, pole.normalize());
        } else swing.rotationTo(from, to);
        return new Matrix4f().translate(x).rotate(swing).translate(origin.negate()).mul(authored);
    }

    private static Matrix4f between(Vector3f a, Vector3f b, Vector3f x, Vector3f y) {
        Vector3f from = new Vector3f(b).sub(a), to = new Vector3f(y).sub(x);
        if (to.lengthSquared() < .000001f) to.set(from);
        Quaternionf q =
                new Quaternionf()
                        .rotationTo(new Vector3f(from).normalize(), new Vector3f(to).normalize());
        return new Matrix4f().translate(x).rotate(q).translate(new Vector3f(a).negate());
    }

    private static Matrix4f mix(Matrix4f a, Matrix4f b, Vector3f pivot, float f) {
        if (f <= 0) return new Matrix4f(a);
        if (f >= 1) return new Matrix4f(b);
        if (Math.abs(a.determinant()) < 1e-10 || Math.abs(b.determinant()) < 1e-10)
            return new Matrix4f(f < .5f ? a : b);
        Vector3f pos =
                a.transformPosition(new Vector3f(pivot))
                        .lerp(b.transformPosition(new Vector3f(pivot)), f);
        Quaternionf q =
                a.getUnnormalizedRotation(new Quaternionf())
                        .slerp(b.getUnnormalizedRotation(new Quaternionf()), f);
        Vector3f s = a.getScale(new Vector3f()).lerp(b.getScale(new Vector3f()), f);
        return new Matrix4f()
                .translate(pos)
                .rotate(q)
                .scale(s)
                .translate(new Vector3f(pivot).negate());
    }

    public static float minY(SamuraiRig.Cube c, Matrix4f m) {
        float y = Float.POSITIVE_INFINITY;
        for (int n = 0; n < 8; n++) {
            Vector3f v =
                    new Vector3f(
                            (n & 1) == 0 ? c.from.x : c.to.x,
                            (n & 2) == 0 ? c.from.y : c.to.y,
                            (n & 4) == 0 ? c.from.z : c.to.z);
            y = Math.min(y, m.transformPosition(v).y);
        }
        return y;
    }

    private DeathMotion() {}
}
