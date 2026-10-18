package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("silver_samurai_r32")
@PrefixGameTestTemplate(false)
public final class FocusedR32Tests {
    @GameTest(template = "arena", timeoutTicks = 30, batch = "damage")
    public static void damage(GameTestHelper h) {
        R32GameTests.damage_policy(h);
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "heal")
    public static void heal(GameTestHelper h) {
        R32GameTests.healing_paths(h);
    }

    @GameTest(template = "arena", timeoutTicks = 170, batch = "expiry")
    public static void expiry(GameTestHelper h) {
        R32GameTests.wound_lifecycle(h);
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "contact")
    public static void contact(GameTestHelper h) {
        R32GameTests.accepted_contact(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "kick")
    public static void kick(GameTestHelper h) {
        R32GameTests.kick_then_blade(h);
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "shared")
    public static void shared(GameTestHelper h) {
        R32GameTests.shared_cooldown_reload(h);
    }

    @GameTest(template = "arena", timeoutTicks = 80, batch = "wolverine")
    public static void wolverine(GameTestHelper h) throws Exception {
        R32GameTests.wolverine_regeneration(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "red_combo")
    public static void red_combo(GameTestHelper h) {
        SamuraiGameTests.combo_red_static(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "red_repeat_0")
    public static void red_repeat_0(GameTestHelper h) {
        SamuraiGameTests.combo_red_static(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "red_repeat_1")
    public static void red_repeat_1(GameTestHelper h) {
        SamuraiGameTests.combo_red_static(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "red_repeat_2")
    public static void red_repeat_2(GameTestHelper h) {
        SamuraiGameTests.combo_red_static(h);
    }

    @GameTest(template = "arena", timeoutTicks = 240, batch = "red_repeat_3")
    public static void red_repeat_3(GameTestHelper h) {
        SamuraiGameTests.combo_red_static(h);
    }

    @GameTest(template = "arena", timeoutTicks = 260, batch = "stairs_repeat_0")
    public static void stairs_repeat_0(GameTestHelper h) {
        ReacquisitionGameTests.stairs_up_live(h);
    }

    @GameTest(template = "arena", timeoutTicks = 260, batch = "stairs_repeat_1")
    public static void stairs_repeat_1(GameTestHelper h) {
        ReacquisitionGameTests.stairs_up_live(h);
    }

    @GameTest(template = "arena", timeoutTicks = 260, batch = "stairs_repeat_2")
    public static void stairs_repeat_2(GameTestHelper h) {
        ReacquisitionGameTests.stairs_up_live(h);
    }

    @GameTest(template = "arena", timeoutTicks = 260, batch = "stairs_repeat_3")
    public static void stairs_repeat_3(GameTestHelper h) {
        ReacquisitionGameTests.stairs_up_live(h);
    }
}
