package io.github.xiangwenquan830.silversamurai;

/** Stops walking in a moving draw without restarting the hands or blade extraction. */
final class DrawMotion {
    static SamuraiRig.Pose plantFeet(SamuraiRig rig, SamuraiRig.Pose pose, float gait) {
        float amount = SamuraiRig.smooth(1 - Math.max(0, Math.min(1, gait)));
        if (amount <= 0) return pose;
        SamuraiRig.Pose standing = rig.pose("", 0);
        int root = rig.bone("行走_重心");
        if (root >= 0) {
            var mixed = DeathMotion.transition(rig, pose, standing, amount);
            // One rigid transform for all upper bones preserves both blade-to-hand relationships.
            FootworkMotion.applyBranch(rig, pose, root, mixed.matrices()[root]);
        }
        var planted = DeathMotion.transition(rig, pose, standing, amount);
        for (int i = 0; i < rig.bones.size(); i++) {
            for (int j = i; j >= 0; j = rig.bones.get(j).parent) {
                String name = rig.bones.get(j).name;
                if (name.equals("左腿") || name.equals("右腿") || name.startsWith("行走_随动_")) {
                    pose.matrices()[i] = planted.matrices()[i];
                    break;
                }
            }
        }
        return pose;
    }

    private DrawMotion() {}
}
