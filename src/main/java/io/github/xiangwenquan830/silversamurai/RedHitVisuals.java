package io.github.xiangwenquan830.silversamurai;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A single fading red hit vignette, sent only after accepted red-blade damage. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class RedHitVisuals {
    private static Object world;
    private static long at = Long.MIN_VALUE;

    static void clear() {
        world = null;
        at = Long.MIN_VALUE;
    }

    static void hit() {
        var mc = Minecraft.getInstance();
        if (mc.level != null) {
            world = mc.level;
            at = mc.level.getGameTime();
        }
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Pre e) {
        var mc = Minecraft.getInstance();
        if (mc.level != world || mc.player == null || !mc.options.getCameraType().isFirstPerson())
            return;
        float age = mc.level.getGameTime() - at + e.getPartialTick();
        if (age < 0 || age >= 18) return;
        float fade = 1 - ClawGrowth.smooth(age / 18);
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        var g = e.getGuiGraphics();
        g.fill(0, 0, w, h, ((int) (24 * fade) << 24) | 0x9b0912);
        for (int i = 0; i < 12; i++) {
            int x = w * i / 72, y = h * i / 72, nx = w * (i + 1) / 72, ny = h * (i + 1) / 72;
            int color = ((int) (65 * fade * (1 - i / 12f)) << 24) | 0xb20a16;
            g.fill(x, y, w - x, ny, color);
            g.fill(x, h - ny, w - x, h - y, color);
            g.fill(x, ny, nx, h - ny, color);
            g.fill(w - nx, ny, w - x, h - ny, color);
        }
    }
}
