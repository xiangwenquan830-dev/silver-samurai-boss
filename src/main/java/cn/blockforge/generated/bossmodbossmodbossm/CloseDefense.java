package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.nbt.CompoundTag;

/** One cooldown owner for both close-range actions. Waiting never owns the AI tick. */
final class CloseDefense {
    enum Action {
        NONE,
        FOCUSED,
        CROWD
    }

    private final SamuraiEntity boss;
    private long nextFocused, nextCrowd, nextShared;

    CloseDefense(SamuraiEntity boss) {
        this.boss = boss;
    }

    private long now() {
        return boss.level().getGameTime();
    }

    Action choose(boolean bladeReady, int recovery) {
        if (bladeReady
                || recovery > 0
                || !boss.weaponsDrawn()
                || !ActionRules.canBeginAttack(boss.state())
                || boss.state() == SamuraiEntity.FOOTWORK
                || now() < nextShared) return Action.NONE;
        boolean crowd = BossConfig.STOMP.get() && now() >= nextCrowd;
        boolean focused =
                BossConfig.CLOSE_DEFENSE.get()
                        && now() >= nextFocused
                        && StompCombat.closeReach(boss, boss.getTarget());
        if (!crowd && !focused) return Action.NONE;
        if (!boss.terrainNavigation().support(SamuraiNavigation.point(boss.position())))
            return Action.NONE;
        if (crowd
                && StompCombat.crowded(
                        boss, BossConfig.STOMP_TRIGGER_RANGE.get(), BossConfig.STOMP_COUNT.get()))
            return Action.CROWD;
        return focused ? Action.FOCUSED : Action.NONE;
    }

    void started(Action action) {
        if (action == Action.NONE) return;
        nextShared = now() + BossConfig.CLOSE_SHARED.get();
        if (action == Action.CROWD) nextCrowd = now() + BossConfig.STOMP_COOLDOWN.get();
        else nextFocused = now() + BossConfig.CLOSE_COOLDOWN.get();
    }

    long crowdRemaining() {
        return Math.max(0, nextCrowd - now());
    }

    long sharedRemaining() {
        return Math.max(0, nextShared - now());
    }

    void save(CompoundTag tag) {
        tag.putLong("SamuraiCloseCooldown", Math.max(0, nextFocused - now()));
        tag.putLong("SamuraiCloseSharedCooldown", sharedRemaining());
    }

    void load(CompoundTag tag, long crowdRemaining) {
        nextCrowd = now() + bounded(crowdRemaining, BossConfig.STOMP_COOLDOWN.get());
        long focused =
                bounded(tag.getLong("SamuraiCloseCooldown"), BossConfig.CLOSE_COOLDOWN.get());
        nextFocused = now() + focused;
        // Old worlds had independent cooldowns: keep their remaining wait rather than opening a
        // free kick.
        long shared =
                tag.contains("SamuraiCloseSharedCooldown")
                        ? tag.getLong("SamuraiCloseSharedCooldown")
                        : Math.max(crowdRemaining, focused);
        nextShared = now() + bounded(shared, BossConfig.CLOSE_SHARED.get());
    }

    private static long bounded(long value, long max) {
        return Math.max(0, Math.min(value, max));
    }
}
