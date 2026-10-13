package cn.blockforge.generated.bossmodbossmodbossm;

/** The authored final frame has both hands settled; hold it while the blade charges. */
final class RedBladeTiming {
    static int gripTicks() {
        return (int) Math.ceil(SamuraiRig.get(2).length(SamuraiRig.TRANSITION) * 20);
    }

    static int pauseTicks() {
        return 4;
    }

    static int chargeTicks() {
        return 18;
    }

    static int totalTicks() {
        return gripTicks() + pauseTicks() + chargeTicks();
    }

    static float progress(double ticksSinceCharge) {
        return SamuraiRig.smooth((float) (ticksSinceCharge / chargeTicks()));
    }

    private RedBladeTiming() {}
}
