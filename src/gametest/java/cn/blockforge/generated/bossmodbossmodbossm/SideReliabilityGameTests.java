package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import static cn.blockforge.generated.bossmodbossmodbossm.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public class SideReliabilityGameTests {
    @GameTest(template = "arena", timeoutTicks = 60, batch = "r41_planner")
    public static void planner_limits(GameTestHelper h) {
        floor(h);
        var b = boss(h, 11.5, 11.5);
        var p = player(h, 11.5, 1, 15);
        b.setNoAi(true);
        double one = BossConfig.RANGE_ONE.get(), two = BossConfig.RANGE_TWO.get();
        long clock = 10000;
        int selected = 0, edge = 0;
        try {
            for (int phase = 1; phase <= 2; phase++) {
                b.testCombat(p, phase, false);
                b.testPursuit(p);
                b.setYRot(0);
                h.assertTrue(
                        SideIntercept.turnBudget(phase) == 24,
                        "turn budget includes nonexistent entry tick");
                for (int deg = -180; deg <= 180; deg += 5) {
                    double a = Math.toRadians(deg);
                    Vec3 pos = b.position().add(Math.sin(a) * 3.7, 0, Math.cos(a) * 3.7);
                    b.attackTracking().sample(null, clock++);
                    p.setPos(pos.add(-.5, 0, 0));
                    b.attackTracking().sample(p, clock++);
                    p.setPos(pos);
                    b.attackTracking().sample(p, clock++);
                    var plan = SideIntercept.choose(b, p);
                    if (plan != null) {
                        selected++;
                        float yaw = 0;
                        for (int tick = 1; tick <= 20; tick++)
                            if (SideIntercept.canTurn(phase, tick * .05f))
                                yaw = Mth.approachDegrees(yaw, plan.yaw(), SideIntercept.TURN_STEP);
                        h.assertTrue(
                                Math.abs(Mth.wrapDegrees(plan.yaw() - yaw)) < .001,
                                "selected angle cannot be reached before cut");
                        if (Math.abs(plan.yaw()) > 12) edge++;
                    }
                    BossConfig.RANGE_ONE.set(1.0);
                    BossConfig.RANGE_TWO.set(1.0);
                    h.assertTrue(
                            SideIntercept.choose(b, p) == null,
                            "selected a blade contact outside configured range");
                    BossConfig.RANGE_ONE.set(one);
                    BossConfig.RANGE_TWO.set(two);
                }
            }
            h.assertTrue(
                    selected > 0 && edge > 0,
                    "planner fixture never exercised boundary selections");
            // Vertical separation also contributes to the damage range; horizontal distance alone
            // is insufficient.
            var high = new net.minecraft.world.phys.AABB(-.1, 3, -.1, .1, 4, .1);
            h.assertTrue(
                    BladeCombat.contactInRange(
                                    high, new Vec3(0, 2, 0), new Vec3(0, 5, 0), Vec3.ZERO, 2)
                            .isEmpty(),
                    "height bypassed range");
            GeneratedMod.LOG.info(
                    "[r41 planner] reachable plans={} boundary plans={}", selected, edge);
        } finally {
            BossConfig.RANGE_ONE.set(one);
            BossConfig.RANGE_TWO.set(two);
            p.discard();
            b.discard();
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 55, batch = "r41_live1l")
    public static void live_one_left(GameTestHelper h) {
        live(h, 1, true);
    }

    @GameTest(template = "arena", timeoutTicks = 55, batch = "r41_live1r")
    public static void live_one_right(GameTestHelper h) {
        live(h, 1, false);
    }

    @GameTest(template = "arena", timeoutTicks = 55, batch = "r41_live2l")
    public static void live_two_left(GameTestHelper h) {
        live(h, 2, true);
    }

    @GameTest(template = "arena", timeoutTicks = 55, batch = "r41_live2r")
    public static void live_two_right(GameTestHelper h) {
        live(h, 2, false);
    }

    private static void live(GameTestHelper h, int phase, boolean left) {
        floor(h);
        var b = boss(h, 11.5, 9.5);
        var p = player(h, 11.5, 1, 13);
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)
                .setBaseValue(1);
        b.testSideSweep(p, phase, left, -24);
        String clip = b.clip();
        float contactTime = SideIntercept.start(phase) + .065f;
        var blade = SideIntercept.blades(phase, clip, contactTime, -24).get(2);
        Vec3 contact =
                SamuraiRig.world(blade.base().lerp(blade.tip(), .5), b.position(), b.getYRot())
                        .add(0, -.8, 0);
        Vec3 travel = new Vec3(left ? .26 : -.26, 0, 0);
        Vec3 initial = contact.subtract(travel.scale(contactTime * 20));
        p.setPos(initial);
        // Preserve the selected cut/yaw, but initialize tracking from the fixture's actual starting
        // location.
        try {
            var begin =
                    SamuraiEntity.class.getDeclaredMethod(
                            "beginSideSweep", SideIntercept.Plan.class);
            begin.setAccessible(true);
            begin.invoke(b, new SideIntercept.Plan(clip, -24, b.getYRot()));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        float[] previous = {p.getHealth()}, damage = {0};
        int[] impacts = {0};
        boolean[] finished = {false};
        float initialYaw = b.getYRot();
        h.onEachTick(
                () -> {
                    if (finished[0]) return;
                    if (b.state() == SamuraiEntity.SIDE_SWEEP) {
                        h.assertTrue(
                                clip.equals(b.clip()), "active cut switched to a different hand");
                        if (p.getHealth() < previous[0]) {
                            damage[0] += previous[0] - p.getHealth();
                            impacts[0]++;
                        }
                        h.assertTrue(
                                Math.abs(Mth.wrapDegrees(b.getYRot() - initialYaw))
                                        <= SideIntercept.turnBudget(phase) + .001,
                                "tracking exceeded preparation turn budget");
                        p.setPos(initial.add(travel.scale(b.age(0) * 20)));
                        p.setDeltaMovement(Vec3.ZERO);
                    } else {
                        finished[0] = true;
                        b.setNoAi(true);
                    }
                    previous[0] = p.getHealth();
                });
        h.runAtTickTime(
                25,
                () -> {
                    GeneratedMod.LOG.info(
                            "[r41 live cut] phase={} left={} damage={} impacts={}",
                            phase,
                            left,
                            damage[0],
                            impacts[0]);
                    h.assertTrue(
                            finished[0] && damage[0] > 0 && impacts[0] == 1,
                            "selected side clip failed its own live moving-target hit, or hit repeatedly");
                    p.discard();
                    b.discard();
                    h.succeed();
                });
    }
}
