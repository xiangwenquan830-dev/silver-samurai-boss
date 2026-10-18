package io.github.xiangwenquan830.silversamurai;

/** Shared server/client clock: faster cadence, with a minimum safe gap between damage windows. */
public final class CombatTiming {
    public static float[][] windows(int phase, boolean b) {
        return phase == 2
                ? new float[][] {{.424f, .625f}, {1.161f, 1.394f}, {2.385f, 2.56f}}
                : new float[][] {
                    {.30f, .47f}, {1.03f, 1.24f}, {b ? 2.34f : 1.93f, b ? 2.51f : 2.14f}
                };
    }

    public static float animationTime(float seconds, float rate, int phase, boolean b) {
        float[][] w = windows(phase, b);
        float time = 0, elapsed = 0;
        for (int i = 0; i < 3; i++) {
            float gap = w[i][0] - time;
            float duration =
                    i == 0
                            ? gap / rate
                            : Math.max(gap / rate, BossConfig.STRIKE_PAUSE.get().floatValue());
            if (seconds < elapsed + duration)
                return time + gap * Math.max(0, (seconds - elapsed) / duration);
            elapsed += duration;
            time = w[i][0];
            duration = (w[i][1] - time) / rate;
            if (seconds < elapsed + duration) return time + (seconds - elapsed) * rate;
            elapsed += duration;
            time = w[i][1];
        }
        return time + (seconds - elapsed) * rate;
    }

    public static float cap(float damage, double cap) {
        return damage <= 0 ? 0 : (float) (cap > 0 ? Math.min(damage, cap) : damage);
    }

    public static double lunge(double horizontal, double desired, double configured) {
        return Math.max(0, Math.min(configured, horizontal - desired));
    }

    private CombatTiming() {}
}
