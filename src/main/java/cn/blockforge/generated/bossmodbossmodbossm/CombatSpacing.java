package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** 距离带只决定是否起手；实际伤害仍全部由原模型刀刃扫掠结算。 */
public final class CombatSpacing {
    public enum Band {
        CLOSE,
        READY,
        APPROACH,
        FAR,
        BLOCKED
    }

    public record Profile(double minimum, double preferred, double maximum) {}

    public record Decision(
            Band band, double distance, Profile profile, BladeAim.Solution aim, String reason) {
        public boolean attackable() {
            return band == Band.READY || band == Band.APPROACH;
        }
    }

    public static Profile profile(
            int phase, String clip, int strike, double width, double range, double dash) {
        double body = Math.max(0, width - .6) * .5;
        double preferred =
                (phase == 2 ? 3.1 : 2.9)
                        + (strike == 2 && clip.equals(SamuraiRig.B) ? .15 : 0)
                        + body;
        double minimum = (phase == 2 ? 2.35 : 2.2) + body;
        return new Profile(
                minimum, preferred, Math.min(range - .25 + body, preferred + dash + .35));
    }

    public static Decision assess(
            SamuraiEntity boss, LivingEntity target, String clip, int strike, boolean wasClose) {
        if (!lane(boss, target))
            return new Decision(
                    Band.BLOCKED,
                    Math.sqrt(target.position().subtract(boss.position()).horizontalDistanceSqr()),
                    profile(
                            boss.phase(),
                            clip,
                            strike,
                            target.getBbWidth(),
                            boss.range(strike),
                            strike == 2 ? BossConfig.HEAVY_DASH.get() : BossConfig.DASH.get()),
                    null,
                    "高度或楼板遮挡，继续寻找同层通路");
        return assessLane(boss, target, clip, strike, wasClose);
    }

    /** Caller has checked the lane within the current decision; do not repeat world ray casts. */
    static Decision assessLane(
            SamuraiEntity boss, LivingEntity target, String clip, int strike, boolean wasClose) {
        double dash = strike == 2 ? BossConfig.HEAVY_DASH.get() : BossConfig.DASH.get();
        Profile p =
                profile(boss.phase(), clip, strike, target.getBbWidth(), boss.range(strike), dash);
        double d = Math.sqrt(target.position().subtract(boss.position()).horizontalDistanceSqr());
        if (d < p.minimum() + (wasClose ? .30 : 0))
            return new Decision(Band.CLOSE, d, p, null, "刀臂过近，先短后撤或斜步");
        if (d > p.maximum()) return new Decision(Band.FAR, d, p, null, "超出本刀可用接近距离，继续寻路");
        double landing = Math.max(p.preferred(), d - dash);
        landing = Math.min(d, landing);
        BladeAim.Solution aim =
                BladeAim.solve(
                        boss.phase(),
                        clip,
                        strike,
                        landing,
                        target.getY() - boss.getY(),
                        target.getBbHeight(),
                        target.getBbWidth());
        if (!aim.contact()) return new Decision(Band.BLOCKED, d, p, aim, "本刀可见刀路不能覆盖目标碰撞箱");
        return new Decision(
                d > p.preferred() + .65 ? Band.APPROACH : Band.READY,
                d,
                p,
                aim,
                d > p.preferred() + .65 ? "稍远，保留该刀原有短接近" : "间距合适，允许蓄力");
    }

    public static boolean lane(SamuraiEntity boss, LivingEntity target) {
        return boss.perception().lane(target);
    }

    public static double forwardAllowance(Vec3 offset, float yaw, double minimum) {
        Vec3 forward = new Vec3(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
        // 使用沿锁定方向的投影，不用斜向距离；目标跑到侧后方后不再向旧方向过冲。
        return Math.max(0, offset.x * forward.x + offset.z * forward.z - minimum);
    }

    private CombatSpacing() {}
}
