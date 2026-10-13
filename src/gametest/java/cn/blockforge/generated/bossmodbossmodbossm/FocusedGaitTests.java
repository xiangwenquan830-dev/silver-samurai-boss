package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("silver_samurai_gait")
@PrefixGameTestTemplate(false)
public final class FocusedGaitTests {
    @GameTest(template = "arena", timeoutTicks = 120, batch = "gait_flat")
    public static void flat(GameTestHelper h) {
        GaitGameTests.flat_node_cadence(h);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "gait_stair_start")
    public static void up(GameTestHelper h) {
        GaitGameTests.uphill_start_cadence(h);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "gait_stair_down")
    public static void down(GameTestHelper h) {
        GaitGameTests.downhill_cadence(h);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "gait_flat_two")
    public static void flat_two(GameTestHelper h) {
        GaitGameTests.phase_two_flat(h);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "gait_up_two")
    public static void up_two(GameTestHelper h) {
        GaitGameTests.phase_two_up(h);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "gait_down_two")
    public static void down_two(GameTestHelper h) {
        GaitGameTests.phase_two_down(h);
    }

    @GameTest(template = "arena", timeoutTicks = 20, batch = "gait_overlay")
    public static void overlay(GameTestHelper h) {
        GaitGameTests.stair_overlay_join(h);
    }

    @GameTest(template = "arena", timeoutTicks = 80, batch = "gait_stop")
    public static void stopped(GameTestHelper h) {
        GaitGameTests.halted_pursuit_does_not_march(h);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "spacing_safety")
    public static void spacing(GameTestHelper h) {
        SamuraiGameTests.spacing_safety(h);
    }
}
