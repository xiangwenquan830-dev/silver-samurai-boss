package io.github.xiangwenquan830.silversamurai;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

final class PainNetwork {
    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    GeneratedMod.id("claw_pain"), () -> "4", "4"::equals, "4"::equals);

    record Broken(UUID player, int ticks, int growing, boolean locked) {}

    static Broken state(ServerPlayer p) {
        return new Broken(
                p.getUUID(),
                ClawSeverance.remaining(p),
                ClawSeverance.growing(p),
                ClawSeverance.locked(p));
    }

    record RedHit() {}

    record Burst(UUID player, double x, double y, double z, float yaw) {}

    static void init() {
        CHANNEL.registerMessage(
                2,
                RedHit.class,
                (m, b) -> {},
                b -> new RedHit(),
                (m, c) -> {
                    c.get()
                            .enqueueWork(
                                    () ->
                                            DistExecutor.unsafeRunWhenOn(
                                                    Dist.CLIENT, () -> () -> RedHitVisuals.hit()));
                    c.get().setPacketHandled(true);
                },
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(
                1,
                Broken.class,
                (m, b) -> {
                    b.writeUUID(m.player);
                    b.writeVarInt(m.ticks);
                    b.writeVarInt(m.growing);
                    b.writeBoolean(m.locked);
                },
                b -> new Broken(b.readUUID(), b.readVarInt(), b.readVarInt(), b.readBoolean()),
                (m, c) -> {
                    c.get()
                            .enqueueWork(
                                    () ->
                                            DistExecutor.unsafeRunWhenOn(
                                                    Dist.CLIENT,
                                                    () ->
                                                            () ->
                                                                    BrokenClawLayer.update(
                                                                            m.player, m.ticks,
                                                                            m.growing, m.locked)));
                    c.get().setPacketHandled(true);
                },
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(
                0,
                Burst.class,
                (m, b) -> {
                    b.writeUUID(m.player);
                    b.writeDouble(m.x);
                    b.writeDouble(m.y);
                    b.writeDouble(m.z);
                    b.writeFloat(m.yaw);
                },
                b ->
                        new Burst(
                                b.readUUID(),
                                b.readDouble(),
                                b.readDouble(),
                                b.readDouble(),
                                b.readFloat()),
                (m, c) -> {
                    c.get()
                            .enqueueWork(
                                    () ->
                                            DistExecutor.unsafeRunWhenOn(
                                                    Dist.CLIENT,
                                                    () -> () -> SeverVisuals.accept(m)));
                    c.get().setPacketHandled(true);
                },
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    static void redHit(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new RedHit());
    }

    static void send(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new Burst(
                        player.getUUID(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.getYRot()));
    }

    static void syncBroken(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), state(player));
    }

    static void syncBrokenTo(ServerPlayer subject, ServerPlayer viewer) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), state(subject));
    }

    private PainNetwork() {}
}
