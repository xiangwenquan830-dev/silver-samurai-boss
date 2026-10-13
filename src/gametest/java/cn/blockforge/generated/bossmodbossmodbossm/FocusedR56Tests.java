package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;

@GameTestHolder("silver_samurai_r56")
@PrefixGameTestTemplate(false)
public class FocusedR56Tests {
    @GameTest(template = "arena", timeoutTicks = 20)
    public static void zero_damage_has_no_red_hit_feedback(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 10.5, 7.5);
        var p = SamuraiGameTests.player(h, 10.5, 1, 10.6);
        b.testCombat(p, 2, false);
        b.setNoAi(true);
        int[] sounds = {0};
        float hp = p.getHealth();
        java.util.function.Consumer<net.minecraftforge.event.entity.living.LivingDamageEvent> deny =
                e -> {
                    if (e.getEntity() == p && e.getSource().getEntity() == b) e.setAmount(0);
                };
        java.util.function.Consumer<net.minecraftforge.event.PlayLevelSoundEvent.AtPosition> hear =
                e -> {
                    if (e.getLevel() == p.level()
                            && e.getPosition().distanceToSqr(p.position()) < 3
                            && e.getSound().value() == GeneratedMod.CUT.get()) sounds[0]++;
                };
        var bus = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.addListener(deny);
        bus.addListener(hear);
        try {
            BladeCombat.damage(b, p, 0);
            h.assertTrue(
                    p.getHealth() == hp
                            && sounds[0] == 0
                            && !p.hasEffect(RedBladeWound.EFFECT.get()),
                    "零伤害仍有红刃命中反馈");
            bus.unregister(deny);
            p.invulnerableTime = 0;
            BladeCombat.damage(b, p, 0);
            h.assertTrue(
                    p.getHealth() < hp && sounds[0] == 1 && p.hasEffect(RedBladeWound.EFFECT.get()),
                    "有效命中丢失音效或伤口效果");
        } finally {
            bus.unregister(deny);
            bus.unregister(hear);
            p.discard();
            b.discard();
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 20)
    public static void death_preserves_recovery_not_growth(GameTestHelper h) {
        var old = new CompoundTag();
        var next = new CompoundTag();
        old.putLong(ClawSeverance.UNTIL, 12345);
        old.putBoolean(ClawSeverance.READY, true);
        old.putLong(ClawSeverance.GROW, 99999);
        old.putInt(ClawSeverance.SOUND, 1);
        old.putLong(ClawPain.UNTIL, 11111);
        next.merge(old);
        ClawSeverance.copyRecovery(old, next);
        h.assertTrue(
                next.getLong(ClawSeverance.UNTIL) == 12345 && next.getBoolean(ClawSeverance.READY),
                "死亡改变了恢复期限");
        h.assertFalse(
                next.contains(ClawSeverance.GROW)
                        || next.contains(ClawSeverance.SOUND)
                        || next.contains(ClawPain.UNTIL),
                "新身体继承旧生长动作或痛苦锁");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 20)
    public static void failed_wall_is_local_to_face_and_height(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 5);
        var a = new WallApproach(b);
        var wall = new WallGeometry.Contact(new Vec3(10, 4, 10), new Vec3(0, 0, 1));
        a.failed(wall);
        h.assertTrue(
                a.blocked(wall)
                        && a.blocked(new WallGeometry.Contact(new Vec3(10, 5, 10), wall.forward())),
                "同一失败入口未记住");
        h.assertFalse(
                a.blocked(new WallGeometry.Contact(new Vec3(10, 4, 11), new Vec3(0, 0, -1))),
                "薄墙背面被连带封锁");
        h.assertFalse(
                a.blocked(new WallGeometry.Contact(new Vec3(10, 12, 10), wall.forward())),
                "上一层入口被误封锁");
        b.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 20)
    public static void heavy_cue_tracks_animation_clock(GameTestHelper h) {
        for (float rate : new float[] {.5f, 1, 1.5f, 2, 3}) {
            int count = 0;
            float previous = 0, cue = CombatTiming.windows(2, false)[2][0] - .4f;
            for (int tick = 1; tick < 300; tick++) {
                float now = CombatTiming.animationTime(tick * .05f, rate, 2, false);
                if (CombatFeedback.crossed(previous, now, cue)) count++;
                previous = now;
            }
            h.assertTrue(count == 1, "变速导致重劈预告丢失或重复");
        }
        h.succeed();
    }
}
