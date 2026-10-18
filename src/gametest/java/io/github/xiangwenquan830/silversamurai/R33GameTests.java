package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class R33GameTests {
    private static RoutePlanner.World flat(boolean blocked) {
        return new RoutePlanner.World() {
            public boolean fits(RoutePlanner.Point p) {
                return p.y() >= 0 && (!blocked || p.x() < 5);
            }

            public boolean support(RoutePlanner.Point p) {
                return Math.abs(p.y()) < 1e-9;
            }

            public boolean clearable(RoutePlanner.Point p, double y) {
                return false;
            }

            public boolean wall(RoutePlanner.Point p, int x, int z, double height) {
                return false;
            }

            public boolean banned(RoutePlanner.Edge e) {
                return false;
            }

            public boolean goal(RoutePlanner.Point p, RoutePlanner.Point t) {
                return p.distance(t) <= 3.6;
            }
        };
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r33_segments")
    public static void segmented_routes(GameTestHelper h) {
        for (var target :
                new RoutePlanner.Point[] {
                    new RoutePlanner.Point(40, 0, 0),
                    new RoutePlanner.Point(48, 0, 0),
                    new RoutePlanner.Point(128, 0, 0),
                    new RoutePlanner.Point(-80, 0, -60)
                }) {
            var world = flat(false);
            var start = new RoutePlanner.Point(0, 0, 0);
            int segments = 0;
            while (!world.goal(start, target) && segments < 10) {
                var plan =
                        new RoutePlanner(
                                world,
                                start,
                                target,
                                new RoutePlanner.Settings(3, 3, 6, 0, 180, 0));
                while (!plan.done()) plan.step(48);
                h.assertTrue(
                        plan.result() != null && !plan.result().isEmpty(),
                        "empty flat segment toward " + target);
                var end = plan.result().get(plan.result().size() - 1).to();
                h.assertTrue(
                        end.distance(target) < start.distance(target) - 1,
                        "segment made no progress");
                h.assertTrue(
                        plan.partial() != world.goal(end, target),
                        "partial goal confused with attack position");
                start = end;
                segments++;
            }
            h.assertTrue(
                    world.goal(start, target),
                    "far goal not reached in bounded segments " + target);
        }
        var blocked =
                new RoutePlanner(
                        flat(true),
                        new RoutePlanner.Point(0, 0, 0),
                        new RoutePlanner.Point(48, 0, 0),
                        new RoutePlanner.Settings(3, 3, 6, 0, 900, 0));
        while (!blocked.done()) blocked.step(48);
        h.assertTrue(blocked.result() == null, "wall incorrectly returned as forward progress");
        h.succeed();
    }

    @GameTest(template = "long_arena", timeoutTicks = 420, batch = "r33_far_live")
    public static void far_live(GameTestHelper h) {
        floor(h);
        for (int x = 0; x < 76; x++)
            for (int z = 0; z < 24; z++) {
                h.setBlock(x, 0, z, Blocks.STONE);
                for (int y = 1; y <= 8; y++) h.setBlock(x, y, z, Blocks.AIR);
            }
        double acquire = BossConfig.ACQUIRE.get(), retain = BossConfig.RETAIN.get();
        BossConfig.ACQUIRE.set(64d);
        BossConfig.RETAIN.set(72d);
        restoreOnFinish(
                h,
                () -> {
                    BossConfig.ACQUIRE.set(acquire);
                    BossConfig.RETAIN.set(retain);
                });
        var b = boss(h, 5.5, 10.5);
        var p = player(h, 18.5, 1, 10.5);
        p.setPos(h.absoluteVec(new Vec3(53.5, 1, 10.5)));
        p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        b.testPursuit(p);
        Vec3[] previous = {b.position()};
        int[] stopped = {0};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    if (h.getTick() % 40 == 0)
                        GeneratedMod.LOG.info(
                                "[r33 far trace] tick={} pos={} target={} partial={} {}",
                                h.getTick(),
                                b.position(),
                                p.position(),
                                b.terrainNavigation().partialRoutes(),
                                b.report());
                    h.assertFalse(
                            b.terrainNavigation().failed(),
                            "open distant goal blacklisted " + b.report());
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.01)),
                            "far route clipped blocks");
                    h.assertTrue(b.position().distanceTo(previous[0]) < .6, "far route teleported");
                    if (h.getTick() > 35
                            && b.distanceTo(p) > 8
                            && b.position().distanceTo(previous[0]) < .005) stopped[0]++;
                    previous[0] = b.position();
                    if (p.getHealth() < 1000) {
                        h.assertTrue(
                                b.terrainNavigation().partialRoutes() > 0,
                                "did not exercise segmentation");
                        h.assertTrue(
                                stopped[0] < 20,
                                "paused too often between route segments: " + stopped[0]);
                        GeneratedMod.LOG.info(
                                "[r33 far-live] real damage tick={} partial={} stopped={}",
                                h.getTick(),
                                b.terrainNavigation().partialRoutes(),
                                stopped[0]);
                        h.succeed();
                    }
                });
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "r33_memory")
    public static void last_seen_timeout(GameTestHelper h) {
        floor(h);
        int old = BossConfig.LAST_SEEN.get(), oldPlayer = BossConfig.PLAYER_MEMORY.get();
        BossConfig.LAST_SEEN.set(0);
        BossConfig.PLAYER_MEMORY.set(0);
        restoreOnFinish(
                h,
                () -> {
                    BossConfig.LAST_SEEN.set(old);
                    BossConfig.PLAYER_MEMORY.set(oldPlayer);
                });
        var b = boss(h, 5.5, 10.5);
        var p = player(h, 18.5, 1, 10.5);
        b.testPursuit(p);
        for (int z = 0; z < 24; z++)
            for (int y = 1; y <= 10; y++) h.setBlock(11, y, z, Blocks.BEDROCK);
        Vec3 origin = b.position();
        h.runAtTickTime(
                12,
                () -> {
                    h.assertTrue(b.position().distanceTo(origin) < .05, "lastSeenTicks=0 ignored");
                    h.assertTrue(
                            b.getTarget() == p && b.weaponsDrawn(),
                            "lost sight timeout incorrectly sheathed/dropped retained target");
                    for (int z = 0; z < 24; z++)
                        for (int y = 1; y <= 10; y++) h.setBlock(11, y, z, Blocks.AIR);
                });
        h.runAtTickTime(
                38,
                () -> {
                    h.assertTrue(
                            b.position().distanceTo(origin) > 1,
                            "visibility return did not resume pursuit");
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "r33_config")
    public static void config_relationship(GameTestHelper h) {
        floor(h);
        double a = BossConfig.ACQUIRE.get(), r = BossConfig.RETAIN.get();
        Runnable restore =
                () -> {
                    BossConfig.ACQUIRE.set(a);
                    BossConfig.RETAIN.set(r);
                };
        restoreOnFinish(h, restore);
        BossConfig.ACQUIRE.set(28d);
        BossConfig.RETAIN.set(8d);
        var b = boss(h, 5.5, 10.5);
        b.setNoAi(true);
        var p = player(h, 20.5, 1, 10.5);
        // This fixture extends across a chunk boundary. Block placement alone does not
        // make the target's entity section visible to queries in a distant test batch.
        var chunk = p.chunkPosition();
        boolean forced = h.getLevel().getForcedChunks().contains(chunk.toLong());
        h.getLevel().setChunkForced(chunk.x, chunk.z, true);
        restoreOnFinish(
                h,
                () -> {
                    if (!forced) h.getLevel().setChunkForced(chunk.x, chunk.z, false);
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            h.getLevel()
                                    .getEntitiesOfClass(
                                            net.minecraft.world.entity.LivingEntity.class,
                                            b.getBoundingBox().inflate(28))
                                    .contains(p),
                            "fixture player not indexed");
                    var targets = new Targeting(b);
                    var selected = targets.select(SamuraiRig.A);
                    h.assertTrue(
                            selected == p && targets.valid(p),
                            "acquired enemy immediately invalidated by smaller retention");
                    b.applyConfig(true);
                    h.assertTrue(
                            b.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 1,
                            "full knockback resistance changed");
                });
    }

    private static void restoreOnFinish(GameTestHelper h, Runnable restore) {
        try {
            var field = GameTestHelper.class.getDeclaredField("testInfo");
            field.setAccessible(true);
            ((GameTestInfo) field.get(h))
                    .addListener(
                            new GameTestListener() {
                                public void testStructureLoaded(GameTestInfo i) {}

                                public void testPassed(GameTestInfo i) {
                                    restore.run();
                                }

                                public void testFailed(GameTestInfo i) {
                                    restore.run();
                                }
                            });
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
