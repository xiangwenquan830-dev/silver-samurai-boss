package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("silver_samurai_r40")
@PrefixGameTestTemplate(false)
public class FocusedR40Tests {
    @GameTest(template = "arena", timeoutTicks = 240, batch = "r40_assets")
    public static void assets(GameTestHelper h) {
        SideInterceptGameTests.assets(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r40_hit_one_left")
    public static void hit_one_left(GameTestHelper h) {
        SideInterceptGameTests.hit_one_left(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r40_hit_one_right")
    public static void hit_one_right(GameTestHelper h) {
        SideInterceptGameTests.hit_one_right(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r40_hit_two_left")
    public static void hit_two_left(GameTestHelper h) {
        SideInterceptGameTests.hit_two_left(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r40_hit_two_right")
    public static void hit_two_right(GameTestHelper h) {
        SideInterceptGameTests.hit_two_right(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r40_wall")
    public static void wall(GameTestHelper h) {
        SideInterceptGameTests.wall(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r40_orbit_one")
    public static void orbit_one(GameTestHelper h) {
        SideInterceptGameTests.orbit_one(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r40_orbit_two")
    public static void orbit_two(GameTestHelper h) {
        SideInterceptGameTests.orbit_two(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r40_reverse")
    public static void reverse(GameTestHelper h) {
        SideInterceptGameTests.reverse(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r40_height")
    public static void height(GameTestHelper h) {
        SideInterceptGameTests.height(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r40_save")
    public static void save(GameTestHelper h) {
        SideInterceptGameTests.save(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "r40_invalid")
    public static void invalid(GameTestHelper h) {
        SideInterceptGameTests.invalid(h);
    }

    @GameTest(template = "arena", timeoutTicks = 220, batch = "r40_crossing")
    public static void crossing(GameTestHelper h) {
        SideInterceptGameTests.crossing(h);
    }

    @GameTest(template = "arena", timeoutTicks = 750, batch = "r40_wolverine")
    public static void wolverine(GameTestHelper h) throws Exception {
        RedBreakGameTests.wolverine_side(h);
    }

    @GameTest(template = "arena", timeoutTicks = 110, batch = "r40_shield")
    public static void shield(GameTestHelper h) {
        RedBreakGameTests.shield(h);
    }

    @GameTest(template = "arena", timeoutTicks = 110, batch = "r40_cancel")
    public static void cancel(GameTestHelper h) {
        RedBreakGameTests.cancelled(h);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r41_planner_limits")
    public static void planner_limits(GameTestHelper h) {
        SideReliabilityGameTests.planner_limits(h);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r41_live_one_left")
    public static void live_one_left(GameTestHelper h) {
        SideReliabilityGameTests.live_one_left(h);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r41_live_one_right")
    public static void live_one_right(GameTestHelper h) {
        SideReliabilityGameTests.live_one_right(h);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r41_live_two_left")
    public static void live_two_left(GameTestHelper h) {
        SideReliabilityGameTests.live_two_left(h);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r41_live_two_right")
    public static void live_two_right(GameTestHelper h) {
        SideReliabilityGameTests.live_two_right(h);
    }
}
