package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import org.joml.*;
import java.lang.Math;
import static cn.blockforge.generated.bossmodbossmodbossm.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class R34GameTests {
    @GameTest(template = "arena", timeoutTicks = 15, batch = "r34_facing_budget")
    public static void facing_budget(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        Vec3 target = b.position().add(-5, 0, 0);
        b.facePoint(target, 7);
        b.facePoint(target, 7);
        h.assertTrue(
                Math.abs(b.getYRot() - 7) < .001,
                "same-tick facing requests doubled body rotation");
        h.runAtTickTime(
                2,
                () -> {
                    b.facePoint(target, 7);
                    b.facePoint(target, 7);
                    h.assertTrue(
                            Math.abs(b.getYRot() - 14) < .001,
                            "new tick did not receive exactly one facing budget");
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r34_head_branch")
    public static void head_branch(GameTestHelper h) {
        for (int phase : new int[] {1, 2}) {
            var rig = SamuraiRig.get(phase);
            int head = rig.bone("头盔");
            for (String clip :
                    phase == 1
                            ? new String[] {"armed_idle", SamuraiRig.A, SamuraiRig.B}
                            : new String[] {
                                SamuraiRig.IDLE_TWO, SamuraiRig.WALK_TWO, SamuraiRig.COMBO_TWO
                            })
                for (float time : new float[] {0, .4f, 1.2f, 2.4f}) {
                    var pose = rig.pose(clip, time);
                    Matrix4f[] before =
                            java.util.Arrays.stream(pose.matrices())
                                    .map(Matrix4f::new)
                                    .toArray(Matrix4f[]::new);
                    Vector3f pivot =
                            before[head].transformPosition(
                                    new Vector3f(rig.bones.get(head).origin));
                    Vector3f wanted =
                            new Vector3f(
                                    (float)
                                            (Math.sin(Math.toRadians(30))
                                                    * Math.cos(Math.toRadians(35))),
                                    (float) -Math.sin(Math.toRadians(35)),
                                    (float)
                                            (-Math.cos(Math.toRadians(30))
                                                    * Math.cos(Math.toRadians(35))));
                    float old =
                            before[head]
                                    .transformDirection(new Vector3f(0, 0, -1))
                                    .normalize()
                                    .dot(wanted);
                    HeadLookMotion.apply(rig, pose, 30, 35, 1);
                    Vector3f forward =
                            pose.matrices()[head]
                                    .transformDirection(new Vector3f(0, 0, -1))
                                    .normalize();
                    h.assertTrue(forward.isFinite(), "head gaze is not finite");
                    h.assertTrue(
                            pose.matrices()[head]
                                            .transformPosition(
                                                    new Vector3f(rig.bones.get(head).origin))
                                            .distance(pivot)
                                    <= 1.321,
                            "neck relief exceeds existing overlap");
                    for (int i = 0; i < rig.bones.size(); i++) {
                        boolean child = false;
                        for (int j = i; j >= 0; j = rig.bones.get(j).parent)
                            if (j == head) {
                                child = true;
                                break;
                            }
                        if (!child)
                            h.assertTrue(
                                    before[i].equals(pose.matrices()[i], .000001f),
                                    "gaze moved sword/arm/body " + rig.bones.get(i).name);
                    }
                }
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 85, batch = "r34_head_live")
    public static void head_live(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        var p = player(h, 10.5, 1, 13.6);
        b.testCombat(p, 2, false);
        b.setNoAi(true);
        float[] yaw = {b.lookYaw()}, pitch = {b.lookPitch()};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    h.assertTrue(
                            Math.abs(b.lookYaw() - yaw[0]) <= 4.01
                                    && Math.abs(b.lookPitch() - pitch[0]) <= 3.01,
                            "head tracking snapped");
                    yaw[0] = b.lookYaw();
                    pitch[0] = b.lookPitch();
                });
        h.runAtTickTime(
                16,
                () -> {
                    h.assertTrue(b.lookPitch() > 25, "attack does not look down at shorter target");
                    p.setPos(h.absoluteVec(new Vec3(8, 1, 13.6)));
                });
        h.runAtTickTime(
                30,
                () -> {
                    h.assertTrue(
                            b.lookYaw() > 20, "head does not turn toward selected side target");
                    // Target moves above the helmet; pitch must cross through neutral smoothly.
                    p.setPos(h.absoluteVec(new Vec3(10.5, 7, 13.6)));
                    p.setNoGravity(true);
                });
        h.runAtTickTime(
                55,
                () -> {
                    h.assertTrue(b.lookPitch() < -10, "head does not follow higher target");
                    p.discard();
                });
        h.runAtTickTime(
                74,
                () -> {
                    h.assertTrue(
                            Math.abs(b.lookYaw()) < .001 && Math.abs(b.lookPitch()) < .001,
                            "lost target left head locked");
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r34_clock")
    public static void faster_clock(GameTestHelper h) {
        h.assertTrue(
                Math.abs(BossConfig.attackRate() - 1.357) < .001,
                "tempo does not apply to existing attackPlaybackRate");
        for (int phase : new int[] {1, 2})
            for (boolean alt : new boolean[] {false, true}) {
                String clip = phase == 2 ? SamuraiRig.COMBO_TWO : alt ? SamuraiRig.B : SamuraiRig.A;
                float length = SamuraiRig.get(phase).length(clip), end = 0, previous = -1;
                float[][] w = CombatTiming.windows(phase, alt);
                int[] first = {-1, -1, -1}, last = {-1, -1, -1};
                for (int tick = 0; tick < 200; tick++) {
                    float now =
                            CombatTiming.animationTime(
                                    tick / 20f, BossConfig.attackRate(), phase, alt);
                    h.assertTrue(now > previous, "nonmonotonic faster clock");
                    previous = now;
                    for (int i = 0; i < 3; i++)
                        if (now >= w[i][0] && now < w[i][1]) {
                            if (first[i] < 0) first[i] = tick;
                            last[i] = tick;
                        }
                    if (now >= length) {
                        end = tick / 20f;
                        break;
                    }
                }
                float old = 0, anim = 0;
                for (int i = 0; i < 3; i++) {
                    old +=
                            i == 0
                                    ? (w[i][0] - anim) / 1.18f
                                    : Math.max((w[i][0] - anim) / 1.18f, .60f);
                    old += (w[i][1] - w[i][0]) / 1.18f;
                    anim = w[i][1];
                }
                old += (length - anim) / 1.18f;
                h.assertTrue(end < old * .93, "combo frequency did not improve");
                for (int i = 1; i < 3; i++)
                    h.assertTrue(
                            first[i] - last[i - 1] >= 10, "inter-strike protection gap too short");
                GeneratedMod.LOG.info(
                        "[r34 tempo] phase={} alt={} oldSeconds={} newSeconds={}",
                        phase,
                        alt,
                        old,
                        end);
            }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 35, batch = "r34_target_lock")
    public static void faster_lock(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 5.5);
        var far = player(h, 10.5, 1, 19.5);
        var near = player(h, 10.5, 1, 9);
        near.setGameMode(GameType.SPECTATOR);
        b.testPursuit(far);
        h.onEachTick(
                () -> {
                    far.setDeltaMovement(Vec3.ZERO);
                    near.setDeltaMovement(Vec3.ZERO);
                });
        h.runAtTickTime(
                6,
                () -> {
                    near.setPos(b.position().add(0, 0, 3.1));
                    near.setGameMode(GameType.SURVIVAL);
                });
        h.runAtTickTime(
                14,
                () -> {
                    h.assertTrue(
                            b.getTarget() == near,
                            "attackable replacement not locked within 8 ticks");
                    h.succeed();
                });
    }
}
