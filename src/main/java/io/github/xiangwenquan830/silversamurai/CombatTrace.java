package io.github.xiangwenquan830.silversamurai;

import java.io.*;
import java.nio.file.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 仅自测启用：导出真实服务器位置与当前骨骼，不伪装成客户端录屏。 */
final class CombatTrace implements AutoCloseable {
    private final DataOutputStream out;
    private final Vec3 origin;

    CombatTrace(String name, Vec3 origin) {
        this.origin = origin;
        try {
            Files.createDirectories(Path.of("combat-traces"));
            out =
                    new DataOutputStream(
                            new BufferedOutputStream(
                                    Files.newOutputStream(
                                            Path.of("combat-traces", name + ".bin"))));
            out.writeInt(0x53533701);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void frame(SamuraiEntity boss, LivingEntity target, int tick) {
        try {
            out.writeInt(tick);
            out.writeInt(boss.phase());
            out.writeInt(boss.state());
            out.writeFloat(boss.getYRot());
            for (Vec3 p :
                    new Vec3[] {
                        boss.position().subtract(origin), target.position().subtract(origin)
                    }) {
                out.writeDouble(p.x);
                out.writeDouble(p.y);
                out.writeDouble(p.z);
            }
            out.writeUTF(boss.behaviorReason());
            var pose = boss.currentPose(0);
            out.writeInt(pose.matrices().length);
            for (int i = 0; i < pose.matrices().length; i++) {
                float[] values = new float[16];
                pose.matrices()[i].get(values);
                for (float f : values) out.writeFloat(f);
                out.writeBoolean(pose.visible()[i]);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            out.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
