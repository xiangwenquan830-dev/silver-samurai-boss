package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AuditGameTests {
    @GameTest(template = "arena", timeoutTicks = 80, batch = "audit_crowded_acquire")
    public static void crowded_acquire(GameTestHelper h) {
        crowded(h, 26);
    }

    @GameTest(template = "arena", timeoutTicks = 80, batch = "audit_crowded_rolling")
    public static void crowded_rolling(GameTestHelper h) {
        crowded(h, 90);
    }

    private static void crowded(GameTestHelper h, int count) {
        floor(h);
        var b = boss(h, 4.5, 10.5);
        var p = player(h, 4.5, 1, 22.5);
        for (int z = 0; z <= 8; z++)
            for (int y = 1; y <= 6; y++) h.setBlock(8, y, z, Blocks.BEDROCK);
        for (int i = 0; i < count; i++) {
            var z = h.spawn(EntityType.ZOMBIE, new BlockPos(10, 1, 3));
            z.setNoAi(true);
            z.setPos(h.absoluteVec(new Vec3(10.5, 1, 3.5)));
        }
        h.runAtTickTime(
                35,
                () -> {
                    h.assertTrue(
                            b.getTarget() == p,
                            "hidden nearby crowd starved visible player acquisition: "
                                    + b.report());
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "audit_goal")
    public static void blocked_goal(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 5.5);
        b.setNoAi(true);
        var p = player(h, 10.5, 1, 8.8);
        b.setTarget(p);
        for (int x = 8; x <= 12; x++) {
            h.setBlock(x, 1, 7, Blocks.STONE);
            h.setBlock(x, 2, 7, Blocks.STONE_SLAB);
        }
        h.assertFalse(
                b.terrainNavigation()
                        .goal(
                                SamuraiNavigation.point(b.position()),
                                SamuraiNavigation.point(p.position())),
                "navigator accepted goal behind low obstruction");
        p.discard();
        b.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 140, batch = "audit_draw_turn")
    public static void draw_turn(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 13.5);
        var p = player(h, 10.5, 1, 4.5);
        b.setTarget(p);
        h.runAtTickTime(
                100,
                () -> {
                    h.assertTrue(
                            b.weaponsDrawn(),
                            "navigation interrupted drawing repeatedly: " + b.report());
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 380, batch = "audit_visible_detour")
    public static void visible_detour(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 6.5);
        var p = player(h, 10.5, 1, 12.5);
        b.testPursuit(p);
        for (int x = 8; x <= 12; x++)
            for (int y = 1; y <= 2; y++) h.setBlock(x, y, 9, Blocks.BEDROCK);
        h.onEachTick(() -> p.setDeltaMovement(Vec3.ZERO));
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            p.getHealth() < 1000,
                            "no real attack after nearby detour: " + b.report());
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }
}
