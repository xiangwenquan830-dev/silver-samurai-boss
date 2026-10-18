package io.github.xiangwenquan830.silversamurai;

import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import java.util.*;

@GameTestHolder("silver_samurai_r62")
@PrefixGameTestTemplate(false)
public final class MaintenanceGameTests {
    @GameTest(template = "arena", timeoutTicks = 30)
    public static void broken_shield_syncs_cooldown_and_preserves_spares(GameTestHelper h) {
        SamuraiGameTests.floor(h);
        var p = SamuraiGameTests.player(h, 10, 1, 10);
        List<ClientboundCooldownPacket> sent = new ArrayList<>();
        p.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(),
                new Connection(PacketFlow.SERVERBOUND), p) {
            @Override public void send(Packet<?> packet) {
                if (packet instanceof ClientboundCooldownPacket cooldown) sent.add(cooldown);
            }
        };
        var shield = new ItemStack(Items.SHIELD);
        var spare = new ItemStack(Items.SHIELD);
        p.setItemInHand(InteractionHand.MAIN_HAND, shield);
        p.getInventory().setItem(5, spare);
        RedBladeBreak.destroyShield(p, InteractionHand.MAIN_HAND, shield);
        h.assertTrue(p.getMainHandItem().isEmpty() && spare.getCount() == 1,
                "Destroyed an unused shield or failed to destroy the used shield");
        h.assertTrue(p.getCooldowns().isOnCooldown(Items.SHIELD), "No server item cooldown");
        h.assertTrue(sent.stream().anyMatch(packet -> packet.getItem() == Items.SHIELD
                && packet.getDuration() == BossConfig.SHIELD.get()), "No client cooldown packet");
        // Simulate a replacement acquired after the initial break and a stale client use request.
        p.getCooldowns().removeCooldown(Items.SHIELD);
        sent.clear();
        var use = new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start(p, spare, 72000);
        BossEvents.shieldUse(use);
        h.assertTrue(use.isCanceled() && p.getCooldowns().isOnCooldown(Items.SHIELD)
                && !sent.isEmpty(), "Late replacement use was not rejected and synchronized");
        p.getPersistentData().putLong("SilverSamuraiShieldUntil", h.getLevel().getGameTime());
        p.getCooldowns().removeCooldown(Items.SHIELD);
        var expired = new net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start(p, spare, 72000);
        BossEvents.shieldUse(expired);
        h.assertTrue(!expired.isCanceled(), "Expired lock continued blocking shields");
        p.discard();
        h.succeed();
    }

    public static final class ContractFixture {
        public boolean mutable;
        public static boolean global;
        public final boolean immutable = true;
        public static void valid() {}
        public void instance() {}
        public static int wrongResult() { return 0; }
    }

    @GameTest(template = "arena", timeoutTicks = 20)
    public static void rejects_incompatible_reflection_contracts(GameTestHelper h) throws Exception {
        WolverineCompat.required(ContractFixture.class, "mutable", boolean.class);
        WolverineCompat.requireMethod(ContractFixture.class.getMethod("valid"), true, void.class);
        for (String name : new String[] {"global", "immutable"}) {
            boolean rejected = false;
            try { WolverineCompat.required(ContractFixture.class, name, boolean.class); }
            catch (NoSuchFieldException expected) { rejected = true; }
            h.assertTrue(rejected, "Accepted unsafe field " + name);
        }
        for (String name : new String[] {"instance", "wrongResult"}) {
            boolean rejected = false;
            try { WolverineCompat.requireMethod(ContractFixture.class.getMethod(name), true, void.class); }
            catch (NoSuchMethodException expected) { rejected = true; }
            h.assertTrue(rejected, "Accepted incompatible method " + name);
        }
        h.succeed();
    }

    @GameTest(template = "arena", timeoutTicks = 20)
    public static void preload_is_shared_and_stable(GameTestHelper h) {
        var first = SamuraiRig.get(1);
        var second = SamuraiRig.get(2);
        SamuraiRig.preload();
        h.assertTrue(first == SamuraiRig.get(1) && second == SamuraiRig.get(2),
                "Repeated preload replaced cached rigs");
        h.succeed();
    }
}
