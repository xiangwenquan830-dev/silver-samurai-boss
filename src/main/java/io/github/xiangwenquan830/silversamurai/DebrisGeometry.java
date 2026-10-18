package io.github.xiangwenquan830.silversamurai;

import org.joml.*;
import java.lang.Math;
import java.util.*;

/** 直接使用第91版原零件，不创建替代材质或可拾取物品。 */
public final class DebrisGeometry {
    public static final String[] NAMES = {
        "背挂武士刀_R", "脱落_半面罩", "脱落_肩甲外板", "脱落_胸甲边角", "脱落_胫甲缺角", "脱落_左腿正面披甲", "脱落_右侧裙甲", "脱落_前裙外片"
    };
    public static final float[] RELEASE = {.65f, .32f, .38f, .42f, .24f, .29f, .36f, .34f};
    private static DebrisGeometry[] cache;
    public final int part, bone;
    public final List<SamuraiRig.Cube> cubes = new ArrayList<>();
    public final List<Vector3f> corners = new ArrayList<>();
    public final SamuraiRig.Pose rest;
    public final Vector3f center;
    public final Matrix4f restMatrix;

    private DebrisGeometry(int part) {
        this.part = part;
        SamuraiRig rig = SamuraiRig.get(2);
        bone = rig.bone(NAMES[part]);
        rest = rig.pose(SamuraiRig.TRANSITION, 6);
        restMatrix = rest.matrices()[bone];
        Vector3f low = new Vector3f(Float.POSITIVE_INFINITY),
                high = new Vector3f(Float.NEGATIVE_INFINITY);
        for (var c : rig.cubes) {
            if (part(c) != part) continue;
            cubes.add(c);
            Matrix4f m = rig.cubeMatrix(c, rest);
            for (int n = 0; n < 8; n++) {
                Vector3f v =
                        m.transformPosition(
                                new Vector3f(
                                        (n & 1) == 0 ? c.from.x : c.to.x,
                                        (n & 2) == 0 ? c.from.y : c.to.y,
                                        (n & 4) == 0 ? c.from.z : c.to.z));
                corners.add(v);
                low.min(v);
                high.max(v);
            }
        }
        center = new Vector3f(low).add(high).mul(.5f);
        for (Vector3f v : corners) v.sub(center);
    }

    public static synchronized DebrisGeometry get(int part) {
        if (cache == null) {
            cache = new DebrisGeometry[8];
            for (int i = 0; i < 8; i++) cache[i] = new DebrisGeometry(i);
        }
        return cache[Math.max(0, Math.min(7, part))];
    }

    public static int part(SamuraiRig.Cube cube) {
        if (!cube.debris) return -1;
        SamuraiRig rig = SamuraiRig.get(2);
        for (int i = cube.bone; i >= 0; i = rig.bones.get(i).parent)
            for (int n = 0; n < NAMES.length; n++)
                if (NAMES[n].equals(rig.bones.get(i).name)) return n;
        return -1;
    }

    public Vector3f centerAt(float time) {
        Matrix4f m = SamuraiRig.get(2).pose(SamuraiRig.TRANSITION, time).matrices()[bone];
        return m.transformPosition(
                new Matrix4f(restMatrix).invert().transformPosition(new Vector3f(center)));
    }

    public Quaternionf rotationAt(float time) {
        Quaternionf now =
                SamuraiRig.get(2)
                        .pose(SamuraiRig.TRANSITION, time)
                        .matrices()[bone]
                        .getUnnormalizedRotation(new Quaternionf());
        return now.mul(restMatrix.getUnnormalizedRotation(new Quaternionf()).invert());
    }

    public float[] bounds(Quaternionf q) {
        Vector3f lo = new Vector3f(Float.POSITIVE_INFINITY),
                hi = new Vector3f(Float.NEGATIVE_INFINITY);
        for (Vector3f v : corners) {
            Vector3f p = q.transform(new Vector3f(v)).mul(SamuraiRig.SCALE);
            lo.min(p);
            hi.max(p);
        }
        return new float[] {lo.x, lo.y, lo.z, hi.x, hi.y, hi.z};
    }
}
