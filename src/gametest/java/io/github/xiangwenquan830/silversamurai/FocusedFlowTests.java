package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("silver_samurai_focus")
@PrefixGameTestTemplate(false)
public final class FocusedFlowTests {
    @GameTest(
            templateNamespace = "silver_samurai_focus",
            template = "arena",
            timeoutTicks = 850,
            batch = "focus_clear")
    public static void clear(GameTestHelper h) {
        SamuraiGameTests.red_clear_corridor(h);
    }

    @GameTest(
            templateNamespace = "silver_samurai_focus",
            template = "arena",
            timeoutTicks = 30,
            batch = "focus_beam")
    public static void beam(GameTestHelper h) {
        CombatFlowGameTests.blade_below_beam(h);
    }
}
