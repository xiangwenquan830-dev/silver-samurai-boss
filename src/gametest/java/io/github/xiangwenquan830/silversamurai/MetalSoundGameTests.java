package io.github.xiangwenquan830.silversamurai;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.*;
import net.minecraft.sounds.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.gametest.*;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MetalSoundGameTests {
    @GameTest(template = "arena", timeoutTicks = 25, batch = "r35_hurt_audio")
    public static void hurt_audio(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        var p = player(h, 10.5, 1, 14);
        List<SoundEvent> sounds = new ArrayList<>();
        Consumer<PlayLevelSoundEvent> listener =
                e -> {
                    if (e.getLevel() == h.getLevel() && e.getSound() != null)
                        sounds.add(e.getSound().value());
                };
        MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            h.assertTrue(b.hurt(b.damageSources().mobAttack(p), 10), "armored hit rejected");
            h.assertTrue(
                    Collections.frequency(sounds, GeneratedMod.ARMOR_SCRAPE.get()) == 1,
                    "phase-one hit missing or doubled");
            h.assertFalse(
                    sounds.contains(SoundEvents.SHIELD_BLOCK)
                            || sounds.contains(SoundEvents.IRON_GOLEM_HURT),
                    "old shield/golem hurt layered onto new sound");
            sounds.clear();
            b.testCombat(p, 2, false);
            b.setNoAi(true);
            b.invulnerableTime = 0;
            h.assertTrue(b.hurt(b.damageSources().magic(), 10), "phase-two hit rejected");
            h.assertTrue(
                    Collections.frequency(sounds, GeneratedMod.ARMOR_GROAN.get()) == 1,
                    "phase-two low hurt sound missing or doubled");
            h.assertFalse(
                    sounds.contains(GeneratedMod.ARMOR_SCRAPE.get())
                            || sounds.contains(SoundEvents.IRON_GOLEM_HURT),
                    "phase two used wrong hurt palette");
            sounds.clear();
            CombatFeedback.armor(b, b.damageSources().magic(), true);
            h.assertTrue(
                    Collections.frequency(sounds, GeneratedMod.ARMOR_GROAN.get()) == 1,
                    "zero-damage contact lost custom audio");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 25, batch = "r35_slam_audio")
    public static void slam_audio(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        Vec3 from = h.absoluteVec(new Vec3(10.5, 2, 10.5)),
                to = h.absoluteVec(new Vec3(10.5, -.25, 10.5));
        List<SoundEvent> sounds = new ArrayList<>();
        Consumer<PlayLevelSoundEvent> listener =
                e -> {
                    if (e.getLevel() == h.getLevel() && e.getSound() != null)
                        sounds.add(e.getSound().value());
                };
        MinecraftForge.EVENT_BUS.addListener(listener);
        boolean feedback = BossConfig.FEEDBACK.get();
        try {
            BossConfig.FEEDBACK.set(false);
            CombatFeedback.bladeContact(b, from, to, 2);
            h.assertTrue(sounds.isEmpty() && !b.groundStrike(), "disabled feedback still sounded");
            BossConfig.FEEDBACK.set(true);
            CombatFeedback.bladeContact(b, from, to, 0);
            CombatFeedback.bladeContact(b, from, from.add(1, 0, 0), 2);
            h.assertTrue(sounds.isEmpty(), "non-final strike or air contact sounded");
            CombatFeedback.bladeContact(b, from, to, 2);
            CombatFeedback.bladeContact(b, from, to, 2);
            h.assertTrue(
                    b.groundStrike()
                            && Collections.frequency(sounds, GeneratedMod.HEAVY_SLAM.get()) == 1,
                    "ground strike not deduplicated");
            h.assertFalse(sounds.contains(SoundEvents.ANVIL_LAND), "old anvil sound still used");
        } finally {
            BossConfig.FEEDBACK.set(feedback);
            MinecraftForge.EVENT_BUS.unregister(listener);
        }
        h.succeed();
    }
}
