package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.world.phys.Vec3;

/** A lower target may still be on a roof: never abandon its elevation for street level. */
final class DropIntent {
    static boolean useful(Vec3 from, Vec3 landing, Vec3 target) {
        return target.y <= from.y - 2
                && landing.y >= target.y - 1.5
                && landing.distanceTo(target) < from.distanceTo(target) - 1;
    }
}
