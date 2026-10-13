package cn.blockforge.generated.bossmodbossmodbossm;

import org.joml.Matrix4f;

/** Resting two-handed grip is rigid relative to the torso, never independently animated. */
final class StableGrip {
    static void idle(SamuraiRig rig, SamuraiRig.Pose pose) {
        var rest = rig.pose(SamuraiRig.IDLE_TWO, 0);
        int torso = rig.bone("腰腹_提身控制");
        var delta =
                new Matrix4f(pose.matrices()[torso])
                        .mul(new Matrix4f(rest.matrices()[torso]).invert());
        for (String name : new String[] {"左肩臂", "右肩臂", "背挂武士刀_L"}) {
            int root = rig.bone(name);
            for (int i = 0; i < rig.bones.size(); i++)
                for (int j = i; j >= 0; j = rig.bones.get(j).parent)
                    if (j == root) {
                        pose.matrices()[i] = new Matrix4f(delta).mul(rest.matrices()[i]);
                        break;
                    }
        }
    }

    private StableGrip() {}
}
