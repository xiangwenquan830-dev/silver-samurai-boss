package io.github.xiangwenquan830.silversamurai;

/** Threat presence and disengagement timing. Observation alone never invents a combat target. */
final class BossAlert {
    private final SamuraiEntity boss;
    private final Targeting targeting;
    private boolean observer, enemy;
    private int quiet;
    private long nextScan;

    BossAlert(SamuraiEntity boss, Targeting targeting) {
        this.boss = boss;
        this.targeting = targeting;
    }

    boolean observer() {
        return observer;
    }

    boolean enemy() {
        return enemy;
    }

    int quiet() {
        return quiet;
    }

    void restore(int ticks) {
        quiet = Math.min(BossConfig.LOST.get(), ticks);
    }

    boolean combat() {
        return enemy || boss.targetValid(boss.getTarget());
    }

    boolean awake() {
        return observer || combat();
    }

    boolean canDisengage() {
        return !combat() && quiet >= BossConfig.LOST.get();
    }

    void tick() {
        observer = boss.senses().observerNearby();
        if (boss.level().getGameTime() >= nextScan) {
            enemy =
                    boss.senses().nearby().stream()
                                    .anyMatch(
                                            e ->
                                                    boss.targetValid(e)
                                                            && (CombatRules.enemy(boss, e, true)
                                                                    || targeting.threat.containsKey(
                                                                            e.getUUID())))
                            || boss.senses().players().stream()
                                    .anyMatch(targeting::playerInAcquire);
            nextScan = boss.level().getGameTime() + 5;
        }
        quiet = combat() ? 0 : Math.min(BossConfig.LOST.get() + 1, quiet + 1);
    }
}
