package cn.blockforge.generated.bossmodbossmodbossm;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** 两拍重装短步：先移重心，第一脚落稳后再挪第二脚。支撑脚补偿实体位移和朝向。 */
public final class FootworkMotion {
    public static final int TICKS = 16;

    public static float travel(float t) {
        return SamuraiRig.smooth((t - .14f) / .72f);
    }

    public static float turn(float t) {
        return SamuraiRig.smooth((t - .20f) / .65f);
    }

    public static float turn(float t, boolean pivot) {
        return pivot ? SamuraiRig.smooth(t) : turn(t);
    }

    public static int pivotTicks(float yaw) {
        return Math.max(TICKS, (int) Math.ceil(Math.abs(yaw) / 4.5f));
    }

    public static SamuraiRig.Pose pose(
            SamuraiRig rig,
            SamuraiRig.Pose source,
            float progress,
            float dx,
            float dz,
            float yaw,
            int side) {
        return pose(rig, source, progress, dx, dz, yaw, side, false);
    }

    public static SamuraiRig.Pose pose(
            SamuraiRig rig,
            SamuraiRig.Pose source,
            float progress,
            float dx,
            float dz,
            float yaw,
            int side,
            boolean braking) {
        float t = Math.max(0, Math.min(1, progress)),
                move = braking ? 2 * t - t * t : travel(t),
                angle = yaw * turn(t, dx == 0 && dz == 0 && yaw != 0);
        SamuraiRig.Pose idle = rig.pose(rig.phase == 2 ? SamuraiRig.IDLE_TWO : "armed_idle", 0);
        if (t <= 0) return source;
        SamuraiRig.Pose pose = join(rig, source, idle, SamuraiRig.smooth(t / .35f));
        float weight = (float) Math.sin(Math.PI * t);
        rotate(rig, pose, "腰腹_提身控制", 0, -yaw * .18f * weight, side * 1.8f * weight, true);
        rotate(rig, pose, "头盔", 0, -yaw * .12f * weight, 0, false);
        for (int foot = 0; foot < 2; foot++) {
            String s = foot == 0 ? "L" : "R";
            int ankle = rig.bone("行走_踝_" + s);
            Vector3f pivot = rig.bones.get(ankle).origin;
            boolean first = (foot == 0) == (side > 0);
            float f = SamuraiRig.smooth((t - (first ? .14f : .53f)) / (first ? .34f : .34f));
            Vector3f start = source.matrices()[ankle].transformPosition(new Vector3f(pivot));
            Vector3f end =
                    idle.matrices()[ankle]
                            .transformPosition(new Vector3f(pivot))
                            .rotateY(rad(-yaw))
                            .add(dx, 0, dz);
            Vector3f plant =
                    new Vector3f(start)
                            .lerp(end, f)
                            .add(0, (float) Math.sin(Math.PI * f) * .68f, 0);
            plant.sub(dx * move, 0, dz * move).rotateY(rad(angle));
            Quaternionf q = source.matrices()[ankle].getUnnormalizedRotation(new Quaternionf());
            Quaternionf endQ =
                    new Quaternionf()
                            .rotationY(rad(-yaw))
                            .mul(idle.matrices()[ankle].getUnnormalizedRotation(new Quaternionf()));
            q.slerp(endQ, f);
            q = new Quaternionf().rotationY(rad(angle)).mul(q);
            Matrix4f footMatrix =
                    new Matrix4f()
                            .translate(plant)
                            .rotate(q)
                            .translate(new Vector3f(pivot).negate());
            leg(rig, pose, s, footMatrix);
        }
        return pose;
    }

    /** 双手握同刀的过渡也先混合刀，再由刀柄关系求两只手。 */
    public static SamuraiRig.Pose join(
            SamuraiRig rig, SamuraiRig.Pose from, SamuraiRig.Pose target, float blend) {
        SamuraiRig.Pose pose = DeathMotion.transition(rig, from, target, blend);
        if (rig.phase != 2 || blend <= 0 || blend >= 1) return pose;
        SamuraiRig.Pose reference =
                new SamuraiRig.Pose(pose.matrices().clone(), pose.visible().clone());
        float correction = (float) Math.pow(Math.sin(Math.PI * blend), 2);
        int sword = rig.bone("背挂武士刀_L");
        for (String side : new String[] {"L", "R"}) {
            int wrist = rig.bone("腕_" + side);
            Matrix4f relativeA =
                    new Matrix4f(from.matrices()[sword]).invert().mul(from.matrices()[wrist]);
            Matrix4f relativeB =
                    new Matrix4f(target.matrices()[sword]).invert().mul(target.matrices()[wrist]);
            Vector3f pivot = rig.bones.get(wrist).origin;
            Vector3f location =
                    relativeA
                            .transformPosition(new Vector3f(pivot))
                            .lerp(relativeB.transformPosition(new Vector3f(pivot)), blend);
            Quaternionf rotation =
                    relativeA
                            .getUnnormalizedRotation(new Quaternionf())
                            .slerp(relativeB.getUnnormalizedRotation(new Quaternionf()), blend);
            Matrix4f desired =
                    new Matrix4f(pose.matrices()[sword])
                            .translate(location)
                            .rotate(rotation)
                            .translate(new Vector3f(pivot).negate());
            DeathMotion.solveArm(rig, pose.matrices(), reference, side, desired, correction);
        }
        return pose;
    }

