package io.github.xiangwenquan830.silversamurai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import java.util.*;

/** Bounded, single-cut response using only the approved animation's active blade. */
final class SideIntercept {
    static final float TURN_STEP = 8;
    static final String ONE_L = "双刀_单手侧向快挥_左侧", ONE_R = "双刀_单手侧向快挥_右侧";
    static final String TWO_L = "单刀_松辅手侧向快挥_左侧", TWO_R = "单刀_松辅手侧向快挥_右侧";

    record Plan(String clip, float pitch, float yaw) {}

    private record Samples(int phase, String clip, float pitch) {}

    private static final Map<Samples, List<List<SamuraiRig.Blade>>> CACHE = new HashMap<>();

    static boolean clip(String name) {
        return ONE_L.equals(name) || ONE_R.equals(name) || TWO_L.equals(name) || TWO_R.equals(name);
    }

    static String clip(int phase, boolean left) {
        return phase == 1 ? (left ? ONE_L : ONE_R) : (left ? TWO_L : TWO_R);
    }

    static String hand(String clip) {
        return ONE_R.equals(clip) ? "R" : "L";
    }

    static float start(int phase) {
        return phase == 1 ? .19f : .24f;
    }

    static float end(int phase) {
        return start(phase) + .13f;
    }

    static boolean canTurn(int phase, float time) {
        return time < start(phase) - .025f;
    }

    static float turnBudget(int phase) {
        int ticks = 0;
        // The first AI update after entry is at 0.05 seconds, never at time zero.
        for (int i = 1; canTurn(phase, i * .05f); i++) ticks++;
        return Math.min(24, ticks * TURN_STEP);
    }

    static List<SamuraiRig.Blade> blades(int phase, String clip, float time, float pitch) {
        var rig = SamuraiRig.get(phase);
        return rig.blades(rig.pose(clip, time, null, 0, 1, 0, pitch), hand(clip));
    }

    private static synchronized List<List<SamuraiRig.Blade>> samples(
            int phase, String clip, float pitch) {
        return CACHE.computeIfAbsent(
                new Samples(phase, clip, pitch),
                k -> {
                    List<List<SamuraiRig.Blade>> out = new ArrayList<>();
                    for (int i = 0; i <= 12; i++)
                        out.add(blades(phase, clip, start(phase) + .13f * i / 12, pitch));
                    return out;
                });
    }

    static Plan choose(SamuraiEntity boss, LivingEntity target) {
        if (target == null || !Targeting.visible(boss, target)) return null;
        Vec3 relative =
                TargetGeometry.bodyPoint(target, boss.position().add(0, 2, 0))
                        .subtract(boss.position());
        double bodyRadians = Math.toRadians(boss.getYRot());
        double lateral =
                Math.abs(relative.x * Math.cos(bodyRadians) + relative.z * Math.sin(bodyRadians));
        double forward = -relative.x * Math.sin(bodyRadians) + relative.z * Math.cos(bodyRadians);
        boolean flank = lateral >= .6 && Math.abs(forward) <= lateral * 1.7;
        if (!flank && !boss.attackTracking().fast(target)) return null;
        double distance = relative.horizontalDistanceSqr();
        double minimum = boss.getBbWidth() * .5 + target.getBbWidth() * .5 + .15;
        if (distance < minimum * minimum
                || distance > 25
                || Math.abs(target.getY() - boss.getY()) > 2.4) return null;
        Vec3 lead = boss.attackTracking().aimPoint(target).subtract(target.position());
        AABB predicted = target.getBoundingBox().move(lead).inflate(.10);
        Plan best = null;
        double bestScore = Double.MAX_VALUE;
        for (boolean left : new boolean[] {true, false}) {
            String clip = clip(boss.phase(), left);
            float budget = turnBudget(boss.phase());
            for (float offset : new float[] {0, -12, 12, -budget, budget})
                for (float pitch : new float[] {0, -12, -24, -36, 12, 24}) {
                    float yaw = boss.getYRot() + offset;
                    boolean touches = false;
                    for (var frame : samples(boss.phase(), clip, pitch)) {
                        for (var blade : frame) {
                            Vec3 a = SamuraiRig.world(blade.base(), boss.position(), yaw),
                                    b = SamuraiRig.world(blade.tip(), boss.position(), yaw);
                            if (BladeCombat.contactInRange(
                                            predicted, a, b, boss.position(), boss.range(0))
                                    .isPresent()) {
                                touches = true;
                                break;
                            }
                        }
                        if (touches) break;
                    }
                    if (touches) {
                        double score = Math.abs(offset) + Math.abs(pitch) * .15;
                        if (score < bestScore) {
                            best = new Plan(clip, pitch, yaw);
                            bestScore = score;
                        }
                    }
                }
        }
        return best;
    }

    private SideIntercept() {}
}
