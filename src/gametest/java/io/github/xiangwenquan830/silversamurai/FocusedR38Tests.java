package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("silver_samurai_r38")
@PrefixGameTestTemplate(false)
public class FocusedR38Tests {
    @GameTest(template = "arena", timeoutTicks = 60)
    public static void clearance(GameTestHelper h) {
        GazeClearanceGameTests.face_clearance(h);
    }

    @GameTest(template = "arena", timeoutTicks = 60)
    public static void grip(GameTestHelper h) {
        GazeClearanceGameTests.bow_grip(h);
    }

    @GameTest(template = "arena", timeoutTicks = 60)
    public static void neck(GameTestHelper h) {
        GazeClearanceGameTests.neck_limit(h);
    }

    @GameTest(template = "arena", timeoutTicks = 60)
    public static void continuity(GameTestHelper h) {
        GazeClearanceGameTests.bow_continuity(h);
    }

    @GameTest(template = "arena", timeoutTicks = 60)
    public static void actions(GameTestHelper h) {
        GazeClearanceGameTests.authored_actions(h);
    }
}
