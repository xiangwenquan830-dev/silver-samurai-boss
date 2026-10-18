package io.github.xiangwenquan830.silversamurai;

import java.lang.reflect.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.client.event.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.vertex.PoseStack;

/** Optional presentation bridge. Every temporary mutation is restored after rendering. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class ClientClawBridge {
    private static final String PREFIX = "cn.blockforge.generated.minecminecraftforge1.";
    private static Method get, frame, hud, wound;
    private static Constructor<?> poseType;
    private static Class<?> config;
    private static final Map<Class<?>, Map<String, Field>> FIELDS = new HashMap<>();
    private static final Map<String, net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?>>
            SETTINGS = new HashMap<>();
    private static float wrist, root;
    private static boolean initialized, available, warned;
    private static final String[] EXT = {
        "mainExtension", "previousMainExtension", "offExtension", "previousOffExtension"
    };

    private record Saved(Object view, float[] values) {}

    private static final Map<UUID, java.util.List<ModelPart>> HIDDEN = new HashMap<>();

    private static void hideArms(AbstractClientPlayer p, PlayerRenderer r) {
        if (!BrokenClawLayer.growthVisible(p)) return;
        var m = r.getModel();
        var parts = new java.util.ArrayList<ModelPart>();
        for (var part : new ModelPart[] {m.leftArm, m.rightArm, m.leftSleeve, m.rightSleeve})
            if (part.visible) {
                parts.add(part);
                part.visible = false;
            }
        HIDDEN.put(p.getUUID(), parts);
    }

    private static final Map<UUID, Saved> SAVED = new HashMap<>();

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut e) {
        for (UUID id : List.copyOf(SAVED.keySet())) restore(id);
        BrokenClawLayer.clear();
        RedHitVisuals.clear();
    }

    private static void init() {
        if (initialized) return;
        initialized = true;
        if (net.minecraftforge.fml.ModList.get() != null
                && !net.minecraftforge.fml.ModList.get().isLoaded("minec_minecraft_forge_1")) return;
        try {
            get = Class.forName(PREFIX + "ClientState").getMethod("get", Player.class);
            var motion = Class.forName(PREFIX + "FirstPersonMotion");
            var type = Class.forName(PREFIX + "FirstPersonMotion$Pose");
            poseType =
                    type.getConstructor(
                            float.class,
                            float.class,
                            float.class,
                            float.class,
                            float.class,
                            float.class);
            frame =
                    Class.forName(PREFIX + "ClawHandFrame")
                            .getMethod(
                                    "apply",
                                    PoseStack.class,
                                    type,
                                    float.class,
                                    float.class,
                                    float.class,
                                    float.class,
                                    float.class,
                                    float.class,
                                    float.class,
                                    float.class,
                                    float.class,
                                    float.class);
            wrist = motion.getField("WRIST_Y").getFloat(null);
            root = motion.getField("ROOT_Z").getFloat(null);
            config = Class.forName(PREFIX + "ModConfig");
            hud =
                    Class.forName(PREFIX + "ClientEvents")
                            .getMethod(
                                    "hud",
                                    net.minecraftforge.client.gui.overlay.ForgeGui.class,
                                    net.minecraft.client.gui.GuiGraphics.class,
                                    int.class,
                                    int.class);
            wound =
                    Class.forName(PREFIX + "WoundRenderer")
                            .getDeclaredMethod(
                                    "arm",
                                    AbstractClientPlayer.class,
                                    HumanoidArm.class,
                                    ModelPart.class,
                                    PoseStack.class,
                                    MultiBufferSource.class,
                                    int.class);
            wound.setAccessible(true);
            Class<?> viewType = Class.forName(PREFIX + "ClientState$View");
            for (String key :
                    new String[] {
                        "main",
                        "off",
                        "mainExtension",
                        "previousMainExtension",
                        "offExtension",
                        "previousOffExtension",
                        "guardBroken",
                        "notice",
                        "noticeTicks",
                        "idle",
                        "guardFeedback"
                    }) cachedField(viewType, key);
            WolverineCompat.required(cachedField(viewType, "guardFeedback").getType(), "ticks", int.class);
            for (String key :
                    new String[] {
                        "FP_X", "FP_Y", "FP_Z", "FP_SCALE", "FP_PITCH", "FP_YAW", "FP_ROLL"
                    }) {
                Object value = config.getField(key).get(null);
                if (!(value
                        instanceof net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?> handle))
                    throw new ReflectiveOperationException("Unsupported config handle: " + key);
                SETTINGS.put(key, handle);
            }
            WolverineCompat.requireMethod(get, true, viewType);
            WolverineCompat.requireMethod(frame, true, void.class);
            WolverineCompat.requireMethod(hud, true, void.class);
            WolverineCompat.requireMethod(wound, true, void.class);
            for (String key : EXT) WolverineCompat.required(viewType, key, float.class);
            for (String key : new String[] {"main", "off"}) WolverineCompat.required(viewType, key, boolean.class);
            for (String key : new String[] {"guardBroken", "noticeTicks", "idle"}) WolverineCompat.required(viewType, key, int.class);
            WolverineCompat.required(viewType, "notice", String.class);
            available = true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            warn(ex);
        }
    }

    private static void warn(Throwable ex) {
        available = false;
        if (!warned) {
            warned = true;
            GeneratedMod.LOG.warn(
                    "Claw presentation bridge unavailable; retaining normal renderer", ex);
        }
    }

    private static Field cachedField(Class<?> type, String key)
            throws ReflectiveOperationException {
        Map<String, Field> fields = FIELDS.computeIfAbsent(type, k -> new HashMap<>());
        Field found = fields.get(key);
        if (found == null) {
            found = type.getField(key);
            fields.put(key, found);
        }
        return found;
    }

    private static Field f(Object view, String key) throws ReflectiveOperationException {
        return cachedField(view.getClass(), key);
    }

    private static Object view(Player p) throws ReflectiveOperationException {
        init();
        return available ? get.invoke(null, p) : null;
    }

    private static float setting(String key) throws ReflectiveOperationException {
        return ((Number) SETTINGS.get(key).get()).floatValue();
    }

    static boolean handoff(AbstractClientPlayer p) {
        try {
            Object v = view(p);
            if (v == null) return false;
            boolean main = f(v, "main").getBoolean(v), off = f(v, "off").getBoolean(v);
            if (!main && !off) return false;
            for (int i = 0; i < EXT.length; i++)
                f(v, EXT[i]).setFloat(v, i < 2 ? (main ? 1 : 0) : (off ? 1 : 0));
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            warn(ex);
            return false;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void before(RenderPlayerEvent.Pre e) {
        if (!(e.getEntity() instanceof AbstractClientPlayer player)
                || !BrokenClawLayer.active(player)
                || !BrokenClawLayer.available()) return;
        try {
            Object v = view(e.getEntity());
            if (v == null) return;
            float[] values = new float[4];
            for (int i = 0; i < 4; i++) values[i] = f(v, EXT[i]).getFloat(v);
            SAVED.put(e.getEntity().getUUID(), new Saved(v, values));
            for (String key : EXT) f(v, key).setFloat(v, 0);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            warn(ex);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void livingBefore(RenderLivingEvent.Pre<?, ?> e) {
        // PlayerRenderer assigns visibility before entering LivingEntityRenderer; hide only after
        // that assignment.
        if (e.getEntity() instanceof AbstractClientPlayer p
                && e.getRenderer() instanceof PlayerRenderer r
                && SAVED.containsKey(p.getUUID())
                && BrokenClawLayer.active(p)
                && BrokenClawLayer.available()) hideArms(p, r);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void after(RenderPlayerEvent.Post e) {
        restore(e.getEntity().getUUID());
    }

    private static void restore(UUID id) {
        var hidden = HIDDEN.remove(id);
        if (hidden != null) hidden.forEach(part -> part.visible = true);
        Saved s = SAVED.remove(id);
        if (s == null) return;
        try {
            for (int i = 0; i < 4; i++) f(s.view, EXT[i]).setFloat(s.view, s.values[i]);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            warn(ex);
        }
    }

    @SubscribeEvent
    public static void frameEnd(net.minecraftforge.event.TickEvent.RenderTickEvent e) {
        if (e.phase == net.minecraftforge.event.TickEvent.Phase.END)
            for (UUID id : List.copyOf(SAVED.keySet())) restore(id);
    }

    static void hand(RenderHandEvent e) {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (e.isCanceled()
                || p == null
                || p.isInvisible()
                || p.isSpectator()
                || !BrokenClawLayer.active(p)
                || !e.getItemStack().isEmpty()
                || p.isUsingItem()) return;
        if (p.getMainHandItem().getItem() instanceof net.minecraft.world.item.MapItem
                || p.getOffhandItem().getItem() instanceof net.minecraft.world.item.MapItem) return;
        if (!BrokenClawLayer.growthVisible(p)
                && BrokenClawLayer.remaining(p, 0)
                        <= ClawSeverance.DURATION
                                - ClawGrowth.RETRACT_AT
                                - ClawGrowth.RETRACT_TICKS) return;
        init();
        if (!available
                || !BrokenClawLayer.available()
                || !(mc.getEntityRenderDispatcher().getRenderer(p)
                        instanceof PlayerRenderer renderer)) return;
        HumanoidArm side =
                e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                        ? p.getMainArm()
                        : p.getMainArm().getOpposite();
        float sign = side == HumanoidArm.RIGHT ? 1 : -1,
                centre = sign * (p.getModelName().equals("slim") ? -.5f : -1f);
        ModelPart
                arm =
                        side == HumanoidArm.RIGHT
                                ? renderer.getModel().rightArm
                                : renderer.getModel().leftArm,
                sleeve =
                        side == HumanoidArm.RIGHT
                                ? renderer.getModel().rightSleeve
                                : renderer.getModel().leftSleeve;
        var oldArm = arm.storePose();
        var oldSleeve = sleeve.storePose();
        float[] scales = {
            arm.xScale, arm.yScale, arm.zScale, sleeve.xScale, sleeve.yScale, sleeve.zScale
        };
        boolean[] flags = {arm.visible, arm.skipDraw, sleeve.visible, sleeve.skipDraw};
        var pose = e.getPoseStack();
        pose.pushPose();
        try {
            float left = BrokenClawLayer.remaining(p, e.getPartialTick()),
                    recoil = SeverVisuals.recoil(600 - left);
            boolean growing = BrokenClawLayer.growthVisible(p);
            float grow = BrokenClawLayer.growing(p, e.getPartialTick()),
                    effort = growing ? ClawGrowth.effort(grow) : 0,
                    shake = growing ? ClawGrowth.shake(grow) : 0;
            Object motion =
                    poseType.newInstance(
                            0f,
                            -.10f * recoil + .004f * shake,
                            .06f * recoil,
                            -8f * recoil - 7 * effort,
                            .8f * shake,
                            3f * recoil + 1.4f * shake);
            frame.invoke(
                    null,
                    pose,
                    motion,
                    sign,
                    centre,
                    e.getEquipProgress(),
                    setting("FP_X"),
                    setting("FP_Y"),
                    setting("FP_Z"),
                    setting("FP_SCALE"),
                    setting("FP_PITCH"),
                    setting("FP_YAW"),
                    setting("FP_ROLL"));
            arm.setPos(0, 0, 0);
            arm.setRotation(0, 0, 0);
            arm.xScale = arm.yScale = arm.zScale = 1;
            arm.visible = true;
            arm.skipDraw = false;
            sleeve.copyFrom(arm);
            sleeve.visible = true;
            sleeve.skipDraw = false;
            arm.render(
                    pose,
                    e.getMultiBufferSource()
                            .getBuffer(RenderType.entitySolid(p.getSkinTextureLocation())),
                    e.getPackedLight(),
                    OverlayTexture.NO_OVERLAY);
            wound.invoke(null, p, side, arm, pose, e.getMultiBufferSource(), e.getPackedLight());
            if (flags[2]
                    && !flags[3]
                    && p.isModelPartShown(
                            side == HumanoidArm.RIGHT
                                    ? PlayerModelPart.RIGHT_SLEEVE
                                    : PlayerModelPart.LEFT_SLEEVE))
                sleeve.render(
                        pose,
                        e.getMultiBufferSource()
                                .getBuffer(
                                        RenderType.entityTranslucent(p.getSkinTextureLocation())),
                        e.getPackedLight(),
                        OverlayTexture.NO_OVERLAY);
            pose.translate(centre / 16f, wrist / 16f, root / 16f);
            BrokenClawLayer.geometry(
                    pose,
                    e.getMultiBufferSource(),
                    e.getPackedLight(),
                    growing ? grow : left,
                    growing);
            e.setCanceled(true);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            warn(ex);
        } finally {
            pose.popPose();
            arm.loadPose(oldArm);
            sleeve.loadPose(oldSleeve);
            arm.xScale = scales[0];
            arm.yScale = scales[1];
            arm.zScale = scales[2];
            sleeve.xScale = scales[3];
            sleeve.yScale = scales[4];
            sleeve.zScale = scales[5];
            arm.visible = flags[0];
            arm.skipDraw = flags[1];
            sleeve.visible = flags[2];
            sleeve.skipDraw = flags[3];
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hud(RenderGuiOverlayEvent.Pre e) {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null
                || !e.getOverlay()
                        .id()
                        .toString()
                        .equals("minec_minecraft_forge_1:wolverine_reserve")
                || BrokenClawLayer.remaining(p, 0) <= 0) return;
        Object v = null, feedback = null;
        Map<String, Object> saved = new LinkedHashMap<>();
        Object ticks = null;
        try {
            v = view(p);
            if (v == null) return;
            for (String key : new String[] {"guardBroken", "notice", "noticeTicks", "idle"})
                saved.put(key, f(v, key).get(v));
            feedback = f(v, "guardFeedback").get(v);
            ticks = f(feedback, "ticks").get(feedback);
            f(v, "guardBroken").setInt(v, 0);
            f(v, "notice")
                    .set(v, net.minecraft.network.chat.Component.translatable("message.silver_samurai.broken_countdown", (int) Math.ceil(BrokenClawLayer.remaining(p, 0) / 20f)).getString());
            f(v, "noticeTicks").setInt(v, 2);
            f(v, "idle").setInt(v, 0);
            f(feedback, "ticks").setInt(feedback, 0);
            hud.invoke(
                    null,
                    (net.minecraftforge.client.gui.overlay.ForgeGui) mc.gui,
                    e.getGuiGraphics(),
                    mc.getWindow().getGuiScaledWidth(),
                    mc.getWindow().getGuiScaledHeight());
            e.setCanceled(true);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            warn(ex);
        } finally {
            try {
                if (v != null)
                    for (var value : saved.entrySet())
                        f(v, value.getKey()).set(v, value.getValue());
                if (feedback != null && ticks != null) f(feedback, "ticks").set(feedback, ticks);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
                warn(ex);
            }
        }
    }
}
