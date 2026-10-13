package cn.blockforge.generated.bossmodbossmodbossm;

import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** One loaded-entity snapshot per tick and position; no cached hostility or game-mode decisions. */
final class BossSenses {
    private final SamuraiEntity boss;
    private long tick = Long.MIN_VALUE, scans;
    private Vec3 origin;
    private double range;
    private List<LivingEntity> nearby = List.of();

    BossSenses(SamuraiEntity boss) {
        this.boss = boss;
    }

    List<LivingEntity> nearby() {
        return nearby(BossConfig.retentionRange());
    }

    List<LivingEntity> nearby(double wanted) {
        long now = boss.level().getGameTime();
        if (tick != now || !boss.position().equals(origin) || range < wanted) {
            tick = now;
            origin = boss.position();
            range = wanted;
            scans++;
            nearby =
                    boss.level()
                            .getEntitiesOfClass(
                                    LivingEntity.class,
                                    Targeting.searchBox(boss, range),
                                    e -> e != boss);
        }
        return nearby;
    }

    List<? extends Player> players() {
        return boss.level().players();
    }

    boolean observerNearby() {
        double r = Math.max(BossConfig.ACTIVATE.get(), BossConfig.PLAYER_ACQUIRE.get());
        for (var p : players())
            if (CombatRules.observer(p) && boss.distanceToSqr(p) <= r * r) return true;
        return false;
    }

    long scans() {
        return scans;
    }
}