    public static SamuraiRig.Pose reaction(
            SamuraiRig rig, SamuraiRig.Pose pose, float intensity, float side) {
        // 两手与刀同属一次刚性上身变换，绝不分别摇手腕。
        rotate(rig, pose, "腰腹_提身控制", intensity * 2.4f, 0, side * intensity * 1.5f, true);
        rotate(rig, pose, "头盔", -intensity * 1.1f, 0, 0, false);
        return pose;
    }

    public static SamuraiRig.Pose look(SamuraiRig rig, SamuraiRig.Pose pose, float yaw) {
        // 观察只交给头颈；腰腹保留原步态/持刀轨道，避免整段上身重复叠转。
        float limited = Math.max(-16, Math.min(16, yaw));
        rotate(rig, pose, "头盔", 0, -limited, 0, false);
        return pose;
    }

    static void leg(SamuraiRig rig, SamuraiRig.Pose pose, String side, Matrix4f foot) {
        int hip = rig.bone(side.equals("L") ? "左腿" : "右腿"),
                knee = rig.bone("行走_膝_" + side),
                ankle = rig.bone("行走_踝_" + side);
        Vector3f a = rig.bones.get(hip).origin,
                b = rig.bones.get(knee).origin,
                c = rig.bones.get(ankle).origin;
        Vector3f start = pose.matrices()[hip].transformPosition(new Vector3f(a)),
                end = foot.transformPosition(new Vector3f(c));
        Vector3f axis = new Vector3f(end).sub(start);
        float distance = axis.length();
        if (distance < .001) return;
        axis.div(distance);
        float l1 = a.distance(b),
                l2 = b.distance(c),
                d = Math.min(distance, l1 + l2 - .001f),
                along = (l1 * l1 - l2 * l2 + d * d) / (2 * d),
                height = (float) Math.sqrt(Math.max(0, l1 * l1 - along * along));
        Vector3f pref = new Vector3f(0, 0, -1);
        pref.sub(new Vector3f(axis).mul(pref.dot(axis))).normalize();
        Vector3f bend =
                new Vector3f(start).add(new Vector3f(axis).mul(along)).add(pref.mul(height));
        applyBranch(rig, pose, hip, between(a, b, start, bend));
        applyBranch(rig, pose, knee, between(b, c, bend, end));
        applyBranch(rig, pose, ankle, foot);
    }

    private static Matrix4f between(Vector3f a, Vector3f b, Vector3f x, Vector3f y) {
        return new Matrix4f()
                .translate(x)
                .rotate(
                        new Quaternionf()
                                .rotationTo(
                                        new Vector3f(b).sub(a).normalize(),
                                        new Vector3f(y).sub(x).normalize()))
                .translate(new Vector3f(a).negate());
    }

    static void applyBranch(SamuraiRig rig, SamuraiRig.Pose pose, int root, Matrix4f replacement) {
        Matrix4f delta =
                new Matrix4f(replacement).mul(new Matrix4f(pose.matrices()[root]).invert());
        for (int i = 0; i < rig.bones.size(); i++)
            for (int j = i; j >= 0; j = rig.bones.get(j).parent)
                if (j == root) {
                    pose.matrices()[i] = new Matrix4f(delta).mul(pose.matrices()[i]);
                    break;
                }
    }

    static void rotate(
            SamuraiRig rig,
            SamuraiRig.Pose pose,
            String name,
            float x,
            float y,
            float z,
            boolean swords) {
        int root = rig.bone(name);
        if (root < 0) return;
        Vector3f pivot =
                pose.matrices()[root].transformPosition(new Vector3f(rig.bones.get(root).origin));
        Matrix4f delta =
                new Matrix4f()
                        .translate(pivot)
                        .rotateXYZ(rad(x), rad(y), rad(z))
                        .translate(new Vector3f(pivot).negate());
        for (int i = 0; i < rig.bones.size(); i++) {
            boolean child = swords && rig.bones.get(i).name.startsWith("背挂武士刀_");
            for (int j = i; j >= 0; j = rig.bones.get(j).parent)
                if (j == root) {
                    child = true;
                    break;
                }
            if (child) pose.matrices()[i] = new Matrix4f(delta).mul(pose.matrices()[i]);
        }
    }

    private static float rad(float x) {
        return (float) Math.toRadians(x);
    }

    private FootworkMotion() {}
}
