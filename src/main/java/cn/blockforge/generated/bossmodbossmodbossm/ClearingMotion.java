package cn.blockforge.generated.bossmodbossmodbossm;

import org.joml.*;
import java.lang.Math;

/** 清障专用短切：保持单刀双手约束，按待清方块的实际高度形成局部刀路。 */
public final class ClearingMotion {
    public static final float START = .30f, END = .56f, DURATION = .94f;

    public static SamuraiRig.Pose pose(float time, float distance, float height) {
        SamuraiRig rig = SamuraiRig.get(2);
        SamuraiRig.Pose source = rig.pose(SamuraiRig.IDLE_TWO, 0);
        Matrix4f[] result = new Matrix4f[source.matrices().length];
        for (int i = 0; i < result.length; i++) result[i] = new Matrix4f(source.matrices()[i]);
        int sword = rig.bone("背挂武士刀_L");
        Vector3f pivot = rig.bones.get(sword).origin;
        Vector3f grip = new Vector3f(0, 25, -7),
                target = new Vector3f(0, height / SamuraiRig.SCALE, -distance / SamuraiRig.SCALE);
        Vector3f axis = target.sub(grip).normalize();
        float f = SamuraiRig.smooth((time - START) / (END - START));
        // 先蓄起32度，再短促切过指定方向，最后缓慢收回。不借用普通攻击伤害状态。
        Quaternionf rotation =
                new Quaternionf()
                        .rotationY((float) Math.toRadians(32 - 64 * f))
                        .mul(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), axis));
        Matrix4f cut =
                new Matrix4f()
                        .translate(grip)
                        .rotate(rotation)
                        .translate(new Vector3f(pivot).negate());
        result[sword] = cut;
        SamuraiRig.Pose desired = new SamuraiRig.Pose(result, source.visible().clone());
        float blend =
                SamuraiRig.smooth(time / .26f) * (1 - SamuraiRig.smooth((time - .64f) / .30f));
        SamuraiRig.Pose pose = DeathMotion.transition(rig, source, desired, blend);
        for (String side : new String[] {"L", "R"}) {
            int wrist = rig.bone("腕_" + side);
            Matrix4f hand =
                    new Matrix4f(pose.matrices()[sword])
                            .mul(new Matrix4f(source.matrices()[sword]).invert())
                            .mul(source.matrices()[wrist]);
            DeathMotion.solveArm(rig, pose.matrices(), source, side, hand, 1);
        }
        return pose;
    }

    private ClearingMotion() {}
}
