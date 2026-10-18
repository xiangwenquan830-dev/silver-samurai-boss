package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.*;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EngineeringGameTests {
    @GameTest(template = "arena", timeoutTicks = 20, batch = "engineering_goal_once")
    public static void goal_once(GameTestHelper h) {
        floor(h);
        var mob = EntityType.ZOMBIE.create(h.getLevel());
        HostileBossTargetGoal.install(mob);
        HostileBossTargetGoal.install(mob);
        HostileBossTargetGoal.install(mob);
        long count =
                mob.targetSelector.getAvailableGoals().stream()
                        .filter(g -> g.getGoal() instanceof HostileBossTargetGoal)
                        .count();
        h.assertTrue(count == 1, "duplicate hostile goal installed");
        var fresh = EntityType.ZOMBIE.create(h.getLevel());
        HostileBossTargetGoal.install(fresh);
        h.assertTrue(
                fresh.targetSelector.getAvailableGoals().stream()
                        .anyMatch(g -> g.getGoal() instanceof HostileBossTargetGoal),
                "new instance lost goal after reload");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 20, batch = "engineering_aim_cache")
    public static void aim_cache(GameTestHelper h) {
        floor(h);
        BladeAim.resetSolutions();
        for (int phase : new int[] {1, 2}) {
            String clip = phase == 1 ? SamuraiRig.A : SamuraiRig.COMBO_TWO;
            for (int strike = 0; strike < 3; strike++) {
                var original = BladeAim.solve(phase, clip, strike, 3.1, 0, 1.8, .6);
                long misses = BladeAim.cacheMisses();
                for (int i = 0; i < 100; i++)
                    h.assertTrue(
                            original.equals(BladeAim.solve(phase, clip, strike, 3.1, 0, 1.8, .6)),
                            "cached aim changed result");
                h.assertTrue(BladeAim.cacheMisses() == misses, "identical request recomputed");
                BladeAim.solve(phase, clip, strike, 3.1, 1, 1.8, .6);
                h.assertTrue(
                        BladeAim.cacheMisses() == misses + 1, "height change returned stale aim");
            }
        }
        h.assertTrue(BladeAim.cacheHits() >= 600, "repeated work not eliminated");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 20, batch = "engineering_place_notify")
    public static void place_notify(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 5.5);
        b.setNoAi(true);
        var p = player(h, 10.5, 1, 15.5);
        // Placement events query existing world entities, not a spawn awaiting section indexing.
        h.runAtTickTime(
                2,
                () -> {
                    h.assertTrue(
                            b.isAlive()
                                    && h.getLevel()
                                            .getEntitiesOfClass(
                                                    SamuraiEntity.class,
                                                    b.getBoundingBox().inflate(8))
                                            .contains(b),
                            "boss fixture not indexed");
                    var nav = b.terrainNavigation();
                    h.assertTrue(nav.approachQuality(p) == 1, "initial corridor not clear");
                    var pos = h.absolutePos(new net.minecraft.core.BlockPos(10, 4, 7));
                    var snapshot =
                            net.minecraftforge.common.util.BlockSnapshot.create(
                                    h.getLevel().dimension(), h.getLevel(), pos);
                    h.setBlock(10, 4, 7, net.minecraft.world.level.block.Blocks.STONE);
                    BossEvents.placed(
                            new net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent(
                                    snapshot,
                                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                                    p));
                    h.assertTrue(
                            nav.approachQuality(p) == -1,
                            "placement did not invalidate approach; indexed="
                                    + h.getLevel()
                                            .getEntitiesOfClass(
                                                    SamuraiEntity.class,
                                                    new net.minecraft.world.phys.AABB(pos)
                                                            .inflate(8))
                                            .contains(b)
                                    + " removed="
                                    + b.isRemoved());
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }
}
