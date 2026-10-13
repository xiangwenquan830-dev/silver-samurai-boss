package cn.blockforge.generated.bossmodbossmodbossm;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Enlarge the authored foot excursion while preserving its lift, timing and upper body. */
public final class StrideMotion {
    public static SamuraiRig.Pose pose(
            SamuraiRig rig, SamuraiRig.Pose pose, String clip, float time) {
        SamuraiRig.Pose idle =
                rig.pose(
                        rig.phase == 2
                                ? SamuraiRig.IDLE_TWO
                                : clip.equals(SamuraiRig.WALK_ONE) ? "" : "armed_idle",
                        0);
        float scale = (float) Math.min(1.45, 1 + (BossConfig.STRIDE_SCALE.get() - 1) * .6);
        for (String side : new String[] {"L", "R"}) {
            int ankle = rig.bone("行走_踝_" + side);
            Vector3f pivot = rig.bones.get(ankle).origin;
            Vector3f foot = pose.matrices()[ankle].transformPosition(new Vector3f(pivot));
            Vector3f rest = idle.matrices()[ankle].transformPosition(new Vector3f(pivot));
            float offset = (foot.z - rest.z) * (scale - 1);
            FootworkMotion.leg(
                    rig,
                    pose,
                    side,
                    new Matrix4f().translate(0, 0, offset).mul(pose.matrices()[ankle]));
        }
        return pose;
    }

    private StrideMotion() {}
}
