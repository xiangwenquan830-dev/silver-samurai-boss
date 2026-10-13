package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.GameType;
import com.google.gson.JsonParser;
import java.nio.file.*;

@GameTestHolder("silver_samurai_r52")
@PrefixGameTestTemplate(false)
public class FocusedR52Tests {
    @GameTest(template = "arena", timeoutTicks = 100, batch = "r52_rig1")
    public static void rig_one_equivalent(GameTestHelper h) throws Exception {
        rig(h, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r52_rig2")
    public static void rig_two_equivalent(GameTestHelper h) throws Exception {
        rig(h, 2);
    }

    private static void rig(GameTestHelper h, int phase) throws Exception {
        SamuraiRig original;
        try (var reader =
                Files.newBufferedReader(Path.of("../model-source/phase" + phase + ".bbmodel"))) {
            original = new SamuraiRig(phase, JsonParser.parseReader(reader).getAsJsonObject());
        }
        var runtime = SamuraiRig.get(phase);
        h.assertTrue(
                original.bones.size() == runtime.bones.size()
                        && original.cubes.size() == runtime.cubes.size()
                        && original.clips.keySet().equals(runtime.clips.keySet()),
                "运行数据丢失结构或动画");
        int poses = 0, keys = 0;
        for (var c : original.clips.values()) {
            var other = runtime.clips.get(c.name);
            h.assertTrue(
                    c.length == other.length
                            && c.loop == other.loop
                            && c.tracks.keySet().equals(other.tracks.keySet()),
                    "动画元数据变化");
            for (var id : c.tracks.keySet())
                for (String channel : new String[] {"rotation", "position", "scale"}) {
                    var a = c.tracks.get(id).channel(channel);
                    var b = other.tracks.get(id).channel(channel);
                    h.assertTrue(a.equals(b), "关键帧数值、时间或插值改变: " + c.name + channel);
                    keys += a.size();
                }
            for (float time = 0; time <= c.length + .099f; time += .1f) {
                float t = Math.min(time, c.length);
                var a = original.pose(c.name, t);
                var b = runtime.pose(c.name, t);
                for (int i = 0; i < a.matrices().length; i++)
                    h.assertTrue(
                            a.matrices()[i].equals(b.matrices()[i])
                                    && a.visible()[i] == b.visible()[i],
                            "采样骨骼/显示不同: " + c.name + " @" + t);
                poses++;
            }
        }
        for (int i = 0; i < original.cubes.size(); i++) {
            var a = original.cubes.get(i);
            var b = runtime.cubes.get(i);
            h.assertTrue(
                    a.name.equals(b.name)
                            && a.uuid.equals(b.uuid)
                            && a.bone == b.bone
                            && a.debris == b.debris
                            && a.from.equals(b.from)
                            && a.to.equals(b.to)
                            && a.origin.equals(b.origin)
                            && a.rotation.equals(b.rotation)
                            && a.faces.size() == b.faces.size(),
                    "方块或挂接变化");
            for (var face : a.faces) {
                var other =
                        b.faces.stream()
                                .filter(f -> f.side().equals(face.side()))
                                .findFirst()
                                .orElseThrow();
                h.assertTrue(
                        java.util.Arrays.equals(face.uv(), other.uv())
                                && face.texture() == other.texture()
                                && face.rotation() == other.rotation(),
                        "UV/材质变化");
            }
        }
        GeneratedMod.LOG.info(
                "[r52 equivalence] phase={} exactKeys={} exactPoses={}", phase, keys, poses);
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 35, batch = "r52_senses")
    public static void snapshot_reuses_query_but_not_hostility(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 12);
        b.setNoAi(true);
        var p = SamuraiGameTests.player(h, 9, 1, 12);
        h.runAtTickTime(
                3,
                () -> {
                    long before = b.senses().scans();
                    b.senses().nearby();
                    new Targeting(b).select(SamuraiRig.A);
                    b.senses().nearby();
                    h.assertTrue(b.senses().scans() - before <= 1, "同刻重复世界查询");
                    p.setGameMode(GameType.CREATIVE);
                    h.assertTrue(new Targeting(b).select(SamuraiRig.A) == null, "缓存了过时敌对状态");
                    b.setPos(b.position().add(1, 0, 0));
                    long scans = b.senses().scans();
                    b.senses().nearby();
                    h.assertTrue(b.senses().scans() == scans + 1, "移动后仍使用旧范围");
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 35, batch = "r52_diagnostic")
    public static void raised_diagnostic_matches_live_rule(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 12);
        b.setNoAi(true);
        var p = SamuraiGameTests.player(h, 8, 5, 12);
        p.setNoGravity(true);
        h.assertTrue(
                b.perception().lane(p) && !b.perception().laneFailure(p).startsWith("高度差="),
                "诊断仍采用旧的1.4格拒绝线");
        b.discard();
        p.discard();
        h.succeed();
    }
}
