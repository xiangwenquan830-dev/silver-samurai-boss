package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("silver_samurai_r34")
@PrefixGameTestTemplate(false)
public final class FocusedR34Tests {
    @GameTest(template = "arena", timeoutTicks = 15, batch = "facing_budget")
    public static void facing_budget(GameTestHelper h) {
        R34GameTests.facing_budget(h);
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "preview_two")
    public static void preview_two(GameTestHelper h) {
        SamuraiGameTests.preview_two(h);
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "head")
    public static void head(GameTestHelper h) {
        R34GameTests.head_branch(h);
    }

    @GameTest(template = "arena", timeoutTicks = 85, batch = "head_live")
    public static void head_live(GameTestHelper h) {
        R34GameTests.head_live(h);
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "clock")
    public static void clock(GameTestHelper h) {
        R34GameTests.faster_clock(h);
    }

    @GameTest(template = "arena", timeoutTicks = 35, batch = "lock")
    public static void lock(GameTestHelper h) {
        R34GameTests.faster_lock(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "a")
    public static void a(GameTestHelper h) {
        SamuraiGameTests.combo_a_static(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "b")
    public static void b(GameTestHelper h) {
        SamuraiGameTests.combo_b_static(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "red")
    public static void red(GameTestHelper h) {
        SamuraiGameTests.combo_red_static(h);
    }

    @GameTest(template = "arena", timeoutTicks = 90, batch = "swing_lock")
    public static void swing_lock(GameTestHelper h) {
        PursuitGameTests.swing_lock(h);
    }

    @GameTest(template = "arena", timeoutTicks = 180, batch = "dodge_lock")
    public static void dodge_lock(GameTestHelper h) {
        SamuraiGameTests.dodge_lock(h);
    }

    @GameTest(template = "arena", timeoutTicks = 25, batch = "crowd_safety")
    public static void safety(GameTestHelper h) {
        CrowdGameTests.safety(h);
    }
}
