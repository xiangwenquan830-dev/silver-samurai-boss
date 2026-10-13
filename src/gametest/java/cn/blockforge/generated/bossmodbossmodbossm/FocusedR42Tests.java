package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("silver_samurai_r42")
@PrefixGameTestTemplate(false)
public class FocusedR42Tests {
    @GameTest(template = "arena", timeoutTicks = 440, batch = "r42_giant_one")
    public static void giant_one(GameTestHelper h) {
        LargeTargetGameTests.giant_one(h);
    }

    @GameTest(template = "arena", timeoutTicks = 440, batch = "r42_giant_two")
    public static void giant_two(GameTestHelper h) {
        LargeTargetGameTests.giant_two(h);
    }

    @GameTest(template = "arena", timeoutTicks = 440, batch = "r42_giant_offset")
    public static void giant_offset(GameTestHelper h) {
        LargeTargetGameTests.giant_offset(h);
    }

    @GameTest(template = "arena", timeoutTicks = 440, batch = "r42_body_goal")
    public static void body_goal(GameTestHelper h) {
        LargeTargetGameTests.body_goal(h);
    }

    @GameTest(template = "arena", timeoutTicks = 440, batch = "r42_body_acquisition")
    public static void body_acquisition(GameTestHelper h) {
        LargeTargetGameTests.body_acquisition(h);
    }

    @GameTest(template = "arena", timeoutTicks = 440, batch = "r42_body_wall")
    public static void body_wall(GameTestHelper h) {
        LargeTargetGameTests.body_wall(h);
    }

    @GameTest(template = "arena", timeoutTicks = 440, batch = "r42_quiet_animation")
    public static void quiet_animation(GameTestHelper h) {
        LargeTargetGameTests.quiet_animation(h);
    }

    @GameTest(template = "arena", timeoutTicks = 440, batch = "r42_nearby_enemy")
    public static void nearby_enemy(GameTestHelper h) {
        LargeTargetGameTests.nearby_enemy(h);
    }

    @GameTest(template = "arena", timeoutTicks = 440, batch = "r42_real_visage")
    public static void real_visage(GameTestHelper h) {
        LargeTargetGameTests.real_visage(h);
    }

    @GameTest(template = "arena", timeoutTicks = 750, batch = "r42_wolverine")
    public static void wolverine(GameTestHelper h) throws Exception {
        RedBreakGameTests.wolverine_side(h);
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "r42_config")
    public static void config(GameTestHelper h) {
        R33GameTests.config_relationship(h);
    }
}
