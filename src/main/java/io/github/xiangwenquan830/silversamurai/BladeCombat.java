package io.github.xiangwenquan830.silversamurai;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;
import java.util.*;

public final class BladeCombat {
    private static final Map<ServerLevel, long[]> WORLD_BREAK_BUDGET = new WeakHashMap<>();
    private static final net.minecraft.core.particles.DustColorTransitionOptions RED =
            new net.minecraft.core.particles.DustColorTransitionOptions(
                    new Vector3f(1, .28f, .055f), new Vector3f(.55f, .025f, .012f), .55f);
    private static final DustParticleOptions DEBUG =
            new DustParticleOptions(new Vector3f(.1f, .85f, 1), .6f);
    private static final GameProfile PROFILE =
            new GameProfile(
                    UUID.fromString("5a1f4e4c-a64f-4d88-96c1-e5ea754dbd9d"), "[SilverSamurai]");

    public static void sweep(
            SamuraiEntity boss,
            float start,
            float end,
            int strike,
            Vec3 previous,
            Set<UUID> hit,
            Set<BlockPos> handled) {
        if (!(boss.level() instanceof ServerLevel level) || !boss.active()) return;
        SamuraiRig rig = SamuraiRig.get(boss.phase());
        int maxTargets = BossConfig.TARGETS.get();
        double range = boss.range(strike);
        List<LivingEntity> targets =
                boss.state() == SamuraiEntity.CLEAR
                        ? new ArrayList<>()
                        : level.getEntitiesOfClass(
                                LivingEntity.class,
                                boss.getBoundingBox().inflate(range),
                                e ->
                                        CombatRules.enemy(boss, e, false)
                                                && Targeting.visible(boss, e));
        targets.sort(Comparator.comparingDouble(boss::distanceToSqr));
        int[] budget = {0, 0};
        List<SamuraiRig.Blade> last = null;
        // 每刻八个姿态样本，再连接相邻样本；快速刀尖不会跳过目标。
        for (int sub = 0; sub <= 8; sub++) {
            float f = sub / 8f, t = start + (end - start) * f;
            Vec3 origin = previous.lerp(boss.position(), f);
            List<SamuraiRig.Blade> blades =
                    boss.state() == SamuraiEntity.CLEAR
                            ? rig.blades(boss.clearingPose(t))
                            : rig.blades(boss.clip(), t, boss.aimPitch(0));
            List<SamuraiRig.Blade> world = new ArrayList<>();
            for (int i = 0; i < blades.size(); i++) {
                var blade = blades.get(i);
                Vec3 a = SamuraiRig.world(blade.base(), origin, boss.getYRot()),
                        b = SamuraiRig.world(blade.tip(), origin, boss.getYRot());
                world.add(new SamuraiRig.Blade(a, b));
                settle(boss, a, b, strike, range, targets, hit, handled, budget, maxTargets);
                if (last != null && i < last.size()) {
                    var old = last.get(i);
                    settle(
                            boss,
                            old.base(),
                            a,
                            strike,
                            range,
                            targets,
                            hit,
                            handled,
                            budget,
                            maxTargets);
                    settle(
                            boss,
                            old.tip(),
                            b,
                            strike,
                            range,
                            targets,
                            hit,
                            handled,
                            budget,
                            maxTargets);
                    settle(
                            boss,
                            old.base().lerp(old.tip(), .5),
                            a.lerp(b, .5),
                            strike,
                            range,
                            targets,
                            hit,
                            handled,
                            budget,
                            maxTargets);
                }
                if (sub == 4 && (boss.redReady() || boss.debug()))
                    particle(boss, a.lerp(b, .65), boss.debug() ? DEBUG : RED);
            }
            last = world;
        }
    }

    static Optional<Vec3> contactInRange(AABB box, Vec3 a, Vec3 b, Vec3 origin, double range) {
        Optional<Vec3> contact = box.contains(a) ? Optional.of(a) : box.clip(a, b);
        return contact.filter(point -> point.distanceToSqr(origin) <= range * range);
    }

