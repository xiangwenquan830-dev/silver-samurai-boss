package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("silver_samurai_sidekick")
@PrefixGameTestTemplate(false)
public final class FocusedSideKickTests {
    @GameTest(template = "arena", timeoutTicks = 240, batch = "sidekick_red_combo_regression")
    public static void red_combo(GameTestHelper h) {
        SamuraiGameTests.combo_red_static(h);
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "sidekick_pose")
    public static void pose(GameTestHelper h) {
        GuardGameTests.kick_pose(h);
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "sidekick_sweep")
    public static void sweep(GameTestHelper h) {
        SideKickGameTests.sidekick_sweep(h);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "sidekick_crowd_one")
    public static void crowd_one(GameTestHelper h) {
        CrowdGameTests.crowd_one(h);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "sidekick_crowd_two")
    public static void crowd_two(GameTestHelper h) {
        CrowdGameTests.crowd_two(h);
    }

    @GameTest(template = "arena", timeoutTicks = 130, batch = "sidekick_near")
    public static void near(GameTestHelper h) {
        NearReproTests.lower_leaf(h);
    }
}
