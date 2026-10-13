package cn.blockforge.generated.bossmodbossmodbossm;

import org.joml.*;
import java.lang.Math;

/** 只修饰实际高差路线；上阶抬腿、下阶探脚，进度来自已走过的路径而非计时。 */
public final class StepMotion {
    public record Frame(float progress, float height, int side) {
        static final Frame REST = new Frame(0, 0, 1);
    }

    public static SamuraiRig.Pose pose(
            SamuraiRig rig, SamuraiRig.Pose source, float progress, float height, int side) {
        var frame = new Frame(progress, height, side);
        return interpolate(rig, source, frame, frame, 1);
    }

    /**
     * Interpolate offsets across a node/leading-foot change instead of snapping progress backwards.
     */
    public static SamuraiRig.Pose interpolate(
            SamuraiRig rig, SamuraiRig.Pose source, Frame previous, Frame current, float blend) {
        blend = Math.max(0, Math.min(1, blend));
        // Within one step keep the authored continuous envelope. Across steps blend both endpoints.
        if (previous.side == current.side
                && previous.height == current.height
                && current.progress >= previous.progress) {
            current =
                    new Frame(
                            mix(previous.progress, current.progress, blend),
                            current.height,
                            current.side);
            previous = current;
            blend = 1;
        }
        if (envelope(previous) == 0 && envelope(current) == 0) return source;
        for (int i = 0; i < 2; i++) {
            String s = i == 0 ? "L" : "R";
            int ankle = rig.bone("行走_踝_" + s);
            Vector3f offset = offset(previous, i).lerp(offset(current, i), blend);
            Matrix4f foot = new Matrix4f().translate(offset).mul(source.matrices()[ankle]);
            FootworkMotion.leg(rig, source, s, foot);
        }
        FootworkMotion.rotate(
                rig,
                source,
                "腰腹_提身控制",
                mix(pitch(previous), pitch(current), blend),
                0,
                mix(
                        previous.side * 1.2f * envelope(previous),
                        current.side * 1.2f * envelope(current),
                        blend),
                true);
        return source;
    }

    private static float mix(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float envelope(Frame f) {
        return Math.abs(f.height) < .01f || f.progress <= 0 || f.progress >= 1
                ? 0
                : (float) Math.sin(Math.PI * f.progress);
    }

    private static float pitch(Frame f) {
        return (f.height > 0 ? -3 : 2) * envelope(f);
    }

    private static Vector3f offset(Frame f, int foot) {
        float envelope = envelope(f);
        if (envelope == 0) return new Vector3f();
        boolean lead = (foot == 0) == (f.side > 0);
        float amount = Math.min(1.5f, Math.abs(f.height)) / SamuraiRig.SCALE;
        float swing = SamuraiRig.smooth((f.progress - (lead ? 0 : .4f)) / (lead ? .65f : .6f));
        float lift =
                (float) Math.sin(Math.PI * swing) * (f.height > 0 ? amount * .58f : amount * .18f);
        float probe = f.height < 0 && lead ? -amount * .22f * envelope : 0;
        return new Vector3f(0, lift + probe, (f.height > 0 ? -.6f : -1.1f) * envelope);
    }

    private StepMotion() {}
}
