package io.github.xiangwenquan830.silversamurai;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** 双刀/单刀分别从本阶段持握姿态进入；腿部随真实移动进度抬膝、提身、稳脚。 */
public final class TraversalMotion {
    public static SamuraiRig.Pose pose(SamuraiRig rig, float progress, boolean climb) {
        float p = Math.max(0, Math.min(1, progress));
        float lift = SamuraiRig.smooth(p / .25f) * (1 - SamuraiRig.smooth((p - .65f) / .35f));
        float trailing =
                SamuraiRig.smooth((p - .22f) / .28f) * (1 - SamuraiRig.smooth((p - .8f) / .2f));
        SamuraiRig.Pose pose = rig.pose(rig.phase == 2 ? SamuraiRig.IDLE_TWO : "armed_idle", 0);
        rotate(rig, pose, "左腿", -30 * lift, 0, 0, false);
        rotate(rig, pose, "行走_膝_L", 48 * lift, 0, 0, false);
        rotate(rig, pose, "右腿", -23 * trailing, 0, 0, false);
        rotate(rig, pose, "行走_膝_R", 36 * trailing, 0, 0, false);
        // 腰、手臂、刀保持整条共同变换，单刀两只手绝不各自移动。
        rotate(rig, pose, "腰腹_提身控制", -(climb ? 7 : 11) * lift, 0, 0, true);
        rotate(rig, pose, "头盔", 5 * lift, 0, 0, false);
        // 手持刀收向侧上方，以握柄的护手抵住墙沿；两阶段分别保持原有握柄关系。
        SamuraiRig.Pose source = rig.pose(rig.phase == 2 ? SamuraiRig.IDLE_TWO : "armed_idle", 0);
        for (String side : rig.phase == 2 ? new String[] {"L"} : new String[] {"L", "R"}) {
            int sword = rig.bone("背挂武士刀_" + side);
            Vector3f grip = rig.bones.get(sword).origin;
            Vector3f location =
                    new Vector3f(rig.phase == 2 ? 0 : side.equals("L") ? 7 : -7, 27, -10f);
            org.joml.Quaternionf q =
                    new org.joml.Quaternionf()
                            .rotationTo(
                                    new Vector3f(0, 1, 0), new Vector3f(0, 1, .25f).normalize());
            Matrix4f target =
                    new Matrix4f()
                            .translate(location)
                            .rotate(q)
                            .translate(new Vector3f(grip).negate());
            Matrix4f[] temp = pose.matrices().clone();
            temp[sword] = target;
            float brace = SamuraiRig.smooth(p / .13f) * (1 - SamuraiRig.smooth((p - .78f) / .22f));
            pose.matrices()[sword] =
                    DeathMotion.transition(
                                    rig, pose, new SamuraiRig.Pose(temp, pose.visible()), brace)
                            .matrices()[sword];
            for (String hand : rig.phase == 2 ? new String[] {"L", "R"} : new String[] {side}) {
                int wrist = rig.bone("腕_" + hand);
                Matrix4f desired =
                        new Matrix4f(pose.matrices()[sword])
                                .mul(new Matrix4f(source.matrices()[sword]).invert())
                                .mul(source.matrices()[wrist]);
                DeathMotion.solveArm(rig, pose.matrices(), source, hand, desired, 1);
            }
        }
        return pose;
    }

    private static void rotate(
            SamuraiRig rig,
            SamuraiRig.Pose pose,
            String bone,
            float x,
            float y,
            float z,
            boolean swords) {
        int root = rig.bone(bone);
        if (root < 0) return;
        Vector3f pivot =
                pose.matrices()[root].transformPosition(new Vector3f(rig.bones.get(root).origin));
        Matrix4f delta =
                new Matrix4f()
                        .translate(pivot)
                        .rotateXYZ(
                                (float) Math.toRadians(x),
                                (float) Math.toRadians(y),
                                (float) Math.toRadians(z))
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

    private TraversalMotion() {}
}
