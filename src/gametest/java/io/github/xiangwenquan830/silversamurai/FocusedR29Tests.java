package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("silver_samurai_r29")
@PrefixGameTestTemplate(false)
public final class FocusedR29Tests {
    @GameTest(template = "arena", timeoutTicks = 120, batch = "r29_old_uphill")
    public static void old_uphill(GameTestHelper h) {
        GaitGameTests.uphill_start_cadence(h);
    }

    @GameTest(template = "arena", timeoutTicks = 20, batch = "r29_old_place")
    public static void old_place(GameTestHelper h) {
        EngineeringGameTests.place_notify(h);
    }

    @GameTest(template = "arena", timeoutTicks = 130, batch = "near_blocked_one")
    public static void one(GameTestHelper h) {
        NearReproTests.blocked_one(h);
    }

    @GameTest(template = "arena", timeoutTicks = 130, batch = "near_blocked_two")
    public static void two(GameTestHelper h) {
        NearReproTests.blocked_two(h);
    }

    @GameTest(template = "arena", timeoutTicks = 130, batch = "near_leaf_lower")
    public static void leaf(GameTestHelper h) {
        NearReproTests.lower_leaf(h);
    }

    @GameTest(template = "arena", timeoutTicks = 180, batch = "r29_red_waits_for_grip")
    public static void red_waits_for_grip(GameTestHelper h) {
        R29FeatureTests.red_waits_for_grip(h);
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r29_red_reload_gate")
    public static void red_reload_gate(GameTestHelper h) {
        R29FeatureTests.red_reload_gate(h);
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r29_damage_classes")
    public static void damage_classes(GameTestHelper h) {
        R29FeatureTests.damage_classes(h);
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r29_poison_is_effect_damage")
    public static void poison_is_effect_damage(GameTestHelper h) {
        R29FeatureTests.poison_is_effect_damage(h);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r29_moving_repath_cadence")
    public static void moving_repath_cadence(GameTestHelper h) {
        R29FeatureTests.moving_repath_cadence(h);
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "r29_kick_respects_new_wall")
    public static void kick_respects_new_wall(GameTestHelper h) {
        R29FeatureTests.kick_respects_new_wall(h);
    }
}
