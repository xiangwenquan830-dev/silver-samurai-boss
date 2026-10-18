package io.github.xiangwenquan830.silversamurai;

import net.minecraft.nbt.CompoundTag;
import static io.github.xiangwenquan830.silversamurai.SamuraiEntity.*;

/** Pure pose composition. Server and client provide the same immutable animation inputs. */
final class PoseComposer {
    record Motion(
            int phase,
            int state,
            float age,
            String clip,
            float time,
            float pitch,
            CompoundTag source,
            CompoundTag transition) {}

    record Details(
            String idle,
            float footProgress,
            float dx,
            float dz,
            float yaw,
            int side,
            boolean braking,
            float recoveryBlend,
            boolean drawn,
            float traverse,
            float sheatheReverse,
            float rate,
            StepMotion.Frame previousTerrain,
            StepMotion.Frame terrain,
            float terrainBlend,
            float reactionAge,
            boolean reactionAllowed,
            float power,
            float hitSide,
            float clearDistance,
            float clearHeight,
            float drawGait) {}

    static SamuraiRig.Pose sample(Motion m, Details d) {
        SamuraiRig rig = SamuraiRig.get(m.phase);
        if (m.state == STOMP)
            return StompMotion.pose(rig, DeathMotion.decode(rig, m.source), m.age, d.idle);
        if (m.state == FOOTWORK)
            return FootworkMotion.pose(
                    rig,
                    DeathMotion.decode(rig, m.source),
                    d.footProgress,
                    d.dx,
                    d.dz,
                    d.yaw,
                    d.side,
                    d.braking);
        if (m.state == SETTLE)
            return FootworkMotion.join(
                    rig,
                    DeathMotion.decode(rig, m.source),
                    rig.pose(m.phase == 2 ? SamuraiRig.IDLE_TWO : "armed_idle", 0),
                    SamuraiRig.smooth(m.age / .30f));
        if (m.state == RECOVER)
            return FootworkMotion.join(
                    rig,
                    DeathMotion.decode(rig, m.source),
                    rig.pose(m.phase == 2 ? SamuraiRig.IDLE_TWO : d.drawn ? "armed_idle" : "", 0),
                    SamuraiRig.smooth(m.age / d.recoveryBlend));
        if (m.state == CLEAR) {
            SamuraiRig.Pose p = ClearingMotion.pose(m.age, d.clearDistance, d.clearHeight);
            if (m.age < .2f)
                return DeathMotion.transition(
                        rig,
                        DeathMotion.decode(rig, m.transition),
                        p,
                        SamuraiRig.smooth(m.age / .2f));
            return p;
        }
        if (m.state == VAULT || m.state == CLIMB) {
            SamuraiRig.Pose motion = TraversalMotion.pose(rig, d.traverse, m.state == CLIMB);
            return DeathMotion.transition(
                    rig,
                    DeathMotion.decode(rig, m.transition),
                    motion,
                    SamuraiRig.smooth(m.age / .25f));
        }
        if (m.state == TRANSITION) {
            SamuraiRig.Pose target = rig.pose(m.clip, m.time);
            if (m.age < .22f)
                return DeathMotion.transition(
                        rig,
                        DeathMotion.decode(rig, m.transition),
                        target,
                        SamuraiRig.smooth(m.age / .22f));
            return target;
        }
        SamuraiRig.Pose pose = rig.pose(m.clip, m.time, null, 0, 1, 0, m.pitch);
        if (m.state == CHASE || m.state == BACK_WALK)
            pose = StrideMotion.pose(rig, pose, m.clip, m.time);
        if (m.phase == 1 && m.state == DRAW && m.clip.equals(SamuraiRig.DRAW))
            pose = DrawMotion.plantFeet(rig, pose, d.drawGait);
        float join =
                m.state == SIDE_SWEEP
                        ? .12f
                        : m.state == ATTACK_A || m.state == ATTACK_B
                                ? Math.min(
                                        .16f,
                                        CombatTiming.windows(m.phase, m.state == ATTACK_B)[0][0]
                                                / d.rate
                                                * .8f)
                                : .3f;
        if (m.state == SHEATHE && d.sheatheReverse >= 0) join = .15f;
        if (m.age < join && m.source.contains("Matrices"))
            pose =
                    FootworkMotion.join(
                            rig,
                            DeathMotion.decode(rig, m.source),
                            pose,
                            SamuraiRig.smooth(m.age / join));
        if (m.state == CHASE || m.state == BACK_WALK)
            pose = StepMotion.interpolate(rig, pose, d.previousTerrain, d.terrain, d.terrainBlend);
        float elapsed = d.reactionAge;
        if (elapsed >= 0 && elapsed < .5f && d.reactionAllowed)
            pose =
                    FootworkMotion.reaction(
                            rig,
                            pose,
                            d.power * (float) Math.sin(Math.PI * elapsed / .5f),
                            d.hitSide);
        return m.state == SIDE_SWEEP ? rig.lockSideGrip(pose) : pose;
    }

    private PoseComposer() {}
}
