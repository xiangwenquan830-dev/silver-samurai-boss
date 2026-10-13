package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/** Re-evaluate only in windup/recovery gaps. Active blade windows remain committed. */
final class ComboPlanner {
    record Preparation(
            boolean abandon,
            String reason,
            double distance,
            CombatSpacing.Profile profile,
            BladeAim.Solution aim,
            float yaw) {}

    static Preparation prepare(
            SamuraiEntity boss, LivingEntity target, int strike, float remaining, float lockedYaw) {
        double distance =
                Math.sqrt(target.position().subtract(boss.position()).horizontalDistanceSqr());
        double dash = strike == 2 ? BossConfig.HEAVY_DASH.get() : BossConfig.DASH.get();
        var profile =
                CombatSpacing.profile(
                        boss.phase(),
                        boss.clip(),
                        strike,
                        target.getBbWidth(),
                        boss.range(strike),
                        dash);
        var aimPoint = boss.attackTracking().aimPoint(target);
        float yaw =
                (float)
                                Math.toDegrees(
                                        Math.atan2(
                                                aimPoint.z - boss.getZ(), aimPoint.x - boss.getX()))
                        - 90;
        double landing = Math.min(distance, Math.max(profile.preferred(), distance - dash));
        var aim =
                BladeAim.solve(
                        boss.phase(),
                        boss.clip(),
                        strike,
                        landing,
                        target.getY() - boss.getY(),
                        target.getBbHeight(),
                        target.getBbWidth());
        boolean turnImpossible = Math.abs(Mth.wrapDegrees(yaw + aim.yawOffset() - lockedYaw)) > 65;
        boolean beyond = distance > profile.maximum() + .5;
        boolean blocked = !CombatSpacing.lane(boss, target);
        // A distant target can return during windup, but do not spend the whole combo stabbing
        // empty air.
        boolean abandon =
                strike > 0
                        && remaining > .16f
                        && (turnImpossible || beyond || blocked || !aim.contact());
        if (abandon && blocked && boss.debug())
            GeneratedMod.LOG.info("[lane audit] {}", boss.perception().laneFailure(target));
        String reason =
                blocked
                        ? "下一刀通路或高度受阻"
                        : turnImpossible ? "下一刀目标已绕到侧后方" : beyond ? "下一刀目标已脱离距离" : "下一刀真实刀路无法覆盖";
        return new Preparation(abandon, reason, distance, profile, aim, yaw + aim.yawOffset());
    }

    private ComboPlanner() {}
}
