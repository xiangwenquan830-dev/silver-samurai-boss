package io.github.xiangwenquan830.silversamurai;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class LoadGameTests {
    private static final List<Long> samples = new ArrayList<>();
    private static boolean measuring;
    private static long began;

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (!measuring) return;
        if (event.phase == TickEvent.Phase.START) began = System.nanoTime();
        else if (began != 0) samples.add(System.nanoTime() - began);
    }

    @GameTest(template = "arena", timeoutTicks = 280, batch = "engineering_load")
    public static void four_bosses_crowd(GameTestHelper h) {
        floor(h);
        samples.clear();
        began = 0;
        measuring = false;
        List<SamuraiEntity> bosses = new ArrayList<>();
        List<Vec3> starts = new ArrayList<>();
        List<Zombie> mobs = new ArrayList<>();
        boolean[] engaged = new boolean[4];
        for (double x : new double[] {4.5, 19.5})
            for (double z : new double[] {4.5, 19.5}) {
                var b = boss(h, x, z);
                bosses.add(b);
                starts.add(b.position());
            }
        for (int i = 0; i < 32; i++) {
            Zombie z = EntityType.ZOMBIE.create(h.getLevel());
            Vec3 p = h.absoluteVec(new Vec3(8 + (i % 8), 1, 10 + (i / 8)));
            z.moveTo(p.x, p.y, p.z, 0, 0);
            z.setNoAi(true);
            z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            z.setHealth(1000);
            h.getLevel().addFreshEntity(z);
            mobs.add(z);
        }
        h.onEachTick(
                () -> {
                    for (int i = 0; i < 4; i++) engaged[i] |= bosses.get(i).getTarget() != null;
                });
        h.runAtTickTime(
                40,
                () -> {
                    samples.clear();
                    began = 0;
                    measuring = true;
                });
        h.runAtTickTime(
                240,
                () -> {
                    measuring = false;
                    int moved = 0;
                    for (int i = 0; i < 4; i++) {
                        h.assertTrue(engaged[i], "boss failed to acquire in crowd");
                        if (bosses.get(i).position().distanceToSqr(starts.get(i)) > .25) moved++;
                    }
                    h.assertTrue(moved == 4, "bosses did not advance in crowd");
                    h.assertTrue(samples.size() >= 150, "insufficient timing samples");
                    Collections.sort(samples);
                    GeneratedMod.LOG.info(
                            "[LOAD_PROFILE] samples={} p50_ms={} p95_ms={} max_ms={}",
                            samples.size(),
                            samples.get(samples.size() / 2) / 1e6,
                            samples.get((int) (samples.size() * .95)) / 1e6,
                            samples.get(samples.size() - 1) / 1e6);
                    bosses.forEach(b -> b.discard());
                    mobs.forEach(z -> z.discard());
                    h.succeed();
                });
    }
}
