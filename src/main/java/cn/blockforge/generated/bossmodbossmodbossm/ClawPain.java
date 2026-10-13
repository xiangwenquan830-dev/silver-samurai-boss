package cn.blockforge.generated.bossmodbossmodbossm;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;

final class ClawPain {
    static final String UNTIL = "SilverSamuraiClawPainUntil";
    private static final UUID SLOW = UUID.fromString("eed2f9ce-485c-4521-9171-18d2c72c3d70");

    static int remaining(ServerPlayer p) {
        return (int)
                Math.max(
                        0,
                        Math.min(20, p.getPersistentData().getLong(UNTIL) - ClawSeverance.now(p)));
    }

    static boolean active(ServerPlayer p) {
        return remaining(p) > 0;
    }

    static void begin(ServerPlayer p) {
        p.getPersistentData().putLong(UNTIL, ClawSeverance.now(p) + 20);
        tick(p);
        PainNetwork.send(p);
        PainNetwork.syncBroken(p);
    }

    static void tick(ServerPlayer p) {
        var speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (active(p)) {
            if (speed != null && speed.getModifier(SLOW) == null)
                speed.addTransientModifier(
                        new AttributeModifier(
                                SLOW,
                                "silver_samurai_claw_pain",
                                -1,
                                AttributeModifier.Operation.MULTIPLY_TOTAL));
            p.setSprinting(false);
            p.stopUsingItem();
            p.setDeltaMovement(0, p.getDeltaMovement().y, 0);
        } else {
            if (speed != null) speed.removeModifier(SLOW);
            p.getPersistentData().remove(UNTIL);
        }
    }

    private ClawPain() {}
}
