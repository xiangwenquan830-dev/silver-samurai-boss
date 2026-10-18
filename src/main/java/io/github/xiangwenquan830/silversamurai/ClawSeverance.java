package io.github.xiangwenquan830.silversamurai;

import net.minecraft.server.level.ServerPlayer;

/**
 * Absolute overworld game time: dimension changes cannot restart the timer. Saved in Forge player
 * data.
 */
final class ClawSeverance {
    static final String UNTIL = "SilverSamuraiClawsUntil", RESTORE = "SilverSamuraiClawsRestore";
    static final int DURATION = 600;
    static final String SOUND = "SilverSamuraiGrowSound",
            READY = "SilverSamuraiClawsReady",
            GROW = "SilverSamuraiClawsGrowthUntil";

    static int growing(ServerPlayer p) {
        return (int)
                Math.max(
                        0,
                        Math.min(
                                ClawGrowth.GROW_TICKS,
                                p.getPersistentData().getLong(GROW) - now(p)));
    }

    static void interrupt(net.minecraft.nbt.CompoundTag data) {
        data.remove(GROW);
        data.remove(SOUND);
    }

    static void copyRecovery(
            net.minecraft.nbt.CompoundTag old, net.minecraft.nbt.CompoundTag next) {
        interrupt(next);
        next.remove(ClawPain.UNTIL);
        if (old.contains(UNTIL)) {
            next.putLong(UNTIL, old.getLong(UNTIL));
            next.putBoolean(READY, old.getBoolean(READY));
            next.putBoolean(RESTORE, false);
        }
    }

    static boolean locked(ServerPlayer p) {
        return p.getPersistentData().contains(UNTIL);
    }

    static void startGrowth(ServerPlayer p) {
        p.getPersistentData().putLong(GROW, now(p) + ClawGrowth.GROW_TICKS);
        p.getPersistentData().remove(SOUND);
    }

    static long now(ServerPlayer p) {
        return p.getServer().overworld().getGameTime();
    }

    static int remaining(ServerPlayer p) {
        return (int) Math.max(0, Math.min(DURATION, p.getPersistentData().getLong(UNTIL) - now(p)));
    }

    static boolean active(ServerPlayer p) {
        return remaining(p) > 0;
    }

    static void begin(ServerPlayer p, boolean restore) {
        if (active(p)) return;
        p.getPersistentData().remove(SOUND);
        p.getPersistentData().remove(READY);
        p.getPersistentData().remove(GROW);
        p.getPersistentData().putLong(UNTIL, now(p) + DURATION);
        p.getPersistentData().putBoolean(RESTORE, restore);
    }

    static void sound(ServerPlayer p) {
        int left = growing(p);
        if (left <= 0 || left > ClawGrowth.GROW_TICKS) return;
        int stage = left > 10 ? 1 : 2;
        var data = p.getPersistentData();
        if (data.getInt(SOUND) >= stage) return;
        data.putInt(SOUND, stage);
        p.level()
                .playSound(
                        null,
                        p.blockPosition(),
                        stage == 1
                                ? net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME
                                : net.minecraft.sounds.SoundEvents.CHAIN_PLACE,
                        net.minecraft.sounds.SoundSource.PLAYERS,
                        stage == 1 ? .65f : .4f,
                        .75f);
    }

    static void clear(ServerPlayer p) {
        p.getPersistentData().remove(SOUND);
        p.getPersistentData().remove(UNTIL);
        p.getPersistentData().remove(RESTORE);
        p.getPersistentData().remove(READY);
        p.getPersistentData().remove(GROW);
    }

    private ClawSeverance() {}
}
