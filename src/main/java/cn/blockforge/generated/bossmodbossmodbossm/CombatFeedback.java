package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;

/** 所有接触反馈由服务端发出；只在几何接触时发声，不用固定脚步计时器。 */
public final class CombatFeedback {
    static boolean crossed(float before, float now, float cue) {
        return before < cue && now >= cue;
    }

    static void heavyWarning(SamuraiEntity boss) {
        if (!BossConfig.FEEDBACK.get() || !boss.redReady()) return;
        boss.playSound(SoundEvents.CHAIN_PLACE, .8f, .65f);
        if (boss.level() instanceof ServerLevel level)
            for (var blade : SamuraiRig.get(boss.phase()).blades(boss.currentPose(0))) {
                if (!boss.consumeParticle()) break;
                Vec3 at =
                        SamuraiRig.world(
                                blade.base().lerp(blade.tip(), .35),
                                boss.position(),
                                boss.getYRot());
                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 2, .03, .03, .03, .01);
            }
    }

    private static BlockHitResult floor(SamuraiEntity boss, Vec3 point, double up, double down) {
        return boss.level()
                .clip(
                        new ClipContext(
                                point.add(0, up, 0),
                                point.add(0, -down, 0),
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                boss));
    }

    public static void footsteps(SamuraiEntity boss, FootPlantTracker tracker) {
        if (!BossConfig.FEEDBACK.get()
                || !boss.onGround()
                        && !boss.terrainNavigation()
                                .support(SamuraiNavigation.point(boss.position()))) return;
        SamuraiRig rig = SamuraiRig.get(boss.phase());
        SamuraiRig.Pose pose = boss.currentPose(0);
        for (int side = 0; side < 2; side++) {
            String name = side == 0 ? "foot_L" : "foot_R";
            Vec3 foot = lowest(rig, pose, name);
            if (foot == null) continue;
            Vec3 world = SamuraiRig.world(foot, boss.position(), boss.getYRot());
            BlockHitResult ground = floor(boss, world, .08, .42);
            boolean collider = ground.getType() == HitResult.Type.BLOCK;
            int ankle = rig.bone(side == 0 ? "行走_踝_L" : "行走_踝_R");
            float z =
                    pose.matrices()[ankle].transformPosition(
                                            new Vector3f(rig.bones.get(ankle).origin))
                                    .z
                            * SamuraiRig.SCALE;
            if (tracker.sample(
                    side,
                    boss.state() == SamuraiEntity.FOOTWORK ? 1 : boss.phase(),
                    z,
                    (float) (world.y - ground.getLocation().y),
                    collider)) impact(boss, ground, .55f, 1.0f, 3, SoundEvents.IRON_GOLEM_STEP);
        }
    }

    public static void bladeContact(SamuraiEntity boss, Vec3 from, Vec3 to, int strike) {
        if (!BossConfig.FEEDBACK.get() || boss.groundStrike() || !boss.active() || strike != 2)
            return;
        // 来自有效挥砍期间的刀刃或扫掠线，射线必须真实撞到方块顶面。
        BlockHitResult hit =
                boss.level()
                        .clip(
                                new ClipContext(
                                        from,
                                        to,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        boss));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) return;
        boss.markGroundStrike();
        impact(boss, hit, .95f, boss.phase() == 2 ? .94f : 1f, 7, GeneratedMod.HEAVY_SLAM.get());
    }

    public static boolean deathContact(SamuraiEntity boss, boolean finalFall) {
        if (!BossConfig.FEEDBACK.get()) return true;
        SamuraiRig rig = SamuraiRig.get(boss.phase());
        SamuraiRig.Pose pose = boss.currentPose(0);
        Vec3 point = lowest(rig, pose, finalFall ? "shoulder_" : "knee_");
        if (point == null) return false;
        Vec3 world = SamuraiRig.world(point, boss.position(), boss.getYRot());
        BlockHitResult hit = floor(boss, world, .12, .22);
        if (hit.getType() != HitResult.Type.BLOCK) return false;
        impact(
                boss,
                hit,
                finalFall ? 1.05f : .65f,
                finalFall ? .65f : .85f,
                finalFall ? 8 : 4,
                finalFall ? SoundEvents.ANVIL_LAND : SoundEvents.IRON_GOLEM_STEP);
        return true;
    }

    public static void armor(SamuraiEntity boss, DamageSource source, boolean silentHit) {
        if (!BossConfig.FEEDBACK.get()) return;
        // Accepted damage already uses the entity hurt sound. Zero-damage armor contact needs its
        // own.
        if (silentHit)
            boss.playSound(
                    boss.getHurtSound(source), .85f, .97f + boss.getRandom().nextFloat() * .06f);
        Vec3 origin = source.getSourcePosition();
        Vec3 p = boss.position().add(0, 2.6, 0);
        if (origin != null) p = p.add(origin.subtract(p).normalize().scale(.85));
        if (boss.level() instanceof ServerLevel level)
            for (int i = 0; i < 3 && boss.consumeParticle(); i++)
                level.sendParticles(
                        ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, .16, .15, .16, .015);
    }

    private static void impact(
            SamuraiEntity boss,
            BlockHitResult hit,
            float volume,
            float pitch,
            int count,
            SoundEvent sound) {
        if (!(boss.level() instanceof ServerLevel level)) return;
        Vec3 p = hit.getLocation();
        level.playSound(null, p.x, p.y, p.z, sound, SoundSource.HOSTILE, volume, pitch);
        var state = level.getBlockState(hit.getBlockPos());
        if (state.isAir()) return;
        BlockParticleOption dust =
                new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(hit.getBlockPos());
        for (int i = 0; i < count && boss.consumeParticle(); i++)
            level.sendParticles(dust, p.x, p.y + .035, p.z, 1, .18, .025, .18, .025);
    }

    private static Vec3 lowest(SamuraiRig rig, SamuraiRig.Pose pose, String prefix) {
        Vector3f found = null;
        for (var c : rig.cubes) {
            if (!c.name.startsWith(prefix) || c.debris || c.bone >= 0 && !pose.visible()[c.bone])
                continue;
            var m = rig.cubeMatrix(c, pose);
            for (int n = 0; n < 8; n++) {
                Vector3f v =
                        m.transformPosition(
                                new Vector3f(
                                        (n & 1) == 0 ? c.from.x : c.to.x,
                                        (n & 2) == 0 ? c.from.y : c.to.y,
                                        (n & 4) == 0 ? c.from.z : c.to.z));
                if (found == null || v.y < found.y) found = v;
            }
        }
        return found == null
                ? null
                : new Vec3(
                        found.x * SamuraiRig.SCALE,
                        found.y * SamuraiRig.SCALE,
                        found.z * SamuraiRig.SCALE);
    }

    private CombatFeedback() {}
}
