package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.damagesource.*;

@GameTestHolder("silver_samurai_r50")
@PrefixGameTestTemplate(false)
public class FocusedR50Tests {
    private static DamageSource bullet(GameTestHelper h, String id) {
        return new DamageSource(
                h.getLevel()
                        .registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(
                                ResourceKey.create(
                                        Registries.DAMAGE_TYPE, new ResourceLocation("tacz", id))));
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r50_far")
    public static void player_96_detection_160_retention(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 12);
        b.setNoAi(true);
        var p = SamuraiGameTests.player(h, 85, 1, 12);
        p.setNoGravity(true);
        var policy = new Targeting(b);
        h.assertTrue(policy.select(SamuraiRig.A) == p, "80格玩家未主动发现");
        b.setTarget(p);
        p.setPos(b.position().add(145, 0, 0));
        h.assertTrue(policy.valid(p), "145格玩家追踪过早丢失");
        p.setPos(b.position().add(170, 0, 0));
        h.assertFalse(policy.valid(p), "超范围玩家永久保留");
        b.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r50_exclusions")
    public static void creative_and_spectator_excluded(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 12);
        b.setNoAi(true);
        var p = SamuraiGameTests.player(h, 40, 1, 12);
        p.setNoGravity(true);
        var t = new Targeting(b);
        for (var mode : new GameType[] {GameType.CREATIVE, GameType.SPECTATOR}) {
            p.setGameMode(mode);
            h.assertTrue(t.select(SamuraiRig.A) == null && !t.valid(p), "非战斗玩家被锁定");
        }
        b.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r50_disabled")
    public static void hostile_player_switch_respected(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 12);
        b.setNoAi(true);
        var p = SamuraiGameTests.player(h, 45, 1, 12);
        boolean old = BossConfig.PLAYERS.get();
        try {
            BossConfig.PLAYERS.set(false);
            h.assertTrue(new Targeting(b).select(SamuraiRig.A) == null, "关闭玩家敌对仍主动攻击");
        } finally {
            BossConfig.PLAYERS.set(old);
            b.discard();
            p.discard();
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r50_occluded")
    public static void unknown_player_behind_wall_not_revealed(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 12);
        b.setNoAi(true);
        var p = SamuraiGameTests.player(h, 20, 1, 12);
        for (int z = 0; z < 24; z++)
            for (int y = 1; y < 10; y++) h.setBlock(12, y, z, Blocks.BEDROCK);
        h.assertTrue(new Targeting(b).select(SamuraiRig.A) == null, "隔墙凭空发现未知玩家");
        b.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 150, batch = "r50_memory")
    public static void player_memory_outlasts_mob_memory(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 4, 12);
        var p = SamuraiGameTests.player(h, 20, 1, 12);
        b.testPursuit(p);
        b.facePoint(p.position(), 360);
        for (int z = 9; z < 16; z++)
            for (int y = 1; y < 10; y++) h.setBlock(15, y, z, Blocks.BEDROCK);
        int old = BossConfig.LAST_SEEN.get();
        BossConfig.LAST_SEEN.set(0);
        Vec3 start = b.position();
        h.runAtTickTime(
                100,
                () -> {
                    BossConfig.LAST_SEEN.set(old);
                    h.assertTrue(
                            b.getTarget() == p && b.position().distanceTo(start) > .4,
                            "玩家专用记忆未保持追击: "
                                    + b.report()
                                    + " delta="
                                    + b.position().distanceTo(start));
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "r50_priority")
    public static void player_priority_keeps_close_defense(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 12);
        b.setNoAi(true);
        var p = SamuraiGameTests.player(h, 21, 1, 12);
        var mob = h.spawn(EntityType.ZOMBIE, new net.minecraft.core.BlockPos(15, 1, 12));
        mob.setNoAi(true);
        h.runAtTickTime(
                3,
                () -> {
                    var t = new Targeting(b);
                    h.assertTrue(t.select(SamuraiRig.A) == p, "同类接近机会未优先玩家");
                    mob.setPos(b.position().add(0, 0, 3));
                    b.facePoint(mob.position(), 360);
                    b.perception().invalidate();
                    h.assertTrue(new Targeting(b).select(SamuraiRig.A) == mob, "远处玩家压制贴身解围");
                    b.discard();
                    p.discard();
                    mob.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r50_gun")
    public static void all_tacz_types_classified_and_reduced(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 12);
        b.setNoAi(true);
        for (String id :
                new String[] {
                    "bullet", "bullet_ignore_armor", "bullet_void", "bullet_void_ignore_armor"
                }) {
            var d = bullet(h, id);
            h.assertTrue(DefensePolicy.ranged(d) && DefensePolicy.gunBullet(d), "TACZ类型未识别: " + id);
            h.assertTrue(Math.abs(DefensePolicy.reduction(b, d) - .99) < .0001, "一阶段枪械未保留99%减伤");
        }
        var p = SamuraiGameTests.player(h, 20, 1, 12);
        b.testCombat(p, 2, false);
        h.assertTrue(
                Math.abs(DefensePolicy.reduction(b, bullet(h, "bullet_ignore_armor")) - .9) < .0001,
                "二阶段枪械减伤不符");
        h.assertFalse(
                DefensePolicy.ranged(b.damageSources().magic())
                        || DefensePolicy.ranged(b.damageSources().genericKill()),
                "魔法/管理员清除误归为枪械");
        b.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "r50_burst")
    public static void gun_burst_shares_final_budget(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 12);
        var p = SamuraiGameTests.player(h, 20, 1, 12);
        b.testCombat(p, 2, false);
        b.setNoAi(true);
        b.getAttribute(Attributes.ARMOR).setBaseValue(0);
        var d = bullet(h, "bullet_ignore_armor");
        float health = b.getHealth();
        b.invulnerableTime = 0;
        b.hurt(d, 1000);
        h.assertTrue(Math.abs(health - b.getHealth() - 3) < .001, "单发超过3点");
        for (int i = 0; i < 12; i++) {
            b.invulnerableTime = 0;
            b.hurt(bullet(h, i % 2 == 0 ? "bullet" : "bullet_void_ignore_armor"), 1000);
        }
        h.assertTrue(Math.abs(health - b.getHealth() - 6) < .001, "多弹种集火突破6点共享上限");
        h.runAtTickTime(
                10,
                () -> {
                    b.invulnerableTime = 0;
                    b.hurt(d, 1000);
                    h.assertTrue(Math.abs(health - b.getHealth() - 6) < .001, "连续20刻窗口过早放开");
                });
        h.runAtTickTime(
                21,
                () -> {
                    b.invulnerableTime = 0;
                    b.hurt(d, 1000);
                    h.assertTrue(Math.abs(health - b.getHealth() - 9) < .001, "窗口到期后未正常恢复3点受伤");
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "r50_budget_load")
    public static void ranged_budget_survives_reload(GameTestHelper h) {
        var b = new RangedDamageBudget();
        h.assertTrue(b.accept(100, 3, 6) == 3 && b.accept(101, 3, 6) == 3, "预算建立失败");
        var n = new net.minecraft.nbt.CompoundTag();
        b.save(n, 105);
        var c = new RangedDamageBudget();
        c.load(n, 300);
        h.assertTrue(c.accept(310, 20, 6) == 0, "重载刷新远程预算");
        h.assertTrue(c.accept(315, 20, 6) == 3, "滚动窗口未逐发恢复");
        h.succeed();
    }
}