    private static void settle(
            SamuraiEntity boss,
            Vec3 a,
            Vec3 b,
            int strike,
            double range,
            List<LivingEntity> targets,
            Set<UUID> hit,
            Set<BlockPos> handled,
            int[] budget,
            int maxTargets) {
        if (!boss.active()
                || !ActionRules.attack(boss.state()) && boss.state() != SamuraiEntity.CLEAR
                || !Double.isFinite(a.lengthSqr())
                || !Double.isFinite(b.lengthSqr())) return;
        CombatFeedback.bladeContact(boss, a, b, strike);
        for (LivingEntity target : targets) {
            if (!boss.active() || hit.size() >= maxTargets) break;
            if (hit.contains(target.getUUID()) || !target.isAlive()) continue;
            AABB box = target.getBoundingBox().inflate(.12);
            Optional<Vec3> contact = contactInRange(box, a, b, boss.position(), range);
            if (contact.isEmpty()) continue;
            // 刀刃到接触点的墙体遮挡；没有有效刀路不会结算。
            BlockHitResult obstruction =
                    boss.level()
                            .clip(
                                    new ClipContext(
                                            a,
                                            contact.get(),
                                            ClipContext.Block.COLLIDER,
                                            ClipContext.Fluid.NONE,
                                            boss));
            if (obstruction.getType() != HitResult.Type.MISS
                    && obstruction.getLocation().distanceToSqr(contact.get()) > .1) continue;
            hit.add(target.getUUID());
            damage(boss, target, strike);
        }
        if (!boss.active()
                || !boss.redReady()
                || !BossConfig.TERRAIN.get()
                || BossConfig.DENY_UNCERTAIN.get()
                || !boss.canBreakMore()) return;
        int tickLimit =
                Math.min(
                        BossConfig.BREAK_TICK.get(),
                        boss.state() == SamuraiEntity.CLEAR
                                ? BossConfig.CLEAR_TICK.get()
                                : Integer.MAX_VALUE);
        if (budget[0] >= tickLimit || budget[1] >= BossConfig.SCAN_TICK.get()) return;
        if (BossConfig.GRIEFING.get() && !ForgeEventFactory.getMobGriefingEvent(boss.level(), boss))
            return;
        int count = Math.max(1, (int) Math.ceil(a.distanceTo(b) / .28));
        for (int n = 0;
                n <= count
                        && budget[0] < tickLimit
                        && budget[1] < BossConfig.SCAN_TICK.get()
                        && boss.canBreakMore();
                n++) {
            Vec3 point = a.lerp(b, n / (double) count);
            if (point.distanceToSqr(boss.position())
                    > BossConfig.BREAK_RANGE.get() * BossConfig.BREAK_RANGE.get()) continue;
            BlockPos pos = BlockPos.containing(point);
            // Terrain cutting must not remove the supporting floor beneath the current movement
            // plane.
            if (pos.getY() < boss.getY() - .01) continue;
            if (boss.state() == SamuraiEntity.CLEAR && !boss.terrainNavigation().clearAllowed(pos))
                continue;
            if (!handled.add(pos)) continue;
            budget[1]++;
            if (breakBlock(boss, pos)) {
                budget[0]++;
                boss.brokeBlock();
            } else if (boss.state() == SamuraiEntity.CLEAR
                    && !boss.level().getBlockState(pos).isAir()
                    && !breakable(boss, pos, true)) boss.terrainNavigation().denied(pos);
        }
    }

    private static final ThreadLocal<DamageAttempt> ATTEMPT = new ThreadLocal<>();

    private static final class DamageAttempt {
        final SamuraiEntity boss;
        final LivingEntity target;
        String cancellation;

        DamageAttempt(SamuraiEntity b, LivingEntity t) {
            boss = b;
            target = t;
        }
    }

