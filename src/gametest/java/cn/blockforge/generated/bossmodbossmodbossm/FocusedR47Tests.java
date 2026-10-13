package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.Registries;

@GameTestHolder("silver_samurai_r47")
@PrefixGameTestTemplate(false)
public class FocusedR47Tests {
    @GameTest(template = "arena", timeoutTicks = 430, batch = "r47_drop12")
    public static void pursue_drop_twelve(GameTestHelper h) {
        drop(h, 12, 1, false);
    }

    @GameTest(template = "arena", timeoutTicks = 430, batch = "r47_drop36")
    public static void pursue_drop_phase_two(GameTestHelper h) {
        drop(h, 36, 2, false);
    }

    @GameTest(template = "arena", timeoutTicks = 430, batch = "r47_drop96")
    public static void pursue_drop_high(GameTestHelper h) {
        drop(h, 96, 1, false);
    }

    @GameTest(template = "arena", timeoutTicks = 430, batch = "r47_dropunder")
    public static void pursue_drop_directly_below(GameTestHelper h) {
        drop(h, 32, 1, true);
    }

    @GameTest(template = "arena", timeoutTicks = 430, batch = "r47_dropwide")
    public static void follow_player_off_wide_roof(GameTestHelper h) {
        drop(h, 32, 1, false, true);
    }

    private static void drop(GameTestHelper h, int height, int phase, boolean under) {
        drop(h, height, phase, under, false);
    }

