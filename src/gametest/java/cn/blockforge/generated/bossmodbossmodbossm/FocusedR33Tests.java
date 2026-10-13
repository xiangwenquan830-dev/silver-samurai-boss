package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("silver_samurai_r33")
@PrefixGameTestTemplate(false)
public final class FocusedR33Tests {
    @GameTest(template = "arena", timeoutTicks = 30, batch = "segments")
    public static void segments(GameTestHelper h) {
        R33GameTests.segmented_routes(h);
    }

    @GameTest(template = "long_arena", timeoutTicks = 420, batch = "far")
    public static void far(GameTestHelper h) {
        R33GameTests.far_live(h);
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "memory")
    public static void memory(GameTestHelper h) {
        R33GameTests.last_seen_timeout(h);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "config")
    public static void config(GameTestHelper h) {
        R33GameTests.config_relationship(h);
    }

    @GameTest(template = "arena", timeoutTicks = 260, batch = "stairs_up")
    public static void stairs_up(GameTestHelper h) {
        ReacquisitionGameTests.stairs_up_live(h);
    }

    @GameTest(template = "arena", timeoutTicks = 260, batch = "stairs_down")
    public static void stairs_down(GameTestHelper h) {
        ReacquisitionGameTests.stairs_down_live(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "combo_red")
    public static void combo_red(GameTestHelper h) {
        SamuraiGameTests.combo_red_static(h);
    }

    @GameTest(template = "arena", timeoutTicks = 450, batch = "changed")
    public static void changed(GameTestHelper h) {
        PressureGameTests.changed_route(h);
    }

    @GameTest(template = "arena", timeoutTicks = 380, batch = "stair_death")
    public static void stair_death(GameTestHelper h) {
        CombatFlowGameTests.stairs_target_dies(h);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "stair_cadence")
    public static void stair_cadence(GameTestHelper h) {
        GaitGameTests.uphill_start_cadence(h);
    }
}
