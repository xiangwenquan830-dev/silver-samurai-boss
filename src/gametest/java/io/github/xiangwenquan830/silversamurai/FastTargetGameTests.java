package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.phys.Vec3;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class FastTargetGameTests {
    @GameTest(template = "arena", timeoutTicks = 30, batch = "r39_motion_reset")
    public static void motion_reset(GameTestHelper h) {
        floor(h);
        var p = player(h, 8, 1, 8);
        var other = player(h, 14, 1, 8);
        var tracking = new AttackTracking();
        tracking.sample(p, 1);
        p.setPos(p.position().add(.6, 0, 0));
        tracking.sample(p, 2);
        h.assertTrue(
                tracking.fast(p)
                        && tracking.aimPoint(p).x > p.getX()
                        && tracking.aimPoint(p).distanceTo(p.position()) <= 1.1001,
                "position-derived bounded lead absent");
        p.setPos(p.position().add(-.6, 0, 0));
        tracking.sample(p, 3);
        h.assertTrue(tracking.aimPoint(p).x < p.getX(), "reversal kept obsolete lead");
        tracking.sample(p, 4);
        h.assertFalse(tracking.fast(p), "stopped target kept fast classification");
        p.setPos(p.position().add(9, 0, 0));
        tracking.sample(p, 5);
        h.assertFalse(tracking.fast(p), "teleport became velocity");
        p.setPos(p.position().add(.6, 0, 0));
        tracking.sample(p, 6);
        tracking.sample(other, 7);
        h.assertFalse(tracking.fast(other) || tracking.fast(p), "target switch inherited velocity");
        tracking.sample(null, 8);
        h.assertTrue(
                tracking.aimPoint(other).equals(other.position()), "lost target retained lead");
        p.discard();
        other.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "r39_orbit_one")
    public static void orbit_one(GameTestHelper h) {
        orbit(h, 1, false);
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "r39_orbit_two")
    public static void orbit_two(GameTestHelper h) {
        orbit(h, 2, false);
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "r39_reversal_one")
    public static void reversal_one(GameTestHelper h) {
        orbit(h, 1, true);
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "r39_reversal_two")
    public static void reversal_two(GameTestHelper h) {
        orbit(h, 2, true);
    }

    private static void orbit(GameTestHelper h, int phase, boolean reverse) {
        floor(h);
        var b = boss(h, 11.5, 11.5);
        var p = player(h, 11.5, 1, 15.2);
        if (phase == 2) b.testCombat(p, 2, false);
        b.testPursuit(p);
        int[] swings = {0};
        long[] first = {-1};
        Vec3 center = b.position();
        float[] activeYaw = {0};
        boolean[] active = {false};
        h.onEachTick(
                () -> {
                    double ticks = h.getTick(), cycle = reverse ? ticks % 64 : ticks;
                    double angle = (reverse && cycle > 32 ? 64 - cycle : cycle) * .15 + 1.1;
                    // Position-derived motion: some modded fast mobs do not expose velocity through
                    // deltaMovement.
                    p.setPos(center.add(Math.sin(angle) * 3.7, 0, Math.cos(angle) * 3.7));
                    p.setDeltaMovement(Vec3.ZERO);
                    if (ActionRules.attack(b.state())) {
                        float t = b.age(0), end = b.windows()[0][1], start = b.windows()[0][0];
                        if (t >= start && t <= end) {
                            if (!active[0]) {
                                swings[0]++;
                                if (first[0] < 0) first[0] = h.getTick();
                                activeYaw[0] = b.getYRot();
                            } else
                                h.assertTrue(
                                        Math.abs(
                                                        net.minecraft.util.Mth.wrapDegrees(
                                                                b.getYRot() - activeYaw[0]))
                                                < .001,
                                        "active strike kept homing");
                            active[0] = true;
                        } else active[0] = false;
                    } else active[0] = false;
                });
        h.runAtTickTime(
                200,
                () -> {
                    GeneratedMod.LOG.info(
                            "[r39 fast target] phase={} reverse={} swings={} first={} damage={} report={}",
                            phase,
                            reverse,
                            swings[0],
                            first[0],
                            1000 - p.getHealth(),
                            b.report());
                    h.assertTrue(
                            swings[0] >= 3 && first[0] >= 0 && first[0] <= 35,
                            "fast target starved real blade windows: swings="
                                    + swings[0]
                                    + " first="
                                    + first[0]
                                    + " "
                                    + b.report());
                    h.assertTrue(
                            p.getHealth() < 1000,
                            "predictable fast path never contacted a real blade");
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }
}