    private static void drop(GameTestHelper h, int height, int phase, boolean under, boolean wide) {
        SamuraiGameTests.floor(h);
        for (int x = wide ? 1 : 7; x <= (wide ? 20 : 15); x++)
            for (int z = wide ? 1 : 7; z <= (wide ? 20 : 15); z++)
                h.setBlock(x, height, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 12, 12);
        b.setPos(h.absoluteVec(new Vec3(12, height + 1, 12)));
        var p = SamuraiGameTests.player(h, under ? 12 : 19, wide ? height + 1 : 1, 12);
        if (wide) h.runAtTickTime(10, () -> p.setPos(h.absoluteVec(new Vec3(23, 1, 12))));
        if (phase == 2) b.testCombat(p, 2, false);
        b.testPursuit(p);
        float hp = b.getHealth();
        boolean[] fell = {false}, landed = {false}, hit = {false};
        double[] last = {b.getY()};
        h.onEachTick(
                () -> {
                    if (b.wallTraversal().status().equals("DROP")) fell[0] = true;
                    if (fell[0] && b.onGround() && h.relativeVec(b.position()).y < 2)
                        landed[0] = true;
                    if (p.getLastDamageSource() != null && p.getLastDamageSource().getEntity() == b)
                        hit[0] = true;
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)), "下跳穿模");
                    h.assertTrue(b.getY() >= last[0] - 5, "下跳发生瞬移");
                    last[0] = b.getY();
                    h.assertTrue(b.getHealth() == hp, "追击下跳受摔落伤害");
                    if (h.getTick() % 80 == 0)
                        GeneratedMod.LOG.info(
                                "[r47 drop] height={} tick={} pos={} report={}",
                                height,
                                h.getTick(),
                                h.relativeVec(b.position()),
                                b.report());
                });
        h.runAtTickTime(
                380,
                () -> {
                    h.assertTrue(
                            fell[0] && landed[0] && hit[0],
                            "未完成下跳落地续攻: "
                                    + fell[0]
                                    + "/"
                                    + landed[0]
                                    + "/"
                                    + hit[0]
                                    + " "
                                    + b.report());
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r47_dropfire")
    public static void reject_burning_drop(GameTestHelper h) {
        unsafeDrop(h, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r47_dropdeep")
    public static void reject_deep_water_drop(GameTestHelper h) {
        unsafeDrop(h, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 40, batch = "r47_dropvoid")
    public static void reject_void_drop(GameTestHelper h) {
        unsafeDrop(h, 3);
    }

    private static void unsafeDrop(GameTestHelper h, int hazard) {
        SamuraiGameTests.floor(h);
        for (int x = 0; x < 24; x++)
            for (int z = 0; z < 24; z++) {
                if (hazard == 1) h.setBlock(x, 1, z, Blocks.FIRE);
                if (hazard == 2) {
                    h.setBlock(x, 1, z, Blocks.WATER);
                    h.setBlock(x, 2, z, Blocks.WATER);
                    h.setBlock(x, 3, z, Blocks.WATER);
                }
                if (hazard == 3)
                    for (int y =
                                    h.getLevel().getMinBuildHeight()
                                            - h.absolutePos(BlockPos.ZERO).getY();
                            y <= 0;
                            y++) h.setBlock(x, y, z, Blocks.AIR);
            }
        for (int x = 9; x <= 15; x++)
            for (int z = 9; z <= 15; z++) h.setBlock(x, 14, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 12, 12);
        b.setPos(h.absoluteVec(new Vec3(12, 15, 12)));
        b.setNoAi(true);
        b.setOnGround(true);
        var p = SamuraiGameTests.player(h, 18, 1, 12);
        p.setNoGravity(true);
        b.testPursuit(p);
        h.assertFalse(b.wallTraversal().tryStart(), "错误向火/深水/虚空下跳");
        b.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 550, batch = "r47_snow1")
    public static void snow_one_layer(GameTestHelper h) {
        stairs(h, 1, 3);
    }

    @GameTest(template = "arena", timeoutTicks = 550, batch = "r47_short")
    public static void short_snow_treads(GameTestHelper h) {
        stairs(h, 1, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 550, batch = "r47_mixed")
    public static void mixed_snow_depth(GameTestHelper h) {
        stairs(h, 0, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "r47_shallow")
    public static void shallow_water_crossing(GameTestHelper h) {
        hazard(h, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "r47_deep")
    public static void deep_water_detour(GameTestHelper h) {
        hazard(h, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "r47_fire")
    public static void fire_detour(GameTestHelper h) {
        hazard(h, 2);
    }

    private static void hazard(GameTestHelper h, int kind) {
        SamuraiGameTests.floor(h);
        h.getLevel()
                .getGameRules()
                .getRule(net.minecraft.world.level.GameRules.RULE_DOFIRETICK)
                .set(false, h.getLevel().getServer());
        int width = kind == 0 ? 23 : 16;
        for (int x = 0; x <= width; x++)
            for (int z = 9; z <= 12; z++) {
                if (kind == 0) h.setBlock(x, 1, z, Blocks.WATER);
                if (kind == 1) {
                    for (int y = -2; y <= 1; y++) h.setBlock(x, y, z, Blocks.WATER);
                    h.setBlock(x, -3, z, Blocks.STONE);
                }
                if (kind == 2) {
                    h.setBlock(x, 0, z, Blocks.NETHERRACK);
                    h.setBlock(x, 1, z, Blocks.FIRE);
                }
            }
        var b = SamuraiGameTests.boss(h, 12.5, 4);
        var p = SamuraiGameTests.player(h, 12.5, 1, 20);
        b.testPursuit(p);
        boolean[] crossed = {false};
        h.onEachTick(
                () -> {
                    Vec3 at = h.relativeVec(b.position());
                    if (at.z > 9 && at.z < 13) {
                        crossed[0] = true;
                        if (kind != 0)
                            h.assertTrue(at.x >= (kind == 1 ? 17.01 : 17.7), "没有绕开火/深水: " + at);
                    }
                });
        observe(h, b, p, 580, 0);
        h.runAtTickTime(579, () -> h.assertTrue(crossed[0], "没有越过障碍带"));
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "r47_slabs")
    public static void slab_and_stair_slope(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 0; x < 24; x++)
            for (int z = 5; z < 24; z++) {
                int half = Math.min(10, (z - 5) / 2 + 1), full = half / 2;
                for (int y = 1; y <= full; y++) h.setBlock(x, y, z, Blocks.STONE);
                if (half % 2 == 1) h.setBlock(x, full + 1, z, Blocks.STONE_SLAB);
            }
        var b = SamuraiGameTests.boss(h, 12.13, 2.37);
        b.setNoAi(true);
        Vec3 goal = h.absoluteVec(new Vec3(12.5, 6, 23.25));
        h.onEachTick(
                () -> {
                    b.terrainNavigation().chaseWaypoint(goal, 1.4);
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)), "半砖穿模");
                });
        h.runAtTickTime(
                580,
                () -> {
                    h.assertTrue(b.position().distanceTo(goal) < .15, "未走完全部半砖台阶: " + b.position());
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 150, batch = "r47_collapse")
    public static void idle_support_removed(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 9; x <= 15; x++)
            for (int z = 7; z <= 13; z++) h.setBlock(x, 8, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 12, 10);
        b.setPos(h.absoluteVec(new Vec3(12, 9, 10)));
        h.runAtTickTime(
                10,
                () -> {
                    for (int x = 9; x <= 15; x++)
                        for (int z = 7; z <= 13; z++) h.setBlock(x, 8, z, Blocks.AIR);
                });
        h.runAtTickTime(
                65,
                () -> {
                    h.assertTrue(b.getY() <= h.absoluteVec(new Vec3(0, 1.1, 0)).y, "支撑消失仍然悬空");
                    b.discard();
                    h.succeed();
                });
    }

    private static void stairs(GameTestHelper h, int layers, int width) {
        SamuraiGameTests.floor(h);
        for (int x = 0; x < 24; x++)
            for (int z = 0; z < 24; z++) {
                int top = Math.min(5, Math.max(0, (z - 5) / width + 1));
                for (int y = 1; y <= top; y++) h.setBlock(x, y, z, Blocks.STONE);
                h.setBlock(
                        new BlockPos(x, top + 1, z),
                        Blocks.SNOW
                                .defaultBlockState()
                                .setValue(
                                        SnowLayerBlock.LAYERS,
                                        layers == 0 ? 1 + (x + z) % 8 : layers));
            }
        var b = SamuraiGameTests.boss(h, 12.27, 1.39);
        b.setPos(b.position().add(0, 1, 0));
        var p = SamuraiGameTests.player(h, 12.5, 6.875, 21);
        b.testPursuit(p);
        observe(h, b, p, 490, 4);
    }

    @GameTest(template = "arena", timeoutTicks = 150, batch = "r47_airidle")
    public static void airborne_spawn_idle(GameTestHelper h) {
        air(h, false);
    }

    @GameTest(template = "arena", timeoutTicks = 150, batch = "r47_airtarget")
    public static void airborne_spawn_target(GameTestHelper h) {
        air(h, true);
    }

    private static void air(GameTestHelper h, boolean enemy) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 12.3, 8.7);
        b.setPos(b.position().add(0, 10, 0));
        var p = enemy ? SamuraiGameTests.player(h, 12.5, 1, 19) : null;
        h.runAtTickTime(
                65,
                () -> {
                    h.assertTrue(
                            b.getY() <= h.absoluteVec(new Vec3(0, 1.1, 0)).y,
                            "空中召唤没有正常落地: " + b.position() + " " + b.report());
                    b.discard();
                    if (p != null) p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 950, batch = "r47_real")
    public static void actual_snow_slope(GameTestHelper h) throws Exception {
        saved(h, 0);
    }

    @GameTest(template = "arena", timeoutTicks = 950, batch = "r47_realair")
    public static void actual_snow_air_spawn(GameTestHelper h) throws Exception {
        saved(h, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 950, batch = "r47_realledge")
    public static void actual_snow_low_ledge(GameTestHelper h) throws Exception {
        saved(h, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 950, batch = "r47_real_two")
    public static void actual_snow_phase_two(GameTestHelper h) throws Exception {
        saved(h, 3);
    }

    private static void saved(GameTestHelper h, int mode) throws Exception {
        SamuraiGameTests.floor(h);
        var arr =
                com.google.gson.JsonParser.parseReader(
                                new java.io.InputStreamReader(
                                        FocusedR47Tests.class.getResourceAsStream(
                                                "/r47_scene.json"),
                                        java.nio.charset.StandardCharsets.UTF_8))
                        .getAsJsonArray();
        var registry = h.getLevel().holderLookup(Registries.BLOCK);
        for (var e : arr) {
            var a = e.getAsJsonArray();
            h.setBlock(
                    new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()),
                    BlockStateParser.parseForBlock(registry, a.get(3).getAsString(), false)
                            .blockState());
        }
        Vec3 start =
                mode == 2
                        ? new Vec3(1.5, 19, 10.4)
                        : new Vec3(18.28719853, mode == 1 ? 9.921843 : 4, 11.1693036);
        var b = SamuraiGameTests.boss(h, start.x, start.z);
        b.setPos(h.absoluteVec(start));
        var p =
                mode == 2
                        ? SamuraiGameTests.player(h, 1.866074, 22, 6.322725)
                        : SamuraiGameTests.player(h, 8.086506, 19.207202, 15.7);
        if (mode == 3) {
            b.testCombat(p, 2, false);
            b.testPursuit(p);
        }
        observe(h, b, p, 880, mode == 2 ? 2.5 : mode == 1 ? 8 : 14);
    }

    private static void observe(
            GameTestHelper h,
            SamuraiEntity b,
            net.minecraft.server.level.ServerPlayer p,
            int end,
            double rise) {
        double start = b.getY();
        boolean[] high = {false}, hit = {false};
        h.onEachTick(
                () -> {
                    if (b.isRemoved()) return;
                    high[0] |= b.getY() >= start + rise;
                    hit[0] |=
                            p.getHealth() < 1000
                                    && p.getLastDamageSource() != null
                                    && p.getLastDamageSource().getEntity() == b;
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)), "穿入地形");
                    if (h.getTick() % 100 == 0)
                        GeneratedMod.LOG.info(
                                "[r47 scene] t={} pos={} gravity={} support={} {}",
                                h.getTick(),
                                b.position(),
                                !b.isNoGravity(),
                                b.terrainNavigation()
                                        .support(SamuraiNavigation.point(b.position())),
                                b.report());
                });
        h.runAtTickTime(
                end,
                () -> {
                    h.assertTrue(high[0], "没有走上雪坡: " + b.position() + " " + b.report());
                    h.assertTrue(hit[0], "没有接近并命中: " + b.report());
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }
}