    static void canceled(
            net.minecraftforge.event.entity.living.LivingEvent event,
            net.minecraft.world.damagesource.DamageSource source,
            String stage) {
        DamageAttempt a = ATTEMPT.get();
        if (a != null && a.target == event.getEntity() && source.getEntity() == a.boss)
            a.cancellation = stage;
    }

    static void damage(SamuraiEntity boss, LivingEntity target, int strike) {
        if (!boss.isAlive() || boss.state() == SamuraiEntity.DEAD) return;
        if (target instanceof Player p && !CombatRules.validPlayer(p)) {
            boss.recordHit(
                    strike,
                    target,
                    target.getHealth(),
                    target.getHealth(),
                    false,
                    false,
                    p.isCreative() ? "创造免疫：跳过伤害" : "旁观或死亡：跳过伤害");
            return;
        }
        if (!CombatRules.enemy(boss, target, false)) return;
        float damage = AttackDamage.blade(boss, strike);
        boolean raised = target.isBlocking();
        boolean red = boss.redReady(),
                shield = red && RedBladeBreak.frontGuard(boss, target) && raised;
        var shieldStack = shield ? target.getUseItem() : net.minecraft.world.item.ItemStack.EMPTY;
        var shieldHand =
                shield ? target.getUsedItemHand() : net.minecraft.world.InteractionHand.OFF_HAND;
        boolean clawGuard =
                red
                        && RedBladeBreak.frontGuard(boss, target)
                        && target instanceof ServerPlayer player
                        && WolverineCompat.guarding(player);
        float absorption = target.getAbsorptionAmount();
        boolean bypass = red && BossConfig.BYPASS_ABSORPTION.get();
        boolean damaged;
        float healthBefore = target.getHealth();
        boolean invulnerable = target.invulnerableTime > 10;
        if (bypass) target.setAbsorptionAmount(0);
        DamageAttempt attempt = new DamageAttempt(boss, target), parent = ATTEMPT.get();
        ATTEMPT.set(attempt);
        try {
            damaged =
                    target.hurt(
                            red ? CombatRules.red(boss) : boss.damageSources().mobAttack(boss),
                            damage);
        } finally {
            if (parent == null) ATTEMPT.remove();
            else ATTEMPT.set(parent);
            if (bypass) target.setAbsorptionAmount(absorption + target.getAbsorptionAmount());
        }
        // Commit equipment consequences only after the damage pipeline accepts this contact.
        boolean effective =
                damaged
                        && attempt.cancellation == null
                        && AttackDamage.effectiveContact(target, healthBefore, absorption);
        if (red && effective) {
            RedBladeWound.apply(target);
            if (target instanceof ServerPlayer player) PainNetwork.redHit(player);
            if (shield) RedBladeBreak.destroyShield(target, shieldHand, shieldStack);
            if (clawGuard && target instanceof ServerPlayer player)
                WolverineCompat.severClaws(player);
        }
        boolean protectedHit = !damaged && invulnerable && attempt.cancellation == null;
        String reason =
                attempt.cancellation != null
                        ? "事件取消：" + attempt.cancellation
                        : damaged
                                ? (healthBefore > target.getHealth() ? "实际扣血" : "已接受但生命未减少（吸收或零伤害）")
                                : protectedHit ? "受伤保护窗口拒绝（非事件取消）" : "其他免疫或伤害拒绝";
        boss.recordHit(
                strike, target, healthBefore, target.getHealth(), damaged, protectedHit, reason);
        if (red ? effective : damaged || raised) {
            boss.level()
                    .playSound(
                            null,
                            target.blockPosition(),
                            red ? GeneratedMod.CUT.get() : SoundEvents.IRON_GOLEM_ATTACK,
                            SoundSource.HOSTILE,
                            red ? 1.1f : .8f,
                            .85f + boss.getRandom().nextFloat() * .2f);
            if (red) {
                Vec3 at = target.position().add(0, target.getBbHeight() * .5, 0);
                particle(boss, at, RED);
                for (int i = 0; i < 4; i++)
                    particle(
                            boss,
                            at.add(
                                    (boss.getRandom().nextDouble() - .5) * .3,
                                    (boss.getRandom().nextDouble() - .5) * .3,
                                    0),
                            net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK);
            }
        }
    }

