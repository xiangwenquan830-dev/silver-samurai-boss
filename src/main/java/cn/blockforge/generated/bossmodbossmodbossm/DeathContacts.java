package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;

/** 一次性服务端地形快照：实际碰撞表面，不加载远处区块，不按水平平面抬全身。 */
public final class DeathContacts {
    public static DeathSurface capture(SamuraiEntity boss) {
        float[] heights = new float[DeathSurface.SIZE * DeathSurface.SIZE];
        for (int iz = 0; iz < DeathSurface.SIZE; iz++)
            for (int ix = 0; ix < DeathSurface.SIZE; ix++) {
                Vec3 p =
                        SamuraiRig.world(
                                new Vec3(
                                        (ix - DeathSurface.SIZE / 2)
                                                * DeathSurface.CELL
                                                * SamuraiRig.SCALE,
                                        0,
                                        (iz - DeathSurface.SIZE / 2)
                                                * DeathSurface.CELL
                                                * SamuraiRig.SCALE),
                                boss.position(),
                                boss.getYRot());
                if (!boss.level().hasChunkAt(BlockPos.containing(p))) {
                    heights[iz * DeathSurface.SIZE + ix] = 0;
                    continue;
                }
                Vec3 a = p.add(0, .65, 0), b = p.add(0, -6, 0);
                var hit =
                        boss.level()
                                .clip(
                                        new ClipContext(
                                                a,
                                                b,
                                                ClipContext.Block.COLLIDER,
                                                ClipContext.Fluid.NONE,
                                                boss));
                heights[iz * DeathSurface.SIZE + ix] =
                        hit.getType() == HitResult.Type.MISS
                                ? -6 / SamuraiRig.SCALE
                                : (float) (hit.getLocation().y - boss.getY()) / SamuraiRig.SCALE;
            }
        float best = 0;
        int bestScore = Integer.MAX_VALUE;
        // 不瞬间旋转实体：只为死亡各段选择接触方向，起始矩阵完全不变。
        for (float yaw : new float[] {0, 45, -45, 90, -90, 135, -135, 180}) {
            int score = 0;
            for (float z : new float[] {-5, -11, -18, -25})
                for (float x : new float[] {-7, 0, 7}) {
                    Vector3f local = new Vector3f(x, 0, z).rotateY((float) Math.toRadians(yaw));
                    Vec3 p =
                            SamuraiRig.world(
                                    new Vec3(
                                            local.x * SamuraiRig.SCALE,
                                            0,
                                            local.z * SamuraiRig.SCALE),
                                    boss.position(),
                                    boss.getYRot());
                    if (!boss.level().hasChunkAt(BlockPos.containing(p))) {
                        score += 10;
                        continue;
                    }
                    AABB box =
                            new AABB(
                                    p.x - .25, p.y + .75, p.z - .25, p.x + .25, p.y + 2.5,
                                    p.z + .25);
                    for (var shape : boss.level().getBlockCollisions(boss, box))
                        if (!shape.isEmpty()) score++;
                }
            if (score < bestScore) {
                best = yaw;
                bestScore = score;
            }
        }
        return new DeathSurface(heights, best, bestScore > 0);
    }

    private DeathContacts() {}
}
