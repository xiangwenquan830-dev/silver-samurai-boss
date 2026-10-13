package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.nbt.CompoundTag;

/** 死亡时由服务端锁定的接触面。客户端、重载和迟加入共用，不重新抽样地形。 */
public final class DeathSurface {
    public static final int SIZE = 25;
    public static final float CELL = 4f;
    public final float[] heights;
    public final float direction;
    public final boolean compact;

    public DeathSurface(float[] heights, float direction, boolean compact) {
        this.heights = heights;
        this.direction = direction;
        this.compact = compact;
    }

    public static DeathSurface flat() {
        return new DeathSurface(new float[SIZE * SIZE], 0, false);
    }

    public float height(float x, float z) {
        float fx = Math.max(0, Math.min(SIZE - 1, x / CELL + SIZE / 2)),
                fz = Math.max(0, Math.min(SIZE - 1, z / CELL + SIZE / 2));
        int ix = (int) fx,
                iz = (int) fz,
                nx = Math.min(SIZE - 1, ix + 1),
                nz = Math.min(SIZE - 1, iz + 1);
        float tx = fx - ix, tz = fz - iz;
        float a = heights[iz * SIZE + ix] * (1 - tx) + heights[iz * SIZE + nx] * tx,
                b = heights[nz * SIZE + ix] * (1 - tx) + heights[nz * SIZE + nx] * tx;
        return a * (1 - tz) + b * tz;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        int[] data = new int[heights.length];
        for (int i = 0; i < data.length; i++) data[i] = Float.floatToIntBits(heights[i]);
        t.putIntArray("Floor", data);
        t.putFloat("Direction", direction);
        t.putBoolean("Compact", compact);
        return t;
    }

    public static DeathSurface load(CompoundTag tag) {
        int[] a = tag.getIntArray("Floor");
        if (a.length != SIZE * SIZE) return flat();
        float[] h = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            h[i] = Float.intBitsToFloat(a[i]);
            if (!Float.isFinite(h[i])) h[i] = 0;
        }
        return new DeathSurface(h, tag.getFloat("Direction"), tag.getBoolean("Compact"));
    }
}
