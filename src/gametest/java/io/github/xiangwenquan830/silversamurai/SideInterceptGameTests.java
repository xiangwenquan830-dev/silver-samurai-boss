package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import java.util.*;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class SideInterceptGameTests {
    @GameTest(template = "arena", timeoutTicks = 50, batch = "r40_assets")
    public static void assets(GameTestHelper h) {
        floor(h);
        for (int phase = 1; phase <= 2; phase++)
            for (boolean left : new boolean[] {true, false}) {
                var rig = SamuraiRig.get(phase);
                String clip = SideIntercept.clip(phase, left);
                h.assertTrue(rig.length(clip) > .8f && rig.length(clip) < 1, "missing side clip");
                var a = rig.pose(clip, 0);
                var b = rig.pose(clip, rig.length(clip));
                for (int i = 0; i < a.matrices().length; i++) {
                    float[] aa = a.matrices()[i].get(new float[16]),
                            bb = b.matrices()[i].get(new float[16]);
                    for (int j = 0; j < 16; j++)
                        h.assertTrue(Math.abs(aa[j] - bb[j]) < .001, "nonmatching guard at exit");
                }
                for (int f = 0; f <= 200; f++) {
                    var pose = rig.pose(clip, rig.length(clip) * f / 200f);
                    int wrist = rig.bone("腕_" + SideIntercept.hand(clip)),
                            sword = rig.bone("背挂武士刀_" + SideIntercept.hand(clip));
                    var baseline =
                            new org.joml.Matrix4f(a.matrices()[wrist])
                                    .invert()
                                    .mul(a.matrices()[sword]);
                    var grip =
                            new org.joml.Matrix4f(pose.matrices()[wrist])
                                    .invert()
                                    .mul(pose.matrices()[sword]);
                    float[] aa = baseline.get(new float[16]), bb = grip.get(new float[16]);
                    for (int j = 0; j < 16; j++)
                        h.assertTrue(
                                Float.isFinite(bb[j]) && Math.abs(aa[j] - bb[j]) < .001,
                                "interpolated hand/sword connection drift");
                    if (phase == 2)
                        h.assertFalse(
                                pose.visible()[rig.bone("背挂武士刀_R")], "dropped sword reappeared");
                }
                h.assertTrue(
                        SideIntercept.blades(phase, clip, SideIntercept.start(phase) + .06f, 0)
                                        .size()
                                == 5,
                        "inactive blade leaked into sweep");
            }
        h.assertFalse(
                ActionRules.retarget(SamuraiEntity.SIDE_SWEEP)
                        || ActionRules.canBeginAttack(SamuraiEntity.SIDE_SWEEP)
                        || ActionRules.reaction(SamuraiEntity.SIDE_SWEEP),
                "side cut can be interrupted");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 45, batch = "r40_hit1l")
    public static void hit_one_left(GameTestHelper h) {
        hit(h, 1, true, false);
    }

    @GameTest(template = "arena", timeoutTicks = 45, batch = "r40_hit1r")
    public static void hit_one_right(GameTestHelper h) {
        hit(h, 1, false, false);
    }

    @GameTest(template = "arena", timeoutTicks = 45, batch = "r40_hit2l")
    public static void hit_two_left(GameTestHelper h) {
        hit(h, 2, true, false);
    }

    @GameTest(template = "arena", timeoutTicks = 45, batch = "r40_hit2r")
    public static void hit_two_right(GameTestHelper h) {
        hit(h, 2, false, false);
    }

    @GameTest(template = "arena", timeoutTicks = 45, batch = "r40_wall")
    public static void wall(GameTestHelper h) {
        hit(h, 1, true, true);
    }

    private static void hit(GameTestHelper h, int phase, boolean left, boolean wall) {
        floor(h);
        var b = boss(h, 11.5, 9.5);
        var p = player(h, 11.5, 1, 13);
        b.testSideSweep(p, phase, left, -24);
        b.setNoAi(true);
        var blades = SideIntercept.blades(phase, b.clip(), SideIntercept.start(phase) + .065f, -24);
        Vec3 contact =
                SamuraiRig.world(
                        blades.get(2).base().lerp(blades.get(2).tip(), .5),
                        b.position(),
                        b.getYRot());
        p.setPos(contact.x, contact.y - .8, contact.z);
        p.setDeltaMovement(Vec3.ZERO);
        if (wall)
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    for (int y = -1; y <= 3; y++)
                        b.level()
                                .setBlockAndUpdate(
                                        BlockPos.containing(p.position()).offset(x, y, z),
                                        Blocks.STONE.defaultBlockState());
        // Directly set the locked pitch after the preparation ease has reached its endpoint.
        h.runAtTickTime(
                5,
                () -> {
                    float before = p.getHealth();
                    Set<UUID> hit = new HashSet<>();
                    Set<BlockPos> blocks = new HashSet<>();
                    float start = SideIntercept.start(phase), end = SideIntercept.end(phase);
                    BladeCombat.sweep(b, start, end, 0, b.position(), hit, blocks);
                    p.invulnerableTime = 0;
                    BladeCombat.sweep(b, start, end, 0, b.position(), hit, blocks);
                    if (wall)
                        h.assertTrue(before == p.getHealth(), "side sweep hit through solid cover");
                    else {
                        float expected = AttackDamage.blade(b, 0);
                        if (phase == 1) {
                            expected =
                                    switch (b.level().getDifficulty()) {
                                        case PEACEFUL -> 0;
                                        case EASY -> Math.min(expected / 2 + 1, expected);
                                        case HARD -> expected * 1.5f;
                                        default -> expected;
                                    };
                        }
                        h.assertTrue(
                                Math.abs(before - p.getHealth() - expected) < .01,
                                "sweep missed or damaged more than once " + b.report());
                        h.assertTrue(b.hitCounts()[0] == 1, "duplicate active-blade contact");
                    }
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "r40_orbit1")
    public static void orbit_one(GameTestHelper h) {
        motion(h, 1, false, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "r40_orbit2")
    public static void orbit_two(GameTestHelper h) {
        motion(h, 2, false, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "r40_reverse")
    public static void reverse(GameTestHelper h) {
        motion(h, 2, true, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "r40_height")
    public static void height(GameTestHelper h) {
        motion(h, 1, true, 1);
    }

    private static void motion(GameTestHelper h, int phase, boolean reverse, double height) {
        floor(h);
        var b = boss(h, 11.5, 11.5);
        var p = player(h, 11.5, 1 + height, 15.2);
        if (phase == 2) b.testCombat(p, 2, false);
        b.testPursuit(p);
        float[] previousHealth = {p.getHealth()}, sideDamage = {0};
        // Keep randomized combo choice reproducible while testing the identical moving target path.
        b.getRandom().setSeed(0L);
        Vec3 center = b.position();
        int[] starts = {0};
        long[] lastStart = {-999}, first = {-1};
        boolean[] in = {false}, cut = {false};
        float[] yaw = {0};
        h.onEachTick(
                () -> {
                    // A newly generated arena may start entity ticks after helper ticks.
                    // Drive the target by actual entity ticks so chunk readiness cannot change the
                    // opening angle.
                    double ticks = b.tickCount,
                            cycle = reverse ? ticks % 48 : ticks,
                            angle = (reverse && cycle > 24 ? 48 - cycle : cycle) * .15 + .35;
                    p.setPos(center.add(Math.sin(angle) * 3.7, height, Math.cos(angle) * 3.7));
                    p.setDeltaMovement(Vec3.ZERO);
                    boolean side = b.state() == SamuraiEntity.SIDE_SWEEP;
                    if (side) sideDamage[0] += Math.max(0, previousHealth[0] - p.getHealth());
                    previousHealth[0] = p.getHealth();
                    if (side && !in[0]) {
                        h.assertTrue(
                                h.getTick() - lastStart[0] >= BossConfig.SIDE_COOLDOWN.get(),
                                "side spam bypassed cooldown");
                        lastStart[0] = h.getTick();
                        starts[0]++;
                        if (first[0] < 0) first[0] = h.getTick();
                    }
                    in[0] = side;
                    boolean active =
                            side
                                    && b.age(0) >= SideIntercept.start(phase)
                                    && b.age(0) <= SideIntercept.end(phase);
                    if (active && cut[0])
                        h.assertTrue(
                                Math.abs(net.minecraft.util.Mth.wrapDegrees(b.getYRot() - yaw[0]))
                                        < .001,
                                "active side blade homed");
                    if (active) yaw[0] = b.getYRot();
                    cut[0] = active;
                });
        h.runAtTickTime(
                200,
                () -> {
                    GeneratedMod.LOG.info(
                            "[r40 side motion] phase={} reverse={} height={} starts={} first={} damage={} entityTicks={} report={}",
                            phase,
                            reverse,
                            height,
                            starts[0],
                            first[0],
                            sideDamage[0],
                            b.tickCount,
                            b.report());
                    h.assertTrue(
                            starts[0] >= 1 && first[0] <= 70,
                            "no timely side intercept " + b.report());
                    h.assertTrue(
                            sideDamage[0] > 0,
                            "side sweep itself never damaged moving target (combo damage excluded)");
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 210, batch = "r40_crossing")
    public static void crossing(GameTestHelper h) {
        floor(h);
        var b = boss(h, 11.5, 9.5);
        var p = player(h, 7.5, 1, 12.8);
        b.testPursuit(p);
        Vec3 origin = b.position();
        int[] starts = {0};
        boolean[] active = {false};
        float[] previousHealth = {p.getHealth()}, sideDamage = {0};
        h.onEachTick(
                () -> {
                    double q = h.getTick() % 28;
                    double x = -4.2 + .6 * (q < 14 ? q : 28 - q);
                    p.setPos(origin.add(x, 0, 3.3));
                    p.setDeltaMovement(Vec3.ZERO);
                    boolean side = b.state() == SamuraiEntity.SIDE_SWEEP;
                    if (side) sideDamage[0] += Math.max(0, previousHealth[0] - p.getHealth());
                    previousHealth[0] = p.getHealth();
                    if (side && !active[0]) starts[0]++;
                    active[0] = side;
                });
        h.runAtTickTime(
                180,
                () -> {
                    h.assertTrue(
                            starts[0] >= 1 && sideDamage[0] > 0,
                            "crossing/returning target escaped all intercepts " + b.report());
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 45, batch = "r40_save")
    public static void save(GameTestHelper h) {
        floor(h);
        var b = boss(h, 8, 8);
        var p = player(h, 8, 1, 11.5);
        b.testSideSweep(p, 2, true, -24);
        b.setNoAi(true);
        var tag = new net.minecraft.nbt.CompoundTag();
        b.addAdditionalSaveData(tag);
        h.assertTrue(tag.getInt("SamuraiSideCooldown") > 0, "cooldown not saved");
        var restored = boss(h, 16, 16);
        restored.setNoAi(true);
        restored.readAdditionalSaveData(tag);
        h.assertTrue(
                restored.phase() == 2
                        && restored.weaponsDrawn()
                        && restored.state() == SamuraiEntity.RECOVER,
                "reload replayed side hit or lost weapon");
        var saved = new net.minecraft.nbt.CompoundTag();
        restored.addAdditionalSaveData(saved);
        h.assertTrue(
                saved.getInt("SamuraiSideCooldown") == tag.getInt("SamuraiSideCooldown"),
                "cooldown lost on restore");
        p.discard();
        b.discard();
        restored.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 65, batch = "r40_invalid")
    public static void invalid(GameTestHelper h) {
        floor(h);
        var b = boss(h, 8, 8);
        var p = player(h, 8, 1, 11.5);
        b.testSideSweep(p, 1, true, -24);
        p.setGameMode(GameType.CREATIVE);
        h.runAtTickTime(
                22,
                () -> {
                    h.assertTrue(p.getHealth() == 1000, "creative damaged");
                    h.assertTrue(
                            b.state() != SamuraiEntity.SIDE_SWEEP, "invalid target left cut stuck");
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }
}
