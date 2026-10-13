package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.nbt.CompoundTag;
import org.joml.*;
import java.util.List;
import java.lang.Math;
import static cn.blockforge.generated.bossmodbossmodbossm.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuardGameTests {
    @GameTest(template = "arena", timeoutTicks = 30, batch = "guard_impulses")
    public static void impulses(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        b.knockback(20, 1, 1);
        b.push(3, 4, 2);
        b.setDeltaMovement(new Vec3(2, 5, -3));
        h.assertTrue(b.getDeltaMovement().lengthSqr() == 0, "direct impulse bypassed guard");
        b.setDeltaMovement(2, 6, 2);
        h.assertTrue(
                b.getDeltaMovement().lengthSqr() == 0, "scalar velocity setter bypassed guard");
        b.setDeltaMovement(new Vec3(2, -.5, 3));
        h.assertTrue(
                b.getDeltaMovement().y == -.5 && b.getDeltaMovement().horizontalDistanceSqr() == 0,
                "guard disabled gravity or allowed horizontal impulse");
        double old = BossConfig.KNOCKBACK.get();
        try {
            BossConfig.KNOCKBACK.set(.1);
            b.applyConfig(true);
            h.assertTrue(
                    b.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 1,
                    "old config lowered resistance");
        } finally {
            BossConfig.KNOCKBACK.set(old);
        }
        b.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "guard_effects")
    public static void effects(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        h.assertTrue(
                BossConfig.IMMUNE_EFFECTS.get().contains("hordes:infected")
                        && BossConfig.IMMUNE_DAMAGE.get().contains("hordes:infection"),
                "Hordes registry IDs missing");
        var oldEffects = BossConfig.IMMUNE_EFFECTS.get();
        var oldDamage = BossConfig.IMMUNE_DAMAGE.get();
        try {
            b.addEffect(new MobEffectInstance(MobEffects.WITHER, 1, 3));
            CompoundTag saved = new CompoundTag();
            b.addAdditionalSaveData(saved);
            BossConfig.IMMUNE_EFFECTS.set(List.of("hordes:infected", "minecraft:wither"));
            b.readAdditionalSaveData(saved);
            float health = b.getHealth();
            b.tick();
            h.assertFalse(
                    b.hasEffect(MobEffects.WITHER),
                    "persisted forbidden effect survived pre-effect cleanup");
            h.assertTrue(b.getHealth() == health, "forbidden effect ticked before cleanup");
            h.assertFalse(
                    b.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 3)),
                    "forbidden effect accepted; blocked="
                            + BossImmunity.blocked(MobEffects.WITHER)
                            + " config="
                            + BossConfig.IMMUNE_EFFECTS.get());
            h.assertTrue(
                    b.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0)),
                    "unrelated effect was stripped");
            BossConfig.IMMUNE_DAMAGE.set(
                    List.of("hordes:infection", "minecraft:magic", "minecraft:generic_kill"));
            h.assertFalse(
                    b.hurt(b.damageSources().magic(), Float.MAX_VALUE),
                    "lethal dedicated damage bypassed ID filter");
            h.assertTrue(b.getHealth() == health, "blocked dedicated damage changed health");
            h.assertFalse(
                    BossImmunity.blocked(b.damageSources().genericKill()),
                    "admin removal blocked by immunity");
        } finally {
            BossConfig.IMMUNE_EFFECTS.set(oldEffects);
            BossConfig.IMMUNE_DAMAGE.set(oldDamage);
            b.discard();
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "guard_kick_pose")
    public static void kick_pose(GameTestHelper h) {
        floor(h);
        for (int phase : new int[] {1, 2}) {
            var rig = SamuraiRig.get(phase);
            String idle = phase == 1 ? "armed_idle" : SamuraiRig.IDLE_TWO;
            var base = rig.pose(idle, 0);
            int ankle = rig.bone("行走_踝_R"), sword = rig.bone("背挂武士刀_L");
            var pivot = rig.bones.get(ankle).origin;
            Vector3f before = base.matrices()[ankle].transformPosition(new Vector3f(pivot));
            var peak = StompMotion.pose(rig, base, StompMotion.IMPACT, idle);
            for (float time : new float[] {0, .32f, StompMotion.IMPACT, StompMotion.DURATION})
                exportKick(rig, StompMotion.pose(rig, base, time, idle), phase, time);
            Vector3f foot = peak.matrices()[ankle].transformPosition(new Vector3f(pivot));
            h.assertTrue(
                    Math.abs(foot.x - before.x) * SamuraiRig.SCALE > .4
                            && Math.abs(foot.x - before.x) * SamuraiRig.SCALE < 1.1
                            && (foot.y - before.y) * SamuraiRig.SCALE > .1
                            && (foot.y - before.y) * SamuraiRig.SCALE < .65,
                    "low kick outside restrained range");
            for (var m : peak.matrices())
                h.assertTrue(m.transformPosition(new Vector3f()).isFinite(), "nonfinite kick pose");
            if (phase == 2)
                for (String side : new String[] {"L", "R"}) {
                    int wrist = rig.bone("腕_" + side);
                    float[] a = new float[16], b = new float[16];
                    new Matrix4f(base.matrices()[sword])
                            .invert()
                            .mul(base.matrices()[wrist])
                            .get(a);
                    new Matrix4f(peak.matrices()[sword])
                            .invert()
                            .mul(peak.matrices()[wrist])
                            .get(b);
                    for (int j = 0; j < 16; j++)
                        h.assertTrue(
                                Math.abs(a[j] - b[j]) < .01,
                                "two handed grip separated during kick");
                }
            var end = StompMotion.pose(rig, base, StompMotion.DURATION, idle);
            h.assertTrue(
                    end.matrices()[ankle].transformPosition(new Vector3f(pivot)).distance(before)
                                    * SamuraiRig.SCALE
                            < .02,
                    "kick foot did not return to stance");
        }
        h.succeed();
    }

    private static void exportKick(SamuraiRig r, SamuraiRig.Pose pose, int phase, float time) {
        var cubes = new com.google.gson.JsonArray();
        for (var c : r.cubes) {
            if (c.debris || (c.bone >= 0 && !pose.visible()[c.bone])) continue;
            var vertices = new com.google.gson.JsonArray();
            var matrix = r.cubeMatrix(c, pose);
            for (int i = 0; i < 8; i++) {
                var point =
                        matrix.transformPosition(
                                new Vector3f(
                                        (i & 1) == 0 ? c.from.x : c.to.x,
                                        (i & 2) == 0 ? c.from.y : c.to.y,
                                        (i & 4) == 0 ? c.from.z : c.to.z));
                var v = new com.google.gson.JsonArray();
                v.add(point.x);
                v.add(point.y);
                v.add(point.z);
                vertices.add(v);
            }
            cubes.add(vertices);
        }
        try {
            var dir = java.nio.file.Path.of("kick-preview");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(
                    dir.resolve("phase" + phase + "-" + time + ".json"), cubes.toString());
        } catch (java.io.IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
