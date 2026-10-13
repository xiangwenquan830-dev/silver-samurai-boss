package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.UUID;

/** 服务端逐件碰撞，客户端只插值；零件没有拾取、掉落、伤害和再次生成能力。 */
public final class DebrisEntity extends Entity {
    private static final EntityDataAccessor<CompoundTag> DATA =
            SynchedEntityData.defineId(DebrisEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private UUID owner;
    private int flightTicks, groundTicks, bounces;
    private long born, settled = -1, fadeAt = -1;
    private int hold = 200, fade = 60, part;
    private float spinX, spinY, spinZ, yaw;
    private Quaternionf initial = new Quaternionf();
    private double tx, ty, tz;
    private int lerpSteps;

    public DebrisEntity(EntityType<? extends DebrisEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noCulling = true;
    }

    public static void releaseDue(SamuraiEntity boss, float time) {
        if (!(boss.level() instanceof ServerLevel level) || !boss.active()) return;
        boolean due = false;
        for (int i = 0; i < 8; i++)
            if (!boss.shed(i) && time >= DebrisGeometry.RELEASE[i]) due = true;
        if (!due) return;
        int count = 0;
        for (Entity entity : level.getAllEntities())
            if (entity instanceof DebrisEntity && !entity.isRemoved()) count++;
        for (int i = 0; i < 8; i++) {
            if (boss.shed(i) || time < DebrisGeometry.RELEASE[i]) continue;
            boss.markShed(i); // 先锁定，再生成：重载、致命伤或达到上限都不补飞一次。
            if (i >= BossConfig.DEBRIS_LIMIT.get() || count >= BossConfig.DEBRIS_WORLD_LIMIT.get())
                continue;
            DebrisEntity e = GeneratedMod.DEBRIS.get().create(level);
            if (e == null) continue;
            DebrisGeometry g = DebrisGeometry.get(i);
            float release = DebrisGeometry.RELEASE[i];
            Vector3f c = g.centerAt(release), after = g.centerAt(release + .05f);
            Vec3 location =
                    SamuraiRig.world(
                            new Vec3(
                                    c.x * SamuraiRig.SCALE,
                                    c.y * SamuraiRig.SCALE,
                                    c.z * SamuraiRig.SCALE),
                            boss.position(),
                            boss.getYRot());
            Vec3 direction = new Vec3(after.x - c.x, 0, after.z - c.z);
            if (direction.lengthSqr() < .001) {
                Vector3f end = g.centerAt(6);
                direction = new Vec3(end.x - c.x, 0, end.z - c.z);
            }
            direction = direction.normalize();
            Vec3 velocity =
                    SamuraiRig.world(direction, Vec3.ZERO, boss.getYRot())
                            .scale(.10 + boss.getRandom().nextDouble() * .065)
                            .add(0, .05 + boss.getRandom().nextDouble() * .07, 0);
            e.part = i;
            e.owner = boss.getUUID();
            e.born = level.getGameTime();
            e.hold = BossConfig.DEBRIS.get() + boss.getRandom().nextInt(31);
            e.fade = BossConfig.DEBRIS_FADE.get();
            e.yaw = 180 - boss.getYRot();
            e.initial =
                    new Quaternionf()
                            .rotationY((float) Math.toRadians(e.yaw))
                            .mul(g.rotationAt(release));
            e.spinX = (boss.getRandom().nextFloat() - .5f) * .14f;
            e.spinY = (boss.getRandom().nextFloat() - .5f) * .16f;
            e.spinZ = (boss.getRandom().nextFloat() - .5f) * .14f;
            float[] bounds = g.bounds(e.initial);
            e.setPos(location.x, location.y + bounds[1], location.z);
            e.setDeltaMovement(velocity);
            e.sync();
            e.updateBox();
            level.addFreshEntity(e);
            count++;
        }
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA, new CompoundTag());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA.equals(key) && level().isClientSide) loadState(entityData.get(DATA));
    }

    public int part() {
        return part;
    }

    public Quaternionf orientation(float partial) {
        float ticks = Math.max(0, level().getGameTime() - born + partial);
        float stop = settled < 0 ? ticks : Math.max(0, settled - born);
        Quaternionf spin =
                new Quaternionf(initial).rotateXYZ(spinX * stop, spinY * stop, spinZ * stop);
        // 初次接触时才贴平，空中不提前变成横躺姿态。
        if (settled >= 0)
            spin.slerp(
                    new Quaternionf().rotationY((float) Math.toRadians(yaw)),
                    SamuraiRig.smooth((level().getGameTime() - settled + partial) / 10f));
        return spin;
    }

    public float dissolve(float partial) {
        return fadeAt < 0
                ? 0
                : Mth.clamp((level().getGameTime() - fadeAt + partial) / (float) fade, 0, 1);
    }

    private void updateBox() {
        float[] b = DebrisGeometry.get(part).bounds(orientation(0));
        setBoundingBox(
                new AABB(
                        getX() + b[0],
                        getY(),
                        getZ() + b[2],
                        getX() + b[3],
                        getY() + b[4] - b[1],
                        getZ() + b[5]));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                setPos(
                        getX() + (tx - getX()) / lerpSteps,
                        getY() + (ty - getY()) / lerpSteps,
                        getZ() + (tz - getZ()) / lerpSteps);
                lerpSteps--;
            }
            return;
        }
        flightTicks++;
        if (fadeAt >= 0 && level().getGameTime() >= fadeAt + fade) {
            discard();
            return;
        }
        if (groundTicks >= 12) {
            setDeltaMovement(Vec3.ZERO);
            updateBox();
            return;
        }
        updateBox();
        Vec3 motion = getDeltaMovement().add(0, -.045, 0);
        Vec3 before = position();
        AABB original = getBoundingBox();
        // Entity.move 会以碰撞箱中心重建position；恢复几何锚点，避免长刀水平偏移。
        move(MoverType.SELF, motion);
        AABB actual = getBoundingBox();
        Vec3 traveled =
                new Vec3(
                        (actual.minX + actual.maxX - original.minX - original.maxX) * .5,
                        actual.minY - original.minY,
                        (actual.minZ + actual.maxZ - original.minZ - original.maxZ) * .5);
        setPos(before.add(traveled));
        if (onGround()) {
            if (settled < 0) {
                settled = level().getGameTime();
                sync();
            }
            if (motion.y < -.16 && bounces == 0) {
                bounces++;
                setDeltaMovement(motion.x * .52, -motion.y * .12, motion.z * .52);
            } else {
                groundTicks++;
                setDeltaMovement(motion.x * .52, 0, motion.z * .52);
            }
            if (groundTicks >= 12) {
                setDeltaMovement(Vec3.ZERO);
                fadeAt = level().getGameTime() + hold;
                sync();
            }
        } else {
            groundTicks = 0;
            setDeltaMovement(
                    horizontalCollision ? new Vec3(0, motion.y * .98, 0) : motion.scale(.98));
        }
        // 虚空或永远落不稳的残骸也有有限寿命，但仍先溶解而不是突然删除。
        if (flightTicks >= 600 && fadeAt < 0) {
            fadeAt = level().getGameTime() + hold;
            sync();
        }
        updateBox();
    }

    @Override
    public void lerpTo(
            double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        tx = x;
        ty = y;
        tz = z;
        lerpSteps = Math.max(1, Math.min(3, steps));
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    private void sync() {
        CompoundTag t = new CompoundTag();
        saveState(t);
        entityData.set(DATA, t);
    }

    private void saveState(CompoundTag t) {
        t.putInt("Part", part);
        t.putLong("Born", born);
        t.putLong("Settled", settled);
        t.putLong("FadeAt", fadeAt);
        t.putInt("Hold", hold);
        t.putInt("Fade", fade);
        t.putFloat("QX", initial.x);
        t.putFloat("QY", initial.y);
        t.putFloat("QZ", initial.z);
        t.putFloat("QW", initial.w);
        t.putFloat("SX", spinX);
        t.putFloat("SY", spinY);
        t.putFloat("SZ", spinZ);
        t.putFloat("Yaw", yaw);
    }

    private void loadState(CompoundTag t) {
        part = Mth.clamp(t.getInt("Part"), 0, 7);
        born = t.getLong("Born");
        settled = t.contains("Settled") ? t.getLong("Settled") : -1;
        fadeAt = t.contains("FadeAt") ? t.getLong("FadeAt") : -1;
        hold = t.getInt("Hold");
        fade = Math.max(10, t.getInt("Fade"));
        initial.set(
                t.getFloat("QX"),
                t.getFloat("QY"),
                t.getFloat("QZ"),
                t.contains("QW") ? t.getFloat("QW") : 1);
        spinX = t.getFloat("SX");
        spinY = t.getFloat("SY");
        spinZ = t.getFloat("SZ");
        yaw = t.getFloat("Yaw");
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (!tag.contains("Part")) {
            fadeAt = level().getGameTime();
            born = fadeAt;
            settled = fadeAt;
            groundTicks = 12;
            sync();
            return;
        } // 旧版整批残骸安全迁移。
        loadState(tag);
        flightTicks = tag.getInt("FlightTicks");
        groundTicks = tag.getInt("GroundTicks");
        bounces = tag.getInt("Bounces");
        if (tag.contains("SavedAt") && groundTicks < 12) {
            long paused = Math.max(0, level().getGameTime() - tag.getLong("SavedAt"));
            born += paused;
            if (settled >= 0) settled += paused;
        }
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        sync();
        updateBox();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        saveState(tag);
        tag.putLong("SavedAt", level().getGameTime());
        tag.putInt("FlightTicks", flightTicks);
        tag.putInt("GroundTicks", groundTicks);
        tag.putInt("Bounces", bounces);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
