package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import static io.github.xiangwenquan830.silversamurai.ReproFixtures.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ReacquisitionGameTests {
    @GameTest(
            templateNamespace = "silver_samurai",
            template = "arena",
            timeoutTicks = 65,
            batch = "repro_turn")
    public static void behind_reaction(GameTestHelper h) {
        floor(h);
        var b = boss(h, 12.5, 12.5);
        var p = player(h, 12.5, 1, 9.4);
        b.testPursuit(p);
        float[] yaw = {b.getYRot()};
        boolean[] attack = {false};
        long[] firstAttack = {-1};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    h.assertTrue(
                            Math.abs(net.minecraft.util.Mth.wrapDegrees(b.getYRot() - yaw[0]))
                                    <= 7.1,
                            "turn teleported: " + b.report());
                    yaw[0] = b.getYRot();
                    attack[0] |= ActionRules.attack(b.state());
                    if (attack[0] && firstAttack[0] < 0) firstAttack[0] = h.getTick();
                });
        h.runAtTickTime(
                45,
                () -> {
                    h.assertTrue(
                            attack[0],
                            "behind target still has no attack after 45 ticks: " + b.report());
                    GeneratedMod.LOG.info(
                            "[r26 reproduction] behind target attack starts at tick={}",
                            firstAttack[0]);
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(
            templateNamespace = "silver_samurai",
            template = "arena",
            timeoutTicks = 30,
            batch = "repro_view")
    public static void partial_visibility(GameTestHelper h) {
        floor(h);
        var b = boss(h, 6.5, 10.5);
        var p = player(h, 14.5, 1, 10.5);
        // An overhead beam cuts the tall boss's eye ray; its torso sees the target underneath.
        for (int z = 8; z <= 12; z++) h.setBlock(8, 4, z, Blocks.STONE);
        h.assertFalse(b.hasLineOfSight(p), "fixture must occlude the vanilla eye ray");
        h.runAtTickTime(
                8,
                () -> {
                    h.assertTrue(
                            b.getTarget() == p,
                            "visible torso target not acquired under beam: " + b.report());
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    static void stairs(GameTestHelper h) {
        var stair =
                Blocks.STONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
        for (int x = 8; x < 24; x++)
            for (int z = 0; z < 24; z++) {
                int top = Math.min(4, x - 7);
                for (int y = 1; y < top; y++) h.setBlock(x, y, z, Blocks.STONE);
                h.setBlock(
                        new BlockPos(x, top, z), x < 12 ? stair : Blocks.STONE.defaultBlockState());
            }
    }

    @GameTest(
            templateNamespace = "silver_samurai",
            template = "arena",
            timeoutTicks = 60,
            batch = "repro_grid")
    public static void stair_grid_offsets(GameTestHelper h) {
        floor(h);
        stairs(h);
        var b = boss(h, 3.25, 10.5);
        b.setNoAi(true);
        for (double fraction : new double[] {.05, .25, .45, .55, .75, .95}) {
            Vec3 start = h.absoluteVec(new Vec3(3 + fraction, 1, 10.5)),
                    goal = h.absoluteVec(new Vec3(17 + fraction, 5, 10.5));
            for (boolean reverse : new boolean[] {false, true}) {
                var planner =
                        new RoutePlanner(
                                b.terrainNavigation(),
                                SamuraiNavigation.point(reverse ? goal : start),
                                SamuraiNavigation.point(reverse ? start : goal),
                                new RoutePlanner.Settings(3, 3, 4, 0, 900, 0));
                while (!planner.done()) planner.step(12);
                h.assertTrue(
                        planner.result() != null,
                        "ordinary stairs fail at offset=" + fraction + " down=" + reverse);
            }
        }
        b.discard();
        h.succeed();
    }

    @GameTest(
            templateNamespace = "silver_samurai",
            template = "arena",
            timeoutTicks = 260,
            batch = "repro_up")
    public static void stairs_up_live(GameTestHelper h) {
        live(h, false);
    }

    @GameTest(
            templateNamespace = "silver_samurai",
            template = "arena",
            timeoutTicks = 260,
            batch = "repro_down")
    public static void stairs_down_live(GameTestHelper h) {
        live(h, true);
    }

    private static void live(GameTestHelper h, boolean down) {
        floor(h);
        stairs(h);
        var b = boss(h, down ? 16.25 : 3.25, 10.5);
        if (down) b.setPos(h.absoluteVec(new Vec3(16.25, 5, 10.5)));
        var p = player(h, down ? 3.25 : 17.25, down ? 1 : 5, 10.5);
        h.runAtTickTime(
                8,
                () ->
                        h.assertTrue(
                                b.getTarget() == p,
                                "stairs acquire exceeded 8 ticks: " + b.report()));
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    if (!h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02))) {
                        GeneratedMod.LOG.error(
                                "[stairs collision] tick={} relative={} box={} nearby={}",
                                h.getTick(),
                                h.relativeVec(b.position()),
                                b.getBoundingBox(),
                                h.getLevel().getEntities(b, b.getBoundingBox().inflate(.02)));
                        for (var pos :
                                net.minecraft.core.BlockPos.betweenClosed(
                                        net.minecraft.core.BlockPos.containing(
                                                b.getBoundingBox().minX,
                                                b.getBoundingBox().minY,
                                                b.getBoundingBox().minZ),
                                        net.minecraft.core.BlockPos.containing(
                                                b.getBoundingBox().maxX,
                                                b.getBoundingBox().maxY,
                                                b.getBoundingBox().maxZ))) {
                            var state = h.getLevel().getBlockState(pos);
                            var shape = state.getCollisionShape(h.getLevel(), pos);
                            if (!shape.isEmpty()
                                    && shape.bounds()
                                            .move(pos)
                                            .intersects(b.getBoundingBox().deflate(.02)))
                                GeneratedMod.LOG.error(
                                        "[stairs collision block] {} {}",
                                        h.relativePos(pos),
                                        state);
                        }
                        h.fail(
                                "stairs clipped during execution: "
                                        + b.position()
                                        + " "
                                        + b.report());
                    }
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            p.getHealth() < 1000,
                            "stairs never reached actual damage: "
                                    + b.report()
                                    + " at "
                                    + b.position());
                    GeneratedMod.LOG.info(
                            "[r26 reproduction] stairs down={} real damage at tick={}",
                            down,
                            h.getTick());
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }
}
