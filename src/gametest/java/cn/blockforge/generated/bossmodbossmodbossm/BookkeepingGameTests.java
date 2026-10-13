package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BookkeepingGameTests {
    @GameTest(template = "arena", timeoutTicks = 20, batch = "bookkeeping_turn")
    public static void shared_turn_allowance(GameTestHelper h) {
        var turn = new FacingBudget();
        float yaw = turn.turn(10, 0, 90, 7);
        h.assertTrue(
                yaw == 7 && turn.turn(10, yaw, -90, 7) == 7,
                "same-tick requests added a second turn allowance");
        h.assertTrue(
                turn.turn(10, yaw, 90, 12) == 12,
                "a larger limit failed to use only its remaining allowance");
        h.assertTrue(turn.turn(11, 12, 90, 7) == 19, "next tick did not replenish the allowance");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 20, batch = "bookkeeping_wrap")
    public static void turn_wrap_and_instances(GameTestHelper h) {
        var first = new FacingBudget();
        float yaw = first.turn(1, 179, -179, 7);
        h.assertTrue(
                Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw + 179)) < .001,
                "turn crossed the long way around the angle boundary");
        h.assertTrue(
                first.turn(1, yaw, 90, 7) == yaw - 5,
                "wrapped two-degree turn consumed an incorrect allowance");
        h.assertTrue(
                new FacingBudget().turn(1, 0, 90, 7) == 7,
                "one entity consumed another entity's budget");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 20, batch = "bookkeeping_hit")
    public static void damage_and_protection_distinct(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var boss = SamuraiGameTests.boss(h, 10, 10);
        boss.setNoAi(true);
        var d = new HitDiagnostics();
        d.record(boss, 0, boss, 20, 15, true, false, "damage");
        d.record(boss, 1, boss, 20, 20, false, true, "protected");
        d.record(boss, 2, boss, 20, 20, true, false, "zero damage");
        h.assertTrue(
                java.util.Arrays.equals(d.acceptedHits(), new int[] {1, 0, 0}),
                "zero damage or protected contact counted as actual damage");
        h.assertTrue(
                d.report().contains("刀路接触=[1, 1, 1]") && d.report().contains("受伤保护=[0, 1, 0]"),
                "diagnostic categories mixed");
        h.assertFalse(d.miss(1), "protected contact was incorrectly called a miss");
        int[] copy = d.acceptedHits();
        copy[0] = 99;
        h.assertTrue(d.acceptedHits()[0] == 1, "diagnostic result exposed mutable counters");
        boss.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 20, batch = "bookkeeping_reset")
    public static void combo_reset_and_last_contact(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var boss = SamuraiGameTests.boss(h, 10, 10);
        boss.setNoAi(true);
        var d = new HitDiagnostics();
        d.record(boss, 0, boss, 20, 15, true, false, "damage");
        d.record(boss, 1, boss, 20, 20, false, true, "protected");
        h.assertFalse(d.miss(-1), "unstarted strike reported as a miss");
        h.assertTrue(d.miss(2), "empty third strike not reported");
        String last = d.lastContact();
        d.resetCounts();
        h.assertTrue(d.lastContact().equals(last), "new combo erased previous diagnostic context");
        h.assertTrue(d.report().contains("刀路接触=[0, 0, 0]"), "combo counters not reset");
        h.assertTrue(
                d.report().contains("有效=[0, 0, 0]") && d.report().contains("受伤保护=[0, 0, 0]"),
                "damage/protection counters leaked into next combo");
        boss.discard();
        h.succeed();
    }
}
