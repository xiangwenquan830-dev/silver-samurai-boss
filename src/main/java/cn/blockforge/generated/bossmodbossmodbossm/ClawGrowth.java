package cn.blockforge.generated.bossmodbossmodbossm;

/** Fixed-root clipping shared by both camera modes. Growth starts only on a new request. */
final class ClawGrowth {
    static final float SPEED = .5f;
    static final int GROW_TICKS = 48, BREAK_TICKS = 6, RETRACT_AT = 40, RETRACT_TICKS = 8;

    static float smooth(float x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    static float growth(float remaining) {
        return smooth(1 - remaining / GROW_TICKS);
    }

    static float tip(float x, float remaining) {
        float age = ClawSeverance.DURATION - remaining, cut = ClawCut.end(x);
        if (age < BREAK_TICKS) return 11.55f + (cut - 11.55f) * smooth(age / BREAK_TICKS);
        return cut * (1 - smooth((age - RETRACT_AT) / RETRACT_TICKS));
    }

    static float grownTip(float remaining) {
        return 11.55f * growth(remaining);
    }

    static float effort(float remaining) {
        float t = 1 - remaining / GROW_TICKS;
        return t <= 0 || t >= 1 ? 0 : (float) Math.sin(Math.PI * t);
    }

    static float shake(float remaining) {
        return effort(remaining) * (float) Math.sin((GROW_TICKS - remaining) * 1.8f);
    }

    static float height(float min, float max, float tip) {
        return Math.max(0, Math.min(max - min, tip - min));
    }

    private ClawGrowth() {}
}
