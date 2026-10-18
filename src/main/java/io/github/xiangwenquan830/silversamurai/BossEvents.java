package io.github.xiangwenquan830.silversamurai;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class BossEvents {
    @SubscribeEvent(
            priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST,
            receiveCanceled = true)
    public static void attackAudit(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.isCanceled()) BladeCombat.canceled(event, event.getSource(), "LivingAttackEvent");
    }

    @SubscribeEvent(
            priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST,
            receiveCanceled = true)
    public static void hurtAudit(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.isCanceled()) BladeCombat.canceled(event, event.getSource(), "LivingHurtEvent");
    }

    @SubscribeEvent(
            priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST,
            receiveCanceled = true)
    public static void damageAudit(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (event.isCanceled()) BladeCombat.canceled(event, event.getSource(), "LivingDamageEvent");
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void finalDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (event.getEntity() instanceof SamuraiEntity boss
                && !CombatRules.absolute(event.getSource())) {
            float value = CombatTiming.cap(event.getAmount(), BossConfig.INCOMING_CAP.get());
            if (DefensePolicy.ranged(event.getSource()))
                value =
                        boss.rangedBudget()
                                .accept(
                                        boss.level().getGameTime(),
                                        CombatTiming.cap(value, BossConfig.RANGED_HIT.get()),
                                        BossConfig.RANGED_SECOND.get());
            event.setAmount(value);
        }
    }

    @SubscribeEvent
    public static void targetChange(
            net.minecraftforge.event.entity.living.LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof SamuraiEntity)
                && event.getNewTarget() instanceof SamuraiEntity boss
                && (!boss.activated() || CombatRules.friend(boss, event.getEntity())))
            event.setNewTarget(null);
    }

    @SubscribeEvent
    public static void sleepTarget(
            net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide
                && event.getEntity() instanceof Mob mob
                && !(mob instanceof SamuraiEntity)
                && mob.getTarget() instanceof SamuraiEntity boss
                && !boss.activated()) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    @SubscribeEvent
    public static void changed(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level)
            for (SamuraiEntity boss :
                    level.getEntitiesOfClass(
                            SamuraiEntity.class,
                            new net.minecraft.world.phys.AABB(event.getPos()).inflate(8)))
                boss.obstacleChanged(event.getPos());
    }

    @SubscribeEvent
    public static void placed(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level)
            notifyChanged(level, java.util.List.of(event.getPos()));
    }

    @SubscribeEvent
    public static void exploded(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level)
            notifyChanged(level, event.getAffectedBlocks());
    }

    private static void notifyChanged(
            ServerLevel level, java.util.List<net.minecraft.core.BlockPos> positions) {
        if (positions.isEmpty()) return;
        net.minecraft.world.phys.AABB bounds = new net.minecraft.world.phys.AABB(positions.get(0));
        for (var pos : positions) bounds = bounds.minmax(new net.minecraft.world.phys.AABB(pos));
        for (SamuraiEntity boss :
                level.getEntitiesOfClass(SamuraiEntity.class, bounds.inflate(8))) {
            for (var pos : positions)
                if (pos.distToCenterSqr(boss.position()) < 64) {
                    boss.obstacleChanged(pos);
                    break;
                }
        }
    }

    @SubscribeEvent
    public static void shieldUse(
            net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start event) {
        if (event.getEntity().level().isClientSide
                || event.getItem().getUseAnimation() != net.minecraft.world.item.UseAnim.BLOCK) return;
        long remaining = event.getEntity().getPersistentData().getLong("SilverSamuraiShieldUntil")
                - event.getEntity().level().getGameTime();
        if (remaining <= 0) return;
        event.setCanceled(true);
        // A shield acquired after the break must also receive a client-visible cooldown.
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player)
            player.getCooldowns().addCooldown(event.getItem().getItem(), (int) Math.min(remaining, Integer.MAX_VALUE));

    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof Mob mob)
                || mob instanceof SamuraiEntity) return;
        HostileBossTargetGoal.install(mob);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void clawsBefore(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.player instanceof net.minecraft.server.level.ServerPlayer player) {
            if (event.phase == net.minecraftforge.event.TickEvent.Phase.START) {
                ClawPain.tick(player);
                WolverineCompat.tickBroken(player);
            } else if (ClawSeverance.locked(player)
                    && (player.tickCount % 20 == 0
                            || ClawSeverance.growing(player) > 0 && player.tickCount % 2 == 0))
                PainNetwork.syncBroken(player);
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void clawsAfter(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END
                && event.player instanceof net.minecraft.server.level.ServerPlayer player)
            WolverineCompat.tickBroken(player);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void brokenClawAttack(
            net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                && (ClawSeverance.locked(player) && WolverineCompat.isClaw(event.getSource())
                        || ClawPain.active(player)
                                && event.getSource().getDirectEntity() == player))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void copyClawTimer(
            net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        ClawSeverance.copyRecovery(
                event.getOriginal().getPersistentData(), event.getEntity().getPersistentData());
    }

    @SubscribeEvent
    public static void brokenLogin(
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) {
            WolverineCompat.interruptGrowth(p);
            PainNetwork.syncBroken(p);
        }
    }

    @SubscribeEvent
    public static void brokenRespawn(
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) {
            WolverineCompat.interruptGrowth(p);
            PainNetwork.syncBroken(p);
        }
    }

    @SubscribeEvent
    public static void brokenDimension(
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)
            PainNetwork.syncBroken(p);
    }

    @SubscribeEvent
    public static void brokenTracking(
            net.minecraftforge.event.entity.player.PlayerEvent.StartTracking e) {
        if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer viewer
                && e.getTarget() instanceof net.minecraft.server.level.ServerPlayer subject)
            PainNetwork.syncBrokenTo(subject, viewer);
    }

    @SubscribeEvent
    public static void painUse(
            net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                && ClawPain.active(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void painAttack(net.minecraftforge.event.entity.player.AttackEntityEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                && ClawPain.active(player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void painJump(
            net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                && ClawPain.active(player))
            player.setDeltaMovement(player.getDeltaMovement().multiply(0, 0, 0));
    }

    @SubscribeEvent
    public static void painMine(
            net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                && ClawPain.active(player)) event.setNewSpeed(0);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher()
                .register(
                        Commands.literal("samurai")
                                .requires(s -> s.hasPermission(2))
                                .then(
                                        Commands.literal("summon")
                                                .executes(
                                                        c ->
                                                                summon(
                                                                        c.getSource().getLevel(),
                                                                        c.getSource()
                                                                                .getPosition()
                                                                                .add(0, 0, 5),
                                                                        c.getSource()))
                                                .then(
                                                        Commands.argument(
                                                                        "pos", Vec3Argument.vec3())
                                                                .executes(
                                                                        c ->
                                                                                summon(
                                                                                        c.getSource()
                                                                                                .getLevel(),
                                                                                        Vec3Argument
                                                                                                .getVec3(
                                                                                                        c,
                                                                                                        "pos"),
                                                                                        c
                                                                                                .getSource()))))
                                .then(
                                        Commands.literal("status")
                                                .then(
                                                        Commands.argument(
                                                                        "boss",
                                                                        EntityArgument.entity())
                                                                .executes(
                                                                        c -> {
                                                                            if (EntityArgument
                                                                                            .getEntity(
                                                                                                    c,
                                                                                                    "boss")
                                                                                    instanceof
                                                                                    SamuraiEntity
                                                                                                    boss) {
                                                                                c.getSource()
                                                                                        .sendSuccess(
                                                                                                () ->
                                                                                                        Component
                                                                                                                .literal(
                                                                                                                        boss
                                                                                                                                .report()),
                                                                                                false);
                                                                                return 1;
                                                                            }
                                                                            c.getSource()
                                                                                    .sendFailure(
                                                                                            Component
                                                                                                    .translatable("message.silver_samurai.select_boss"));
                                                                            return 0;
                                                                        })))
                                .then(
                                        Commands.literal("phase2")
                                                .then(
                                                        Commands.argument(
                                                                        "boss",
                                                                        EntityArgument.entity())
                                                                .executes(
                                                                        c -> {
                                                                            if (EntityArgument
                                                                                            .getEntity(
                                                                                                    c,
                                                                                                    "boss")
                                                                                    instanceof
                                                                                    SamuraiEntity
                                                                                                    boss) {
                                                                                boss
                                                                                        .requestSecondPhase();
                                                                                c.getSource()
                                                                                        .sendSuccess(
                                                                                                () ->
                                                                                                        Component
                                                                                                                .translatable("message.silver_samurai.phase_requested"),
                                                                                                true);
                                                                                return 1;
                                                                            }
                                                                            return 0;
                                                                        })))
                                .then(
                                        Commands.literal("debug")
                                                .then(
                                                        Commands.argument(
                                                                        "boss",
                                                                        EntityArgument.entity())
                                                                .then(
                                                                        Commands.argument(
                                                                                        "enabled",
                                                                                        BoolArgumentType
                                                                                                .bool())
                                                                                .executes(
                                                                                        c -> {
                                                                                            if (EntityArgument
                                                                                                            .getEntity(
                                                                                                                    c,
                                                                                                                    "boss")
                                                                                                    instanceof
                                                                                                    SamuraiEntity
                                                                                                                    boss) {
                                                                                                boss
                                                                                                        .debug(
                                                                                                                BoolArgumentType
                                                                                                                        .getBool(
                                                                                                                                c,
                                                                                                                                "enabled"));
                                                                                                c.getSource()
                                                                                                        .sendSuccess(
                                                                                                                () ->
                                                                                                                        Component
                                                                                                                                .literal(
                                                                                                                                        "刀路显示："
                                                                                                                                                + (boss
                                                                                                                                                                .debug()
                                                                                                                                                        ? "开启"
                                                                                                                                                        : "关闭")),
                                                                                                                false);
                                                                                                return 1;
                                                                                            }
                                                                                            return 0;
                                                                                        }))))
                                .then(
                                        Commands.literal("reload")
                                                .executes(
                                                        c -> {
                                                            int count = 0;
                                                            // Forge自动重读TOML，此命令明确刷新现存实体属性，保留当前生命绝对值，不治疗。
                                                            for (ServerLevel level :
                                                                    c.getSource()
                                                                            .getServer()
                                                                            .getAllLevels())
                                                                for (Entity e :
                                                                        level.getAllEntities())
                                                                    if (e
                                                                            instanceof
                                                                            SamuraiEntity boss) {
                                                                        boss.applyConfig(true);
                                                                        count++;
                                                                    }
                                                            final int total = count;
                                                            c.getSource()
                                                                    .sendSuccess(
                                                                            () ->
                                                                                    Component
                                                                                            .translatable("message.silver_samurai.config_applied", total),
                                                                            true);
                                                            return count;
                                                        }))
                                .then(
                                        Commands.literal("compat")
                                                .executes(
                                                        c -> {
                                                            c.getSource()
                                                                    .sendSuccess(
                                                                            () ->
                                                                                    WolverineCompat.statusComponent(),
                                                                            false);
                                                            return 1;
                                                        })));
    }

    private static int summon(
            ServerLevel level, Vec3 pos, net.minecraft.commands.CommandSourceStack source) {
        SamuraiEntity boss = GeneratedMod.SAMURAI.get().create(level);
        if (boss == null) return 0;
        boss.moveTo(pos.x, pos.y, pos.z, source.getRotation().y, 0);
        boss.applyConfig(false);
        if (!level.noCollision(boss)) {
            source.sendFailure(Component.translatable("message.silver_samurai.summon_space"));
            return 0;
        }
        level.addFreshEntity(boss);
        source.sendSuccess(() -> Component.translatable("message.silver_samurai.summoned"), true);
        return 1;
    }

    private BossEvents() {}
}
