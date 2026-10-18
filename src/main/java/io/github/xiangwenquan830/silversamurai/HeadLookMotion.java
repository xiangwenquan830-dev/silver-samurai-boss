package io.github.xiangwenquan830.silversamurai;

import org.joml.*;
import java.lang.Math;

/** Shares downward gaze with the waist and limits the neck relative to the chest. */
final class HeadLookMotion {
    static boolean allowed(int state) {
        return state != SamuraiEntity.WALL
                && state != SamuraiEntity.DEAD
                && state != SamuraiEntity.TRANSITION
                && state != SamuraiEntity.SLEEP;
    }

    static boolean bowAllowed(int state) {
        return state == SamuraiEntity.IDLE
                || state == SamuraiEntity.CHASE
                || state == SamuraiEntity.BACK_WALK
                || state == SamuraiEntity.CLOSE_GUARD;
    }

    static SamuraiRig.Pose apply(
            SamuraiRig rig, SamuraiRig.Pose pose, float yaw, float pitch, float weight) {
        return apply(rig, pose, yaw, pitch, weight, 0);
    }

    static SamuraiRig.Pose apply(
            SamuraiRig rig,
            SamuraiRig.Pose pose,
            float yaw,
            float pitch,
            float weight,
            float bowWeight) {
        int head = rig.bone("头盔");
        if (head < 0 || weight <= 0) return pose;
        int torso = rig.bone("腰腹_提身控制");
        if (torso >= 0 && bowWeight > 0) {
            Vector3f front =
                    pose.matrices()[torso].transformDirection(new Vector3f(0, 0, -1)).normalize();
            float current =
                    (float)
                            Math.toDegrees(
                                    Math.atan2(
                                            -front.y,
                                            Math.sqrt(front.x * front.x + front.z * front.z)));
            float strength =
                    SamuraiRig.smooth((pitch - 8) / 30)
                            * Math.min(1, weight)
                            * Math.min(1, bowWeight);
            // An absolute forward-lean floor prevents captured poses from accumulating another bow.
            float target = 10 * strength;
            if (current < 0) target = current + (10 - current) * strength;
            if (strength > 0 && current < target) {
                Vector3f axis = new Vector3f(-front.z, 0, front.x).normalize();
                Vector3f pivot =
                        pose.matrices()[torso].transformPosition(
                                new Vector3f(rig.bones.get(torso).origin));
                Matrix4f delta =
                        new Matrix4f()
                                .translate(pivot)
                                .rotate((float) Math.toRadians(current - target), axis)
                                .translate(new Vector3f(pivot).negate());
                for (int i = 0; i < rig.bones.size(); i++)
                    for (int j = i; j >= 0; j = rig.bones.get(j).parent) {
                        String name = rig.bones.get(j).name;
                        if (j == torso
                                || name.equals("背挂武士刀_L")
                                || (rig.phase == 1 && name.equals("背挂武士刀_R"))) {
                            pose.matrices()[i] = new Matrix4f(delta).mul(pose.matrices()[i]);
                            break;
                        }
                    }
            }
        }
        float y = (float) Math.toRadians(Math.max(-42, Math.min(42, yaw)));
        float p = (float) Math.toRadians(Math.max(-18, Math.min(38, pitch)));
        Vector3f wanted =
                new Vector3f(
                        (float) (Math.sin(y) * Math.cos(p)),
                        (float) -Math.sin(p),
                        (float) (-Math.cos(y) * Math.cos(p)));
        Quaternionf wantedRotation = null;
        if (torso >= 0) {
            Matrix4f chest = pose.matrices()[torso];
            Vector3f local =
                    new Matrix4f(chest)
                            .invert()
                            .transformDirection(new Vector3f(wanted))
                            .normalize();
            float localPitch =
                    (float)
                            Math.toDegrees(
                                    Math.atan2(
                                            -local.y,
                                            Math.sqrt(local.x * local.x + local.z * local.z)));
            float localYaw = (float) Math.atan2(local.x, -local.z);
            float sideRelief = SamuraiRig.smooth(Math.abs((float) Math.toDegrees(localYaw)) / 28);
            float relief = Math.max(SamuraiRig.smooth((localPitch + 5) / 13), sideRelief);
            float safePitch =
                    Math.max(-18, Math.min(-5 + 5 * relief * relief * relief, localPitch));
            float r = (float) Math.toRadians(safePitch);
            // Keep the helmet upright relative to the chest: a shortest-vector turn alone
            // introduces roll when a tilted helmet looks sideways, dipping one jaw corner.
            wantedRotation =
                    chest.getUnnormalizedRotation(new Quaternionf()).rotateY(-localYaw).rotateX(-r);
            wanted.set(
                    (float) (Math.sin(localYaw) * Math.cos(r)),
                    (float) -Math.sin(r),
                    (float) (-Math.cos(localYaw) * Math.cos(r)));
            chest.transformDirection(wanted).normalize();
            // The existing neck is hidden inside the helmet. Use its available overlap to lift
            // the chin by at most 0.15 blocks; center gaze needs less lift, keeping the neck
            // connected.
            Vector3f pivot =
                    pose.matrices()[head].transformPosition(
                            new Vector3f(rig.bones.get(head).origin));
            float localY = new Matrix4f(chest).invert().transformPosition(new Vector3f(pivot)).y;
            float lift =
                    Math.max(
                            0,
                            rig.bones.get(head).origin.y
                                    + (.7f + .62f * sideRelief) * relief * Math.min(1, weight)
                                    - localY);
            if (lift > 0) {
                Vector3f offset = chest.transformDirection(new Vector3f(0, lift, 0));
                FootworkMotion.applyBranch(
                        rig,
                        pose,
                        head,
                        new Matrix4f().translate(offset).mul(pose.matrices()[head]));
            }
        }
        Matrix4f original = pose.matrices()[head];
        Vector3f forward = original.transformDirection(new Vector3f(0, 0, -1)).normalize();
        Quaternionf correction =
                wantedRotation == null
                        ? new Quaternionf().rotationTo(forward, wanted)
                        : wantedRotation
                                .mul(
                                        original.getUnnormalizedRotation(new Quaternionf())
                                                .conjugate())
                                .normalize();
        float angle = (float) Math.toDegrees(2 * Math.acos(Math.min(1, Math.abs(correction.w))));
        float blend = Math.min(1, weight) * Math.min(1, 70 / Math.max(.001f, angle));
        correction = new Quaternionf().slerp(correction, blend);
        Vector3f pivot = original.transformPosition(new Vector3f(rig.bones.get(head).origin));
        Matrix4f delta =
                new Matrix4f()
                        .translate(pivot)
                        .rotate(correction)
                        .translate(new Vector3f(pivot).negate());
        FootworkMotion.applyBranch(rig, pose, head, delta.mul(new Matrix4f(original)));
        return pose;
    }

    private HeadLookMotion() {}
}
