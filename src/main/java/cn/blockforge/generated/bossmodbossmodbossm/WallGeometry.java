package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.core.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Small bounded probes only: never request a chunk or search the entire building. */
final class WallGeometry {
    record Contact(Vec3 face, Vec3 forward) {
        Vec3 stand(double y) {
            return new Vec3(face.x, y, face.z).subtract(forward.scale(1.485));
        }
    }

    static BlockHitResult ray(SamuraiEntity b, Vec3 from, Vec3 to) {
        int samples = Math.max(1, (int) Math.ceil(from.distanceTo(to)));
        for (int i = 0; i <= samples; i++)
            if (!b.level().hasChunkAt(BlockPos.containing(from.lerp(to, i / (double) samples))))
                return null;
        BlockHitResult h =
                b.level()
                        .clip(
                                new ClipContext(
                                        from,
                                        to,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        b));
        return h.getType() == HitResult.Type.BLOCK ? h : null;
    }

    static Contact find(SamuraiEntity b, Vec3 toward, double distance) {
        Vec3 d = toward.subtract(b.position()).multiply(1, 0, 1);
        if (d.lengthSqr() < .01) return null;
        Direction dir =
                Math.abs(d.x) > Math.abs(d.z)
                        ? (d.x > 0 ? Direction.EAST : Direction.WEST)
                        : (d.z > 0 ? Direction.SOUTH : Direction.NORTH);
        Vec3 f = Vec3.atLowerCornerOf(dir.getNormal());
        for (double probe : new double[] {3.2, 2.2, 1.2}) {
            Vec3 origin = b.position().add(0, probe, 0);
            BlockHitResult h = ray(b, origin, origin.add(f.scale(distance)));
            if (h == null
                    || h.getDirection().getAxis() == Direction.Axis.Y
                    || h.getDirection() != dir.getOpposite()) continue;
            Contact c = new Contact(h.getLocation(), f);
            if (grips(b, c, b.getY())) return c;
        }
        return null;
    }

    static boolean grips(SamuraiEntity b, Contact c, double y) {
        Vec3 side = new Vec3(c.forward.z, 0, -c.forward.x);
        for (double height : new double[] {3.2, 2.2, 4.2}) {
            int hits = 0;
            for (double x : new double[] {-.48, .48}) {
                Vec3 p = new Vec3(c.face.x, y + height, c.face.z).add(side.scale(x));
                BlockHitResult h =
                        ray(b, p.subtract(c.forward.scale(.5)), p.add(c.forward.scale(.25)));
                if (h != null
                        && h.getDirection().getAxis() != Direction.Axis.Y
                        && b.level().getWorldBorder().isWithinBounds(h.getBlockPos())) hits++;
            }
            if (hits == 2) return true;
        }
        return false;
    }

    static Contact alternative(SamuraiEntity b, Contact old) {
        Vec3 side = new Vec3(old.forward.z, 0, -old.forward.x);
        for (double shift : new double[] {-.65, .65}) {
            Contact c = new Contact(old.face.add(side.scale(shift)), old.forward);
            if (grips(b, c, b.getY()) && fits(b, c.stand(b.getY()))) return c;
        }
        return null;
    }

    static Vec3 roof(SamuraiEntity b, Contact c, double base) {
        for (double y = Math.ceil((base + 2.5) * 2) / 2;
                y <= base + BossConfig.WALL_HEIGHT.get();
                y += .5) {
            for (double depth : new double[] {1.0125, 2.0125, .5}) {
                Vec3 dest = new Vec3(c.face.x, y, c.face.z).add(c.forward.scale(depth));
                // A roof stair may be higher under the uphill side of this wide body
                // than under its centre. Use the same footprint as ordinary navigation.
                var surface = b.terrainNavigation().floor(dest.x, dest.z, y, .6, .1);
                if (surface == null) continue;
                dest = SamuraiNavigation.vec(surface);
                if (dest.y < base + 2.5 || dest.y > base + BossConfig.WALL_HEIGHT.get()) continue;
                if (b.terrainNavigation().support(SamuraiNavigation.point(dest))
                        && fits(b, dest)
                        && routeClear(b, c, base, dest)) return dest;
            }
        }
        return null;
    }

    /** Same up-then-over collision path as TOP, including the overhang above the hands. */
    static boolean routeClear(SamuraiEntity b, Contact c, double base, Vec3 roof) {
        if (roof == null || roof.y < base + 2.5 || roof.y > base + BossConfig.WALL_HEIGHT.get())
            return false;
        double topStart = Math.max(base, roof.y - 35.75 * SamuraiRig.SCALE);
        for (double y = base; y < roof.y; y += .4) {
            if (!fits(b, c.stand(y))) return false;
            if (y < topStart && !grips(b, c, y)) return false;
        }
        Vec3 lip = c.stand(roof.y);
        int steps = Math.max(1, (int) Math.ceil(lip.distanceTo(roof) / .25));
        for (int i = 0; i <= steps; i++)
            if (!fits(b, lip.lerp(roof, i / (double) steps))) return false;
        return b.terrainNavigation().support(SamuraiNavigation.point(roof));
    }

    static boolean fits(SamuraiEntity b, Vec3 p) {
        return b.terrainNavigation().fits(SamuraiNavigation.point(p));
    }

    private WallGeometry() {}
}
