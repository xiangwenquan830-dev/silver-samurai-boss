package io.github.xiangwenquan830.silversamurai;

import net.minecraft.util.Mth;

/** Shares the turn allowance across close-defense and attack requests in one server tick. */
final class FacingBudget {
    private long tick = Long.MIN_VALUE;
    private float spent;

    float turn(long now, float before, float desired, float limit) {
        if (tick != now) {
            tick = now;
            spent = 0;
        }
        float after = Mth.approachDegrees(before, desired, Math.max(0, limit - spent));
        spent += Math.abs(Mth.wrapDegrees(after - before));
        return after;
    }
}
