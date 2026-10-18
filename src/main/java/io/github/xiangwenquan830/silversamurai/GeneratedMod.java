package io.github.xiangwenquan830.silversamurai;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "silver_samurai";
    public static final Logger LOG = LogUtils.getLogger();
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MOD_ID);
    public static final RegistryObject<EntityType<SamuraiEntity>> SAMURAI =
            ENTITIES.register(
                    "silver_samurai",
                    () ->
                            EntityType.Builder.of(SamuraiEntity::new, MobCategory.MONSTER)
                                    .sized(1.65f, 4.5f)
                                    .clientTrackingRange(16)
                                    .updateInterval(1)
                                    .build(MOD_ID + ":silver_samurai"));
    public static final RegistryObject<EntityType<DebrisEntity>> DEBRIS =
            ENTITIES.register(
                    "debris",
                    () ->
                            EntityType.Builder.of(DebrisEntity::new, MobCategory.MISC)
                                    .sized(.1f, .1f)
                                    .noSummon()
                                    .clientTrackingRange(12)
                                    .updateInterval(1)
                                    .build(MOD_ID + ":debris"));
    public static final RegistryObject<SoundEvent> CUT =
            SOUNDS.register("red_cut", () -> SoundEvent.createVariableRangeEvent(id("red_cut")));
    public static final RegistryObject<SoundEvent> ARMOR_SCRAPE =
            SOUNDS.register(
                    "armor_scrape", () -> SoundEvent.createVariableRangeEvent(id("armor_scrape")));
    public static final RegistryObject<SoundEvent> ARMOR_GROAN =
            SOUNDS.register(
                    "armor_groan", () -> SoundEvent.createVariableRangeEvent(id("armor_groan")));
    public static final RegistryObject<SoundEvent> HEAVY_SLAM =
            SOUNDS.register(
                    "heavy_slam", () -> SoundEvent.createVariableRangeEvent(id("heavy_slam")));

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public GeneratedMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        RedBladeWound.EFFECTS.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::commonSetup);
        ModLoadingContext.get()
                .registerConfig(
                        ModConfig.Type.SERVER, BossConfig.SPEC, "silver-samurai-server.toml");
        MinecraftForge.EVENT_BUS.register(BossEvents.class);
        MinecraftForge.EVENT_BUS.register(RedBladeWound.class);
        PainNetwork.init();
    }

    private void commonSetup(net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) {
        // The parallel lifecycle event finishes before gameplay begins. Sequential phase loads
        // avoid concurrent parse peaks, and get() safely publishes fully constructed rigs.
        long started = System.nanoTime();
        SamuraiRig.preload();
        LOG.info("Silver Samurai rigs preloaded in {} ms", (System.nanoTime() - started) / 1_000_000);
    }

    private void attributes(EntityAttributeCreationEvent event) {
        event.put(SAMURAI.get(), SamuraiEntity.attributes().build());
    }
}
