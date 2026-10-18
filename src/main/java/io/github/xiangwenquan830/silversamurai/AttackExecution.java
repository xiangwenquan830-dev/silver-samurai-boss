package io.github.xiangwenquan830.silversamurai;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;

/** Owns per-swing bookkeeping and execution. Entity state transitions stay with SamuraiEntity. */
final class AttackExecution {
    private final Set<UUID> hit = new HashSet<>();
    private final Set<BlockPos> blocks = new HashSet<>();
    private int lastStrike = -1, brokenBlocks;
    private boolean groundStrike;
    private double dash;

    int lastStrike() {
        return lastStrike;
    }

    int brokenBlocks() {
        return brokenBlocks;
    }

    boolean groundStrike() {
        return groundStrike;
    }

    void markGroundStrike() {
        groundStrike = true;
    }

    void brokeBlock() {
        brokenBlocks++;
    }

    void clearContacts() {
        hit.clear();
        blocks.clear();
    }

    void reset() {
        lastStrike = -1;
        clearContacts();
        brokenBlocks = 0;
    }

    void resetClearing() {
        lastStrike = -1;
        blocks.clear();
        brokenBlocks = 0;
    }

    void startClearing() {
        lastStrike = -1;
    }

    void startSide() {
        lastStrike = 0;
        clearContacts();
        brokenBlocks = 0;
    }

    void startClearStrike() {
        lastStrike = 0;
        blocks.clear();
        brokenBlocks = 0;
    }

    void sweep(SamuraiEntity boss, float from, float to, int strike, Vec3 previous) {
        BladeCombat.sweep(boss, from, to, strike, previous, hit, blocks);
    }

    void combo(
            SamuraiEntity boss, int strike, float t, float before, float[][] windows, float yaw) {
        if (lastStrike != strike) {
            boss.recordMiss(lastStrike);
            lastStrike = strike;
            clearContacts();
            brokenBlocks = 0;
            groundStrike = false;
            float pitch = boss.freezeBladeAim();
            double distance =
                    Math.sqrt(
                            boss.getTarget()
                                    .position()
                                    .subtract(boss.position())
                                    .horizontalDistanceSqr());
            dash =
                    CombatTiming.lunge(
                            distance,
                            boss.phase() == 2 ? 3.1 : 2.9,
                            strike == 2 ? BossConfig.HEAVY_DASH.get() : BossConfig.DASH.get());
            boss.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1, boss.phase() == 2 ? .65f : .9f);
            if (boss.debug())
                GeneratedMod.LOG.info(
                        "[银色武士刀路] Boss={} 第{}刀 起始距离={} 总突进={} 朝向={} 俯仰={}",
                        boss.getId(),
                        strike + 1,
                        distance,
                        dash,
                        yaw,
                        pitch);
        }
        float start = Math.max(before, windows[strike][0]), end = Math.min(t, windows[strike][1]);
        Vec3 previous = boss.position();
        double minimum =
                Math.max(2.6, boss.getBbWidth() * .5 + boss.getTarget().getBbWidth() * .5 + .35);
        boss.lunge(
                Math.min(
                        dash * (end - start) / (windows[strike][1] - windows[strike][0]),
                        CombatSpacing.forwardAllowance(
                                boss.getTarget().position().subtract(boss.position()),
                                yaw,
                                minimum)));
        boss.executingStrike(strike);
        sweep(boss, start, end, strike, previous);
    }
}
