package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("silver_samurai_r39")
@PrefixGameTestTemplate(false)
public class FocusedR39Tests {
    @GameTest(template = "arena", timeoutTicks = 30, batch = "motion")
    public static void motion(GameTestHelper h) {
        FastTargetGameTests.motion_reset(h);
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "orbit1")
    public static void orbit1(GameTestHelper h) {
        FastTargetGameTests.orbit_one(h);
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "orbit2")
    public static void orbit2(GameTestHelper h) {
        FastTargetGameTests.orbit_two(h);
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "reverse1")
    public static void reverse1(GameTestHelper h) {
        FastTargetGameTests.reversal_one(h);
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "reverse2")
    public static void reverse2(GameTestHelper h) {
        FastTargetGameTests.reversal_two(h);
    }
}
