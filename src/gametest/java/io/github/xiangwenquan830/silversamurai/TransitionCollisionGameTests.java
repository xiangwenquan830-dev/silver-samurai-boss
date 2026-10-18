package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import org.joml.Vector3f;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TransitionCollisionGameTests {
    @GameTest(template = "arena", timeoutTicks = 180, batch = "transition_armed_walk")
    public static void armed_walk(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        var p = player(h, 10.5, 1, 17);
        b.testPursuit(p);
        b.currentPose(0);
        b.requestSecondPhase();
        h.assertTrue(
                b.phase() == 2 && b.state() == SamuraiEntity.TRANSITION,
                "armed phase transition did not start");
        h.onEachTick(
                () -> {
                    for (var m : b.currentPose(.5f).matrices()) {
                        var v = m.transformPosition(new Vector3f());
                        h.assertTrue(v.isFinite(), "nonfinite transition pose");
                    }
                });
        h.runAtTickTime(
                RedBladeTiming.totalTicks() + 2,
                () -> {
                    h.assertTrue(
                            b.phase() == 2 && b.state() != SamuraiEntity.TRANSITION,
                            "transition failed to finish");
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 200, batch = "transition_in_crowd")
    public static void in_crowd(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            var e = h.spawn(EntityType.HUSK, new BlockPos(10, 1, 10));
            e.setPos(
                    h.absoluteVec(new Vec3(10.5 + Math.cos(a) * 1.5, 1, 10.5 + Math.sin(a) * 1.5)));
            if (i == 0) b.testPursuit(e);
        }
        double y = b.getY();
        h.runAtTickTime(8, () -> b.requestSecondPhase());
        h.onEachTick(
                () -> {
                    h.assertTrue(b.getY() <= y + .08, "crowd lifted boss above solid ground");
                    b.currentPose(.5f);
                });
        // The phase request can wait for an active crowd-clearing kick to recover.
        // Allow that bounded preparation, then the authored grip and red-charge duration.
        h.runAtTickTime(
                50 + RedBladeTiming.totalTicks(),
                () -> {
                    h.assertTrue(
                            b.phase() == 2 && b.state() != SamuraiEntity.TRANSITION,
                            "crowd transition did not complete age="
                                    + b.stateAge(0)
                                    + " "
                                    + b.report());
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 30, batch = "solid_living_collision")
    public static void solid_living(GameTestHelper h) {
        floor(h);
        var b = boss(h, 6.5, 10.5);
        b.setNoAi(true);
        b.setOnGround(true);
        for (int i = 0; i < 8; i++) {
            var e = new SolidChicken(h.getLevel());
            e.setAge(-24000);
            e.setNoAi(true);
            e.setPos(h.absoluteVec(new Vec3(7.5 + i * .45, 1, 10.5)));
            h.getLevel().addFreshEntity(e);
            h.assertFalse(b.canCollideWith(e), "solid living mob is still a step surface");
            b.push(e);
        }
        h.assertFalse(b.isPushable(), "boss accepts crowd pushing");
        double y = b.getY(), x = b.getX();
        for (int i = 0; i < 30; i++) b.move(MoverType.SELF, new Vec3(.12, 0, 0));
        h.assertTrue(
                Math.abs(b.getY() - y) < .001 && b.getX() > x + 3,
                "solid living collision lifted or blocked boss");
        // Still collide with real blocks; do not solve the bug by enabling noPhysics.
        h.setBlock(12, 1, 10, net.minecraft.world.level.block.Blocks.BEDROCK);
        b.move(MoverType.SELF, new Vec3(3, 0, 0));
        h.assertTrue(
                b.getX() < h.absoluteVec(new Vec3(12, 1, 10)).x, "boss passed through a real wall");
        b.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 20, batch = "phase_alias_fallback")
    public static void aliases(GameTestHelper h) {
        floor(h);
        var rig = SamuraiRig.get(2);
        for (String name : new String[] {"armed_idle", "armed_walk", "missing_old_animation"})
            for (var m : rig.pose(name, .3f).matrices())
                h.assertTrue(
                        m.transformPosition(new Vector3f()).isFinite(),
                        "old phase animation alias crashed");
        h.succeed();
    }

    private static final class SolidChicken extends Chicken {
        SolidChicken(Level level) {
            super(EntityType.CHICKEN, level);
        }

        @Override
        public boolean canBeCollidedWith() {
            return true;
        }
    }
}
