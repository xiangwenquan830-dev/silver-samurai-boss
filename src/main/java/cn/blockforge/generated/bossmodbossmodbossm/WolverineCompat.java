package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.*;

/** Capability-based optional bridge. Version labels do not disable compatible interfaces. */
public final class WolverineCompat {
    private static final String PREFIX = "cn.blockforge.generated.minecminecraftforge1.";
    private static boolean initialized, available, rageAvailable;
    private static String detectedVersion = "未安装";
    private static Method getData, directClaw, stopGuard, cancelAction, validateHands, sync;
    private static Field action,
            actionRage,
            actionHeld,
            guard,
            active,
            broken,
            perfect,
            counter,
            reaction,
            reactionKind,
            load,
            dirty,
            requested,
            mainClaw,
            offClaw,
            enabled;

    public static void init() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded("minec_minecraft_forge_1")) return;
        detectedVersion =
                ModList.get()
                        .getModContainerById("minec_minecraft_forge_1")
                        .orElseThrow()
                        .getModInfo()
                        .getVersion()
                        .toString();
        try {
            Class<?> data = Class.forName(PREFIX + "AbilityData");
            Class<?> state = data.getField("guard").getType();
            getData = data.getMethod("get", Player.class);
            if (!Modifier.isStatic(getData.getModifiers())
                    || !data.isAssignableFrom(getData.getReturnType()))
                throw new NoSuchMethodException("AbilityData.get return/static contract");
            try {
                directClaw =
                        Class.forName(PREFIX + "ClawDamage")
                                .getMethod("isDirectClaw", DamageSource.class);
            } catch (ReflectiveOperationException ex) {
                directClaw = null;
            }
            if (directClaw != null
                    && (directClaw.getReturnType() != boolean.class
                            || !Modifier.isStatic(directClaw.getModifiers()))) directClaw = null;
            stopGuard =
                    Class.forName(PREFIX + "GuardEngine")
                            .getMethod("stop", ServerPlayer.class, data, boolean.class);
            action = optional(data, "action", int.class);
            actionRage = optional(data, "actionRage", double.class);
            actionHeld = optional(data, "actionHeldRage", boolean.class);
            rageAvailable = action != null && actionRage != null && actionHeld != null;
            guard = data.getField("guard");
            dirty = required(data, "dirty", boolean.class);
            active = required(state, "active", boolean.class);
            broken = optional(state, "broken", int.class);
            perfect = optional(state, "perfectLeft", int.class);
            counter = optional(state, "counter", int.class);
            reaction = optional(state, "reaction", int.class);
            reactionKind = optional(state, "reactionKind", int.class);
            load = optional(state, "load", float.class);
            requested = required(data, "requested", boolean.class);
            mainClaw = required(data, "mainClaw", boolean.class);
            offClaw = required(data, "offClaw", boolean.class);
            enabled = required(data, "enabled", boolean.class);
            cancelAction = data.getMethod("cancelAction");
            validateHands =
                    Class.forName(PREFIX + "CombatEngine")
                            .getMethod("validateHands", Player.class, data);
            sync = Class.forName(PREFIX + "Network").getMethod("sync", ServerPlayer.class, data);
            available = true;
            GeneratedMod.LOG.info("金刚狼 {} 接口检查通过：断爪启用，满怒识别={}", detectedVersion, rageAvailable);
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ex) {
            fail(ex);
        }
    }

    public static boolean fullRageClaw(DamageSource source) {
        init();
        if (!available
                || !rageAvailable
                || !(source.getEntity() instanceof ServerPlayer player)
                || source.getDirectEntity() != player) return false;
        try {
            if (!isClaw(source)) return false;
            Object data = getData.invoke(null, player);
            // 使用本次起手冻结的资格，不以手持物或命中后增长的怒气冒充红爪。
            return data != null
                    && intValue(action, data) > 0
                    && (actionHeld.getBoolean(data) || actionRage.getDouble(data) >= 99.999);
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ex) {
            rageAvailable = false;
            GeneratedMod.LOG.warn("满怒识别接口变化，断爪仍启用", ex);
            return false;
        }
    }

    public static boolean guarding(ServerPlayer player) {
        init();
        if (!available || ClawSeverance.active(player)) return false;
        try {
            Object data = getData.invoke(null, player);
            Object state = data == null ? null : guard.get(data);
            return state != null
                    && active.getBoolean(state)
                    && (mainClaw.getBoolean(data) || offClaw.getBoolean(data));
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ex) {
            fail(ex);
            return false;
        }
    }

    public static boolean isClaw(DamageSource source) {
        init();
        if (!available) return false;
        try {
            return directClaw != null
                    ? (boolean) directClaw.invoke(null, source)
                    : source.getDirectEntity() == source.getEntity()
                            && source.typeHolder()
                                    .unwrapKey()
                                    .map(
                                            k ->
                                                    k.location()
                                                            .toString()
                                                            .equals("minec_minecraft_forge_1:claw"))
                                    .orElse(false);
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ex) {
            fail(ex);
            return false;
        }
    }

    public static void severClaws(ServerPlayer player) {
        init();
        if (!available || ClawSeverance.active(player)) return;
        try {
            Object data = getData.invoke(null, player);
            if (data == null || guard.get(data) == null) return;
            ClawSeverance.begin(
                    player,
                    requested.getBoolean(data)
                            || mainClaw.getBoolean(data)
                            || offClaw.getBoolean(data));
            ClawPain.begin(player);
            enforce(player, data);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("钢爪被红刃斩断，30 秒后再生"), true);
            player.level()
                    .playSound(
                            null,
                            player.blockPosition(),
                            net.minecraft.sounds.SoundEvents.ITEM_BREAK,
                            net.minecraft.sounds.SoundSource.PLAYERS,
                            1,
                            .7f);
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ex) {
            fail(ex);
        }
    }

    /** Called before ability ticks and again after them to reject queued extension inputs. */
    static void interruptGrowth(ServerPlayer player) {
        if (!ClawSeverance.locked(player)) return;
        ClawSeverance.interrupt(player.getPersistentData());
        init();
        if (!available) return;
        try {
            Object data = getData.invoke(null, player);
            if (data == null) return;
            enforce(player, data);
            dirty.setBoolean(data, true);
            sync.invoke(null, player, data);
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ex) {
            fail(ex);
        }
    }

    public static void tickBroken(ServerPlayer player) {
        if (!player.getPersistentData().contains(ClawSeverance.UNTIL)) return;
        init();
        if (!available) return;
        try {
            Object data = getData.invoke(null, player);
            if (data == null || guard.get(data) == null) return;
            if (ClawSeverance.active(player)) {
                enforce(player, data);
                return;
            }
            var tag = player.getPersistentData();
            if (!tag.getBoolean(ClawSeverance.READY)) {
                tag.putBoolean(ClawSeverance.READY, true);
                requested.setBoolean(data, false);
                mainClaw.setBoolean(data, false);
                offClaw.setBoolean(data, false);
                setInt(broken, guard.get(data), 0);
                dirty.setBoolean(data, true);
                sync.invoke(null, player, data);
                PainNetwork.syncBroken(player);
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("钢爪已恢复，按伸爪键重新长出"), true);
                return;
            }
            if (!enabled.getBoolean(data) || !requested.getBoolean(data)) {
                if (tag.contains(ClawSeverance.GROW)) {
                    tag.remove(ClawSeverance.GROW);
                    tag.remove(ClawSeverance.SOUND);
                    PainNetwork.syncBroken(player);
                }
                return;
            }
            if (!tag.contains(ClawSeverance.GROW)) {
                ClawSeverance.startGrowth(player);
                PainNetwork.syncBroken(player);
            }
            if (ClawSeverance.growing(player) > 0) {
                stopGuard.invoke(null, player, data, true);
                if (intValue(action, data) > 0) cancelAction.invoke(data);
                ClawSeverance.sound(player);
                return;
            }
            ClawSeverance.clear(player);
            PainNetwork.syncBroken(player);
            requested.setBoolean(data, enabled.getBoolean(data));
            setInt(broken, guard.get(data), 0);
            validateHands.invoke(null, player, data);
            dirty.setBoolean(data, true);
            sync.invoke(null, player, data);
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("钢爪已伸出"), true);
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException ex) {
            fail(ex);
        }
    }

    private static void enforce(ServerPlayer player, Object data)
            throws ReflectiveOperationException {
        Object state = guard.get(data);
        boolean changed =
                requested.getBoolean(data)
                        || mainClaw.getBoolean(data)
                        || offClaw.getBoolean(data)
                        || active.getBoolean(state)
                        || intValue(action, data) > 0;
        stopGuard.invoke(null, player, data, true);
        if (intValue(action, data) > 0) cancelAction.invoke(data);
        requested.setBoolean(data, false);
        mainClaw.setBoolean(data, false);
        offClaw.setBoolean(data, false);
        setInt(broken, state, Math.max(intValue(broken, state), ClawSeverance.remaining(player)));
        setInt(perfect, state, 0);
        setInt(counter, state, 0);
        if (load != null) load.setFloat(state, 0);
        if (changed) {
            setInt(reaction, state, ClawPain.remaining(player));
            setInt(reactionKind, state, 3);
            dirty.setBoolean(data, true);
            sync.invoke(null, player, data);
        }
    }

    static Field required(Class<?> owner, String name, Class<?> type)
            throws ReflectiveOperationException {
        Field f = owner.getField(name);
        if (f.getType() != type)
            throw new NoSuchFieldException(owner.getName() + "." + name + " expected " + type);
        return f;
    }

    static Field optional(Class<?> owner, String name, Class<?> type) {
        try {
            return required(owner, name, type);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    private static int intValue(Field field, Object owner) throws IllegalAccessException {
        return field == null ? 0 : field.getInt(owner);
    }

    private static void setInt(Field field, Object owner, int value) throws IllegalAccessException {
        if (field != null) field.setInt(owner, value);
    }

    public static String status() {
        init();
        return available
                ? "金刚狼 " + detectedVersion + " 接口兼容：断爪启用，满怒=" + rageAvailable
                : "独立模式（未安装或接口变化）";
    }

    private static void fail(Throwable ex) {
        available = false;
        GeneratedMod.LOG.warn("金刚狼兼容接口不可用，已安全退回独立模式", ex);
    }

    private WolverineCompat() {}
}
