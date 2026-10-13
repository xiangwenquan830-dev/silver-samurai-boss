package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import static cn.blockforge.generated.bossmodbossmodbossm.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CombatFlowGameTests {
    @GameTest(template = "arena", timeoutTicks = 30, batch = "flow_perception_cache")
    public static void perception_cache(GameTestHelper h) {
        floor(h);
        var b = boss(h, 6.5, 10.5);
        b.setNoAi(true);
        var p = player(h, 14.5, 1, 10.5);
        for (int z = 8; z <= 12; z++) h.setBlock(8, 4, z, Blocks.STONE);
        h.assertFalse(b.hasLineOfSight(p), "fixture eye ray should be occluded");
        h.assertTrue(Targeting.visible(b, p), "torso cannot see beneath beam");
        CombatSpacing.lane(b, p);
        long count = b.perception().rayCount();
        for (int i = 0; i < 100; i++) {
            Targeting.visible(b, p);
            CombatSpacing.lane(b, p);
        }
        h.assertTrue(b.perception().rayCount() == count, "same-pose rays repeated");
        p.setPos(p.position().add(.2, 0, 0));
        Targeting.visible(b, p);
        h.assertTrue(b.perception().rayCount() > count, "target motion used stale visibility");
        for (int y = 1; y < 7; y++) for (int z = 8; z <= 12; z++) h.setBlock(8, y, z, Blocks.STONE);
        b.obstacleChanged(h.absolutePos(new BlockPos(8, 4, 10)));
        h.assertFalse(
                Targeting.visible(b, p),
                "same-tick obstacle notification did not invalidate visibility");
        GeneratedMod.LOG.info(
                "[r27 perception] cached 100 selection/lane pairs, ray casts={}", count);
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "flow_blade_visibility")
    public static void blade_below_beam(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        b.testCombat(p, 1, false);
        p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        for (int x = 8; x <= 12; x++) h.setBlock(x, 3, 9, Blocks.STONE);
        h.assertFalse(b.hasLineOfSight(p), "fixture must hide eye ray");
        h.assertTrue(CombatSpacing.lane(b, p), "fixture must retain clear torso lane");
        h.onEachTick(() -> p.setDeltaMovement(Vec3.ZERO));
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            p.getHealth() < 1000,
                            "actual blade below beam rejected: " + b.report());
                    b.setNoAi(true);
                    var window = b.windows()[0];
                    p.invulnerableTime = 0;
                    float health = p.getHealth();
                    for (int x = 8; x <= 12; x++)
                        for (int y = 1; y <= 6; y++) h.setBlock(x, y, 9, Blocks.STONE);
                    b.obstacleChanged(h.absolutePos(new BlockPos(10, 2, 9)));
                    BladeCombat.sweep(
                            b,
                            window[0],
                            window[1],
                            0,
                            b.position(),
                            new java.util.HashSet<>(),
                            new java.util.HashSet<>());
                    h.assertTrue(
                            p.getHealth() == health,
                            "visibility fix allowed damage through solid wall");
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "flow_ready_now")
    public static void ready_without_stale_wait(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 14);
        b.testPursuit(p);
        h.runAtTickTime(
                3,
                () -> {
                    p.setPos(b.position().add(0, 0, 3.1));
                    p.setDeltaMovement(Vec3.ZERO);
                });
        h.runAtTickTime(
                5,
                () -> {
                    h.assertTrue(
                            ActionRules.attack(b.state()),
                            "ready target waited for old distance cache: " + b.report());
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 110, batch = "flow_flee_one")
    public static void flee_combo_one(GameTestHelper h) {
        flee(h, 1, false);
    }

    @GameTest(template = "arena", timeoutTicks = 110, batch = "flow_flee_two")
    public static void flee_combo_two(GameTestHelper h) {
        flee(h, 2, false);
    }

    @GameTest(template = "arena", timeoutTicks = 110, batch = "flow_hide")
    public static void hide_between_strikes(GameTestHelper h) {
        flee(h, 1, true);
    }

    private static void flee(GameTestHelper h, int phase, boolean wall) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.6);
        p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        b.testCombat(p, phase, false);
        long[] changed = {-1};
        boolean[] recovery = {false};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    if (changed[0] < 0 && b.hitCounts()[0] > 0 && b.age(0) > b.windows()[0][1]) {
                        changed[0] = h.getTick();
                        p.setPos(b.position().add(0, 0, 10));
                        if (wall) {
                            int z = (int) Math.ceil(h.relativeVec(b.position()).z + 2);
                            for (int x = 6; x <= 15; x++)
                                for (int y = 1; y <= 6; y++) h.setBlock(x, y, z, Blocks.STONE);
                            b.obstacleChanged(h.absolutePos(new BlockPos(10, 3, z)));
                        }
                    }
                    if (changed[0] >= 0) {
                        recovery[0] |= b.state() == SamuraiEntity.RECOVER;
                        if (h.getTick() - changed[0] >= 15) {
                            h.assertTrue(
                                    recovery[0] && !ActionRules.attack(b.state()),
                                    "kept swinging after target escaped: " + b.report());
                            h.assertTrue(
                                    b.state() == SamuraiEntity.CHASE
                                            && (b.walkingSpeed() > 0
                                                    || b.terrainNavigation().planning()),
                                    "safe recovery did not reconnect to pursuit: " + b.report());
                            h.assertTrue(
                                    b.hitCounts()[1] == 0 && b.hitCounts()[2] == 0,
                                    "unreachable later swings inflicted damage");
                            h.succeed();
                        }
                    }
                });
    }

    @GameTest(template = "arena", timeoutTicks = 420, batch = "flow_stair_reverse")
    public static void stairs_reverse(GameTestHelper h) {
        floor(h);
        ReacquisitionGameTests.stairs(h);
        var b = boss(h, 3.25, 10.5);
        var p = player(h, 17.25, 5, 10.5);
        boolean[] reversed = {false};
        p.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    if (!reversed[0] && b.getY() > h.absoluteVec(new Vec3(0, 2, 0)).y) {
                        reversed[0] = true;
                        p.setPos(h.absoluteVec(new Vec3(3.25, 1, 10.5)));
                    }
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "stair reversal clipped blocks");
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            reversed[0] && p.getHealth() < 1000,
                            "failed to pursue reversing stair target: " + b.report());
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 380, batch = "flow_stair_death")
    public static void stairs_target_dies(GameTestHelper h) {
        stairReplacement(h, false);
    }

    @GameTest(template = "arena", timeoutTicks = 380, batch = "flow_stair_creative")
    public static void stairs_target_creative(GameTestHelper h) {
        stairReplacement(h, true);
    }

    private static void stairReplacement(GameTestHelper h, boolean creative) {
        floor(h);
        ReacquisitionGameTests.stairs(h);
        var b = boss(h, 3.25, 10.5);
        var first = player(h, 17.25, 5, 10.5);
        var second = player(h, 3.25, 1, 10.5);
        second.setGameMode(GameType.SPECTATOR);
        long[] changed = {-1};
        boolean[] replaced = {false};
        h.onEachTick(
                () -> {
                    if (h.getTick() % 40 == 0)
                        GeneratedMod.LOG.info(
                                "[r33 replacement] tick={} pos={} first={} second={} changed={} state={} nav={}",
                                h.getTick(),
                                h.relativeVec(b.position()),
                                h.relativeVec(first.position()),
                                h.relativeVec(second.position()),
                                changed[0],
                                b.state(),
                                b.terrainNavigation().report());
                    first.setDeltaMovement(Vec3.ZERO);
                    second.setDeltaMovement(Vec3.ZERO);
                    if (changed[0] < 0 && b.getY() > h.absoluteVec(new Vec3(0, 2, 0)).y) {
                        changed[0] = h.getTick();
                        if (creative) first.setGameMode(GameType.CREATIVE);
                        else first.discard();
                        second.setGameMode(GameType.SURVIVAL);
                    }
                    if (changed[0] >= 0 && b.getTarget() == second) replaced[0] = true;
                    if (changed[0] >= 0 && h.getTick() - changed[0] == 15)
                        h.assertTrue(
                                replaced[0],
                                "stair traversal starved replacement acquisition: " + b.report());
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "replacement canceled safe landing");
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            replaced[0] && second.getHealth() < 1000,
                            "replacement never reached actual attack: " + b.report());
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 300, batch = "flow_stair_crowd")
    public static void stair_entrance_crowd(GameTestHelper h) {
        floor(h);
        ReacquisitionGameTests.stairs(h);
        var b = boss(h, 3.25, 10.5);
        var p = player(h, 17.25, 5, 10.5);
        java.util.List<net.minecraft.world.entity.monster.Zombie> enemies =
                new java.util.ArrayList<>();
        for (int z : new int[] {9, 10, 11}) {
            var e = h.spawn(EntityType.ZOMBIE, new BlockPos(7, 1, z));
            e.setNoAi(true);
            e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            e.setHealth(1000);
            enemies.add(e);
        }
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "crowd drove boss into stairs");
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            enemies.stream().anyMatch(e -> e.getHealth() < 1000),
                            "stair entrance crowd caused turn-only lock: " + b.report());
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "flow_progress")
    public static void oscillation_watchdog(GameTestHelper h) {
        var watch = new PursuitProgress();
        var goal = new Vec3(10, 0, 0);
        PursuitProgress.Result result = null;
        for (int i = 0; i <= 40; i++)
            result = watch.sample(i, new Vec3(Math.sin(i * Math.PI / 10) * .3, 0, 0), goal);
        h.assertTrue(
                result == PursuitProgress.Result.REPLAN,
                "oscillation did not request another approach");
        for (int i = 41; i <= 80; i++)
            result = watch.sample(i, new Vec3(Math.sin(i * Math.PI / 10) * .3, 0, 0), goal);
        h.assertTrue(
                result == PursuitProgress.Result.SWITCH_TARGET,
                "repeated oscillation did not permit switching");
        watch.reset();
        for (int i = 0; i <= 120; i++)
            h.assertTrue(
                    watch.sample(i, new Vec3(0, i * .035, 0), new Vec3(0, 8, 0))
                            == PursuitProgress.Result.ADVANCING,
                    "vertical climbing counted as stuck");
        watch.reset();
        for (int i = 0; i <= 120; i++)
            h.assertTrue(
                    watch.sample(i, new Vec3(-i * .05, 0, 0), goal)
                            == PursuitProgress.Result.ADVANCING,
                    "detour away from target counted as stuck");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "flow_isolation")
    public static void fixture_isolation(GameTestHelper h) {
        var outsider = EntityType.ZOMBIE.create(h.getLevel());
        outsider.setNoAi(true);
        outsider.setPos(h.absoluteVec(new Vec3(60, 10, 60)));
        h.getLevel().addFreshEntity(outsider);
        try {
            floor(h);
            h.assertFalse(outsider.isRemoved(), "fixture cleanup deleted another arena entity");
            var p = player(h, 10.5, 1, 10.5);
            h.assertFalse(
                    p.isInvulnerableTo(p.damageSources().generic()),
                    "fixture retains login protection");
        } finally {
            outsider.discard();
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "flow_bounded_candidates")
    public static void bounded_candidates(GameTestHelper h) {
        floor(h);
        var b = boss(h, 3.5, 10.5);
        b.setNoAi(true);
        for (int i = 0; i < 140; i++) {
            var e = h.spawn(EntityType.ZOMBIE, new BlockPos(15, 1, 10));
            e.setNoAi(true);
        }
        Targeting policy = new Targeting(b);
        policy.select(SamuraiRig.A);
        h.assertTrue(policy.evaluated() <= 64, "candidate evaluation exceeded bound");
        h.succeed();
    }
}
