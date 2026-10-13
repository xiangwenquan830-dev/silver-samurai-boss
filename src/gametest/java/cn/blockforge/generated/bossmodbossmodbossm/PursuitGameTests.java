package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import static cn.blockforge.generated.bossmodbossmodbossm.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PursuitGameTests {
    @GameTest(template = "arena", timeoutTicks = 20, batch = "ai_body_corridor")
    public static void body_corridor(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 5.5);
        b.setNoAi(true);
        var p = player(h, 10.5, 1, 15.5);
        var nav = new SamuraiNavigation(b);
        h.assertTrue(nav.approachQuality(p) == 1, "open approach not recognized");
        // Eye ray stays clear, but the head cannot pass beneath this low ceiling.
        for (int z = 7; z <= 10; z++)
            for (int x = 9; x <= 11; x++) h.setBlock(x, 4, z, Blocks.STONE);
        nav.obstacleChanged(h.absolutePos(new net.minecraft.core.BlockPos(10, 4, 7)));
        h.assertTrue(nav.approachQuality(p) == -1, "full body ceiling ignored");
        for (int z = 7; z <= 10; z++) for (int x = 9; x <= 11; x++) h.setBlock(x, 4, z, Blocks.AIR);
        nav.obstacleChanged(h.absolutePos(new net.minecraft.core.BlockPos(10, 4, 7)));
        h.assertTrue(nav.approachQuality(p) == 1, "changed obstacle retained stale cache");
        p.discard();
        b.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 70, batch = "ai_stable_lock")
    public static void stable_lock(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 6.5);
        var first = player(h, 10.5, 1, 15.5);
        var other = player(h, 12.5, 1, 15.5);
        b.testPursuit(first);
        h.onEachTick(
                () -> {
                    first.setDeltaMovement(Vec3.ZERO);
                    other.setDeltaMovement(Vec3.ZERO);
                });
        h.runAtTickTime(
                18,
                () -> {
                    h.assertTrue(b.getTarget() == first, "similar candidates caused lock churn");
                    first.discard();
                    other.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 90, batch = "ai_swing_lock")
    public static void swing_lock(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var first = player(h, 10.5, 1, 10.6);
        var other = player(h, 12, 1, 10.6);
        b.testCombat(first, 2, false);
        h.onEachTick(
                () -> {
                    first.setDeltaMovement(Vec3.ZERO);
                    other.setDeltaMovement(Vec3.ZERO);
                    if (b.state() == SamuraiEntity.ATTACK_A)
                        h.assertTrue(
                                b.getTarget() == first, "changed target during committed swing");
                });
        h.runAtTickTime(
                45,
                () -> {
                    first.discard();
                    other.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 300, batch = "ai_failed_goal")
    public static void failed_goal(GameTestHelper h) {
        floor(h);
        var b = boss(h, 5.5, 5.5);
        b.setNoAi(true);
        var nav = new SamuraiNavigation(b);
        for (int x = 3; x <= 7; x++)
            for (int z = 3; z <= 7; z++)
                if (x == 3 || x == 7 || z == 3 || z == 7)
                    for (int y = 1; y <= 10; y++) h.setBlock(x, y, z, Blocks.BEDROCK);
        Vec3 target = h.absoluteVec(new Vec3(18.5, 1, 5.5));
        h.onEachTick(
                () -> {
                    nav.chase(target, 1);
                    if (nav.failed()) {
                        h.assertTrue(
                                nav.failedDestination(target),
                                "failed route destination not remembered");
                        nav.cancel();
                        h.assertTrue(
                                nav.failedDestination(target),
                                "target reset erased failed route memory");
                        h.assertTrue(
                                !nav.failedDestination(target.add(4, 0, 0)),
                                "moved destination remained blacklisted");
                        nav.obstacleChanged(
                                h.absolutePos(new net.minecraft.core.BlockPos(7, 1, 5)));
                        h.assertTrue(
                                !nav.failedDestination(target),
                                "obstacle change did not permit retry");
                        b.discard();
                        h.succeed();
                    }
                });
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "ai_reachable_choice")
    public static void reachable_choice(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 5.5);
        var blocked = player(h, 10.5, 1, 13.5);
        var open = player(h, 21.5, 1, 5.5);
        for (int z = 7; z <= 9; z++)
            for (int x = 9; x <= 11; x++) h.setBlock(x, 4, z, Blocks.STONE);
        h.runAtTickTime(
                8,
                () -> {
                    h.assertTrue(
                            b.getTarget() == open,
                            "preferred visible low-ceiling route over open target: " + b.report());
                    blocked.discard();
                    open.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 300, batch = "ai_hidden_memory")
    public static void hidden_memory(GameTestHelper h) {
        floor(h);
        var b = boss(h, 5.5, 5.5);
        var p = player(h, 18.5, 1, 5.5);
        b.testPursuit(p);
        for (int x = 3; x <= 7; x++)
            for (int z = 3; z <= 7; z++)
                if (x == 3 || x == 7 || z == 3 || z == 7)
                    for (int y = 1; y <= 10; y++) h.setBlock(x, y, z, Blocks.BEDROCK);
        h.runAtTickTime(
                280,
                () -> {
                    h.assertTrue(
                            b.getTarget() == null,
                            "reacquired unseen remembered target through wall: " + b.report());
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }
}