    public static boolean breakable(SamuraiEntity boss, BlockPos pos, boolean events) {
        if (!(boss.level() instanceof ServerLevel level)
                || !level.hasChunkAt(pos)
                || !level.getWorldBorder().isWithinBounds(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()
                || state.getDestroySpeed(level, pos) < 0
                || !state.getFluidState().isEmpty()
                || BossConfig.PROTECTED
                        .get()
                        .contains(String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock()))))
            return false;
        if (state.hasBlockEntity() && !BossConfig.BLOCK_ENTITIES.get()) return false;
        if (level.getBlockEntity(pos) instanceof Container && !BossConfig.CONTAINERS.get())
            return false;
        if (!state.canEntityDestroy(level, pos, boss)) return false;
        if (events && BossConfig.CLAIM_EVENTS.get()) {
            if (!ForgeEventFactory.onEntityDestroyBlock(boss, pos, state)) return false;
            var player = FakePlayerFactory.get(level, PROFILE);
            player.setPos(boss.getX(), boss.getY(), boss.getZ());
            if (MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)))
                return false;
        }
        return true;
    }

    static int wallContact(SamuraiEntity boss) {
        if (!boss.terrainNavigation().canClear()) return 0;
        Set<BlockPos> touched = new HashSet<>();
        int count = 0, limit = Math.min(3, BossConfig.BREAK_TICK.get());
        for (var blade : SamuraiRig.get(boss.phase()).blades(boss.currentPose(0))) {
            Vec3 a = SamuraiRig.world(blade.base(), boss.position(), boss.getYRot()),
                    z = SamuraiRig.world(blade.tip(), boss.position(), boss.getYRot());
            var hit = WallGeometry.ray(boss, a, z);
            if (hit == null
                    || !touched.add(hit.getBlockPos())
                    || hit.getLocation().distanceToSqr(boss.position()) > 49) continue;
            if (count >= limit) break;
            if (breakBlock(boss, hit.getBlockPos())) count++;
        }
        return count;
    }

    private static boolean breakBlock(SamuraiEntity boss, BlockPos pos) {
        if (!breakable(boss, pos, true)) return false;
        ServerLevel level = (ServerLevel) boss.level();
        long[] budget = WORLD_BREAK_BUDGET.computeIfAbsent(level, l -> new long[] {-1, 0});
        if (budget[0] != level.getGameTime()) {
            budget[0] = level.getGameTime();
            budget[1] = 0;
        }
        if (budget[1] >= BossConfig.BREAK_WORLD.get()) return false;
        boolean success = level.destroyBlock(pos, BossConfig.DROPS.get(), boss);
        if (success) {
            budget[1]++;
            boss.obstacleChanged(pos);
        }
        if (success && boss.consumeParticle()) {
            level.playSound(null, pos, GeneratedMod.CUT.get(), SoundSource.BLOCKS, .45f, 1.4f);
        }
        return success;
    }

    public static void charge(SamuraiEntity boss) {
        List<SamuraiRig.Blade> blades = SamuraiRig.get(2).blades(boss.clip(), boss.age(0));
        float progress = boss.redProgress(0);
        for (int i = 0; i < blades.size(); i++) {
            var b = blades.get(i);
            if (i > progress * blades.size()) break;
            particle(
                    boss,
                    SamuraiRig.world(b.base().lerp(b.tip(), .3), boss.position(), boss.getYRot()),
                    RED);
        }
    }

    private static void particle(
            SamuraiEntity boss, Vec3 p, net.minecraft.core.particles.ParticleOptions type) {
        if (boss.consumeParticle() && boss.level() instanceof ServerLevel server)
            server.sendParticles(type, p.x, p.y, p.z, 1, .015, .015, .015, 0);
    }

    private BladeCombat() {}
}
