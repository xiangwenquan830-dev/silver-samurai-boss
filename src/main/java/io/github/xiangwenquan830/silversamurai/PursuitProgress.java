package io.github.xiangwenquan830.silversamurai;

import net.minecraft.world.phys.Vec3;

/** Detect repeated local oscillation across route rebuilds, not just a single failed edge. */
final class PursuitProgress {
    enum Result {
        ADVANCING,
        REPLAN,
        SWITCH_TARGET
    }

    private long start = -1, last = -1;
    private Vec3 origin = Vec3.ZERO, goal = Vec3.ZERO;
    private double distance;
    private int stalledWindows;

    void reset() {
        start = last = -1;
        stalledWindows = 0;
    }

    int stalledWindows() {
        return stalledWindows;
    }

    Result sample(long now, Vec3 position, Vec3 target) {
        if (start < 0 || now - last > 20 || goal.distanceToSqr(target) > 4) {
            start = now;
            origin = position;
            goal = target;
            distance = position.distanceTo(target);
            stalledWindows = 0;
        }
        last = now;
        if (now - start < 40) return Result.ADVANCING;
        boolean moved = position.distanceToSqr(origin) > .75 * .75;
        boolean closer = distance - position.distanceTo(target) > .35;
        if (moved || closer) stalledWindows = 0;
        else stalledWindows++;
        start = now;
        origin = position;
        goal = target;
        distance = position.distanceTo(target);
        return stalledWindows >= 2
                ? Result.SWITCH_TARGET
                : stalledWindows == 1 ? Result.REPLAN : Result.ADVANCING;
    }
}
