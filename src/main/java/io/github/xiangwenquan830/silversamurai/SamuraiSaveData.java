package io.github.xiangwenquan830.silversamurai;

import net.minecraft.nbt.CompoundTag;

/** Backwards-compatible NBT contract; applying loaded state remains the entity's responsibility. */
record SamuraiSaveData(
        int phase,
        boolean phaseRequested,
        boolean configured,
        int shed,
        boolean drawn,
        int quiet,
        int state,
        float time,
        int side,
        long stompCooldown,
        CompoundTag loadPose,
        Death death) {
    record Death(
            boolean settled,
            CompoundTag surface,
            boolean landed,
            CompoundTag pose,
            int elapsed,
            int hold,
            int feedback,
            float yaw) {}

    void write(CompoundTag tag) {
        tag.putInt("SamuraiPhase", phase);
        tag.putBoolean("SamuraiPhaseRequested", phaseRequested);
        tag.putBoolean("SamuraiConfigured", configured);
        tag.putInt("SamuraiShed", shed);
        tag.putBoolean("SamuraiDrawn", drawn);
        tag.putInt("SamuraiQuiet", quiet);
        tag.putInt("SamuraiSavedState", state);
        tag.putFloat("SamuraiSavedTime", time);
        tag.putInt("SamuraiStepSide", side);
        tag.putLong("SamuraiStompCooldown", stompCooldown);
        if (loadPose != null) tag.put("SamuraiLoadPose", loadPose);
        tag.putBoolean("SamuraiDead", death != null);
        tag.putBoolean("DeathSettled", death != null && death.settled);
        if (death != null) {
            tag.put("DeathSurface", death.surface.copy());
            tag.putBoolean("DeathLanded", death.landed);
            tag.put("DeathPose", death.pose.copy());
            tag.putInt("DeathElapsed", death.elapsed);
            tag.putInt("CorpseHold", death.hold);
            tag.putInt("DeathFeedback", death.feedback);
            tag.putFloat("DeathYaw", death.yaw);
        }
    }

    static SamuraiSaveData read(
            CompoundTag tag, int defaultHold, float defaultYaw, boolean deadHealth) {
        Death death =
                tag.getBoolean("SamuraiDead") || deadHealth
                        ? new Death(
                                tag.getBoolean("DeathSettled"),
                                tag.getCompound("DeathSurface"),
                                !tag.contains("DeathLanded") || tag.getBoolean("DeathLanded"),
                                tag.getCompound("DeathPose"),
                                Math.max(0, tag.getInt("DeathElapsed")),
                                tag.contains("CorpseHold") ? tag.getInt("CorpseHold") : defaultHold,
                                tag.getInt("DeathFeedback"),
                                tag.contains("DeathYaw") ? tag.getFloat("DeathYaw") : defaultYaw)
                        : null;
        return new SamuraiSaveData(
                tag.getInt("SamuraiPhase") == 2 ? 2 : 1,
                tag.getBoolean("SamuraiPhaseRequested"),
                tag.getBoolean("SamuraiConfigured"),
                tag.getInt("SamuraiShed"),
                tag.getBoolean("SamuraiDrawn"),
                Math.max(0, tag.getInt("SamuraiQuiet")),
                tag.getInt("SamuraiSavedState"),
                tag.getFloat("SamuraiSavedTime"),
                tag.getInt("SamuraiStepSide") == -1 ? -1 : 1,
                Math.max(0, tag.getLong("SamuraiStompCooldown")),
                tag.contains("SamuraiLoadPose") ? tag.getCompound("SamuraiLoadPose") : null,
                death);
    }
}
