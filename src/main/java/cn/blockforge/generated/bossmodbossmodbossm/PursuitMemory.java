package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Last confirmed position and bounded progress-based memory; never reads an unseen target position.
 */
final class PursuitMemory {
    enum Decision {
        FOLLOW,
        HOLD,
        REACQUIRE
    }

    private Vec3 point = Vec3.ZERO;
    private double bestDistance = Double.MAX_VALUE;
    private int unseen, extension;

    Vec3 point() {
        return point;
    }

    int unseen() {
        return unseen;
    }

    void targetChanged() {
        extension = 0;
        bestDistance = Double.MAX_VALUE;
    }

    void confirm(Vec3 observed) {
        point = observed;
        unseen = 0;
    }

    void clearUnseen() {
        unseen = 0;
    }

    void observe(Vec3 observer, LivingEntity target, boolean seen) {
        if (seen) {
            confirm(target.position());
            extension = 0;
            bestDistance = observer.distanceTo(point);
        } else {
            unseen++;
            double distance = observer.distanceTo(point);
            if (distance < bestDistance - .5) {
                bestDistance = distance;
                extension =
                        Math.min(
                                BossConfig.lostTicks(target),
                                unseen + BossConfig.memoryTicks(target));
            }
        }
    }

    Decision decision(LivingEntity target, boolean seen, boolean routeFailed) {
        boolean progressMemory = unseen < extension;
        if (unseen >= BossConfig.lostTicks(target) && !progressMemory || routeFailed)
            return Decision.REACQUIRE;
        if (!seen && unseen >= BossConfig.memoryTicks(target) && !progressMemory)
            return Decision.HOLD;
        return Decision.FOLLOW;
    }
}
