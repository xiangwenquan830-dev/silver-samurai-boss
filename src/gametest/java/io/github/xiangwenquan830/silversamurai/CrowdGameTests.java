package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
import static io.github.xiangwenquan830.silversamurai.SamuraiGameTests.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrowdGameTests {
    private static LivingEntity mob(GameTestHelper h, double x, double y, double z) {
        var e = h.spawn(EntityType.HUSK, new BlockPos((int) x, (int) y, (int) z));
        e.setNoAi(true);
        e.setPos(h.absoluteVec(new Vec3(x, y, z)));
        e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        e.setHealth(200);
        return e;
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "crowd_one")
    public static void crowd_one(GameTestHelper h) {
        crowd(h, 1);
    }

    @GameTest(template = "arena", timeoutTicks = 100, batch = "crowd_two")
    public static void crowd_two(GameTestHelper h) {
        crowd(h, 2);
    }

    private static void crowd(GameTestHelper h, int phase) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        List<LivingEntity> enemies = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            var e = mob(h, 10.5 + Math.cos(a) * 1.5, 1, 10.5 + Math.sin(a) * 1.5);
            ((Mob) e).setNoAi(false);
            enemies.add(e);
        }
        if (phase == 2) b.testCombat(enemies.get(0), 2, false);
        b.testPursuit(enemies.get(0));
        float[] yaw = {0};
        boolean[] started = {false}, hit = {false};
        double[] maxGap = {0};
        int[] starts = {0};
        int[] last = {-1};
        h.onEachTick(
                () -> {
                    if (hit[0])
                        for (var e : enemies)
                            maxGap[0] =
                                    Math.max(
                                            maxGap[0],
                                            e.position()
                                                    .subtract(b.position())
                                                    .horizontalDistanceSqr());
                    if (b.state() == SamuraiEntity.STOMP) {
                        if (last[0] != SamuraiEntity.STOMP) {
                            starts[0]++;
                            yaw[0] = b.getYRot();
                            started[0] = true;
                        }
                        h.assertTrue(
                                Math.abs(net.minecraft.util.Mth.wrapDegrees(b.getYRot() - yaw[0]))
                                        < .01,
                                "crowd stomp spun toward targets");
                        if (b.stateAge(0) < StompMotion.IMPACT)
                            for (var e : enemies)
                                h.assertTrue(
                                        e.getHealth() == 200, "damage happened before foot impact");
                        if (b.stateAge(0) >= StompMotion.IMPACT + .05f && !hit[0]) {
                            h.assertTrue(
                                    enemies.stream().filter(e -> e.getHealth() < 200).count() >= 3,
                                    "stomp did not damage multiple nearby enemies");
                            hit[0] = true;
                            CompoundTag tag = new CompoundTag();
                            b.addAdditionalSaveData(tag);
                            h.assertTrue(
                                    tag.getLong("SamuraiStompCooldown") > 50,
                                    "cooldown missing from saved state");
                        }
                    }
                    last[0] = b.state();
                });
        h.runAtTickTime(
                85,
                () -> {
                    h.assertTrue(
                            started[0] && hit[0],
                            "crowded boss only turned, never stomped: " + b.report());
                    h.assertTrue(starts[0] == 1, "stomp spam ignored cooldown");
                    h.assertTrue(
                            maxGap[0] > 4,
                            "stomp did not create space; max squared gap=" + maxGap[0]);
                    enemies.forEach(Entity::discard);
                    b.discard();
                    h.succeed();
                });
    }

    @GameTest(template = "arena", timeoutTicks = 25, batch = "crowd_safety")
    public static void safety(GameTestHelper h) {
        floor(h);
        var b = boss(h, 10.5, 10.5);
        b.setNoAi(true);
        var near = mob(h, 10.5, 1, 12);
        var behind = mob(h, 13.3, 1, 10.5);
        var high = mob(h, 10.5, 3, 10.5);
        high.setNoGravity(true);
        var cow = h.spawn(EntityType.COW, new BlockPos(9, 1, 10));
        cow.setNoAi(true);
        var creative = player(h, 10.5, 1, 9);
        creative.setGameMode(GameType.CREATIVE);
        for (int y = 1; y <= 4; y++)
            for (int z = 9; z <= 12; z++) h.setBlock(12, y, z, Blocks.STONE);
        float cowHealth = cow.getHealth();
        // Fresh far-away test chunks may not expose newly spawned entities to spatial queries yet.
        // Wait for fixture registration, not a damage result; keep all impact assertions unchanged.
        h.onEachTick(
                () -> {
                    if (!h.getLevel()
                            .getEntitiesOfClass(LivingEntity.class, b.getBoundingBox().inflate(4))
                            .contains(near)) return;
                    GeneratedMod.LOG.info(
                            "[crowd safety] boss={} target={} eligible={} reach={} enemies={} damage={} radius={} health={}",
                            b.position(),
                            near.position(),
                            CombatRules.enemy(b, near, true),
                            StompCombat.closeReach(b, near),
                            StompCombat.enemies(b, BossConfig.STOMP_RADIUS.get()).size(),
                            BossConfig.STOMP_DAMAGE.get(),
                            BossConfig.STOMP_RADIUS.get(),
                            b.getHealth());
                    StompCombat.impact(b);
                    h.assertTrue(near.getHealth() < 200, "near hostile not damaged");
                    h.assertTrue(
                            behind.getHealth() == 200 && high.getHealth() == 200,
                            "shock passed wall or floor height");
                    h.assertTrue(
                            cow.getHealth() == cowHealth && creative.getHealth() == 1000,
                            "friendly/creative damaged");
                    for (int y = 1; y <= 4; y++)
                        for (int z = 9; z <= 12; z++)
                            h.assertBlockPresent(Blocks.STONE, new BlockPos(12, y, z));
                    for (int x = 0; x < 24; x++)
                        for (int z = 0; z < 24; z++)
                            h.assertBlockPresent(Blocks.STONE, new BlockPos(x, 0, z));
                    for (var e : List.of(near, behind, high, cow, creative, b)) e.discard();
                    h.succeed();
                });
    }
}
