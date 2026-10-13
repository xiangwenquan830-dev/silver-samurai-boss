package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import com.google.gson.*;
import java.io.*;

@GameTestHolder("silver_samurai_r59")
@PrefixGameTestTemplate(false)
public class FocusedR59Tests {
    @GameTest(template = "arena", timeoutTicks = 850, batch = "r59_two")
    public static void roof_two(GameTestHelper h) {
        scene(h, 2);
    }

    @GameTest(template = "arena", timeoutTicks = 850, batch = "r59_one")
    public static void roof_one(GameTestHelper h) {
        scene(h, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 20, batch = "r59_footprint")
    public static void sloped_landing_uses_full_body(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 8; x <= 16; x++)
            for (int z = 10; z <= 19; z++) {
                for (int y = 1; y <= 5; y++) h.setBlock(x, y, z, Blocks.STONE);
                h.setBlock(x, 6, z, Blocks.DARK_OAK_SLAB);
            }
        for (int z = 10; z <= 19; z++)
            h.setBlock(
                    11,
                    6,
                    z,
                    Blocks.DARK_OAK_STAIRS
                            .defaultBlockState()
                            .setValue(StairBlock.FACING, net.minecraft.core.Direction.WEST));
        var b = SamuraiGameTests.boss(h, 11.75, 6.6);
        b.setNoAi(true);
        var wall =
                new WallGeometry.Contact(h.absoluteVec(new Vec3(11.75, 4, 10)), new Vec3(0, 0, 1));
        var roof = WallGeometry.roof(b, wall, b.getY());
        h.assertTrue(roof != null, "wide body rejected stair landing");
        h.assertTrue(
                roof.y >= h.absoluteVec(new Vec3(0, 7, 0)).y - .01,
                "landing clips uphill half of stair");
        b.discard();
        h.succeed();
    }

    private static <T extends Comparable<T>> BlockState prop(
            BlockState s, Property<T> p, String v) {
        return p.getValue(v).map(x -> s.setValue(p, x)).orElse(s);
    }

    private static void scene(GameTestHelper h, int phase) {
        SamuraiGameTests.floor(h);
        JsonObject data =
                JsonParser.parseReader(
                                new InputStreamReader(
                                        FocusedR59Tests.class.getResourceAsStream("/roof-r59.json"),
                                        java.nio.charset.StandardCharsets.UTF_8))
                        .getAsJsonObject();
        var palette = new java.util.ArrayList<BlockState>();
        for (var element : data.getAsJsonArray("palette")) {
            var def = element.getAsJsonObject();
            String name = def.get("Name").getAsString();
            if (!name.startsWith("minecraft:"))
                name =
                        name.contains("hedge")
                                ? "minecraft:dark_oak_leaves"
                                : name.contains("window_pane")
                                        ? "minecraft:glass_pane"
                                        : name.equals("create:cut_calcite_slab")
                                                ? "minecraft:smooth_quartz_slab"
                                                : name.equals("create:cut_calcite")
                                                        ? "minecraft:calcite"
                                                        : name.equals(
                                                                        "embellishcraft:polished_paving")
                                                                ? "minecraft:stone"
                                                                : "minecraft:air";
            var s = BuiltInRegistries.BLOCK.get(new ResourceLocation(name)).defaultBlockState();
            if (def.has("Properties"))
                for (var e : def.getAsJsonObject("Properties").entrySet()) {
                    var p = s.getBlock().getStateDefinition().getProperty(e.getKey());
                    if (p != null) s = prop(s, p, e.getValue().getAsString());
                }
            if (s.hasProperty(BlockStateProperties.PERSISTENT))
                s = s.setValue(BlockStateProperties.PERSISTENT, true);
            palette.add(s);
        }
        for (int x = 0; x < 34; x++)
            for (int z = 0; z < 34; z++)
                for (int y = 0; y < 23; y++) h.setBlock(x, y, z, Blocks.AIR);
        for (var cell : data.getAsJsonArray("blocks")) {
            var a = cell.getAsJsonArray();
            h.setBlock(
                    a.get(0).getAsInt(),
                    a.get(1).getAsInt(),
                    a.get(2).getAsInt(),
                    palette.get(a.get(3).getAsInt()));
        }
        var b = SamuraiGameTests.boss(h, 14.695, 19.182);
        b.setPos(h.absoluteVec(new Vec3(14.695, 5, 19.182)));
        var p = SamuraiGameTests.player(h, 15.5, 10.5, 13.5);
        b.testCombat(p, phase, false);
        b.testPursuit(p);
        double base = b.getY();
        boolean[] reached = {false}, restored = {false};
        h.onEachTick(
                () -> {
                    h.assertTrue(
                            h.getLevel().noCollision(b, b.getBoundingBox().deflate(.02)),
                            "roof pursuit clipped through solid blocks");
                    p.setPos(h.absoluteVec(new Vec3(15.5, 10.5, 13.5)));
                    p.setDeltaMovement(Vec3.ZERO);
                    if (phase == 2 && b.pursuitJump().airborne() && !restored[0]) {
                        var tag = new net.minecraft.nbt.CompoundTag();
                        Vec3 motion = b.getDeltaMovement();
                        b.pursuitJump().save(tag);
                        b.pursuitJump().load(tag);
                        h.assertTrue(b.pursuitJump().airborne(), "roof jump lost on save/load");
                        h.assertTrue(
                                b.getDeltaMovement().distanceTo(motion) < .00001,
                                "roof jump velocity lost on reload");
                        restored[0] = true;
                    }
                    if (b.getY() >= base + 5.4 && b.onGround()) reached[0] = true;
                    if (h.getTick() % 80 == 0)
                        GeneratedMod.LOG.info(
                                "[R59 ROOF] t={} pos={} wall={} {}",
                                h.getTick(),
                                b.position(),
                                b.wallTraversal().status(),
                                b.report());
                });
        h.runAtTickTime(
                800,
                () -> {
                    h.assertTrue(reached[0], "never reached roof: " + b.report());
                    if (phase == 2)
                        h.assertTrue(restored[0], "roof jump reload scenario not exercised");
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }
}
