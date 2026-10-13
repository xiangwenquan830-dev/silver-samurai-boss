package cn.blockforge.generated.bossmodbossmodbossm;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Six short, block-shaped steel tips snap off; body recoil lasts exactly one second. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class SeverVisuals {
    private record Entry(PainNetwork.Burst burst, long tick) {}

    private static final Map<UUID, Entry> ACTIVE = new LinkedHashMap<>();
    private static net.minecraft.client.multiplayer.ClientLevel world;

    public static void accept(PainNetwork.Burst burst) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        if (world != level) {
            ACTIVE.clear();
            world = level;
        }
        if (ACTIVE.size() >= 64) ACTIVE.remove(ACTIVE.keySet().iterator().next());
        ACTIVE.put(burst.player(), new Entry(burst, level.getGameTime()));
    }

    private static float age(Entry entry, float partial) {
        return world == null ? 100 : (float) (world.getGameTime() - entry.tick) + partial;
    }

    static float recoil(float age) {
        if (age < 0 || age >= 20) return 0;
        if (age < 3) return age / 3;
        if (age < 8) return 1;
        float f = (age - 8) / 12;
        return 1 - f * f * (3 - 2 * f);
    }

    @SubscribeEvent
    public static void player(RenderPlayerEvent.Pre event) {
        if (world != Minecraft.getInstance().level) return;
        Entry e = ACTIVE.get(event.getEntity().getUUID());
        if (e == null) return;
        float r = recoil(age(e, event.getPartialTick()));
        if (r <= 0) return;
        var pose = event.getPoseStack();
        pose.translate(0, 1 - .10 * r, 0);
        pose.mulPose(Axis.YP.rotationDegrees(180 - event.getEntity().yBodyRot));
        pose.mulPose(Axis.XP.rotationDegrees(14 * r));
        pose.mulPose(Axis.ZP.rotationDegrees(-4 * r));
        pose.mulPose(Axis.YP.rotationDegrees(event.getEntity().yBodyRot - 180));
        pose.translate(0, -1, 0);
    }

    @SubscribeEvent
    public static void input(MovementInputUpdateEvent event) {
        if (world != Minecraft.getInstance().level) return;
        Entry e = ACTIVE.get(event.getEntity().getUUID());
        if (e == null || age(e, 0) >= 20) return;
        var input = event.getInput();
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
    }

    @SubscribeEvent
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        var p = Minecraft.getInstance().player;
        if (p == null || world != p.level()) return;
        Entry e = ACTIVE.get(p.getUUID());
        if (e == null) return;
        float r = recoil(age(e, (float) event.getPartialTick()));
        event.setPitch(event.getPitch() + 3 * r);
        event.setRoll(event.getRoll() - 2 * r);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (world != Minecraft.getInstance().level) {
            ACTIVE.clear();
            world = null;
            return;
        }
        ACTIVE.entrySet().removeIf(e -> age(e.getValue(), 0) > 26);
        var source = Minecraft.getInstance().renderBuffers().bufferSource();
        var out = source.getBuffer(SilverRenderTypes.SHARDS);
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        for (Entry entry : ACTIVE.values()) {
            float t = age(entry, event.getPartialTick());
            if (t < 0 || t > 24) continue;
            var b = entry.burst;
            pose.pushPose();
            pose.translate(b.x() - camera.x, b.y() - camera.y, b.z() - camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(-b.yaw()));
            for (int side : new int[] {-1, 1})
                for (int finger = 0; finger < 3; finger++) {
                    float travel = Math.max(0, t - 2), alpha = t > 18 ? (24 - t) / 6 : 1;
                    pose.pushPose();
                    pose.translate(
                            side * (.32 + .055 * finger + .047 * travel),
                            1.22 + .065 * travel - .007 * travel * travel,
                            .22 + .045 * travel);
                    pose.mulPose(Axis.XP.rotationDegrees(25 + travel * (13 + finger * 3)));
                    pose.mulPose(Axis.ZP.rotationDegrees(side * (10 + travel * 9)));
                    box(pose, out, .023f, .032f, .34f, alpha);
                    pose.popPose();
                }
            pose.popPose();
        }
        source.endBatch(SilverRenderTypes.SHARDS);
    }

    private static void box(
            PoseStack pose, VertexConsumer out, float x, float y, float z, float alpha) {
        float[][] normals = {{0, 0, -1}, {0, 0, 1}, {0, -1, 0}, {0, 1, 0}, {1, 0, 0}, {-1, 0, 0}};
        float[][] p = {
            {-x, -y, 0}, {x, -y, 0}, {x, y, 0}, {-x, y, 0}, {-x, -y, z}, {x, -y, z}, {x, y, z},
            {-x, y, z}
        };
        int[][] faces = {
            {0, 3, 2, 1}, {4, 5, 6, 7}, {0, 1, 5, 4}, {3, 7, 6, 2}, {1, 2, 6, 5}, {0, 4, 7, 3}
        };
        for (int i = 0; i < 6; i++) {
            float shade = i == 3 ? .96f : i % 2 == 0 ? .72f : .84f;
            for (int index : faces[i]) {
                float[] v = p[index];
                out.vertex(pose.last().pose(), v[0], v[1], v[2])
                        .color(shade * .98f, shade, Math.min(1, shade * 1.025f), alpha)
                        .uv((index & 1) == 0 ? 0 : 1, index < 4 ? 0 : 1)
                        .overlayCoords(
                                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                        .uv2(LightTexture.FULL_BRIGHT)
                        .normal(pose.last().normal(), normals[i][0], normals[i][1], normals[i][2])
                        .endVertex();
            }
        }
    }
}
