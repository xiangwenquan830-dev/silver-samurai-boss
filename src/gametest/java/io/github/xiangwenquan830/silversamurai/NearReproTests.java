package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NearReproTests {
    @GameTest(template = "arena", timeoutTicks = 130, batch = "near_blocked_one")
    public static void blocked_one(GameTestHelper h) {
        run(h, 1, false);
    }

    @GameTest(template = "arena", timeoutTicks = 130, batch = "near_blocked_two")
    public static void blocked_two(GameTestHelper h) {
        run(h, 2, false);
    }

    @GameTest(template = "arena", timeoutTicks = 130, batch = "near_leaf_lower")
    public static void lower_leaf(GameTestHelper h) {
        run(h, 2, true);
    }

    static void run(GameTestHelper h, int phase, boolean lower) {
        floor(h);
        for (int y = 1; y < 8; y++)
            for (int a = 9; a < 16; a++) {
                h.setBlock(9, y, a, Blocks.STONE);
                h.setBlock(a, y, 9, Blocks.STONE);
                h.setBlock(a, y, 12, Blocks.STONE);
            }
        if (lower)
            for (int x = 10; x <= 11; x++)
                for (int z = 10; z <= 11; z++) h.setBlock(x, 1, z, Blocks.OAK_LEAVES);
        var b = boss(h, 11, 11);
        if (lower) b.setPos(h.absoluteVec(new Vec3(11, 2, 11)));
        var e = h.spawn(EntityType.HUSK, new BlockPos(12, 1, 11));
        e.setNoAi(true);
        e.setPos(h.absoluteVec(new Vec3(12.4, 1, 11)));
        e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        e.setHealth(200);
        e.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        if (phase == 2) b.testCombat(e, 2, false);
        b.facePoint(e.position(), 360);
        b.testPursuit(e);
        int[] changes = {0}, last = {b.state()}, hits = {0};
        float[] hp = {200};
        long[] first = {-1};
        int[] guardSamples = {0};
        h.onEachTick(
                () -> {
                    e.setDeltaMovement(Vec3.ZERO);
                    if (e.getHealth() < hp[0]) {
                        hits[0]++;
                        if (first[0] < 0) first[0] = h.getTick();
                        hp[0] = e.getHealth();
                    }
                    if (b.state() != last[0]) {
                        changes[0]++;
                        last[0] = b.state();
                    }
                    if (phase == 2
                            && b.state() == SamuraiEntity.CLOSE_GUARD
                            && b.stateAge(0) >= .3f) {
                        var rig = SamuraiRig.get(2);
                        var pose = b.currentPose(0);
                        var rest = rig.pose(SamuraiRig.IDLE_TWO, 0);
                        int torso = rig.bone("腰腹_提身控制");
                        for (String name : new String[] {"腕_L", "腕_R", "背挂武士刀_L"}) {
                            int i = rig.bone(name);
                            var local =
                                    new org.joml.Matrix4f(pose.matrices()[torso])
                                            .invert()
                                            .mul(pose.matrices()[i]);
                            var expected =
                                    new org.joml.Matrix4f(rest.matrices()[torso])
                                            .invert()
                                            .mul(rest.matrices()[i]);
                            h.assertTrue(
                                    local.equals(expected, .0001f),
                                    "close guard hand/blade shifted relative to torso");
                        }
                        guardSamples[0]++;
                    }
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "close response clips enclosure");
                });
        h.runAtTickTime(
                115,
                () -> {
                    GeneratedMod.LOG.info(
                            "[r29 near] phase={} lower={} firstHit={} hits={} stateChanges={} report={}",
                            phase,
                            lower,
                            first[0],
                            hits[0],
                            changes[0],
                            b.report());
                    h.assertTrue(
                            first[0] >= 0 && first[0] <= 40,
                            "no prompt actual damage against one enemy at feet: " + b.report());
                    h.assertTrue(
                            hits[0] >= 2, "close enemy remained indefinitely invulnerable to boss");
                    h.assertTrue(changes[0] <= 12, "repeated animation restarts: " + changes[0]);
                    // r32 deliberately spends cooldown repositioning. Validate any settled guard
                    // above, without requiring idle time.
                    h.assertBlockPresent(Blocks.STONE, new BlockPos(9, 1, 11));
                    if (lower) h.assertBlockPresent(Blocks.OAK_LEAVES, new BlockPos(11, 1, 11));
                    h.succeed();
                });
    }
}
