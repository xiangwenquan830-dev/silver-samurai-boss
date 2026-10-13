package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import org.joml.Matrix4f;
import static cn.blockforge.generated.bossmodbossmodbossm.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DrawGameTests {
    @GameTest(template = "arena", timeoutTicks = 100, batch = "r37_standing_draw")
    public static void standing_draw(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 11.5);
        b.setTarget(p);
        Vec3 start = b.position();
        boolean[] sampled = {false};
        h.onEachTick(
                () -> {
                    if (b.state() == SamuraiEntity.DRAW) {
                        h.assertTrue(
                                b.clip().equals(SamuraiRig.DRAW_STILL),
                                "stationary boss selected walking draw");
                        h.assertTrue(
                                b.position().subtract(start).horizontalDistance() < .02,
                                "standing draw translated boss");
                        if (b.stateAge(0) > .4f) {
                            var rig = SamuraiRig.get(1);
                            var expected = rig.pose(SamuraiRig.DRAW_STILL, b.visualTime(0));
                            var actual = b.currentPose(0);
                            for (String name : new String[] {"左腿", "右腿", "行走_膝_L", "行走_膝_R"}) {
                                int bone = rig.bone(name);
                                h.assertTrue(
                                        actual.matrices()[bone].equals(
                                                expected.matrices()[bone], .0001f),
                                        "standing draw mixed in walking: " + name);
                            }
                            var tag = new CompoundTag();
                            b.addAdditionalSaveData(tag);
                            var copy = boss(h, 4.5, 4.5);
                            copy.readAdditionalSaveData(tag);
                            copy.setNoAi(true);
                            h.assertTrue(
                                    copy.clip().equals(SamuraiRig.DRAW_STILL)
                                            && Math.abs(copy.visualTime(0) - b.visualTime(0))
                                                    < .051,
                                    "reload lost standing clip/time");
                            copy.discard();
                            sampled[0] = true;
                        }
                    }
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            sampled[0] && b.weaponsDrawn() && b.state() != SamuraiEntity.DRAW,
                            "standing extraction did not complete");
                    h.assertTrue(h.getTick() < 50, "standing draw retained old 2.8 second delay");
                    p.discard();
                    b.discard();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 120, batch = "r37_walking_draw_stop")
    public static void walking_draw_stop(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 3.5);
        var p = player(h, 10.5, 1, 21.5);
        b.setTarget(p);
        long[] stopped = {-1};
        float[] clock = {0};
        boolean[] checked = {false};
        h.onEachTick(
                () -> {
                    if (stopped[0] < 0 && b.state() == SamuraiEntity.DRAW && b.stateAge(0) > .35f) {
                        h.assertTrue(
                                b.clip().equals(SamuraiRig.DRAW),
                                "moving boss did not select moving draw");
                        h.assertTrue(b.drawGait(0) > .5, "moving draw gait absent");
                        stopped[0] = h.getTick();
                        clock[0] = b.visualTime(0);
                        p.setPos(b.position().add(0, 0, 4));
                    }
                    if (stopped[0] >= 0 && !checked[0] && h.getTick() >= stopped[0] + 8) {
                        h.assertTrue(
                                b.state() == SamuraiEntity.DRAW && b.clip().equals(SamuraiRig.DRAW),
                                "stop restarted/replaced extraction");
                        h.assertTrue(
                                Math.abs(
                                                b.visualTime(0)
                                                        - clock[0]
                                                        - (h.getTick() - stopped[0]) / 20f)
                                        < .051,
                                "draw clock jumped");
                        h.assertTrue(b.drawGait(0) == 0, "stopped draw continues walking cycle");
                        var rig = SamuraiRig.get(1);
                        var actual = b.currentPose(0);
                        var neutral = rig.pose("", 0);
                        for (String name :
                                new String[] {"左腿", "右腿", "行走_膝_L", "行走_膝_R", "行走_踝_L", "行走_踝_R"}) {
                            int i = rig.bone(name);
                            h.assertTrue(
                                    actual.matrices()[i].equals(neutral.matrices()[i], .0001f),
                                    "stopped leg still cycling: " + name);
                        }
                        checked[0] = true;
                    }
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            checked[0] && b.weaponsDrawn() && b.state() != SamuraiEntity.DRAW,
                            "paused moving draw did not finish");
                    p.discard();
                    b.discard();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 20, batch = "r37_draw_pose")
    public static void stopped_draw_grip(GameTestHelper h) {
        var rig = SamuraiRig.get(1);
        int upper = rig.bone("腰腹_提身控制");
        for (float time = .3f; time < 2.8f; time += .05f)
            for (float gait = 0; gait <= 1; gait += .25f) {
                var raw = rig.pose(SamuraiRig.DRAW, time);
                var adjusted = DrawMotion.plantFeet(rig, rig.pose(SamuraiRig.DRAW, time), gait);
                Matrix4f delta =
                        new Matrix4f(adjusted.matrices()[upper])
                                .mul(new Matrix4f(raw.matrices()[upper]).invert());
                for (String name : new String[] {"背挂武士刀_L", "背挂武士刀_R"}) {
                    int i = rig.bone(name);
                    h.assertTrue(
                            new Matrix4f(delta)
                                    .mul(raw.matrices()[i])
                                    .equals(adjusted.matrices()[i], .002f),
                            "gait suppression changed sword relative to upper body");
                }
            }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r37_draw_lost_target")
    public static void standing_draw_lost_target(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 7.5);
        var p = player(h, 10.5, 1, 11.5);
        b.setTarget(p);
        boolean[] lost = {false};
        h.onEachTick(
                () -> {
                    if (!lost[0] && b.state() == SamuraiEntity.DRAW && b.stateAge(0) > .4f) {
                        p.discard();
                        lost[0] = true;
                    }
                });
        h.succeedWhen(
                () -> {
                    h.assertTrue(
                            lost[0] && b.weaponsDrawn() && b.state() != SamuraiEntity.DRAW,
                            "lost target interrupted standing extraction");
                    h.assertTrue(h.getTick() < 50, "lost target retained wrong draw duration");
                    b.discard();
                });
    }
}
