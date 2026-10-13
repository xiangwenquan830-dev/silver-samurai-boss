package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import static cn.blockforge.generated.bossmodbossmodbossm.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class R29FeatureTests {
    @GameTest(template = "arena", timeoutTicks = 180, batch = "r29_red_grip")
    public static void red_waits_for_grip(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        b.requestSecondPhase();
        float[] previous = {0};
        h.onEachTick(
                () -> {
                    float progress = b.redProgress(0);
                    h.assertTrue(progress + 1e-5 >= previous[0], "red charge went backwards");
                    if (b.state() == SamuraiEntity.TRANSITION
                            && b.stateAge(0) * 20
                                    < RedBladeTiming.gripTicks() + RedBladeTiming.pauseTicks())
                        h.assertTrue(
                                progress == 0 && !b.redReady(),
                                "red blade before settled two-hand grip");
                    if (progress > 0 && progress < 1) {
                        var rig = SamuraiRig.get(2);
                        var pose = b.currentPose(0);
                        var settled =
                                rig.pose(SamuraiRig.TRANSITION, rig.length(SamuraiRig.TRANSITION));
                        for (String bone : new String[] {"腕_L", "腕_R", "背挂武士刀_L"}) {
                            int i = rig.bone(bone);
                            h.assertTrue(
                                    pose.matrices()[i].equals(settled.matrices()[i], .0001f),
                                    "hand or blade still moving while charging");
                        }
                        h.assertTrue(!b.redReady(), "red damage active before full charge");
                    }
                    previous[0] = progress;
                });
        h.runAtTickTime(
                RedBladeTiming.totalTicks() + 3,
                () -> {
                    h.assertTrue(
                            b.redReady() && b.state() != SamuraiEntity.TRANSITION,
                            "charge never finished");
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r29_red_reload")
    public static void red_reload_gate(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        b.requestSecondPhase();
        h.runAtTickTime(
                15,
                () -> {
                    CompoundTag tag = new CompoundTag();
                    b.saveWithoutId(tag);
                    var copy = boss(h, 4.5, 10.5);
                    copy.load(tag);
                    copy.setNoAi(true);
                    h.assertTrue(
                            copy.redProgress(0) == 0 && !copy.redReady(),
                            "reload prematurely enabled red blade");
                    var p = player(h, 14.5, 1, 10.5);
                    copy.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
                    float before = p.getHealth();
                    BladeCombat.damage(copy, p, 0);
                    h.assertTrue(before - p.getHealth() <= 10.01, "unready red damage doubled");
                    h.assertFalse(
                            copy.terrainNavigation().canClear(),
                            "unready red blade clears terrain");
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r29_defense")
    public static void damage_classes(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        var p = player(h, 16.5, 1, 10.5);
        var arrow = new Arrow(h.getLevel(), p);
        var ranged = b.damageSources().arrow(arrow, p);
        h.assertTrue(
                Math.abs(DefensePolicy.reduction(b, ranged) - .99) < 1e-6,
                "ranged stacks multiplicatively with 99% armor");
        float before = b.getHealth();
        b.hurt(ranged, 20);
        h.assertTrue(
                Math.abs(before - b.getHealth() - .2f) < .01,
                "phase-one projectile actual damage differs");
        b.testCombat(p, 2, false);
        b.setNoAi(true);
        b.getAttribute(Attributes.ARMOR).setBaseValue(0);
        b.invulnerableTime = 0;
        before = b.getHealth();
        b.hurt(ranged, 20);
        h.assertTrue(
                Math.abs(before - b.getHealth() - 2) < .01,
                "phase-two ranged damage is not reduced by 90%");
        b.invulnerableTime = 0;
        before = b.getHealth();
        b.hurt(b.damageSources().wither(), 20);
        h.assertTrue(Math.abs(before - b.getHealth() - 5) < .01, "wither not reduced by 75%");
        b.invulnerableTime = 0;
        before = b.getHealth();
        b.hurt(b.damageSources().magic(), 20);
        h.assertTrue(
                Math.abs(before - b.getHealth() - 20) < .01,
                "ordinary magic misclassified as periodic");
        b.invulnerableTime = 0;
        before = b.getHealth();
        b.hurt(ranged, 10000);
        h.assertTrue(
                Math.abs(before - b.getHealth() - 3) < .01,
                "single hit cap must clamp the heavy projectile to 3");
        b.invulnerableTime = 0;
        b.hurt(b.damageSources().genericKill(), 100000);
        h.assertTrue(!b.isAlive(), "admin kill blocked");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r29_poison")
    public static void poison_is_effect_damage(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        float before = b.getHealth();
        b.addEffect(new MobEffectInstance(MobEffects.POISON, 25, 0));
        h.runAtTickTime(
                8,
                () -> {
                    h.assertTrue(
                            Math.abs(before - b.getHealth() - .25) < .01,
                            "actual poison tick not reduced to 0.25");
                    h.assertFalse(b.processingEffects(), "effect damage context leaked");
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r29_moving_repath")
    public static void moving_repath_cadence(GameTestHelper h) {
        floor(h);
        var b = boss(h, 2.5, 10.5);
        var p = player(h, 13.5, 1, 10.5);
        b.facePoint(p.position(), 360);
        b.testPursuit(p);
        Vec3[] last = {b.position()};
        int[] pauses = {0}, samples = {0};
        h.onEachTick(
                () -> {
                    if (h.getTick() < 44) {
                        p.setPos(h.absoluteVec(new Vec3(13.5 + h.getTick() * .19, 1, 10.5)));
                        p.setDeltaMovement(Vec3.ZERO);
                    }
                    if (h.getTick() > 12 && h.getTick() < 44) {
                        double distance = b.position().distanceTo(last[0]);
                        samples[0]++;
                        if (distance < .25) pauses[0]++;
                        h.assertTrue(distance < .381, "repath teleported or doubled movement");
                        h.assertTrue(
                                b.position().x >= last[0].x - .001,
                                "repath walked back to obsolete origin");
                    }
                    last[0] = b.position();
                });
        h.runAtTickTime(
                45,
                () -> {
                    GeneratedMod.LOG.info(
                            "[r29 moving replan] joins={} samples={} slow={}",
                            b.terrainNavigation().movingReplans(),
                            samples[0],
                            pauses[0]);
                    h.assertTrue(
                            b.terrainNavigation().movingReplans() >= 2,
                            "did not test moving replans");
                    h.assertTrue(
                            pauses[0] == 0,
                            "moving target caused repeated stop/reaccelerate: " + pauses[0]);
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "r29_kick_wall")
    public static void kick_respects_new_wall(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.15);
        var p = player(h, 10.5, 1, 12.33);
        b.testPursuit(p);
        h.runAtTickTime(
                6,
                () -> {
                    h.assertTrue(
                            b.state() == SamuraiEntity.STOMP && b.focusedKick(),
                            "single-target defense did not wind up");
                    for (int x = 9; x <= 12; x++)
                        for (int y = 1; y <= 5; y++) h.setBlock(x, y, 11, Blocks.STONE);
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox())
                                    && h.getLevel().noCollision(p, p.getBoundingBox()),
                            "new wall overlaps actor fixture");
                });
        h.runAtTickTime(
                23,
                () -> {
                    h.assertTrue(p.getHealth() == 1000, "kick passed newly placed wall");
                    h.assertBlockPresent(Blocks.STONE, new BlockPos(10, 1, 11));
                    h.succeed();
                });
    }
}
