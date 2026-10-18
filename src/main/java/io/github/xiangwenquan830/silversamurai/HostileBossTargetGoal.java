package io.github.xiangwenquan830.silversamurai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/** One goal per live Mob instance; no persistent NBT flag that could suppress it after reload. */
final class HostileBossTargetGoal extends NearestAttackableTargetGoal<SamuraiEntity> {
    private final Mob owner;

    private HostileBossTargetGoal(Mob mob) {
        super(
                mob,
                SamuraiEntity.class,
                20,
                true,
                false,
                e ->
                        BossConfig.MOB_AGGRO.get()
                                && CombatRules.hostileMob(mob)
                                && e instanceof SamuraiEntity boss
                                && boss.active()
                                && boss.activated()
                                && !CombatRules.friend(boss, mob));
        owner = mob;
    }

    static void install(Mob mob) {
        if (mob.targetSelector.getAvailableGoals().stream()
                .noneMatch(g -> g.getGoal() instanceof HostileBossTargetGoal))
            mob.targetSelector.addGoal(3, new HostileBossTargetGoal(mob));
    }

    @Override
    public boolean canUse() {
        return BossConfig.MOB_AGGRO.get()
                && CombatRules.hostileMob(owner)
                && (owner.getTarget() == null || !owner.getTarget().isAlive())
                && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return BossConfig.MOB_AGGRO.get()
                && CombatRules.hostileMob(owner)
                && target instanceof SamuraiEntity boss
                && boss.active()
                && boss.activated()
                && !CombatRules.friend(boss, owner)
                && super.canContinueToUse();
    }
}
