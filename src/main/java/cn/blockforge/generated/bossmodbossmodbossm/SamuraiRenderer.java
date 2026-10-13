package cn.blockforge.generated.bossmodbossmodbossm;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.*;
import java.lang.Math;
import java.util.*;

@Mod.EventBusSubscriber(
        modid = GeneratedMod.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class SamuraiRenderer extends EntityRenderer<SamuraiEntity> {
    private static final Map<Integer, ResourceLocation> TEXTURES = new HashMap<>();

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GeneratedMod.SAMURAI.get(), SamuraiRenderer::new);
        event.registerEntityRenderer(GeneratedMod.DEBRIS.get(), DebrisRenderer::new);
    }

    public SamuraiRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 1.5f;
    }

    @Override
    public ResourceLocation getTextureLocation(SamuraiEntity boss) {
        return texture(boss.phase(), 0);
    }

    private static ResourceLocation texture(int phase, int index) {
        int key = (phase << 16) | index;
        return TEXTURES.computeIfAbsent(
                key,
                k ->
                        GeneratedMod.id(
                                "textures/entity/phase"
                                        + phase
                                        + "/texture_"
                                        + (index < 10 ? "0" : "")
                                        + index
                                        + ".png"));
    }

    @Override
    public void render(
            SamuraiEntity boss,
            float yaw,
            float partial,
            PoseStack stack,
            MultiBufferSource buffers,
            int light) {
        SamuraiRig rig = SamuraiRig.get(boss.phase());
        SamuraiRig.Pose pose = boss.currentPose(partial);
        boolean[] visible = pose.visible().clone();
        if (boss.phase() == 2)
            for (int i = 0; i < 8; i++)
                if (boss.shed(i)) visible[rig.bone(DebrisGeometry.NAMES[i])] = false;
        pose = new SamuraiRig.Pose(pose.matrices(), visible);
        stack.pushPose();
        stack.mulPose(Axis.YP.rotationDegrees(180 - boss.displayYaw(partial)));
        stack.scale(SamuraiRig.SCALE, SamuraiRig.SCALE, SamuraiRig.SCALE);
        draw(
                rig,
                pose,
                stack,
                buffers,
                light,
                boss.redProgress(partial),
                false,
                boss.state() == SamuraiEntity.TRANSITION || boss.state() == SamuraiEntity.DEAD);
        stack.popPose();
        super.render(boss, yaw, partial, stack, buffers, light);
    }

    static void draw(
            SamuraiRig rig,
            SamuraiRig.Pose pose,
            PoseStack stack,
            MultiBufferSource buffers,
            int light,
            float red,
            boolean debrisOnly,
            boolean showDebris) {
        draw(rig, pose, stack, buffers, light, red, debrisOnly, showDebris, -1, 0);
    }

    static void draw(
            SamuraiRig rig,
            SamuraiRig.Pose pose,
            PoseStack stack,
            MultiBufferSource buffers,
            int light,
            float red,
            boolean debrisOnly,
            boolean showDebris,
            int part,
            float dissolve) {
        Map<Integer, List<SamuraiRig.Cube>> batches =
                GEOMETRY.computeIfAbsent(rig, RenderGeometry::new).batches;
        for (int texture : batches.keySet()) {
            VertexConsumer consumer =
                    buffers.getBuffer(RenderType.entityCutoutNoCull(texture(rig.phase, texture)));
            for (SamuraiRig.Cube cube : batches.get(texture)) {
                if (cube.bone >= 0 && !pose.visible()[cube.bone]) continue;
                if (part >= 0 && DebrisGeometry.part(cube) != part) continue;
                if (debrisOnly ? !cube.debris : (!showDebris && cube.debris)) continue;
                Matrix4f transform = rig.cubeMatrix(cube, pose);
                Matrix3f normalTransform = new Matrix3f(transform).invert().transpose();
                boolean blade = rig.phase == 2 && cube.name.startsWith("katana_L") && cube.blade();
                for (SamuraiRig.Face face : cube.faces) {
                    if (face.texture() != texture) continue;
                    FaceGeometry geometry = GEOMETRY.get(rig).faces.get(face);
                    Vector3f[] vertices = geometry.vertices;
                    Vector3f normal = new Vector3f(geometry.normal);
                    normalTransform.transform(normal).normalize();
                    float[] uv = face.uv();
                    float[][] coords = geometry.uv;
                    if (dissolve <= 0) {
                        // Ordinary armor emits the four cached corners directly. Never mutate the
                        // cache.
                        Vector3f point = new Vector3f();
                        for (int i = 0; i < 4; i++) {
                            point.set(vertices[i]);
                            float[] tex = coords[Math.floorMod(i + face.rotation() / 90, 4)];
                            emit(
                                    consumer,
                                    stack,
                                    transform,
                                    normal,
                                    point,
                                    tex[0],
                                    tex[1],
                                    blade,
                                    red,
                                    cube.name.contains("edge"),
                                    light);
                        }
                        continue;
                    }
                    int nx =
                            dissolve > 0
                                    ? Math.max(
                                            4,
                                            Math.min(32, (int) Math.ceil(Math.abs(uv[2] - uv[0]))))
                                    : 1;
                    int ny =
                            dissolve > 0
                                    ? Math.max(
                                            4,
                                            Math.min(32, (int) Math.ceil(Math.abs(uv[3] - uv[1]))))
                                    : 1;
                    for (int x = 0; x < nx; x++)
                        for (int y = 0; y < ny; y++) {
                            // UV像素格固定在零件表面，逐格减少；不缩放、不弹走、不整块删。
                            int hash =
                                    cube.uuid.hashCode()
                                            ^ face.side().hashCode()
                                            ^ x * 374761393
                                            ^ y * 668265263;
                            hash = (hash ^ (hash >>> 13)) * 1274126177;
                            hash ^= hash >>> 16;
                            if (dissolve > 0 && (hash & 0xffff) / 65535f < dissolve) continue;
                            float[][] tile = {
                                {x / (float) nx, y / (float) ny},
                                {(x + 1f) / nx, y / (float) ny},
                                {(x + 1f) / nx, (y + 1f) / ny},
                                {x / (float) nx, (y + 1f) / ny}
                            };
                            for (int i = 0; i < 4; i++) {
                                float u = tile[i][0], v = tile[i][1];
                                Vector3f point =
                                        new Vector3f(vertices[0])
                                                .lerp(vertices[1], u)
                                                .lerp(
                                                        new Vector3f(vertices[3])
                                                                .lerp(vertices[2], u),
                                                        v);
                                float along = Mth.clamp((point.y - 45.8f) / 27.5f, 0, 1);
                                boolean glow = blade && red > 0 && along <= red;
                                float strength = glow ? Math.min(1, red * 2) : 0;
                                float r = 1,
                                        g =
                                                1
                                                        - strength
                                                                * (cube.name.contains("edge")
                                                                        ? .48f
                                                                        : .84f),
                                        b =
                                                1
                                                        - strength
                                                                * (cube.name.contains("edge")
                                                                        ? .78f
                                                                        : .93f);
                                transform.transformPosition(point);
                                float[] a = coords[Math.floorMod(face.rotation() / 90, 4)],
                                        buv = coords[Math.floorMod(1 + face.rotation() / 90, 4)],
                                        cuv = coords[Math.floorMod(2 + face.rotation() / 90, 4)],
                                        d = coords[Math.floorMod(3 + face.rotation() / 90, 4)];
                                float
                                        tu =
                                                Mth.lerp(
                                                        v,
                                                        Mth.lerp(u, a[0], buv[0]),
                                                        Mth.lerp(u, d[0], cuv[0])),
                                        tv =
                                                Mth.lerp(
                                                        v,
                                                        Mth.lerp(u, a[1], buv[1]),
                                                        Mth.lerp(u, d[1], cuv[1]));
                                consumer.vertex(stack.last().pose(), point.x, point.y, point.z)
                                        .color(r, g, b, 1)
                                        .uv(tu, tv)
                                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                                        .uv2(glow ? LightTexture.FULL_BRIGHT : light)
                                        .normal(stack.last().normal(), normal.x, normal.y, normal.z)
                                        .endVertex();
                            }
                        }
                }
            }
        }
    }

    private static void emit(
            VertexConsumer consumer,
            PoseStack stack,
            Matrix4f transform,
            Vector3f normal,
            Vector3f point,
            float u,
            float v,
            boolean blade,
            float red,
            boolean edge,
            int light) {
        float along = Mth.clamp((point.y - 45.8f) / 27.5f, 0, 1);
        boolean glow = blade && red > 0 && along <= red;
        float strength = glow ? Math.min(1, red * 2) : 0;
        transform.transformPosition(point);
        consumer.vertex(stack.last().pose(), point.x, point.y, point.z)
                .color(
                        1,
                        1 - strength * (edge ? .48f : .84f),
                        1 - strength * (edge ? .78f : .93f),
                        1)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(glow ? LightTexture.FULL_BRIGHT : light)
                .normal(stack.last().normal(), normal.x, normal.y, normal.z)
                .endVertex();
    }

    private record FaceGeometry(Vector3f[] vertices, Vector3f normal, float[][] uv) {}

    private static final Map<SamuraiRig, RenderGeometry> GEOMETRY = new IdentityHashMap<>();

    private static final class RenderGeometry {
        final Map<Integer, List<SamuraiRig.Cube>> batches = new TreeMap<>();
        final Map<SamuraiRig.Face, FaceGeometry> faces = new IdentityHashMap<>();

        RenderGeometry(SamuraiRig rig) {
            for (var cube : rig.cubes) {
                Set<Integer> used = new HashSet<>();
                for (var face : cube.faces) {
                    if (used.add(face.texture()))
                        batches.computeIfAbsent(face.texture(), key -> new ArrayList<>()).add(cube);
                    float[] uv = face.uv();
                    faces.put(
                            face,
                            new FaceGeometry(
                                    corners(cube.from, cube.to, face.side()),
                                    normal(face.side()),
                                    new float[][] {
                                        {uv[0] / 32, uv[1] / 32},
                                        {uv[2] / 32, uv[1] / 32},
                                        {uv[2] / 32, uv[3] / 32},
                                        {uv[0] / 32, uv[3] / 32}
                                    }));
                }
            }
        }
    }

    private static Vector3f[] corners(Vector3f a, Vector3f b, String face) {
        return switch (face) {
            case "north" ->
                    new Vector3f[] {
                        v(b.x, b.y, a.z), v(a.x, b.y, a.z), v(a.x, a.y, a.z), v(b.x, a.y, a.z)
                    };
            case "south" ->
                    new Vector3f[] {
                        v(a.x, b.y, b.z), v(b.x, b.y, b.z), v(b.x, a.y, b.z), v(a.x, a.y, b.z)
                    };
            case "east" ->
                    new Vector3f[] {
                        v(b.x, b.y, a.z), v(b.x, b.y, b.z), v(b.x, a.y, b.z), v(b.x, a.y, a.z)
                    };
            case "west" ->
                    new Vector3f[] {
                        v(a.x, b.y, b.z), v(a.x, b.y, a.z), v(a.x, a.y, a.z), v(a.x, a.y, b.z)
                    };
            case "up" ->
                    new Vector3f[] {
                        v(a.x, b.y, a.z), v(b.x, b.y, a.z), v(b.x, b.y, b.z), v(a.x, b.y, b.z)
                    };
            default ->
                    new Vector3f[] {
                        v(a.x, a.y, b.z), v(b.x, a.y, b.z), v(b.x, a.y, a.z), v(a.x, a.y, a.z)
                    };
        };
    }

    private static Vector3f v(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    private static Vector3f normal(String face) {
        return switch (face) {
            case "north" -> v(0, 0, -1);
            case "south" -> v(0, 0, 1);
            case "east" -> v(1, 0, 0);
            case "west" -> v(-1, 0, 0);
            case "up" -> v(0, 1, 0);
            default -> v(0, -1, 0);
        };
    }

    public static final class DebrisRenderer extends EntityRenderer<DebrisEntity> {
        public DebrisRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public ResourceLocation getTextureLocation(DebrisEntity entity) {
            return texture(2, 0);
        }

        @Override
        public void render(
                DebrisEntity entity,
                float yaw,
                float partial,
                PoseStack stack,
                MultiBufferSource buffers,
                int light) {
            SamuraiRig rig = SamuraiRig.get(2);
            DebrisGeometry shape = DebrisGeometry.get(entity.part());
            Quaternionf q = entity.orientation(partial);
            float[] bounds = shape.bounds(q);
            stack.pushPose();
            stack.translate(0, -bounds[1], 0);
            stack.mulPose(q);
            stack.scale(SamuraiRig.SCALE, SamuraiRig.SCALE, SamuraiRig.SCALE);
            stack.translate(-shape.center.x, -shape.center.y, -shape.center.z);
            draw(
                    rig,
                    shape.rest,
                    stack,
                    buffers,
                    light,
                    0,
                    true,
                    true,
                    entity.part(),
                    entity.dissolve(partial));
            stack.popPose();
        }
    }
}
