package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.Registries;

@GameTestHolder("silver_samurai_r46")
@PrefixGameTestTemplate(false)
public class FocusedR46Tests {
    @GameTest(template = "arena", timeoutTicks = 1250, batch = "r46_saved")
    public static void saved_snow_pillar(GameTestHelper h) throws Exception {
        saved(h, 1, true);
    }

    @GameTest(template = "arena", timeoutTicks = 1250, batch = "r46_saved_two")
    public static void saved_snow_pillar_two(GameTestHelper h) throws Exception {
        saved(h, 2, true);
    }

    @GameTest(template = "arena", timeoutTicks = 1250, batch = "r46_saved_natural")
    public static void saved_snow_pillar_natural(GameTestHelper h) throws Exception {
        saved(h, 1, false);
    }

    private static void saved(GameTestHelper h, int phase, boolean armed) throws Exception {
        SamuraiGameTests.floor(h);
        var arr =
                com.google.gson.JsonParser.parseReader(
                                new java.io.InputStreamReader(
                                        FocusedR46Tests.class.getResourceAsStream(
                                                "/r46_scene.json"),
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
        var b = SamuraiGameTests.boss(h, 12.140958701777, 11.469012506192);
        b.setPos(h.absoluteVec(new Vec3(12.140958701777, 22, 11.469012506192)));
        var p = SamuraiGameTests.player(h, 7.68608479645, 37, 8.946472928);
        if (phase == 2) b.testCombat(p, 2, false);
        if (armed) b.testPursuit(p);
        observe(h, b, p, 1100, 8);
    }

    @GameTest(template = "arena", timeoutTicks = 650, batch = "r46_pillar")
    public static void narrow_pillar(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int y = 1; y <= 5; y++)
            for (int z = 10; z <= 11; z++) h.setBlock(12, y, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 12.5, 5.5);
        var p = SamuraiGameTests.player(h, 12.5, 6, 10.5);
        b.testPursuit(p);
        observe(h, b, p, 570, 4.8);
    }

    @GameTest(template = "arena", timeoutTicks = 600, batch = "r46_snowstep")
    public static void snow_stairs(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 0; x < 24; x++)
            for (int z = 4; z < 24; z++) {
                int top = Math.min(5, (z - 4) / 3 + 1);
                for (int y = 1; y <= top; y++) h.setBlock(x, y, z, Blocks.STONE);
                h.setBlock(
                        new BlockPos(x, top + 1, z),
                        Blocks.SNOW
                                .defaultBlockState()
                                .setValue(
                                        net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 2));
            }
        var b = SamuraiGameTests.boss(h, 12.5, 2);
        var p = SamuraiGameTests.player(h, 12.5, 6.125, 20.5);
        b.testPursuit(p);
        observe(h, b, p, 550, 4.8);
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
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)), "穿入实体地形");
                    if (h.getTick() % 100 == 0)
                        GeneratedMod.LOG.info(
                                "[r46 scene] tick={} pos={} support={} ground={} {}",
                                h.getTick(),
                                b.position(),
                                b.terrainNavigation()
                                        .support(SamuraiNavigation.point(b.position())),
                                b.onGround(),
                                b.report());
                });
        h.runAtTickTime(
                end,
                () -> {
                    h.assertTrue(high[0], "未爬上目标高度: " + b.report());
                    h.assertTrue(hit[0] && p.getHealth() < 1000, "没有实际接近并命中目标: " + b.report());
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }
}
