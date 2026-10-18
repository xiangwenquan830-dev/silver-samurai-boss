package io.github.xiangwenquan830.silversamurai;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Independently rendered broken steel bases; no reliance on Wolverine's full-claw visibility. */
final class BrokenClawLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation METAL =
            new ResourceLocation("minec_minecraft_forge_1", "textures/entity/claw_steel.png");
    private static final Map<UUID, Long> UNTIL = new HashMap<>();
    private static Object level;

    private record Segment(ModelPart part, float x, float min, float max) {}

    private static java.util.List<Segment> segments;
    private static java.lang.reflect.Method attachment;
    private static boolean initialized;

    BrokenClawLayer(PlayerRenderer renderer) {
        super(renderer);
    }

    private static final Map<UUID, Long> GROW = new HashMap<>();
    private static final Set<UUID> LOCKED = new HashSet<>();

    static void clear() {
        UNTIL.clear();
        GROW.clear();
        LOCKED.clear();
        level = null;
    }

    static void update(UUID id, int ticks, int growing, boolean locked) {
        var world = Minecraft.getInstance().level;
        if (world == null) return;
        if (level != world) {
            clear();
            level = world;
        }
        UNTIL.put(id, world.getGameTime() + Math.max(0, Math.min(600, ticks)));
        if (locked) LOCKED.add(id);
        else LOCKED.remove(id);
        if (growing > 0)
            GROW.put(id, world.getGameTime() + Math.min(ClawGrowth.GROW_TICKS, growing));
        else if (locked) GROW.remove(id);
        else if (!GROW.containsKey(id)) UNTIL.remove(id);
    }

    static float remaining(AbstractClientPlayer p, float partial) {
        if (level != p.level()) return -100;
        Long end = UNTIL.get(p.getUUID());
        return end == null ? -100 : end - p.level().getGameTime() - partial;
    }

    static float growing(AbstractClientPlayer p, float partial) {
        if (level != p.level()) return -100;
        Long end = GROW.get(p.getUUID());
        return end == null ? -100 : end - p.level().getGameTime() - partial;
    }

    static boolean growthVisible(AbstractClientPlayer p) {
        return level == p.level() && GROW.containsKey(p.getUUID());
    }

    static boolean active(AbstractClientPlayer p) {
        if (level != p.level()) return false;
        UUID id = p.getUUID();
        if (LOCKED.contains(id)) return true;
        if (!GROW.containsKey(id)) return false;
        if (growing(p, 0) < -8 || ClientClawBridge.handoff(p)) {
            GROW.remove(id);
            UNTIL.remove(id);
            return false;
        }
        return true;
    }

    private static void init() {
        if (initialized) return;
        initialized = true;
        if (net.minecraftforge.fml.ModList.get() != null
                && !net.minecraftforge.fml.ModList.get().isLoaded("minec_minecraft_forge_1")) return;
        try {
            Class<?> geometry =
                    Class.forName("cn.blockforge.generated.minecminecraftforge1.ClawGeometry");
            ModelPart original = (ModelPart) geometry.getMethod("create").invoke(null);
            Class<?> renderer =
                    Class.forName("cn.blockforge.generated.minecminecraftforge1.ClawRenderer");
            attachment =
                    renderer.getDeclaredMethod(
                            "thirdPersonAttachment",
                            PoseStack.class,
                            HumanoidArm.class,
                            boolean.class,
                            float.class);
            WolverineCompat.requireMethod(attachment, true, void.class);
            attachment.setAccessible(true);
            java.util.List<Segment> built = new java.util.ArrayList<>();
            original.visit(
                    new PoseStack(),
                    (pose, path, n, c) -> {
                        // Keep the actual root and steel ridges. Only remove the distal blade.
                        if (!pose.pose().equals(new org.joml.Matrix4f(), .00001f))
                            throw new IllegalStateException(
                                    "Claw geometry coordinate system changed");
                        boolean ridge = c.maxZ - c.minZ < .2f;
                        MeshDefinition mesh = new MeshDefinition();
                        mesh.getRoot()
                                .addOrReplaceChild(
                                        "blade",
                                        CubeListBuilder.create()
                                                .texOffs(ridge ? 8 : 0, 0)
                                                .addBox(
                                                        c.minX,
                                                        0,
                                                        c.minZ,
                                                        c.maxX - c.minX,
                                                        c.maxY - c.minY,
                                                        c.maxZ - c.minZ),
                                        PartPose.offset(0, c.minY, 0));
                        built.add(
                                new Segment(
                                        LayerDefinition.create(mesh, 16, 16)
                                                .bakeRoot()
                                                .getChild("blade"),
                                        (c.minX + c.maxX) * .5f,
                                        c.minY,
                                        c.maxY));
                    });
            segments = java.util.List.copyOf(built);
            com.mojang.logging.LogUtils.getLogger()
                    .info("Broken claws use original Wolverine roots, geometry and attachment");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            segments = null;
            com.mojang.logging.LogUtils.getLogger()
                    .warn(
                            "Broken claw visual interface unavailable; refusing an estimated floating model",
                            ex);
        }
    }

    static boolean available() {
        init();
        return segments != null;
    }

    static void geometry(PoseStack pose, MultiBufferSource buffers, int light, float remaining) {
        geometry(pose, buffers, light, remaining, false);
    }

    static void geometry(
            PoseStack pose, MultiBufferSource buffers, int light, float remaining, boolean growth) {
        if (!available()
                || (!growth
                        && remaining
                                <= ClawSeverance.DURATION
                                        - ClawGrowth.RETRACT_AT
                                        - ClawGrowth.RETRACT_TICKS)
                || (growth && remaining >= ClawGrowth.GROW_TICKS)) return;
        for (var s : segments) {
            float h =
                    ClawGrowth.height(
                            s.min,
                            s.max,
                            growth
                                    ? ClawGrowth.grownTip(remaining)
                                    : ClawGrowth.tip(s.x, remaining));
            if (h <= 0) continue;
            s.part.yScale = h / (s.max - s.min);
            s.part.render(
                    pose,
                    buffers.getBuffer(RenderType.entityCutoutNoCull(METAL)),
                    light,
                    OverlayTexture.NO_OVERLAY,
                    1,
                    1,
                    1,
                    1);
        }
    }

    private static void draw(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            HumanoidArm side,
            boolean slim,
            float remaining,
            boolean growth) {
        if (!available()) return;
        try {
            attachment.invoke(null, pose, side, slim, 1f);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            segments = null;
            com.mojang.logging.LogUtils.getLogger().warn("Broken claw attachment changed", ex);
            return;
        }
        geometry(pose, buffers, light, remaining, growth);
    }

    @Override
    public void render(
            PoseStack pose,
            MultiBufferSource buffers,
            int light,
            AbstractClientPlayer p,
            float walk,
            float amount,
            float partial,
            float age,
            float yaw,
            float pitch) {
        if (!active(p) || p.isInvisible()) return;
        for (HumanoidArm side : HumanoidArm.values()) {
            pose.pushPose();
            var arm =
                    side == HumanoidArm.RIGHT
                            ? getParentModel().rightArm
                            : getParentModel().leftArm;
            var sleeve =
                    side == HumanoidArm.RIGHT
                            ? getParentModel().rightSleeve
                            : getParentModel().leftSleeve;
            boolean grow = growthVisible(p);
            float time = growing(p, partial);
            var old = arm.storePose();
            var oldSleeve = sleeve.storePose();
            boolean av = arm.visible, sv = sleeve.visible;
            float sx = sleeve.xScale, sy = sleeve.yScale, sz = sleeve.zScale;
            try {
                if (grow) {
                    arm.xRot -= .18f * ClawGrowth.effort(time);
                    arm.zRot +=
                            (side == HumanoidArm.RIGHT ? 1 : -1) * .025f * ClawGrowth.shake(time);
                    sleeve.copyFrom(arm);
                    arm.visible = true;
                    sleeve.visible = true;
                    arm.render(
                            pose,
                            buffers.getBuffer(RenderType.entitySolid(p.getSkinTextureLocation())),
                            light,
                            OverlayTexture.NO_OVERLAY);
                    if (p.isModelPartShown(
                            side == HumanoidArm.RIGHT
                                    ? net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE
                                    : net.minecraft.world.entity.player.PlayerModelPart
                                            .LEFT_SLEEVE))
                        sleeve.render(
                                pose,
                                buffers.getBuffer(
                                        RenderType.entityTranslucent(p.getSkinTextureLocation())),
                                light,
                                OverlayTexture.NO_OVERLAY);
                }
                arm.translateAndRotate(pose);
                draw(
                        pose,
                        buffers,
                        light,
                        side,
                        p.getModelName().equals("slim"),
                        grow ? time : remaining(p, partial),
                        grow);
            } finally {
                arm.loadPose(old);
                sleeve.loadPose(oldSleeve);
                sleeve.xScale = sx;
                sleeve.yScale = sy;
                sleeve.zScale = sz;
                arm.visible = av;
                sleeve.visible = sv;
                pose.popPose();
            }
        }
    }

    @Mod.EventBusSubscriber(
            modid = GeneratedMod.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.MOD,
            value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent
        public static void layers(EntityRenderersEvent.AddLayers e) {
            for (String skin : e.getSkins()) {
                PlayerRenderer r = e.getSkin(skin);
                if (r != null) r.addLayer(new BrokenClawLayer(r));
            }
        }
    }

    @Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
    public static final class FirstPerson {
        @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
        public static void hand(RenderHandEvent e) {
            ClientClawBridge.hand(e);
        }
    }
}
