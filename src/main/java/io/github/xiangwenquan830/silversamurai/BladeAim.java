package io.github.xiangwenquan830.silversamurai;

import net.minecraft.world.phys.Vec3;
import java.util.*;

/** 蓄力期用真实刀路选俯仰；只转腰腹整条骨骼链，不移动独立刀柄。 */
public final class BladeAim {
    public record Solution(float pitch, float yawOffset, double distance, double error) {
        public boolean contact() {
            return Math.abs(error - (Math.abs(pitch) * .0005 + Math.abs(yawOffset) * .001)) < 1e-9;
        }
    }

    private static final Map<String, List<List<SamuraiRig.Blade>>> CACHE = new HashMap<>();

    public static Solution solve(
            int phase,
            String clip,
            int strike,
            double distance,
            double targetY,
            double height,
            double width) {
        return solve(phase, clip, strike, distance, targetY, height, width, false);
    }

    private record Request(
            int phase,
            String clip,
            int strike,
            double distance,
            double y,
            double height,
            double width,
            boolean clearing) {}

    private static final Map<Request, Solution> SOLUTIONS =
            new LinkedHashMap<>(128, .75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Request, Solution> eldest) {
                    return size() > 512;
                }
            };
    private static long hits, misses;

    static synchronized long cacheHits() {
        return hits;
    }

    static synchronized long cacheMisses() {
        return misses;
    }

    static synchronized void resetSolutions() {
        SOLUTIONS.clear();
        hits = misses = 0;
    }

    public static synchronized Solution solve(
            int phase,
            String clip,
            int strike,
            double distance,
            double targetY,
            double height,
            double width,
            boolean clearing) {
        Request key = new Request(phase, clip, strike, distance, targetY, height, width, clearing);
        Solution cached = SOLUTIONS.get(key);
        if (cached != null) {
            hits++;
            return cached;
        }
        misses++;
        Solution result =
                solveUncached(phase, clip, strike, distance, targetY, height, width, clearing);
        SOLUTIONS.put(key, result);
        return result;
    }

    private static Solution solveUncached(
            int phase,
            String clip,
            int strike,
            double distance,
            double targetY,
            double height,
            double width,
            boolean clearing) {
        float[][] windows = CombatTiming.windows(phase, clip.equals(SamuraiRig.B));
        Solution best = new Solution(0, 0, distance, Double.MAX_VALUE);
        for (float pitch = clearing ? -72 : -36; pitch <= (clearing ? 66 : 24); pitch += 6) {
            String key = phase + "/" + clip + "/" + strike + "/" + pitch;
            var samples = CACHE.get(key);
            if (samples == null) {
                samples = new ArrayList<>();
                for (int i = 0; i <= 12; i++)
                    samples.add(
                            SamuraiRig.get(phase)
                                    .blades(
                                            clip,
                                            windows[strike][0]
                                                    + (windows[strike][1] - windows[strike][0])
                                                            * i
                                                            / 12f,
                                            pitch));
                CACHE.put(key, samples);
            }
            for (float yaw : new float[] {-18, -9, 0, 9, 18}) {
                double min = Double.MAX_VALUE;
                sampleLoop:
                for (var blades : samples)
                    for (var blade : blades) {
                        Vec3 a = SamuraiRig.world(blade.base(), Vec3.ZERO, yaw),
                                b = SamuraiRig.world(blade.tip(), Vec3.ZERO, yaw);
                        if (intersects(
                                a,
                                b,
                                -width / 2 - .1,
                                targetY - .1,
                                distance - width / 2 - .1,
                                width / 2 + .1,
                                targetY + height + .1,
                                distance + width / 2 + .1)) {
                            min = 0;
                            break sampleLoop;
                        }
                        Vec3 mid = a.lerp(b, .5);
                        min =
                                Math.min(
                                        min,
                                        Math.min(
                                                error(a, distance, targetY, height, width),
                                                Math.min(
                                                        error(b, distance, targetY, height, width),
                                                        error(
                                                                mid, distance, targetY, height,
                                                                width))));
                    }
                double score = min + Math.abs(pitch) * .0005 + Math.abs(yaw) * .001;
                if (score < best.error) best = new Solution(pitch, yaw, distance, score);
                if (score == 0) return best;
            }
        }
        return best;
    }

    private static double error(Vec3 p, double distance, double y, double h, double w) {
        double dx = Math.max(0, Math.abs(p.x) - w / 2),
                dy = Math.max(0, Math.max(y - p.y, p.y - y - h)),
                dz = Math.max(0, Math.abs(p.z - distance) - w / 2);
        return dx * dx + dy * dy + dz * dz;
    }

    public static boolean intersects(
            Vec3 a,
            Vec3 b,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ) {
        double lo = 0, hi = 1;
        double[] aa = {a.x, a.y, a.z},
                bb = {b.x, b.y, b.z},
                min = {minX, minY, minZ},
                max = {maxX, maxY, maxZ};
        for (int axis = 0; axis < 3; axis++) {
            double d = bb[axis] - aa[axis];
            if (Math.abs(d) < 1e-9) {
                if (aa[axis] < min[axis] || aa[axis] > max[axis]) return false;
            } else {
                double t1 = (min[axis] - aa[axis]) / d, t2 = (max[axis] - aa[axis]) / d;
                lo = Math.max(lo, Math.min(t1, t2));
                hi = Math.min(hi, Math.max(t1, t2));
                if (lo > hi) return false;
            }
        }
        return true;
    }

    private BladeAim() {}
}
