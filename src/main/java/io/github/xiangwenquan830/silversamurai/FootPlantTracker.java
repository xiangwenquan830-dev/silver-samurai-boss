package io.github.xiangwenquan830.silversamurai;

/** 位移驱动步态的接地边沿检测，不含固定计时器。 二阶段原动作是贴地负重拖步：检测脚从前摆转为支撑后移的几何拐点。 */
public final class FootPlantTracker {
    private final boolean[] initialized = new boolean[2],
            raised = new boolean[2],
            forward = new boolean[2];
    private final float[] lastZ = new float[2];

    public boolean sample(int side, int phase, float localZ, float clearance, boolean collider) {
        if (!initialized[side]) {
            initialized[side] = true;
            lastZ[side] = localZ;
            return false;
        }
        float dz = localZ - lastZ[side];
        lastZ[side] = localZ;
        if (phase == 2) {
            boolean plant = forward[side] && dz > .002f;
            if (dz < -.002f) forward[side] = true;
            else if (dz > .002f) forward[side] = false;
            return plant && collider && clearance < .09f;
        }
        if (!collider || clearance > .05f) {
            raised[side] = true;
            return false;
        }
        if (raised[side] && clearance < .025f) {
            raised[side] = false;
            return true;
        }
        return false;
    }

    public void reset() {
        for (int i = 0; i < 2; i++) {
            initialized[i] = raised[i] = forward[i] = false;
        }
    }
}
