package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class LargeTargetGameTests {
    static final class SizedZombie extends Zombie {
        private float width, height;

        SizedZombie(net.minecraft.server.level.ServerLevel level, float w, float h) {
            super(EntityType.ZOMBIE, level);
            width = w;
            height = h;
            refreshDimensions();
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
            return EntityDimensions.scalable(
                    width == 0 ? .6f : width, height == 0 ? 1.95f : height);
        }
    }

    private static SizedZombie giant(
            GameTestHelper h, float width, float height, double x, double y, double z) {
        var mob = new SizedZombie(h.getLevel(), width, height);
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.setPersistenceRequired();
        Vec3 p = h.absoluteVec(new Vec3(x, y, z));
        mob.setPos(p);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000);
        mob.setHealth(2000);
        mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        h.getLevel().addFreshEntity(mob);
        return mob;
    }

    @GameTest(template = "arena", timeoutTicks = 380, batch = "r42_giant_one")
    public static void giant_one(GameTestHelper h) {
        combat(h, 1, 8, 12, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 380, batch = "r42_giant_two")
    public static void giant_two(GameTestHelper h) {
        combat(h, 2, 8, 12, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 380, batch = "r42_giant_offset")
    public static void giant_offset(GameTestHelper h) {
        combat(h, 1, 5, 12, -2);
    }

    private static void combat(GameTestHelper h, int phase, float width, float height, double y) {
        floor(h);
        var b = boss(h, 4, 12);
        var mob = giant(h, width, height, 17, y, 12);
        Vec3 fixed = mob.position();
        if (phase == 2) {
            b.testCombat(mob, 2, false);
            b.testPursuit(mob);
            b.setTarget(null);
        }
        boolean[] accepted = {false};
        int[] selected = {0}, slashes = {0};
        double[] travel = {0};
        Vec3 start = b.position();
        h.onEachTick(
                () -> {
                    mob.setPos(fixed);
                    mob.setDeltaMovement(Vec3.ZERO);
                    if (b.getTarget() == mob) selected[0]++;
                    if (ActionRules.combo(b.state())) {
                        slashes[0]++;
                        accepted[0] |= java.util.Arrays.stream(b.hitCounts()).sum() > 0;
                    }
                    travel[0] = Math.max(travel[0], start.distanceTo(b.position()));
                });
        h.runAtTickTime(
                320,
                () -> {
                    GeneratedMod.LOG.info(
                            "[r42 giant] phase={} width={} y={} selected={} slashTicks={} travel={} damage={} report={}",
                            phase,
                            width,
                            y,
                            selected[0],
                            slashes[0],
                            travel[0],
                            2000 - mob.getHealth(),
                            b.report());
                    h.assertTrue(
                            selected[0] > 0
                                    && slashes[0] > 0
                                    && accepted[0]
                                    && mob.getHealth() < 2000,
                            "large target not acquired/approached/slashed: " + b.report());
                    h.assertTrue(travel[0] > 1, "no actual approach to large target");
                    mob.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "r42_giant_goal")
    public static void body_goal(GameTestHelper h) {
        floor(h);
        var b = boss(h, 5, 12);
        var mob = giant(h, 5, 9, 10, 1, 12);
        b.setNoAi(true);
        b.testPursuit(mob);
        h.assertTrue(
                b.terrainNavigation()
                        .goal(
                                SamuraiNavigation.point(b.position()),
                                SamuraiNavigation.point(mob.position())),
                "5-wide boss has no valid standing goal (old 3.6 maximum)");
        mob.discard();
        b.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 70, batch = "r42_giant_range")
    public static void body_acquisition(GameTestHelper h) {
        floor(h);
        var b = boss(h, 4, 12);
        var mob = giant(h, 10, 12, 15, 1, 12);
        double old = BossConfig.ACQUIRE.get();
        BossConfig.ACQUIRE.set(8.0);
        try {
            h.assertTrue(
                    new Targeting(b).select(SamuraiRig.A) == mob,
                    "near body excluded because distant center is outside acquisition range");
        } finally {
            BossConfig.ACQUIRE.set(old);
            mob.discard();
            b.discard();
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 70, batch = "r42_giant_wall")
    public static void body_wall(GameTestHelper h) {
        floor(h);
        var b = boss(h, 5, 12);
        var mob = giant(h, 5, 9, 11, 1, 12);
        b.setNoAi(true);
        for (int y = 1; y < 12; y++)
            for (int z = 0; z < 24; z++) h.setBlock(7, y, z, Blocks.BEDROCK);
        h.assertFalse(
                Targeting.visible(b, mob) || CombatSpacing.lane(b, mob),
                "body surface sensing penetrated wall");
        mob.discard();
        b.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 380, batch = "r42_quiet_animation")
    public static void quiet_animation(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10, 10);
        var p = player(h, 10, 1, 14);
        b.testPursuit(p);
        p.setGameMode(GameType.CREATIVE);
        int[] frames = {0};
        boolean[] moved = {false};
        SamuraiRig.Pose[] first = {null};
        Vec3 at = b.position();
        h.onEachTick(
                () -> {
                    if (b.state() == SamuraiEntity.SHEATHE) {
                        frames[0]++;
                        var pose = b.currentPose(0);
                        int bone = SamuraiRig.get(1).bone("背挂武士刀_L");
                        if (first[0] == null) first[0] = pose;
                        else if (!first[0].matrices()[bone].equals(pose.matrices()[bone], .01f))
                            moved[0] = true;
                        h.assertTrue(
                                b.position().distanceTo(at) < .1, "quiet sheathe moved the boss");
                    }
                });
        h.runAtTickTime(
                300,
                () -> {
                    h.assertTrue(
                            frames[0] > 20
                                    && moved[0]
                                    && !b.weaponsDrawn()
                                    && b.state() == SamuraiEntity.IDLE,
                            "no complete visible sheathe with only creative observer: "
                                    + b.report());
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 380, batch = "r42_nearby_enemy")
    public static void nearby_enemy(GameTestHelper h) {
        floor(h);
        var b = boss(h, 5, 12);
        var p = player(h, 4, 1, 10);
        p.setGameMode(GameType.CREATIVE);
        var mob = giant(h, 5, 9, 12, 1, 12);
        b.testPursuit(mob);
        for (int y = 1; y < 12; y++)
            for (int z = 0; z < 24; z++) h.setBlock(8, y, z, Blocks.BEDROCK);
        h.onEachTick(
                () ->
                        h.assertTrue(
                                b.weaponsDrawn() && b.state() != SamuraiEntity.SHEATHE,
                                "nearby enemy incorrectly allowed sheathing"));
        h.runAtTickTime(
                300,
                () -> {
                    p.discard();
                    mob.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 420, batch = "r42_real_visage")
    public static void real_visage(GameTestHelper h) {
        floor(h);
        var type =
                net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(
                        new net.minecraft.resources.ResourceLocation("deaceased", "great_visage"));
        if (!net.minecraftforge.fml.ModList.get().isLoaded("deaceased")) {
            GeneratedMod.LOG.info("[VISAGE_SKIP] DeceasedBeast not present");
            h.succeed();
            return;
        }
        var b = boss(h, 4, 12);
        var mob = (Mob) type.create(h.getLevel());
        h.assertTrue(mob != null, "real Great Visage could not be constructed");
        Vec3 fixed = h.absoluteVec(new Vec3(16, 1, 12));
        mob.setPos(fixed);
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000);
        mob.setHealth(2000);
        h.getLevel().addFreshEntity(mob);
        int[] chosen = {0};
        boolean[] accepted = {false};
        h.onEachTick(
                () -> {
                    mob.setPos(fixed);
                    mob.setDeltaMovement(Vec3.ZERO);
                    if (b.getTarget() == mob) chosen[0]++;
                    if (ActionRules.combo(b.state()))
                        accepted[0] |= java.util.Arrays.stream(b.hitCounts()).sum() > 0;
                });
        h.runAtTickTime(
                360,
                () -> {
                    h.assertTrue(
                            chosen[0] > 0 && accepted[0] && mob.getHealth() < 2000,
                            "actual Great Visage never acquired/damaged: " + b.report());
                    GeneratedMod.LOG.info(
                            "[VISAGE_PASS] actual installed boss acquired and accepted sword damage verified; width={} height={} damage={}",
                            mob.getBbWidth(),
                            mob.getBbHeight(),
                            2000 - mob.getHealth());
                    mob.discard();
                    b.discard();
                    h.succeed();
                });
    }
}
