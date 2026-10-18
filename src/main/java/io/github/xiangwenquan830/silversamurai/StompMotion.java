package io.github.xiangwenquan830.silversamurai;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** The hip sends the low kick while the chest counter-rotates over the planted leg. */
public final class StompMotion {
    public static final float IMPACT = .55f, DURATION = 1.25f;

    public static SamuraiRig.Pose pose(
            SamuraiRig rig, SamuraiRig.Pose source, float t, String idle) {
        var pose = FootworkMotion.join(rig, source, rig.pose(idle, 0), SamuraiRig.smooth(t / .24f));
        float chamber = SamuraiRig.smooth(t / .32f);
        float extension = SamuraiRig.smooth((t - .38f) / .17f);
        float bodyReturn = 1 - SamuraiRig.smooth((t - .64f) / .61f);
        float legReturn = 1 - SamuraiRig.smooth((t - .62f) / .44f);
        float side = Math.signum(rig.bones.get(rig.bone("行走_踝_R")).origin.x);
        if (side == 0) side = -1;
        float twist = side * (-18 * chamber + 46 * extension) * bodyReturn;
        float pelvis = -side * (-10 * chamber + 22 * extension) * bodyReturn;
        FootworkMotion.rotate(
                rig,
                pose,
                "腰腹_提身控制",
                -4 * chamber * bodyReturn,
                twist,
                side * (4 * chamber + 2 * extension) * bodyReturn,
                true);
        FootworkMotion.rotate(rig, pose, "头盔", 0, -twist * .65f, 0, false);
        // Hips and shoulder line release in opposite directions, not as one rigid swivel.
        for (String armor : new String[] {"骨盆", "前侧裙甲", "后裙甲"})
            FootworkMotion.rotate(rig, pose, armor, 0, pelvis, 0, false);
        // Chamber first, then drive across and forward; recovery cannot rewind into the load pose.
        joint(
                rig,
                pose,
                "右腿",
                (20 * chamber + 8 * extension) * legReturn,
                side * (-12 * chamber + 26 * extension) * legReturn,
                side * (12 * chamber + 16 * extension) * legReturn);
        joint(rig, pose, "行走_膝_R", -(48 * chamber - 40 * extension) * legReturn, 0, 0);
        joint(
                rig,
                pose,
                "行走_踝_R",
                (10 * chamber - 10 * extension) * legReturn,
                0,
                -side * 7 * extension * legReturn);
        FootworkMotion.rotate(
                rig,
                pose,
                "行走_随动_side_skirt_R",
                (10 * chamber + 5 * extension) * legReturn,
                0,
                side * 8 * chamber * legReturn,
                false);
        return pose;
    }

    private static void joint(
            SamuraiRig rig, SamuraiRig.Pose pose, String name, float x, float y, float z) {
        int i = rig.bone(name);
        var p = rig.bones.get(i).origin;
        Matrix4f m =
                new Matrix4f(pose.matrices()[i])
                        .translate(p)
                        .rotateZ((float) Math.toRadians(z))
                        .rotateY((float) Math.toRadians(y))
                        .rotateX((float) Math.toRadians(x))
                        .translate(new Vector3f(p).negate());
        FootworkMotion.applyBranch(rig, pose, i, m);
    }

    private StompMotion() {}
}
