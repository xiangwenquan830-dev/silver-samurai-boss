package io.github.xiangwenquan830.silversamurai;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

final class SilverRenderTypes {
    static final RenderType SHARDS =
            RenderType.entityTranslucent(
                    new ResourceLocation("minecraft", "textures/block/iron_block.png"));

    private SilverRenderTypes() {}
}
