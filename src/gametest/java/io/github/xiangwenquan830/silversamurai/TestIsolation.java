package io.github.xiangwenquan830.silversamurai;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Test-only lifecycle: own spawned entities, including those that later leave the arena. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class TestIsolation {
    @SubscribeEvent
    public static void serverStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        if (event.getServer() instanceof net.minecraft.gametest.framework.GameTestServer) {
            // Test mutations must not race Forge's asynchronous disk watcher. Production keeps its
            // real file.
            BossConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
        }
    }

    private record Fixture(ServerLevel level, AABB bounds, Set<Entity> entities) {}

    private static final Map<GameTestHelper, Fixture> ACTIVE = new IdentityHashMap<>();

    static void begin(GameTestHelper helper) {
        Vec3 a = helper.absoluteVec(Vec3.ZERO), b = helper.absoluteVec(new Vec3(24, 128, 24));
        Fixture fixture =
                new Fixture(
                        helper.getLevel(),
                        new AABB(a, b),
                        Collections.newSetFromMap(new IdentityHashMap<>()));
        // Only this arena, never every living entity in the dimension.
        for (Entity e :
                new ArrayList<>(helper.getLevel().getEntities((Entity) null, fixture.bounds)))
            e.discard();
        ACTIVE.put(helper, fixture);
        try {
            var field = GameTestHelper.class.getDeclaredField("testInfo");
            field.setAccessible(true);
            ((GameTestInfo) field.get(helper))
                    .addListener(
                            new GameTestListener() {
                                public void testStructureLoaded(GameTestInfo info) {}

                                public void testPassed(GameTestInfo info) {
                                    end(helper);
                                }

                                public void testFailed(GameTestInfo info) {
                                    end(helper);
                                }
                            });
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot isolate GameTest lifecycle", e);
        }
    }

    private static void end(GameTestHelper helper) {
        Fixture fixture = ACTIVE.remove(helper);
        if (fixture != null) for (Entity e : fixture.entities) e.discard();
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent event) {
        for (Fixture fixture : ACTIVE.values())
            if (event.getLevel() == fixture.level
                    && fixture.bounds.contains(event.getEntity().position()))
                fixture.entities.add(event.getEntity());
    }

    static void readyPlayer(net.minecraft.server.level.ServerPlayer player) {
        // Combat fixtures are established players, not newly logged-in invulnerable players.
        try {
            var field =
                    net.minecraft.server.level.ServerPlayer.class.getDeclaredField(
                            "spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot initialize player fixture", e);
        }
    }

    private TestIsolation() {}
}
