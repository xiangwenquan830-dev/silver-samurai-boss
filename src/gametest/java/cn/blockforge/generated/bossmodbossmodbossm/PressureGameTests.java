package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import org.joml.Vector3f;
import static cn.blockforge.generated.bossmodbossmodbossm.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PressureGameTests {
    @GameTest(template = "arena", timeoutTicks = 450, batch = "pressure_changed_route")
    public static void changed_route(GameTestHelper h) {
        floor(h);
        var b = boss(h, 3.5, 12.5);
        var p = player(h, 20.5, 1, 12.5);
        b.setYRot(-90);
        b.testPursuit(p);
        h.runAtTickTime(
                10,
                () -> {
                    for (int z = 10; z <= 14; z++)
                        for (int y = 1; y <= 6; y++) h.setBlock(10, y, z, Blocks.BEDROCK);
                });
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    if (h.getTick() % 30 == 0)
                        GeneratedMod.LOG.info(
                                "[r33 changed] tick={} pos={} visible={} state={} nav={} target={}",
                                h.getTick(),
                                h.relativeVec(b.position()),
                                Targeting.visible(b, p),
                                b.state(),
                                b.terrainNavigation().report(),
                                b.getTarget() != null);
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            h.getTick() > 10 && p.getHealth() < 1000,
                            "new obstacle left old route stuck: " + b.report());
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "pressure_switch_far")
    public static void switch_far(GameTestHelper h) {
        switchTarget(h, false);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "pressure_switch_wall")
    public static void switch_wall(GameTestHelper h) {
        switchTarget(h, true);
    }

    private static void switchTarget(GameTestHelper h, boolean wall) {
        floor(h);
        var b = boss(h, 10.5, 8.5);
        var far = player(h, 10.5, 1, 14.5);
        var near = player(h, 10.5, 1, 11.6);
        near.setGameMode(GameType.CREATIVE);
        if (wall)
            for (int x = 6; x <= 15; x++)
                for (int y = 1; y <= 6; y++) h.setBlock(x, y, 16, Blocks.BEDROCK);
        h.runAtTickTime(
                12,
                () -> {
                    h.assertTrue(b.getTarget() == far, "initial target was not naturally acquired");
                    far.setPos(h.absoluteVec(new Vec3(10.5, 1, 20.5)));
                    near.setGameMode(GameType.SURVIVAL);
                    b.testPursuit(far);
                });
        h.runAtTickTime(
                32,
                () ->
                        h.assertTrue(
                                b.getTarget() == near,
                                "did not replace recently locked remote/blocked target: "
                                        + b.report()));
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            h.getTick() > 32 && near.getHealth() < 1000,
                            "nearby replacement was not actually hit: " + b.report());
                    near.discard();
                    far.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 210, batch = "pressure_recovery")
    public static void recovery(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 10.5);
        b.testCombat(p, 1, false);
        long[] began = {-1};
        h.onEachTick(
                () -> {
                    p.setDeltaMovement(Vec3.ZERO);
                    if (b.state() == SamuraiEntity.RECOVER && began[0] < 0) began[0] = h.getTick();
                    if (began[0] >= 0 && h.getTick() - began[0] > 8) {
                        h.assertTrue(
                                b.state() != SamuraiEntity.RECOVER,
                                "extra recovery still stalls pursuit");
                        p.discard();
                        b.discard();
                        h.succeed();
                    }
                });
    }

    @GameTest(template = "arena", timeoutTicks = 20, batch = "pressure_stride")
    public static void stride(GameTestHelper h) {
        floor(h);
        h.assertTrue(
                BossConfig.STRIDE.get() * BossConfig.STRIDE_SCALE.get() > 3,
                "stride still too short for boss scale");
        for (int phase : new int[] {1, 2}) {
            var rig = SamuraiRig.get(phase);
            String clip = phase == 1 ? SamuraiRig.WALK_ONE : SamuraiRig.WALK_TWO;
            int ankle = rig.bone("行走_踝_L");
            var pivot = rig.bones.get(ankle).origin;
            float oldMin = Float.MAX_VALUE,
                    oldMax = -Float.MAX_VALUE,
                    newMin = Float.MAX_VALUE,
                    newMax = -Float.MAX_VALUE;
            for (int i = 0; i < 40; i++) {
                float t = rig.length(clip) * i / 40;
                var pose = rig.pose(clip, t);
                float old = pose.matrices()[ankle].transformPosition(new Vector3f(pivot)).z;
                StrideMotion.pose(rig, pose, clip, t);
                float now = pose.matrices()[ankle].transformPosition(new Vector3f(pivot)).z;
                h.assertTrue(Float.isFinite(now), "nonfinite enlarged foot pose");
                oldMin = Math.min(oldMin, old);
                oldMax = Math.max(oldMax, old);
                newMin = Math.min(newMin, now);
                newMax = Math.max(newMax, now);
            }
            h.assertTrue(
                    newMax - newMin > (oldMax - oldMin) * 1.25,
                    "stride was slowed without enlarging foot excursion");
        }
        h.succeed();
    }
}
