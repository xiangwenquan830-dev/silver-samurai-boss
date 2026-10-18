package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

@GameTestHolder("silver_samurai_r43")
@PrefixGameTestTemplate(false)
public class FocusedR43Tests {
    @GameTest(template = "arena", timeoutTicks = 60, batch = "r43_budget")
    public static void budget(GameTestHelper h) {
        RangedDamageBudget b = new RangedDamageBudget();
        float total = 0;
        for (int i = 0; i < 100; i++) total += b.accept(100 + i / 10, 7, 20);
        h.assertTrue(total == 20, "滚动窗口内超过20");
        h.assertTrue(b.accept(119, 50, 20) == 0, "窗口过早补满");
        CompoundTag tag = new CompoundTag();
        b.save(tag, 119);
        RangedDamageBudget copy = new RangedDamageBudget();
        copy.load(tag, 400);
        h.assertTrue(copy.accept(400, 30, 20) == 0, "重载重置限额");
        h.assertTrue(copy.accept(401, 50, 20) == 20, "窗口未恢复");
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "r43_fall")
    public static void fall_immune(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 5);
        b.setNoAi(true);
        float old = b.getHealth();
        b.causeFallDamage(100, 1, b.damageSources().fall());
        b.hurt(b.damageSources().fall(), 10000);
        h.assertTrue(b.getHealth() == old, "摔落来源仍扣血");
        b.discard();
        h.succeed();
    }

    private static SamuraiEntity setup(GameTestHelper h, int phase) {
        SamuraiGameTests.floor(h);
        for (int x = 7; x <= 16; x++)
            for (int z = 9; z <= 16; z++)
                for (int y = 1; y <= 8; y++) h.setBlock(x, y, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 11.5, 5.5);
        var p = SamuraiGameTests.player(h, 11.5, 9, 12.5);
        b.testCombat(p, phase, false);
        b.testPursuit(p);
        return b;
    }

    @GameTest(template = "arena", timeoutTicks = 420, batch = "r43_wall1")
    public static void wall_one(GameTestHelper h) {
        climb(h, 1, false);
    }

    @GameTest(template = "arena", timeoutTicks = 450, batch = "r43_wall2")
    public static void wall_two(GameTestHelper h) {
        climb(h, 2, false);
    }

    @GameTest(template = "arena", timeoutTicks = 450, batch = "r43_reload")
    public static void wall_reload(GameTestHelper h) {
        climb(h, 1, true);
    }

    private static void climb(GameTestHelper h, int phase, boolean reload) {
        var b = setup(h, phase);
        final boolean[] entered = {false}, top = {false}, finished = {false};
        double ground = b.getY();
        h.onEachTick(
                () -> {
                    if (b.state() == SamuraiEntity.WALL) entered[0] = true;
                    if (b.getY() >= ground + 7.95 && b.getZ() > h.absoluteVec(new Vec3(0, 0, 9)).z)
                        top[0] = true;
                    if (top[0]
                            && b.state() != SamuraiEntity.WALL
                            && !b.isNoGravity()
                            && b.onGround()) finished[0] = true;
                    h.assertTrue(!Double.isNaN(b.currentPose(0).matrices()[0].m00()), "姿态NaN");
                });
        if (reload)
            h.runAtTickTime(
                    88,
                    () -> {
                        CompoundTag n = new CompoundTag();
                        b.addAdditionalSaveData(n);
                        b.readAdditionalSaveData(n);
                    });
        h.runAtTickTime(
                350,
                () -> {
                    GeneratedMod.LOG.info(
                            "[r43 climb] {} entered={} top={} pos={} {}",
                            phase,
                            entered[0],
                            top[0],
                            b.position(),
                            b.report());
                    h.assertTrue(entered[0], "未进入高墙动作");
                    h.assertTrue(top[0], "没有登顶:" + b.report());
                    h.assertTrue(finished[0], "登顶后未在实地退出动作并恢复重力");
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 220, batch = "r43_broken")
    public static void broken_wall(GameTestHelper h) {
        var b = setup(h, 1);
        h.runAtTickTime(
                65,
                () -> {
                    for (int x = 7; x <= 16; x++)
                        for (int y = 1; y <= 8; y++) h.setBlock(x, y, 9, Blocks.AIR);
                });
        h.runAtTickTime(
                200,
                () -> {
                    h.assertTrue(!b.isNoGravity() || b.wallTraversal().active(), "失去移动归属仍悬空");
                    h.assertTrue(b.isAlive(), "支点损坏导致死亡");
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "r43_assets")
    public static void assets(GameTestHelper h) {
        for (int p = 1; p <= 2; p++) {
            var rig = SamuraiRig.get(p);
            for (String c :
                    new String[] {
                        WallClips.REACH,
                        WallClips.FIRST,
                        WallClips.LOOP,
                        WallClips.TOP,
                        WallClips.CATCH,
                        WallClips.STAB,
                        WallClips.RED_STAB,
                        WallClips.EXTRACT,
                        WallClips.RED_EXTRACT,
                        WallClips.sheathe(p),
                        WallClips.draw(p)
                    }) {
                h.assertTrue(rig.length(c) > 0, "缺少动作" + c);
                for (float t = 0; t < rig.length(c); t += 1 / 120f)
                    for (var m : rig.pose(c, t).matrices()) h.assertTrue(m.isFinite(), "非法姿态" + c);
            }
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 80, batch = "r43_actual_bullets")
    public static void actual_bullets(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 5, 5);
        var p = SamuraiGameTests.player(h, 12, 1, 12);
        b.testCombat(p, 2, false);
        b.setNoAi(true);
        var arrow = new net.minecraft.world.entity.projectile.Arrow(h.getLevel(), p);
        float health = b.getHealth();
        for (int i = 1; i <= 18; i++)
            h.runAtTickTime(
                    i,
                    () -> {
                        b.invulnerableTime = 0;
                        b.hurt(b.damageSources().arrow(arrow, p), 100000);
                        h.assertTrue(health - b.getHealth() <= 6.01, "实际投射物结算超过6");
                    });
        h.runAtTickTime(
                19,
                () -> {
                    h.assertTrue(Math.abs(health - b.getHealth() - 6) < .01, "上限误变完全免疫");
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 170, batch = "r43_red_fall")
    public static void red_fall(GameTestHelper h) {
        fall(h, 2, true);
    }

    @GameTest(template = "arena", timeoutTicks = 170, batch = "r43_normal_fall")
    public static void normal_fall(GameTestHelper h) {
        fall(h, 1, true);
    }

    @GameTest(template = "arena", timeoutTicks = 230, batch = "r43_empty_fall")
    public static void empty_fall(GameTestHelper h) {
        fall(h, 1, false);
    }

    private static void fall(GameTestHelper h, int phase, boolean armed) {
        SamuraiGameTests.floor(h);
        for (int x = 7; x <= 16; x++)
            for (int y = 1; y <= 16; y++)
                for (int z = 9; z <= 12; z++) h.setBlock(x, y, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 11.5, 7.4);
        var p = SamuraiGameTests.player(h, 11.5, 17, 10.5);
        b.testCombat(p, phase, false);
        b.testPursuit(p);
        b.wallDrawn(armed);
        Vec3 start = h.absoluteVec(new Vec3(11.5, 10, 7.4));
        b.setPos(start);
        b.setOnGround(false);
        b.fallDistance = 2;
        b.setDeltaMovement(0, -.4, 0);
        java.util.Set<String> stages = new java.util.HashSet<>();
        h.onEachTick(
                () -> {
                    stages.add(b.wallTraversal().status());
                    if (!b.isRemoved()) h.assertTrue(b.isAlive(), "止坠期间死亡");
                    if (b.wallTraversal().status().equals("BRACE") && b.stateAge(0) > .15f) {
                        var rig = SamuraiRig.get(phase);
                        var pose = b.currentPose(0);
                        double plane = h.absoluteVec(new Vec3(0, 0, 9)).z;
                        for (String side : new String[] {"L", "R"}) {
                            var toe =
                                    rig.cubes.stream()
                                            .filter(c -> c.name.equals("toe_cap_" + side))
                                            .findFirst()
                                            .orElseThrow();
                            var m = rig.cubeMatrix(toe, pose);
                            double front = -Double.MAX_VALUE;
                            for (float x : new float[] {toe.from.x, toe.to.x})
                                for (float y : new float[] {toe.from.y, toe.to.y})
                                    for (float z : new float[] {toe.from.z, toe.to.z}) {
                                        var v = m.transformPosition(new org.joml.Vector3f(x, y, z));
                                        Vec3 world =
                                                SamuraiRig.world(
                                                        new Vec3(v.x, v.y, v.z)
                                                                .scale(SamuraiRig.SCALE),
                                                        b.position(),
                                                        b.getYRot());
                                        front = Math.max(front, world.z);
                                    }
                            h.assertTrue(
                                    Math.abs(front - plane) < .035,
                                    "稳撑脚尖没有贴住实际墙面:" + (front - plane));
                        }
                    }
                });
        h.runAtTickTime(
                145,
                () -> {
                    GeneratedMod.LOG.info(
                            "[r43 rescue] phase={} armed={} stages={}", phase, armed, stages);
                    h.assertTrue(stages.contains(armed ? "STAB" : "CATCH"), "没有开始止坠");
                    h.assertTrue(stages.contains(armed ? "EXTRACT" : "FIRST"), "没有执行拔刀脱离/继续攀爬");
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r43_down_pursuit")
    public static void down_pursuit(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        for (int x = 5; x <= 14; x++)
            for (int z = 3; z <= 8; z++)
                for (int y = 1; y <= 7; y++) h.setBlock(x, y, z, Blocks.STONE);
        var b = SamuraiGameTests.boss(h, 10, 7.6);
        b.setPos(h.absoluteVec(new Vec3(10, 8, 7.6)));
        var p = SamuraiGameTests.player(h, 10, 1, 12);
        b.testPursuit(p);
        double y = b.getY();
        h.runAtTickTime(
                85,
                () -> {
                    h.assertTrue(b.getY() < y - 4, "没有主动下落追击");
                    h.assertTrue(b.isAlive(), "追击摔伤");
                    b.discard();
                    p.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 80, batch = "r43_slow_flank")
    public static void slow_flank(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var b = SamuraiGameTests.boss(h, 12, 9);
        var p = SamuraiGameTests.player(h, 15, 1, 9);
        b.testPursuit(p);
        b.setYRot(0);
        b.setNoAi(true);
        boolean found = false;
        for (double x : new double[] {-3, -2, 2, 3})
            for (double z : new double[] {-.5, .5, 1.5, 2}) {
                p.setPos(b.position().add(x, 0, z));
                b.attackTracking().sample(p, h.getLevel().getGameTime());
                if (SideIntercept.choose(b, p) != null) found = true;
            }
        h.assertTrue(found, "零速度侧身目标仍不能触发快刀");
        b.discard();
        p.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 60, batch = "r43_low_ceiling")
    public static void low_ceiling(GameTestHelper h) {
        var b = setup(h, 1);
        for (int x = 7; x <= 16; x++)
            for (int z = 9; z <= 16; z++) h.setBlock(x, 11, z, Blocks.STONE);
        var contact = WallGeometry.find(b, b.getTarget().position(), 4.3);
        h.assertTrue(contact != null, "没有检测墙面");
        Vec3 top = WallGeometry.roof(b, contact, b.getY());
        h.assertTrue(top == null || top.y >= h.absoluteVec(new Vec3(0, 12, 0)).y, "把低净空夹层选成落脚点");
        b.discard();
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r43_death_wall")
    public static void death_wall(GameTestHelper h) {
        var b = setup(h, 1);
        h.runAtTickTime(
                65,
                () -> {
                    h.assertTrue(b.wallTraversal().active(), "未进入攀爬");
                    b.hurt(b.damageSources().genericKill(), 100000);
                    h.assertTrue(!b.wallTraversal().active() && !b.isNoGravity(), "死亡仍被攀爬控制");
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r43_bad_reload")
    public static void bad_reload(GameTestHelper h) {
        var b = setup(h, 1);
        h.runAtTickTime(
                65,
                () -> {
                    CompoundTag n = new CompoundTag();
                    b.addAdditionalSaveData(n);
                    n.getCompound("SamuraiWall").putDouble("Rise", Double.NaN);
                    b.readAdditionalSaveData(n);
                    h.assertTrue(!b.wallTraversal().active() && !b.isNoGravity(), "损坏的攀爬存档导致悬空");
                    h.assertTrue(Double.isFinite(b.getY()), "非数坐标");
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "r43_terrain_off")
    public static void terrain_off(GameTestHelper h) {
        var b = setup(h, 2);
        b.setNoAi(true);
        boolean old = BossConfig.TERRAIN.get();
        try {
            BossConfig.TERRAIN.set(false);
            h.assertTrue(BladeCombat.wallContact(b) == 0, "关闭破坏仍切墙");
        } finally {
            BossConfig.TERRAIN.set(old);
        }
        b.discard();
        h.succeed();
    }
}
