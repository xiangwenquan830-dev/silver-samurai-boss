package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Compatibility facade for the r26 reproductions; one shared fixture implementation. */
final class ReproFixtures {
    static void floor(GameTestHelper h) {
        SamuraiGameTests.floor(h);
    }

    static SamuraiEntity boss(GameTestHelper h, double x, double z) {
        return SamuraiGameTests.boss(h, x, z);
    }

    static ServerPlayer player(GameTestHelper h, double x, double y, double z) {
        return SamuraiGameTests.player(h, x, y, z);
    }
}
