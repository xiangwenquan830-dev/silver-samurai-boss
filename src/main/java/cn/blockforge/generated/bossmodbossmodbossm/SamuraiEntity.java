package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public class SamuraiEntity extends PathfinderMob
        implements net.minecraft.world.entity.monster.Enemy {
    public static final int IDLE = 0,
            SLEEP = 1,
            BACK_WALK = 2,
            DRAW = 3,
            CHASE = 4,
            ATTACK_A = 5,
            ATTACK_B = 6,
            RECOVER = 7,
            SHEATHE = 8,
            TRANSITION = 9,
            DEAD = 10,
            VAULT = 11,
            CLIMB = 12,
            CLEAR = 13,
            FOOTWORK = 14,
            SETTLE = 15,
            STOMP = 16,
            CLOSE_GUARD = 17,
            SIDE_SWEEP = 18,
            WALL = 19;
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> START =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<String> CLIP =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> OLD_CLIP =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> OLD_TIME =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DEBUG =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.BOOLEAN);
    private final ServerBossEvent bar =
            new ServerBossEvent(
                    Component.literal("银色武士"),
                    BossEvent.BossBarColor.WHITE,
                    BossEvent.BossBarOverlay.PROGRESS);
    private final AttackExecution execution = new AttackExecution();
    private final Targeting targeting = new Targeting(this);
    private final BossAlert alertState = new BossAlert(this, targeting);
    private final CombatPerception perception = new CombatPerception(this);
    private final AttackTracking attackTracking = new AttackTracking();

    AttackTracking attackTracking() {
        return attackTracking;
    }

    CombatPerception perception() {
        return perception;
    }

    private boolean configured, phaseRequested;
    private static final EntityDataAccessor<Boolean> DRAWN =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SHEATHE_REVERSE =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RECOVERY_BLEND =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT);
    private long nextSideSweep, nextSideDecision;
    private float sideDesiredYaw, sideTargetYaw, sideEntryYaw;
    private final CloseDefense closeDefense = new CloseDefense(this);
    private boolean effectsTick;
    private static final EntityDataAccessor<Boolean> FOCUSED_KICK =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float>
            LOOK_PITCH =
                    SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            LOOK_WEIGHT =
                    SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> RED_START =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.LONG);

    boolean processingEffects() {
        return effectsTick;
    }

    public boolean focusedKick() {
        return entityData.get(FOCUSED_KICK);
    }

    @Override
    protected void tickEffects() {
        effectsTick = true;
        try {
            super.tickEffects();
        } finally {
            effectsTick = false;
        }
    }

    public boolean redReady() {
        return phase() == 2 && redProgress(0) >= 1;
    }

    private final PursuitJump pursuitJump = new PursuitJump(this);

    PursuitJump pursuitJump() {
        return pursuitJump;
    }

    private final WallTraversal wallTraversal = new WallTraversal(this);
    private final WallApproach wallApproach = new WallApproach(this);
    private static final EntityDataAccessor<Float>
            WALL_X = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            WALL_Y = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            WALL_Z = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT);
    private Vec3 oldWall = Vec3.ZERO, clientWall = Vec3.ZERO;

    WallTraversal wallTraversal() {
        return wallTraversal;
    }

    WallApproach wallApproach() {
        return wallApproach;
    }

    void wallDrawn(boolean v) {
        setDrawn(v);
    }

    Vec3 wallOffset() {
        return new Vec3(entityData.get(WALL_X), entityData.get(WALL_Y), entityData.get(WALL_Z));
    }

    void wallOffset(Vec3 v) {
        entityData.set(WALL_X, (float) v.x);
        entityData.set(WALL_Y, (float) v.y);
        entityData.set(WALL_Z, (float) v.z);
    }

    void wallResume(float time) {
        entityData.set(START, level().getGameTime() - Math.round(time * 20));
    }

    void wallEnter(String animation) {
        var source = currentPose(0);
        enter(WALL, animation, source);
        entityData.set(MOTION_POSE, DeathMotion.encode(source, 0));
        entityData.set(START, level().getGameTime());
        wallOffset(Vec3.ZERO);
        oldWall = clientWall = Vec3.ZERO;
        entityData.set(HIT_POWER, 0f);
        entityData.set(AIM, 0f);
        entityData.set(OLD_AIM, 0f);
    }

    void wallFinish() {
        finishRecovery(6);
        wallOffset(Vec3.ZERO);
    }

    private SamuraiRig.Pose wallPose(float partial) {
        var rig = SamuraiRig.get(phase());
        float time = stateAge(partial);
        var pose = rig.pose(clip(), time);
        Vec3 shift =
                level().isClientSide
                        ? oldWall.lerp(clientWall, partial)
                        : new Vec3(
                                entityData.get(WALL_X),
                                entityData.get(WALL_Y),
                                entityData.get(WALL_Z));
        if (!WallClips.grounded(clip())) shift = shift.add(0, -rig.rootMotion(clip(), time).y, 0);
        for (int i = 0; i < pose.matrices().length; i++)
            pose.matrices()[i] =
                    new org.joml.Matrix4f()
                            .translation((float) shift.x, (float) shift.y, (float) shift.z)
                            .mul(pose.matrices()[i]);
        if (time < .12f)
            pose =
                    FootworkMotion.join(
                            rig,
                            DeathMotion.decode(rig, entityData.get(MOTION_POSE)),
                            pose,
                            SamuraiRig.smooth(time / .12f));
        return pose;
    }

    private final RangedDamageBudget rangedBudget = new RangedDamageBudget();

    RangedDamageBudget rangedBudget() {
        return rangedBudget;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    private boolean stompLanded;
    private boolean motionGuardReady;

    @Override
    public void setTarget(LivingEntity target) {
        if (getTarget() != target) {
            targeting.changed();
            resetTargetDecision();
            pursuit.targetChanged();
            if (terrainNavigation != null) terrainNavigation.targetChanged();
        }
        super.setTarget(target);
    }

    private void resetTargetDecision() {
        spacing = null;
        closeSolution = null;
        closeBand = false;
        nextDecision = nextCloseSolution = nextSpacing = 0;
        spacingFailures = 0;
    }

    private float oldLookYaw,
            clientLookYaw,
            oldLookPitch,
            clientLookPitch,
            oldLookWeight,
            clientLookWeight;

    private boolean drawn() {
        return entityData.get(DRAWN);
    }

    private void setDrawn(boolean value) {
        entityData.set(DRAWN, value);
    }

    public int quietTicks() {
        return alertState.quiet();
    }

    public boolean alert() {
        return alertState.awake();
    }

    private final PursuitMemory pursuit = new PursuitMemory();
    private int cooldown;
    private float lockedYaw;
    private String lastAttack = SamuraiRig.A;
    private int particlesThisTick;
    private static final EntityDataAccessor<Float>
            RATE = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            CYCLES = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            AIM = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            OLD_AIM = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<CompoundTag> DEATH_POSE =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> TRANSITION_POSE =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> SHED =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.INT);
    private float previousCycles, clientCycles;
    private static final EntityDataAccessor<Float> DRAW_GAIT =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT);
    private float oldDrawGait, clientDrawGait;
    private double lastHorizontalTravel;
    private boolean drawWalking;

    float drawGait(float partial) {
        return level().isClientSide
                ? Mth.lerp(partial, oldDrawGait, clientDrawGait)
                : entityData.get(DRAW_GAIT);
    }

    private float drawLength() {
        return SamuraiRig.get(1).length(clip());
    }

    private int corpseHold = 100, deathFeedback, lastArmorFeedback = -100;
    private boolean deathSettled, chargePlayed;
    private final FootPlantTracker footTracker = new FootPlantTracker();
    private CompoundTag cachedDeathTag;
    private SamuraiRig.Pose cachedDeathPose;
    private DeathSurface cachedSurface;
    private CompoundTag cachedSurfaceTag;
    private static final EntityDataAccessor<CompoundTag> DEATH_SURFACE =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> DEATH_LANDED =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.BOOLEAN);

    public DeathSurface deathSurface() {
        CompoundTag tag = entityData.get(DEATH_SURFACE);
        if (tag != cachedSurfaceTag) {
            cachedSurfaceTag = tag;
            cachedSurface = DeathSurface.load(tag);
        }
        return cachedSurface;
    }

    public float deathElapsed(float partial) {
        return entityData.get(DEATH_LANDED) ? stateAge(partial) : Math.min(.15f, stateAge(partial));
    }

    private final SamuraiNavigation terrainNavigation = new SamuraiNavigation(this);
    private static final EntityDataAccessor<Float> TRAVERSE =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> AIM_START =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<CompoundTag> MOTION_POSE =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Float>
            CLEAR_DISTANCE =
                    SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            CLEAR_HEIGHT =
                    SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT);

    public SamuraiRig.Pose clearingPose(float t) {
        return ClearingMotion.pose(t, entityData.get(CLEAR_DISTANCE), entityData.get(CLEAR_HEIGHT));
    }

    private float aimYawOffset;
    private int preparedStrike = -1, clearMisses;
    private final HitDiagnostics hitDiagnostics = new HitDiagnostics();
    private static final EntityDataAccessor<Float>
            STEP_X = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            STEP_Z = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            STEP_YAW = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            STEP_PROGRESS =
                    SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            BODY_YAW = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            LOOK_YAW = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            HIT_POWER =
                    SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            HIT_SIDE = SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> STEP_SIDE =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> HIT_START =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> BRAKING =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.BOOLEAN);
    private Vec3 brakeMotion = Vec3.ZERO;
    private Vec3 stepOrigin = Vec3.ZERO, stepDelta = Vec3.ZERO;
    private float stepStartYaw, oldStepProgress, clientStepProgress, oldBodyYaw, clientBodyYaw;
    private long clientMotionStart = Long.MIN_VALUE;
    private int stableSide = 1,
            spacingFailures,
            reactionUntil,
            lastReaction = -100,
            recoveryTicks = 8,
            settleTicks;
    private long nextSpacing, nextDecision;
    private boolean closeBand, abortCombo;
    private int safeAbortStrike = -1;
    private String chosenClip = SamuraiRig.A, behaviorReason = "等待目标";
    private CombatSpacing.Decision spacing;
    private BladeAim.Solution closeSolution;
    private long nextCloseSolution;
    private double desiredDistance = 2.9, currentDistance;

    public float displayYaw(float partial) {
        return level().isClientSide ? Mth.rotLerp(partial, oldBodyYaw, clientBodyYaw) : getYRot();
    }

    public float footworkProgress(float partial) {
        return level().isClientSide
                ? Mth.lerp(partial, oldStepProgress, clientStepProgress)
                : entityData.get(STEP_PROGRESS);
    }

    public String behaviorReason() {
        return behaviorReason;
    }

    public double walkingSpeed() {
        return terrainNavigation.walkingSpeed();
    }

    public boolean reactionActive() {
        return level().getGameTime() - entityData.get(HIT_START) < 10
                && entityData.get(HIT_POWER) > 0;
    }

    long reactionStarted() {
        return entityData.get(HIT_START);
    }

    float reactionPower() {
        return entityData.get(HIT_POWER);
    }

    private static final EntityDataAccessor<Float>
            TERRAIN_PROGRESS =
                    SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT),
            TERRAIN_HEIGHT =
                    SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TERRAIN_SIDE =
            SynchedEntityData.defineId(SamuraiEntity.class, EntityDataSerializers.INT);
    private StepMotion.Frame oldTerrainFrame = StepMotion.Frame.REST,
            clientTerrainFrame = StepMotion.Frame.REST;

    private StepMotion.Frame terrainFrame() {
        return new StepMotion.Frame(
                entityData.get(TERRAIN_PROGRESS),
                entityData.get(TERRAIN_HEIGHT),
                entityData.get(TERRAIN_SIDE));
    }

    public void terrainStep(float progress, float height, boolean starting) {
        if (starting) {
            entityData.set(TERRAIN_SIDE, stableSide);
            stableSide = -stableSide;
        }
        entityData.set(TERRAIN_PROGRESS, progress);
        entityData.set(TERRAIN_HEIGHT, height);
    }

    public SamuraiNavigation terrainNavigation() {
        return terrainNavigation;
    }

    // 仅同包GameTest使用；不是指令或普通AI入口。
    void testCombat(LivingEntity target, int phase, boolean alt) {
        entityData.set(PHASE, phase);
        if (phase == 2) entityData.set(SHED, 255);
        applyConfig(true);
        setDrawn(true);
        setTarget(target);
        face(target.position(), 360);
        enter(
                alt ? ATTACK_B : ATTACK_A,
                phase == 2 ? SamuraiRig.COMBO_TWO : alt ? SamuraiRig.B : SamuraiRig.A);
    }

    void testPursuit(LivingEntity target) {
        setTarget(target);
        pursuit.confirm(target.position());
        setDrawn(true);
        enter(CHASE, phase() == 2 ? SamuraiRig.WALK_TWO : "armed_walk");
    }

    int[] hitCounts() {
        return hitDiagnostics.acceptedHits();
    }

    public boolean weaponsDrawn() {
        return drawn() || (phase() == 2 && state() != WALL);
    }

    public boolean activated() {
        return isAlive()
                && state() != DEAD
                && (playerNearby() || alertState.enemy() || targetValid(getTarget()));
    }

    public void haltMotion() {
        stopMoving();
    }

    public void facePoint(Vec3 point, float limit) {
        face(point, limit);
    }

    public void traversalProgress(float value) {
        entityData.set(TRAVERSE, value);
    }

    public void beginTraversal(boolean climb) {
        entityData.set(TRANSITION_POSE, DeathMotion.encode(currentPose(0), 0));
        entityData.set(TRAVERSE, 0f);
        enter(climb ? CLIMB : VAULT, phase() == 2 ? SamuraiRig.IDLE_TWO : "armed_idle");
    }

    public void endTraversal() {
        entityData.set(TRANSITION_POSE, DeathMotion.encode(currentPose(0), 0));
        finishRecovery(8);
        setNoGravity(false);
    }

    public void beginClearing() {
        clearMisses = 0;
        entityData.set(TRANSITION_POSE, DeathMotion.encode(currentPose(0), 0));
        enter(CLEAR, SamuraiRig.IDLE_TWO);
        execution.startClearing();
    }

    public boolean canStartAttack(LivingEntity target) {
        return target != null
                && CombatSpacing.assess(
                                this,
                                target,
                                phase() == 2 ? SamuraiRig.COMBO_TWO : chosenClip,
                                0,
                                false)
                        .attackable();
    }

    public void recordHit(
            int strike,
            LivingEntity target,
            float before,
            float after,
            boolean accepted,
            boolean invulnerability,
            String reason) {
        hitDiagnostics.record(
                this, strike, target, before, after, accepted, invulnerability, reason);
    }

    public boolean shed(int part) {
        return (entityData.get(SHED) & (1 << part)) != 0;
    }

    public void markShed(int part) {
        entityData.set(SHED, entityData.get(SHED) | (1 << part));
    }

    public boolean groundStrike() {
        return execution.groundStrike();
    }

    public void markGroundStrike() {
        execution.markGroundStrike();
    }

    public float stateAge(float partial) {
        return Math.max(0, (level().getGameTime() - entityData.get(START) + partial) / 20f);
    }

    public float playbackRate() {
        return entityData.get(RATE);
    }

    public float aimPitch(float partial) {
        return Mth.lerp(
                SamuraiRig.smooth(
                        (level().getGameTime() - entityData.get(AIM_START) + partial) / 3f),
                entityData.get(OLD_AIM),
                entityData.get(AIM));
    }

    private void aim(float pitch) {
        float old = aimPitch(0);
        entityData.set(OLD_AIM, old);
        entityData.set(AIM, pitch);
        entityData.set(AIM_START, level().getGameTime());
    }

    public SamuraiRig.Pose currentPose(float partial) {
        var pose = samplePose(partial);
        if (phase() == 2
                && (state() == IDLE || state() == SLEEP || state() == CLOSE_GUARD)
                && stateAge(partial) >= .3f) StableGrip.idle(SamuraiRig.get(2), pose);
        if (HeadLookMotion.allowed(state()))
            HeadLookMotion.apply(
                    SamuraiRig.get(phase()),
                    pose,
                    level().isClientSide ? Mth.lerp(partial, oldLookYaw, clientLookYaw) : lookYaw(),
                    level().isClientSide
                            ? Mth.lerp(partial, oldLookPitch, clientLookPitch)
                            : lookPitch(),
                    level().isClientSide
                            ? Mth.lerp(partial, oldLookWeight, clientLookWeight)
                            : entityData.get(LOOK_WEIGHT),
                    HeadLookMotion.bowAllowed(state())
                            ? SamuraiRig.smooth(stateAge(partial) / .3f)
                            : 0);
        return pose;
    }

    float lookYaw() {
        return entityData.get(LOOK_YAW);
    }

    float lookPitch() {
        return entityData.get(LOOK_PITCH);
    }

    /**
     * Runs after AI/body rotation, including committed attacks; only the selected visible target is
     * followed.
     */
    private void updateHeadLook() {
        LivingEntity target = getTarget();
        boolean tracking =
                HeadLookMotion.allowed(state())
                        && targetValid(target)
                        && Targeting.visible(this, target);
        float yaw = 0, pitch = 0;
        if (tracking) {
            var rig = SamuraiRig.get(phase());
            int head = rig.bone("头盔");
            var v =
                    samplePose(0)
                            .matrices()[head]
                            .transformPosition(new org.joml.Vector3f(rig.bones.get(head).origin));
            Vec3 anchor =
                    SamuraiRig.world(
                            new Vec3(v.x, v.y, v.z).scale(SamuraiRig.SCALE), position(), getYRot());
            Vec3 delta = target.getEyePosition().subtract(anchor);
            double horizontal = Math.sqrt(delta.horizontalDistanceSqr());
            yaw =
                    horizontal < .35
                            ? lookYaw()
                            : Mth.clamp(
                                    Mth.wrapDegrees(
                                            (float) Math.toDegrees(Math.atan2(delta.z, delta.x))
                                                    - 90
                                                    - getYRot()),
                                    -42,
                                    42);
            pitch =
                    Mth.clamp(
                            (float) -Math.toDegrees(Math.atan2(delta.y, Math.max(.35, horizontal))),
                            -18,
                            38);
        }
        entityData.set(LOOK_YAW, Mth.approach(lookYaw(), yaw, 4));
        entityData.set(LOOK_PITCH, Mth.approach(lookPitch(), pitch, 3));
        entityData.set(
                LOOK_WEIGHT, Mth.approach(entityData.get(LOOK_WEIGHT), tracking ? 1 : 0, .15f));
    }

    private SamuraiRig.Pose samplePose(float partial) {
        if (state() == WALL) return wallPose(partial);
        if (state() == DEAD) {
            CompoundTag tag = entityData.get(DEATH_POSE);
            if (cachedDeathTag != tag) {
                cachedDeathTag = tag;
                cachedDeathPose = DeathMotion.decode(SamuraiRig.get(phase()), tag);
            }
            return DeathMotion.pose(
                    SamuraiRig.get(phase()),
                    cachedDeathPose,
                    deathElapsed(partial),
                    deathSurface());
        }
        var motion =
                new PoseComposer.Motion(
                        phase(),
                        state(),
                        stateAge(partial),
                        clip(),
                        visualTime(partial),
                        aimPitch(partial),
                        entityData.get(MOTION_POSE),
                        entityData.get(TRANSITION_POSE));
        var detail =
                new PoseComposer.Details(
                        idleClip(),
                        footworkProgress(partial),
                        entityData.get(STEP_X),
                        entityData.get(STEP_Z),
                        entityData.get(STEP_YAW),
                        entityData.get(STEP_SIDE),
                        entityData.get(BRAKING),
                        entityData.get(RECOVERY_BLEND),
                        drawn(),
                        entityData.get(TRAVERSE),
                        entityData.get(SHEATHE_REVERSE),
                        playbackRate(),
                        level().isClientSide ? oldTerrainFrame : terrainFrame(),
                        level().isClientSide ? clientTerrainFrame : terrainFrame(),
                        level().isClientSide ? partial : 1,
                        (level().getGameTime() - entityData.get(HIT_START) + partial) / 20f,
                        reactionAllowed(),
                        entityData.get(HIT_POWER),
                        entityData.get(HIT_SIDE),
                        entityData.get(CLEAR_DISTANCE),
                        entityData.get(CLEAR_HEIGHT),
                        drawGait(partial));
        return PoseComposer.sample(motion, detail);
    }

    public SamuraiEntity(EntityType<? extends SamuraiEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
        noCulling = true;
        this.moveControl =
                new net.minecraft.world.entity.ai.control.MoveControl(this) {
                    @Override
                    public void tick() {
                        /* 仅由完整体型路线或独立攻击状态执行移动，禁止旧wanted位置回转。 */
                    }
                };
        motionGuardReady = true;
    }

    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 600)
                .add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.MOVEMENT_SPEED, .24)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.ARMOR, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    // A heavy boss cannot be lifted by another living entity's solid collision shape.
    // Block collision and the custom terrain navigator remain active.
    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity other) {}

    @Override
    public void push(double x, double y, double z) {}

    @Override
    public void knockback(double strength, double x, double z) {}

    private boolean jumpTravel;

    void jumpVelocity(Vec3 motion) {
        super.setDeltaMovement(motion);
    }

    @Override
    public void travel(Vec3 input) {
        jumpTravel = pursuitJump != null && pursuitJump.airborne();
        try {
            super.travel(jumpTravel ? Vec3.ZERO : input);
        } finally {
            jumpTravel = false;
        }
    }

    @Override
    public void setDeltaMovement(Vec3 motion) {
        // Only owned jump travel may retain launch velocity; external impulses remain blocked.
        if (motionGuardReady && isAlive() && !jumpTravel) {
            if (pursuitJump != null && pursuitJump.airborne())
                return; // Ignore external knockback during owned flight.
            motion = new Vec3(0, Math.min(0, motion.y), 0);
        }
        super.setDeltaMovement(motion);
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        return !BossImmunity.blocked(effect.getEffect()) && super.canBeAffected(effect);
    }

    @Override
    public boolean addEffect(net.minecraft.world.effect.MobEffectInstance effect, Entity source) {
        return !BossImmunity.blocked(effect.getEffect()) && super.addEffect(effect, source);
    }

    private void clearForbiddenEffects() {
        for (var effect : new ArrayList<>(getActiveEffects()))
            if (BossImmunity.blocked(effect.getEffect())) removeEffect(effect.getEffect());
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return !(other instanceof LivingEntity) && super.canCollideWith(other);
    }

    @Override
    protected void registerGoals() {}

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                yBodyRot = getYRot();
                yHeadRot = getYRot();
            }
        };
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(WALL_X, 0f);
        entityData.define(WALL_Y, 0f);
        entityData.define(WALL_Z, 0f);
        entityData.define(PHASE, 1);
        entityData.define(STATE, IDLE);
        entityData.define(START, 0L);
        entityData.define(CLIP, "");
        entityData.define(OLD_CLIP, "");
        entityData.define(OLD_TIME, 0f);
        entityData.define(DEBUG, false);
        entityData.define(RATE, 1f);
        entityData.define(CYCLES, 0f);
        entityData.define(AIM, 0f);
        entityData.define(OLD_AIM, 0f);
        entityData.define(DEATH_POSE, new CompoundTag());
        entityData.define(SHED, 0);
        entityData.define(TRANSITION_POSE, new CompoundTag());
        entityData.define(TRAVERSE, 0f);
        entityData.define(AIM_START, 0L);
        entityData.define(MOTION_POSE, new CompoundTag());
        entityData.define(CLEAR_DISTANCE, 2f);
        entityData.define(CLEAR_HEIGHT, 2f);
        entityData.define(STEP_X, 0f);
        entityData.define(STEP_Z, 0f);
        entityData.define(STEP_YAW, 0f);
        entityData.define(STEP_PROGRESS, 0f);
        entityData.define(BODY_YAW, 0f);
        entityData.define(LOOK_YAW, 0f);
        entityData.define(LOOK_PITCH, 0f);
        entityData.define(LOOK_WEIGHT, 0f);
        entityData.define(STEP_SIDE, 1);
        entityData.define(HIT_START, -100L);
        entityData.define(HIT_POWER, 0f);
        entityData.define(HIT_SIDE, 1f);
        entityData.define(BRAKING, false);
        entityData.define(DRAWN, false);
        entityData.define(SHEATHE_REVERSE, -1f);
        entityData.define(RECOVERY_BLEND, .4f);
        entityData.define(TERRAIN_PROGRESS, 0f);
        entityData.define(TERRAIN_HEIGHT, 0f);
        entityData.define(TERRAIN_SIDE, 1);
        entityData.define(DEATH_SURFACE, new CompoundTag());
        entityData.define(DEATH_LANDED, true);
        entityData.define(FOCUSED_KICK, false);
        entityData.define(RED_START, -1L);
        entityData.define(DRAW_GAIT, 0f);
    }

    public int phase() {
        return entityData.get(PHASE);
    }

    public int state() {
        return entityData.get(STATE);
    }

    public float age(float partial) {
        return ActionRules.combo(state())
                ? CombatTiming.animationTime(
                        stateAge(partial), playbackRate(), phase(), state() == ATTACK_B)
                : stateAge(partial) * playbackRate();
    }

    public float visualTime(float partial) {
        if (state() == SHEATHE && entityData.get(SHEATHE_REVERSE) >= 0)
            return Math.max(0, entityData.get(SHEATHE_REVERSE) - stateAge(partial));
        if (state() == CLEAR) return 0;
        if (state() == RECOVER) return previousTime();
        if (state() == CHASE || state() == BACK_WALK)
            return (level().isClientSide
                            ? Mth.lerp(partial, previousCycles, clientCycles)
                            : entityData.get(CYCLES))
                    * SamuraiRig.get(phase())
                            .length(phase() == 2 ? SamuraiRig.WALK_TWO : SamuraiRig.WALK_ONE);
        return state() == IDLE || state() == SLEEP || state() == DEAD ? 0 : age(partial);
    }

    public String clip() {
        return entityData.get(CLIP);
    }

    public String previousClip() {
        return entityData.get(OLD_CLIP);
    }

    public float previousTime() {
        return entityData.get(OLD_TIME);
    }

    public boolean debug() {
        return entityData.get(DEBUG);
    }

    public void debug(boolean value) {
        entityData.set(DEBUG, value);
    }

    public boolean active() {
        return state() != SLEEP && state() != DEAD && isAlive();
    }

    public float redProgress(float partial) {
        if (phase() != 2) return 0;

        if (state() == DEAD)
            return entityData.get(DEATH_POSE).getFloat("Red")
                    * (1 - SamuraiRig.smooth(stateAge(partial) / 3.4f));
        long start = entityData.get(RED_START);
        return start < 0 ? 1 : RedBladeTiming.progress(level().getGameTime() + partial - start);
    }

    public void applyConfig(boolean preserveHealth) {
        float oldMax = getMaxHealth(), oldHealth = getHealth();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(BossConfig.HEALTH.get());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BossConfig.ATTACK.get());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(BossConfig.MOVE_SPEED.get());
        getAttribute(Attributes.ARMOR).setBaseValue(phase() == 2 ? BossConfig.ARMOR.get() : 0);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        getAttribute(Attributes.FOLLOW_RANGE)
                .setBaseValue(
                        Math.max(
                                BossConfig.retentionRange(),
                                Math.max(
                                        BossConfig.PLAYER_ACQUIRE.get(),
                                        BossConfig.PLAYER_RETAIN.get())));
        setHealth(preserveHealth ? Math.min(oldHealth, getMaxHealth()) : getMaxHealth());
        configured = true;
    }

    private void enter(int next, String animation) {
        enter(next, animation, null);
    }

    private void enter(int next, String animation, SamuraiRig.Pose snapshot) {
        if (state() == DEAD || state() == next && clip().equals(animation)) return;
        entityData.set(
                MOTION_POSE, DeathMotion.encode(snapshot == null ? currentPose(0) : snapshot, 0));
        entityData.set(OLD_CLIP, clip());
        entityData.set(OLD_TIME, visualTime(0));
        entityData.set(OLD_AIM, aimPitch(0));
        float pitch = 0;
        if ((ActionRules.attack(next)) && getTarget() != null) {
            double horizontal =
                    Math.sqrt(getTarget().position().subtract(position()).horizontalDistanceSqr());
            pitch =
                    Mth.clamp(
                            (float)
                                    Math.toDegrees(
                                            Math.atan2(
                                                    getTarget().getY()
                                                            + getTarget().getBbHeight() * .55
                                                            - getY()
                                                            - 2.9,
                                                    horizontal + 1.5)),
                            -28,
                            0);
        }
        if (next == RECOVER) pitch = entityData.get(AIM);
        entityData.set(AIM, pitch);
        entityData.set(AIM_START, level().getGameTime());
        entityData.set(RATE, ActionRules.combo(next) ? BossConfig.attackRate() : 1f);
        entityData.set(STATE, next);
        entityData.set(CLIP, animation);
        entityData.set(START, level().getGameTime());
        if (ActionRules.motion(next) != ActionRules.MotionOwner.NAVIGATION) {
            stopMoving();
            footTracker.reset();
        }
        if (ActionRules.attack(next)) beginAttackState(animation);
    }

    /** One entry point owns strike accounting; pose capture in enter() must precede all resets. */
    private void beginAttackState(String animation) {
        abortCombo = false;
        safeAbortStrike = -1;
        spacingFailures = 0;
        entityData.set(HIT_POWER, 0f);
        terrainNavigation.cancel();
        preparedStrike = -1;
        execution.reset();
        lockedYaw = getYRot();
        if (!SideIntercept.clip(animation)) lastAttack = animation;
        hitDiagnostics.resetCounts();
    }

    private void stopMoving() {
        getNavigation().stop();
        setZza(0);
        setXxa(0);
        setSpeed(0);
        setNoGravity(false);
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0);
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    private final BossSenses senses = new BossSenses(this);

    BossSenses senses() {
        return senses;
    }

    private boolean playerNearby() {
        return senses.observerNearby();
    }

    private boolean canDisengage() {
        return alertState.canDisengage();
    }

    private String idleClip() {
        return phase() == 2 ? SamuraiRig.IDLE_TWO : drawn() ? "armed_idle" : "";
    }

    private void idleWithoutTarget() {
        terrainNavigation.cancel();
        stopMoving();
        if (canDisengage()) {
            if (phase() == 1 && drawn()) {
                entityData.set(SHEATHE_REVERSE, -1f);
                enter(SHEATHE, SamuraiRig.SHEATHE);
            } else enter(alertState.observer() ? IDLE : SLEEP, idleClip());
        } else enter(IDLE, idleClip());
    }

    private void sheatheTick() {
        stopMoving();
        float reverse = entityData.get(SHEATHE_REVERSE);
        if (reverse < 0 && !canDisengage()) {
            // 从当前收刀时间原路退回握刀端，不跳转到拔刀首帧。
            entityData.set(MOTION_POSE, DeathMotion.encode(currentPose(0), 0));
            entityData.set(SHEATHE_REVERSE, Math.min(3, stateAge(0)));
            entityData.set(START, level().getGameTime());
            reverse = entityData.get(SHEATHE_REVERSE);
        }
        if (reverse >= 0) {
            if (stateAge(0) >= reverse) {
                setDrawn(true);
                enter(IDLE, idleClip());
                entityData.set(SHEATHE_REVERSE, -1f);
            }
            return;
        }
        if (stateAge(0) >= drawLength() && canDisengage()) {
            setDrawn(false);
            enter(alertState.observer() ? IDLE : SLEEP, idleClip());
        }
    }

    private boolean safeForTransition() {
        return state() != WALL
                && state() != STOMP
                && !ActionRules.attack(state())
                && state() != VAULT
                && state() != CLIMB
                && !terrainNavigation.terrainMotion()
                && terrainNavigation.support(SamuraiNavigation.point(position()));
    }

    boolean targetValid(LivingEntity target) {
        return targeting.valid(target);
    }

    private void acquire() {
        acquire(false);
    }

    void acquire(boolean traversing) {
        if (!ActionRules.retarget(state()) && !(traversing && pursuitJump.active())) return;
        LivingEntity best = targeting.select(chosenClip);
        if (best == null || best == getTarget()) return;
        if (!traversing) terrainNavigation.cancel();
        setTarget(best);
        pursuit.confirm(best.position());
        chosenClip = phase() == 2 ? SamuraiRig.COMBO_TWO : SamuraiRig.A;
        if (state() == FOOTWORK || state() == SETTLE)
            enter(CHASE, phase() == 2 ? SamuraiRig.WALK_TWO : "armed_walk");
    }

    private void markUnreachable(LivingEntity target) {
        targeting.failed(target);
        terrainNavigation.cancel();
    }

    public void obstacleChanged(net.minecraft.core.BlockPos pos) {
        perception.invalidate();
        terrainNavigation.obstacleChanged(pos);
        if (pos.distToCenterSqr(position()) < 64) {
            targeting.obstacleChanged();
            spacingFailures = 0;
        }
    }

    private final FacingBudget facingBudget = new FacingBudget();

    private void faceYaw(float yaw, float limit) {
        setYRot(facingBudget.turn(level().getGameTime(), getYRot(), yaw, limit));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private void face(Vec3 target, float limit) {
        faceYaw(yawTo(target), limit);
    }

    private float yawTo(Vec3 point) {
        return (float) Math.toDegrees(Math.atan2(point.z - getZ(), point.x - getX())) - 90;
    }

    private void beginFootwork(Vec3 delta, float yaw, String reason, boolean cancelRoute) {
        if (state() == FOOTWORK) return;
        if (delta.lengthSqr() > 0 && !terrainNavigation.safeStep(position(), position().add(delta)))
            return;
        if (cancelRoute) terrainNavigation.cancel();
        else terrainNavigation.resetSpeed();
        stepOrigin = position();
        stepDelta = delta;
        stepStartYaw = getYRot();
        double a = Math.toRadians(180 - stepStartYaw), c = Math.cos(a), s = Math.sin(a);
        entityData.set(STEP_X, (float) ((delta.x * c - delta.z * s) / SamuraiRig.SCALE));
        entityData.set(STEP_Z, (float) ((delta.x * s + delta.z * c) / SamuraiRig.SCALE));
        entityData.set(STEP_YAW, yaw);
        entityData.set(STEP_PROGRESS, 0f);
        entityData.set(STEP_SIDE, stableSide);
        entityData.set(BRAKING, false);
        enter(FOOTWORK, idleClip());
        behaviorReason = reason;
    }

    private void footworkTick() {
        stopMoving();
        boolean braking = entityData.get(BRAKING),
                pivot = stepDelta.lengthSqr() == 0 && entityData.get(STEP_YAW) != 0;
        float
                p =
                        Math.min(
                                1,
                                stateAge(0)
                                        * 20
                                        / (braking
                                                ? 6
                                                : pivot
                                                        ? FootworkMotion.pivotTicks(
                                                                entityData.get(STEP_YAW))
                                                        : FootworkMotion.TICKS)),
                old = entityData.get(STEP_PROGRESS);
        Vec3 destination =
                stepOrigin.add(stepDelta.scale(braking ? 2 * p - p * p : FootworkMotion.travel(p)));
        if (stepDelta.lengthSqr() > .000001
                && !terrainNavigation.safeStep(position(), destination)) {
            spacingFailures++;
            nextSpacing = level().getGameTime() + 32;
            behaviorReason = "短步遇到新障碍，落稳后换路";
            finishRecovery(8);
            return;
        }
        move(MoverType.SELF, destination.subtract(position()));
        setYRot(stepStartYaw + entityData.get(STEP_YAW) * FootworkMotion.turn(p, pivot));
        yBodyRot = yHeadRot = getYRot();
        entityData.set(STEP_PROGRESS, p);
        if (p > old) CombatFeedback.footsteps(this, footTracker);
        if (p >= (attackTracking.fast(getTarget()) ? .35f : .55f)
                && startReadyAttack(getTarget(), 35)) return;
        if (pivot
                && p >= .5f
                && attackTracking.fast(getTarget())
                && Math.abs(
                                Mth.wrapDegrees(
                                        yawTo(attackTracking.aimPoint(getTarget()))
                                                - (stepStartYaw + entityData.get(STEP_YAW))))
                        > 25) {
            // First foot is planted; leave an obsolete pivot instead of finishing its stale
            // heading.
            nextDecision = 0;
            enter(IDLE, idleClip());
            return;
        }
        if (p >= 1) {
            stableSide = -stableSide;
            nextDecision = 0;
            enter(IDLE, idleClip());
        }
    }

    private boolean makeSpace(LivingEntity target) {
        if (level().getGameTime() < nextSpacing) return false;
        if (spacingFailures >= 2) {
            spacingFailures = 0;
            stableSide = -stableSide;
        }
        nextSpacing = level().getGameTime() + FootworkMotion.TICKS + 8;
        Vec3 away = position().subtract(target.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < .01)
            away =
                    new Vec3(
                            Math.sin(Math.toRadians(getYRot())),
                            0,
                            -Math.cos(Math.toRadians(getYRot())));
        away = away.normalize();
        Vec3 side = new Vec3(-away.z, 0, away.x).scale(stableSide);
        // 同一轮最多三个候选；左右选择在一步完成后才换，不每刻随机绕圈。
        for (Vec3 direction :
                new Vec3[] {
                    away,
                    away.scale(.65).add(side.scale(.76)),
                    away.scale(.65).subtract(side.scale(.76)),
                    side,
                    side.scale(-1)
                }) {
            Vec3 delta = direction.normalize().scale(BossConfig.SPACING_STEP.get());
            if (terrainNavigation.safeStep(position(), position().add(delta))) {
                beginFootwork(delta, 0, "过近，检查落脚后短后撤/斜步", true);
                return true;
            }
        }
        spacingFailures++;
        if (spacingFailures >= 2) nextSpacing = level().getGameTime() + 80;
        behaviorReason = "后方、斜侧与侧向均受阻，冷却后换侧重试或等待空间，不补隐形伤害";
        return false;
    }

    private void finishRecovery(int ticks) {
        entityData.set(RECOVERY_BLEND, Math.max(.2f, ticks / 20f));
        recoveryTicks = Math.max(4, ticks);
        cooldown = Math.max(cooldown, recoveryTicks);
        enter(RECOVER, phase() == 2 ? SamuraiRig.IDLE_TWO : "armed_idle");
    }

    private boolean reactionAllowed() {
        return ActionRules.reaction(state());
    }

    private boolean startReadyAttack(LivingEntity target, float yawLimit) {
        if (trySideSweep(target)) return true;
        if (!ActionRules.canBeginAttack(state())
                || !weaponsDrawn()
                || !targetValid(target)
                || cooldown > 0
                || tickCount < reactionUntil) return false;
        CombatSpacing.Decision decision = CombatSpacing.assess(this, target, chosenClip, 0, false);
        if (!decision.attackable()) return false;
        boolean fast = attackTracking.fast(target);
        float desired = yawTo(attackTracking.aimPoint(target)) + decision.aim().yawOffset();
        float tolerance = fast ? Math.min(28, yawLimit) : Math.min(15, yawLimit);
        float error = Mth.wrapDegrees(desired - getYRot());
        if (Math.abs(error) > tolerance) {
            if (state() != FOOTWORK) faceYaw(desired, fast ? 18 : 7);
            // Recheck after this tick's turn instead of losing another opening to a moving target.
            if (Math.abs(Mth.wrapDegrees(desired - getYRot())) > tolerance) return false;
        }
        closeBand = false;
        spacing = decision;
        spacingFailures = 0;
        enter(chosenClip.equals(SamuraiRig.B) ? ATTACK_B : ATTACK_A, chosenClip);
        return true;
    }

    private boolean tryCloseDefense(boolean bladeReady) {
        if (tickCount < reactionUntil) return false;
        CloseDefense.Action action = closeDefense.choose(bladeReady, cooldown);
        if (action == CloseDefense.Action.NONE) return false;
        if (action == CloseDefense.Action.FOCUSED) {
            LivingEntity target = getTarget();
            double gap = target.position().subtract(position()).horizontalDistanceSqr();
            float error = gap < .16 ? 0 : Mth.wrapDegrees(yawTo(target.position()) - getYRot());
            if (Math.abs(error) > 30) {
                face(target.position(), 7);
                return false;
            }
        }
        entityData.set(FOCUSED_KICK, action == CloseDefense.Action.FOCUSED);
        entityData.set(HIT_POWER, 0f);
        terrainNavigation.cancel();
        lockedYaw = getYRot();
        stompLanded = false;
        closeDefense.started(action);
        enter(STOMP, idleClip());
        behaviorReason =
                action == CloseDefense.Action.FOCUSED ? "刀下盲区短踢解围，收势后接换位或挥刀" : "脚下敌群解围，收势后恢复刀路进攻";
        return true;
    }

    private void stompTick() {
        stopMoving();
        setYRot(lockedYaw);
        yBodyRot = yHeadRot = lockedYaw;
        float t = stateAge(0);
        if (!stompLanded && t >= StompMotion.IMPACT) {
            stompLanded = true;
            if (terrainNavigation.support(SamuraiNavigation.point(position())))
                StompCombat.impact(this, focusedKick());
        }
        if (t >= StompMotion.DURATION) {
            nextDecision = 0;
            spacing = null;
            closeBand = false;
            finishRecovery(4);
        }
    }

    private void dropTarget() {
        wallApproach.clear();
        terrainNavigation.cancel();
        setTarget(null);
        pursuit.clearUnseen();
        idleWithoutTarget();
    }

    private void beginTransition() {
        if (!isAlive() || phase() == 2) return;
        SamuraiRig oldRig = SamuraiRig.get(1), newRig = SamuraiRig.get(2);
        SamuraiRig.Pose source = currentPose(0), first = newRig.pose(SamuraiRig.TRANSITION, 0);
        for (int i = 0; i < newRig.bones.size(); i++) {
            int old = oldRig.bone(newRig.bones.get(i).name);
            if (old >= 0) first.matrices()[i] = new org.joml.Matrix4f(source.matrices()[old]);
        }
        entityData.set(TRANSITION_POSE, DeathMotion.encode(first, 0));
        entityData.set(PHASE, 2);
        entityData.set(
                RED_START,
                level().getGameTime() + RedBladeTiming.gripTicks() + RedBladeTiming.pauseTicks());
        phaseRequested = false;
        setDrawn(true);
        getAttribute(Attributes.ARMOR).setBaseValue(BossConfig.ARMOR.get());
        terrainNavigation.cancel();
        enter(TRANSITION, SamuraiRig.TRANSITION, first);
        lockedYaw = getYRot();
        playSound(GeneratedMod.ARMOR_GROAN.get(), 1.05f, .88f);
    }

    public void requestSecondPhase() {
        if (phase() == 1 && isAlive()) {
            phaseRequested = true;
            if (safeForTransition()) beginTransition();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        particlesThisTick = 0;
        attackTracking.sample(getTarget(), level().getGameTime());
        if (!isAlive() || state() == DEAD) return;
        if (!configured) applyConfig(false);
        bar.setProgress(getHealth() / getMaxHealth());
        bar.setColor(phase() == 2 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.WHITE);
        bar.setName(
                Component.literal(
                        (phase() == 2 ? "银色武士 · 战损红刃" : "银色武士 · 重装双刀")
                                + "  "
                                + (int) Math.ceil(getHealth())
                                + " / "
                                + (int) getMaxHealth()));
        if (phase() == 1 && getHealth() <= BossConfig.PHASE_HEALTH.get()) phaseRequested = true;
        if (cooldown > 0) cooldown--;
        alertState.tick();
        boolean awake = alert();
        if (tickCommittedAction(awake)) return;
        if (!targetValid(getTarget())) {
            if (getTarget() != null) {
                setTarget(null);
                acquire();
                if (getTarget() == null) {
                    dropTarget();
                    return;
                }
            } else if ((tickCount + getId()) % 2 == 0) acquire();
        }
        if ((tickCount + getId()) % 30 == 0) {
            targeting.threat.replaceAll((u, v) -> v * .75f);
        }
        LivingEntity target = getTarget();
        if (target == null) {
            wallApproach.clear();
            idleWithoutTarget();
            return;
        }
        boolean seen = Targeting.visible(this, target);
        pursuit.observe(position(), target, seen);
        if (wallApproach.tick(target, pursuit.point(), seen, pursuit.unseen())) {
            behaviorReason = wallApproach.status();
            return;
        }
        PursuitMemory.Decision pursuitDecision =
                pursuit.decision(target, seen, terrainNavigation.failed());
        if (pursuitDecision == PursuitMemory.Decision.REACQUIRE) {
            markUnreachable(target);
            setTarget(null);
            acquire();
            if (getTarget() == null) dropTarget();
            return;
        }
        // Stop blind pursuit at its own timeout; keep alert and allow visible replacements until
        // LOST.
        if (pursuitDecision == PursuitMemory.Decision.HOLD) {
            terrainNavigation.cancel();
            stopMoving();
            enter(IDLE, idleClip());
            behaviorReason = "最后位置追踪到期，持刀观察并寻找可见目标";
            acquire();
            return;
        }
        tickEngagement(target, seen);
    }

    /** Continue an already committed action before starting a new targeting decision. */
    private boolean tickCommittedAction(boolean awake) {
        if (state() == TRANSITION) {
            stopMoving();
            setYRot(lockedYaw);
            yBodyRot = lockedYaw;
            DebrisEntity.releaseDue(this, age(0));
            if (redProgress(0) > 0 && !chargePlayed) {
                chargePlayed = true;
                playSound(SoundEvents.BEACON_ACTIVATE, .65f, .8f);
            }
            if (awake && redProgress(0) > 0) BladeCombat.charge(this);
            if (redReady() && age(0) * 20 >= RedBladeTiming.totalTicks())
                enter(awake ? IDLE : SLEEP, SamuraiRig.IDLE_TWO);
            return true;
        }
        if (TraversalCoordinator.tick(this)) return true;
        if (state() == SLEEP) {
            if (!awake) {
                stopMoving();
                return true;
            }
            enter(IDLE, idleClip());
        }
        if (phaseRequested && safeForTransition()) {
            beginTransition();
            return true;
        }
        if (state() == SHEATHE) {
            sheatheTick();
            return true;
        }
        if (state() == CLEAR) {
            clearingTick();
            return true;
        }
        if (state() == DRAW && !targetValid(getTarget())) {
            setTarget(null);
            acquire();
            if (getTarget() == null) {
                stopMoving();
                if (age(0) >= drawLength()) {
                    setDrawn(true);
                    enter(IDLE, idleClip());
                }
                return true;
            }
        }
        if (state() == SIDE_SWEEP) {
            sideSweepTick();
            return true;
        }
        if (ActionRules.combo(state())) {
            attackTick();
            return true;
        }
        if (state() == STOMP) {
            stompTick();
            return true;
        }
        if ((tickCount + getId()) % BossConfig.TARGET_SCAN.get() == 0) acquire();
        if (state() == FOOTWORK) {
            if (!targetValid(getTarget())) {
                setTarget(null);
                finishRecovery(10);
            } else footworkTick();
            return true;
        }
        if (state() == SETTLE) {
            if (startReadyAttack(getTarget(), 30)) return true;
            stopMoving();
            if (++settleTicks >= 6) enter(IDLE, idleClip());
            return true;
        }
        if (state() == RECOVER) {
            stopMoving();
            if (stateAge(0) * 20 >= recoveryTicks) {
                enter(IDLE, idleClip());
            } else if (stateAge(0) >= .20f
                    && targetValid(getTarget())
                    && Targeting.visible(this, getTarget())
                    && !CombatSpacing.assess(this, getTarget(), chosenClip, 0, false)
                            .attackable()) {
                behaviorReason = "收势安全段完成，目标已离开刀路，接续追击";
                enter(CHASE, phase() == 2 ? SamuraiRig.WALK_TWO : "armed_walk");
                // Cooldown remains in force while movement resumes.
                path(
                        getTarget().position(),
                        phase() == 2
                                ? BossConfig.PHASE2_SPEED.get()
                                : BossConfig.CHASE_SPEED.get());
            }
            return true;
        }
        return false;
    }

    /** Choose spacing and draw/attack/approach behavior for the current valid target. */
    private void tickEngagement(LivingEntity target, boolean seen) {
        // Losing sight does not disable replanning toward the last confirmed position.
        double distance = distanceToSqr(target);
        if (phase() == 1 && !drawn()) {
            if (state() != DRAW && distance > 144 && !terrainNavigation.hasRoute()) {
                enter(BACK_WALK, SamuraiRig.WALK_ONE);
                path(pursuit.point(), BossConfig.CHASE_SPEED.get());
                return;
            }
            if (state() != DRAW) {
                // Choose from actual travel, not a stale navigation request. Keep this clip until
                // extraction ends.
                boolean moving =
                        lastHorizontalTravel > .02 && lastHorizontalTravel < 1.5 && distance > 25;
                enter(DRAW, moving ? SamuraiRig.DRAW : SamuraiRig.DRAW_STILL);
                drawWalking = moving;
                entityData.set(DRAW_GAIT, moving ? 1f : 0f);
            }
            if (age(0) < drawLength()) {
                if (clip().equals(SamuraiRig.DRAW) && distance > 25)
                    path(pursuit.point(), BossConfig.CHASE_SPEED.get() * .85);
                else stopMoving();
                return;
            }
            setDrawn(true);
            enter(CHASE, "armed_walk");
        }
        currentDistance = Math.sqrt(target.position().subtract(position()).horizontalDistanceSqr());
        if (seen && currentDistance < 7 + TargetGeometry.extraRadius(target)) {
            if (spacing == null || level().getGameTime() >= nextDecision) {
                chosenClip =
                        phase() == 2
                                ? SamuraiRig.COMBO_TWO
                                : (attackTracking.fast(target)
                                                || target.getDeltaMovement().horizontalDistanceSqr()
                                                        > .016)
                                        ? SamuraiRig.A
                                        : lastAttack.equals(SamuraiRig.A)
                                                ? SamuraiRig.B
                                                : SamuraiRig.A;
                nextDecision = level().getGameTime() + 6;
            }
            spacing = CombatSpacing.assess(this, target, chosenClip, 0, closeBand);
            closeBand = spacing.band() == CombatSpacing.Band.CLOSE;
            desiredDistance = spacing.profile().preferred();
            behaviorReason = spacing.reason();
            if (tryCloseDefense(spacing.attackable())) return;
            if (trySideSweep(target)) return;
            float error = Mth.wrapDegrees(yawTo(target.position()) - getYRot());
            float attackError =
                    spacing.aim() == null
                            ? error
                            : Mth.wrapDegrees(error + spacing.aim().yawOffset());
            if (spacing.attackable() && attackTracking.fast(target)) {
                // A fast runner must not keep restarting a turn toward an already obsolete point.
                if (startReadyAttack(target, 35)) return;
                faceYaw(yawTo(attackTracking.aimPoint(target)) + spacing.aim().yawOffset(), 18);
                stopMoving();
                return;
            }
            if (spacing.attackable()
                    && CombatSpacing.lane(this, target)
                    && Math.abs(attackError) > 35) {
                beginFootwork(Vec3.ZERO, Mth.clamp(attackError, -90, 90), "连续转身跟进侧后方目标", true);
                return;
            }
            if (closeBand) {
                if (makeSpace(target)) return;
                // 无处后退时只允许真正能覆盖近身碰撞箱的原有刀路，不凭空补伤害。
                if (closeSolution == null || level().getGameTime() >= nextCloseSolution) {
                    closeSolution =
                            BladeAim.solve(
                                    phase(),
                                    chosenClip,
                                    0,
                                    currentDistance,
                                    target.getY() - getY(),
                                    target.getBbHeight(),
                                    target.getBbWidth());
                    nextCloseSolution = level().getGameTime() + 8;
                }
                if (currentDistance > getBbWidth() * .5 + target.getBbWidth() * .5 + .08
                        && closeSolution.contact()
                        && CombatSpacing.lane(this, target)
                        && cooldown == 0) {
                    float closeYaw = yawTo(target.position()) + closeSolution.yawOffset();
                    if (Math.abs(Mth.wrapDegrees(closeYaw - getYRot())) > 20) {
                        faceYaw(closeYaw, 7);
                        return;
                    }
                    behaviorReason = "退路受阻，原有近距刀路可覆盖，取消额外突进";
                    enter(chosenClip.equals(SamuraiRig.B) ? ATTACK_B : ATTACK_A, chosenClip);
                    return;
                }
                if (Math.abs(target.getY() - getY()) > .4
                        && !StompCombat.closeReach(this, target)) {
                    enter(CHASE, phase() == 2 ? SamuraiRig.WALK_TWO : "armed_walk");
                    terrainNavigation.reposition(
                            pursuit.point(),
                            phase() == 2
                                    ? BossConfig.PHASE2_SPEED.get()
                                    : BossConfig.CHASE_SPEED.get());
                    return;
                }
                stopMoving();
                if (state() != CLOSE_GUARD) enter(CLOSE_GUARD, idleClip());
                return;
            }
            if (spacing.attackable() && CombatSpacing.lane(this, target)) {
                spacingFailures = 0;
                if (startReadyAttack(target, 35)) return;
                stopMoving();
                return;
            }
        } else {
            spacing = null;
            closeBand = false;
            spacingFailures = 0;
            behaviorReason = seen ? "目标拉远，沿完整体型路线追击" : "视线中断，追向最后确认位置";
            if (tryCloseDefense(false)) return;
        }
        enter(CHASE, phase() == 2 ? SamuraiRig.WALK_TWO : "armed_walk");
        path(
                pursuit.point(),
                phase() == 2 ? BossConfig.PHASE2_SPEED.get() : BossConfig.CHASE_SPEED.get());
    }

    boolean queueWallRoute(Vec3 destination) {
        return wallApproach.acceptRoute(destination);
    }

    void wallApproachWalk(Vec3 point) {
        enter(
                weaponsDrawn() ? CHASE : BACK_WALK,
                phase() == 2
                        ? SamuraiRig.WALK_TWO
                        : weaponsDrawn() ? "armed_walk" : SamuraiRig.WALK_ONE);
        terrainNavigation.chaseWaypoint(
                point, phase() == 2 ? BossConfig.PHASE2_SPEED.get() : BossConfig.CHASE_SPEED.get());
    }

    private void path(Vec3 pos, double speed) {
        if (ActionRules.motion(state()) == ActionRules.MotionOwner.NAVIGATION)
            terrainNavigation.chase(pos, speed);
    }

    public float[][] windows() {
        if (state() == SIDE_SWEEP)
            return new float[][] {{SideIntercept.start(phase()), SideIntercept.end(phase())}};
        return CombatTiming.windows(phase(), state() == ATTACK_B);
    }

    private boolean trySideSweep(LivingEntity target) {
        if (!BossConfig.SIDE_SWEEP.get()
                || !ActionRules.canBeginAttack(state())
                || !weaponsDrawn()
                || !targetValid(target)
                || cooldown > 0
                || tickCount < reactionUntil
                || level().getGameTime() < nextSideSweep
                || level().getGameTime() < nextSideDecision
                || terrainNavigation.terrainMotion()) return false;
        nextSideDecision = level().getGameTime() + 3;
        SideIntercept.Plan plan = SideIntercept.choose(this, target);
        if (plan == null) return false;
        beginSideSweep(plan);
        return true;
    }

    private void beginSideSweep(SideIntercept.Plan plan) {
        enter(SIDE_SWEEP, plan.clip());
        nextSideSweep = level().getGameTime() + BossConfig.SIDE_COOLDOWN.get();
        sideEntryYaw = getYRot();
        sideDesiredYaw = plan.yaw();
        sideTargetYaw = yawTo(attackTracking.aimPoint(getTarget()));
        aim(plan.pitch());
        nextDecision = 0;
        spacing = null;
        closeBand = false;
        behaviorReason = "目标进入侧刀路，单手截击";
    }

    void testSideSweep(LivingEntity target, int phase, boolean left, float pitch) {
        testCombat(target, phase, false);
        beginSideSweep(new SideIntercept.Plan(SideIntercept.clip(phase, left), pitch, getYRot()));
    }

    private void sideSweepTick() {
        stopMoving();
        float t = age(0),
                before = Math.max(0, t - .05f),
                start = SideIntercept.start(phase()),
                end = SideIntercept.end(phase());
        // Lost targets cannot cause a mid-cut switch. Finish the visible recovery without damage.
        if (!targetValid(getTarget())) abortCombo = true;
        if (!abortCombo && SideIntercept.canTurn(phase(), t)) {
            float tracking =
                    Mth.clamp(
                            Mth.wrapDegrees(
                                    yawTo(attackTracking.aimPoint(getTarget())) - sideTargetYaw),
                            -16,
                            16);
            float budget = SideIntercept.turnBudget(phase());
            float desired =
                    sideEntryYaw
                            + Mth.clamp(
                                    Mth.wrapDegrees(sideDesiredYaw + tracking - sideEntryYaw),
                                    -budget,
                                    budget);
            lockedYaw = Mth.approachDegrees(lockedYaw, desired, SideIntercept.TURN_STEP);
        }
        setYRot(lockedYaw);
        yBodyRot = yHeadRot = lockedYaw;
        if (!abortCombo && t >= start && before < end) {
            if (execution.lastStrike() < 0) {
                execution.startSide();
                float pitch = aimPitch(0);
                entityData.set(AIM, pitch);
                entityData.set(OLD_AIM, pitch);
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, .9f, phase() == 2 ? .85f : 1.05f);
            }
            execution.sweep(this, Math.max(before, start), Math.min(t, end), 0, position());
        }
        if (t >= SamuraiRig.get(phase()).length(clip())) {
            nextDecision = 0;
            spacing = null;
            closeBand = false;
            if (abortCombo) setTarget(null);
            if (phaseRequested) {
                beginTransition();
                return;
            }
            if (targetValid(getTarget())) {
                enter(CHASE, phase() == 2 ? SamuraiRig.WALK_TWO : "armed_walk");
                path(
                        getTarget().position(),
                        phase() == 2
                                ? BossConfig.PHASE2_SPEED.get()
                                : BossConfig.CHASE_SPEED.get());
            } else idleWithoutTarget();
        }
    }

    private void attackTick() {
        if (!active()) return;
        stopMoving();
        float t = age(0),
                before =
                        CombatTiming.animationTime(
                                Math.max(0, stateAge(0) - .05f),
                                playbackRate(),
                                phase(),
                                state() == ATTACK_B);
        float[][] windows = windows();
        boolean overlaps = false;
        for (float[] w : windows) if (t >= w[0] && before < w[1]) overlaps = true;
        if (!targetValid(getTarget()) && !abortCombo) {
            abortCombo = true;
            safeAbortStrike = overlaps ? execution.lastStrike() : -1;
            behaviorReason = "目标失效，锁方向完成当前刀的收住，不在途中换人";
        }
        if (abortCombo) {
            if (!overlaps || execution.lastStrike() != safeAbortStrike) {
                setTarget(null);
                finishRecovery(10);
                return;
            }
            setYRot(lockedYaw);
            yBodyRot = yHeadRot = lockedYaw;
            return;
        }
        if (!overlaps && targetValid(getTarget())) {
            int next = -1;
            for (int i = 0; i < 3; i++)
                if (t < windows[i][0]) {
                    next = i;
                    break;
                }
            if (next >= 0) {
                LivingEntity target = getTarget();
                float remaining = windows[next][0] - t;
                var preparation = ComboPlanner.prepare(this, target, next, remaining, lockedYaw);
                double horizontal = preparation.distance(),
                        preferred = preparation.profile().preferred();
                var profile = preparation.profile();
                currentDistance = horizontal;
                desiredDistance = preferred;
                if (preparation.abandon()) {
                    behaviorReason = preparation.reason() + "，安全收势后追击";
                    finishRecovery(6);
                    return;
                }
                if (preparedStrike != next || t < windows[next][0] - .10f) {
                    if (t < windows[next][0] - .10f) aim(preparation.aim().pitch());
                    aimYawOffset = preparation.aim().yawOffset();
                    preparedStrike = next;
                }
                float desired = preparation.yaw() - preparation.aim().yawOffset() + aimYawOffset;
                float turnLimit =
                        attackTracking.fast(target)
                                ? (remaining > .16f ? 18 : remaining > .08f ? 8 : 0)
                                : (remaining > .30f
                                        ? 4
                                        : remaining > .16f ? 2 : remaining > .08f ? .7f : 0);
                setYRot(Mth.approachDegrees(lockedYaw, desired, turnLimit));
                lockedYaw = getYRot();
                // 每刀只在准备期调整；越临近刀口生效，转向越收紧。
                behaviorReason = "第" + (next + 1) + "刀准备，重新判断距离；临出刀收紧追踪";
                if (horizontal < profile.minimum() && remaining > .16f) spacingBack(.055);
                else if (next > 0
                        && horizontal > preferred + .7
                        && horizontal < profile.maximum() + .5
                        && CombatSpacing.lane(this, target)
                        && remaining > .16f) lunge(Math.min(.085, horizontal - preferred - .7));
            }
        }
        setYRot(lockedYaw);
        yBodyRot = yHeadRot = lockedYaw;
        if (!targetValid(getTarget())) {
            setTarget(null);
            finishRecovery(10);
            return;
        }
        if (!phaseRequested && redReady() && CombatFeedback.crossed(before, t, windows[2][0] - .4f))
            CombatFeedback.heavyWarning(this);
        int activeStrike = -1;
        for (int i = 0; i < windows.length; i++)
            if (t >= windows[i][0] && before < windows[i][1]) {
                activeStrike = i;
                break;
            }
        if (phaseRequested && (activeStrike < 0 || execution.lastStrike() != activeStrike)) {
            beginTransition();
            return;
        }
        if (activeStrike >= 0) execution.combo(this, activeStrike, t, before, windows, lockedYaw);
        if (t >= SamuraiRig.get(phase()).length(clip())) {
            if (execution.lastStrike() == 2) recordMiss(2);
            finishRecovery(BossConfig.RECOVERY.get());
        }
    }

    float freezeBladeAim() {
        float pitch = aimPitch(0);
        entityData.set(AIM, pitch);
        entityData.set(OLD_AIM, pitch);
        return pitch;
    }

    void executingStrike(int strike) {
        behaviorReason = "第" + (strike + 1) + "刀有效挥砍，锁方向与逐刀命中记录";
    }

    void recordMiss(int strike) {
        if (hitDiagnostics.miss(strike) && debug())
            GeneratedMod.LOG.info("[银色武士刀路] Boss={} {}", getId(), hitDiagnostics.lastContact());
    }

    private void spacingBack(double distance) {
        Vec3 delta =
                new Vec3(
                                Math.sin(Math.toRadians(lockedYaw)),
                                0,
                                -Math.cos(Math.toRadians(lockedYaw)))
                        .scale(distance);
        Vec3 dest = position().add(delta);
        if (terrainNavigation.safeStep(position(), dest)) move(MoverType.SELF, delta);
    }

    private void clearingTick() {
        stopMoving();
        if (!targetValid(getTarget()) || !terrainNavigation.clearingTick()) {
            finishRecovery(10);
            return;
        }
        float t = stateAge(0), before = Math.max(0, t - .05f);
        if (t < .23f) {
            var block = terrainNavigation.nextObstruction();
            if (block != null) {
                double distance =
                        Math.sqrt(
                                Vec3.atCenterOf(block)
                                        .subtract(position())
                                        .horizontalDistanceSqr());
                entityData.set(CLEAR_DISTANCE, (float) distance);
                entityData.set(CLEAR_HEIGHT, (float) (block.getY() + .5 - getY()));
                face(Vec3.atCenterOf(block), 10);
                lockedYaw = getYRot();
            }
        } else {
            setYRot(lockedYaw);
            yBodyRot = yHeadRot = lockedYaw;
        }
        if (t >= ClearingMotion.START && before < ClearingMotion.END) {
            if (execution.lastStrike() != 0) {
                execution.startClearStrike();
                playSound(SoundEvents.PLAYER_ATTACK_SWEEP, .9f, .6f);
            }
            execution.sweep(
                    this,
                    Math.max(before, ClearingMotion.START),
                    Math.min(t, ClearingMotion.END),
                    0,
                    position());
        }
        if (t >= ClearingMotion.DURATION) {
            if (debug())
                GeneratedMod.LOG.info(
                        "[银色武士清障] 本刀破坏={} 目标高度={} 距离={}",
                        execution.brokenBlocks(),
                        entityData.get(CLEAR_HEIGHT),
                        entityData.get(CLEAR_DISTANCE));
            if (execution.brokenBlocks() == 0) clearMisses++;
            else clearMisses = 0;
            if (clearMisses >= 2) {
                terrainNavigation.clearMiss();
                finishRecovery(10);
                return;
            }
            entityData.set(TRANSITION_POSE, DeathMotion.encode(currentPose(0), 0));
            entityData.set(START, level().getGameTime());
            execution.resetClearing();
        }
    }

    void lunge(double distance) {
        if (distance <= 0 || ActionRules.motion(state()) != ActionRules.MotionOwner.COMBAT) return;
        Vec3 d =
                new Vec3(
                                -Math.sin(Math.toRadians(lockedYaw)),
                                0,
                                Math.cos(Math.toRadians(lockedYaw)))
                        .scale(distance);
        int steps = Math.max(1, (int) Math.ceil(distance / .18));
        d = d.scale(1.0 / steps);
        for (int i = 0; i < steps; i++) {
            var box = getBoundingBox().move(d);
            double lift = 0;
            if (!level().noCollision(this, box)) {
                if (level().noCollision(this, box.move(0, .6, 0))) lift = .6;
                else break;
            }
            if (level().noCollision(this, box.move(0, lift - .8, 0))) break;
            Vec3 step = d.add(0, lift, 0);
            if (!terrainNavigation.safeStep(position(), position().add(step))) break;
            move(MoverType.SELF, step);
        }
        setDeltaMovement(0, getDeltaMovement().y, 0);
    }

    public double range(int strike) {
        return phase() == 1
                ? BossConfig.RANGE_ONE.get()
                : strike == 2 ? BossConfig.RANGE_HEAVY.get() : BossConfig.RANGE_TWO.get();
    }

    public boolean consumeParticle() {
        return particlesThisTick++ < BossConfig.PARTICLES.get();
    }

    public boolean canBreakMore() {
        return execution.brokenBlocks()
                < Math.min(
                        BossConfig.BREAK_MAX.get(),
                        state() == CLEAR ? BossConfig.CLEAR_STRIKE.get() : Integer.MAX_VALUE);
    }

    public void brokeBlock() {
        execution.brokeBlock();
    }

    public String report() {
        return "阶段="
                + phase()
                + "，状态="
                + stateName()
                + "，生命="
                + getHealth()
                + "，护甲="
                + getArmorValue()
                + "，基础攻击="
                + getAttributeValue(Attributes.ATTACK_DAMAGE)
                + "，目标="
                + (getTarget() == null ? "无" : getTarget().getName().getString())
                + "，当前距离="
                + String.format(java.util.Locale.ROOT, "%.2f", currentDistance)
                + "，期望距离="
                + String.format(java.util.Locale.ROOT, "%.2f", desiredDistance)
                + "，持刀="
                + weaponsDrawn()
                + "，观察者="
                + alertState.observer()
                + "，附近敌人="
                + alertState.enemy()
                + "，无威胁="
                + alertState.quiet()
                + "刻，行为原因="
                + behaviorReason
                + "，遮挡="
                + pursuit.unseen()
                + "刻，上墙="
                + wallApproach.status()
                + "，寻路="
                + terrainNavigation.report()
                + "，索敌="
                + targeting.report()
                + "，移动归属="
                + ActionRules.motion(state())
                + hitDiagnostics.report()
                + "，"
                + WolverineCompat.status();
    }

    public String stateName() {
        return switch (state()) {
            case SLEEP -> "休眠";
            case BACK_WALK -> "背刀接敌";
            case DRAW -> clip().equals(SamuraiRig.DRAW_STILL) ? "原地拔刀" : "行进拔刀";
            case CHASE -> "追击";
            case ATTACK_A, ATTACK_B -> "连斩";
            case SIDE_SWEEP -> "侧向快挥截击";
            case RECOVER -> "收势";
            case SHEATHE -> "脱战收刀";
            case TRANSITION -> "战损转场";
            case DEAD -> "死亡";
            case VAULT -> "翻越";
            case CLIMB -> "机械攀爬";
            case WALL ->
                    pursuitJump.active()
                            ? "追击跳跃·" + pursuitJump.status()
                            : "墙面动作·" + wallTraversal.status();
            case CLEAR -> "红刀清障";
            case FOOTWORK -> "重装站位步";
            case SETTLE -> "落脚停稳";
            case STOMP -> focusedKick() ? "近身短踢" : "侧腰踢蹬解围";
            case CLOSE_GUARD -> "近身持刀戒备";
            default -> "静止待机";
        };
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide
                || state() == DEAD
                || deathSettled
                || BossImmunity.blocked(source)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) return false;
        if (!configured) applyConfig(true);
        if (!Float.isFinite(amount) || amount <= 0) return false;
        boolean armored = false;
        if (!CombatRules.absolute(source)) {
            if (state() == TRANSITION && age(0) * 20 < BossConfig.INVULNERABLE_TICKS.get())
                amount *= 1 - BossConfig.TRANSITION_REDUCTION.get();
            if (phase() == 1 && CombatRules.damageClass(source).equals("physical")) {
                double bypass =
                        WolverineCompat.fullRageClaw(source) ? BossConfig.CLAW_BYPASS.get() : 0;
                armored = BossConfig.PHYSICAL_REDUCTION.get() * (1 - bypass) > .5;
            }
        }
        amount *= 1 - DefensePolicy.reduction(this, source);
        float healthBefore = getHealth();
        boolean accepted = amount > 0 && super.hurt(source, amount);
        float effective = Math.max(0, healthBefore - getHealth());
        if (armored
                && isAlive()
                && (accepted || amount == 0)
                && tickCount - lastArmorFeedback >= 5) {
            lastArmorFeedback = tickCount;
            CombatFeedback.armor(this, source, !accepted);
        }
        if (accepted && isAlive()) {
            if (BossConfig.FEEDBACK.get()
                    && effective > 0
                    && reactionAllowed()
                    && tickCount - lastReaction >= BossConfig.HIT_REACTION_COOLDOWN.get()) {
                boolean strong = effective >= (phase() == 2 ? 5 : 8),
                        physical = CombatRules.damageClass(source).equals("physical");
                float power =
                        strong
                                ? (phase() == 2 ? 1.2f : .75f)
                                : physical && phase() == 1 ? .10f : .25f;
                entityData.set(HIT_START, level().getGameTime());
                entityData.set(HIT_POWER, power);
                Vec3 origin = source.getSourcePosition();
                entityData.set(
                        HIT_SIDE,
                        origin != null && Mth.wrapDegrees(yawTo(origin) - getYRot()) < 0
                                ? -1f
                                : 1f);
                lastReaction = tickCount;
                if (strong) {
                    reactionUntil = tickCount + 4;
                    behaviorReason = "有效重击的短局部受力，不强制打断连招或攀爬";
                }
            }
            if (phase() == 1 && getHealth() <= BossConfig.PHASE_HEALTH.get()) phaseRequested = true;
            if (BossConfig.RETALIATE.get()
                    && source.getEntity() instanceof LivingEntity attacker
                    && !CombatRules.friend(this, attacker)) {
                targeting.forgetFailure(attacker);
                targeting.threat.merge(attacker.getUUID(), effective, Float::sum);
                LivingEntity current = getTarget();
                if (current == null
                        || !targetValid(current)
                        || targeting.threat.get(attacker.getUUID())
                                > Math.max(
                                        24,
                                        targeting.threat.getOrDefault(current.getUUID(), 0f) * 2)) {
                    if (ActionRules.retarget(state()) && state() != VAULT && state() != CLIMB) {
                        terrainNavigation.cancel();
                        setTarget(attacker);
                        pursuit.confirm(attacker.position());
                    }
                }
            }
        }
        return accepted;
    }

    @Override
    public void die(DamageSource source) {
        if (state() == DEAD || deathSettled) return;
        SamuraiRig.Pose snapshot = currentPose(0);
        float red = redProgress(0);
        if (phase() == 2) {
            boolean[] visible = snapshot.visible().clone();
            for (int i = 0; i < 8; i++)
                if (shed(i)) visible[SamuraiRig.get(2).bone(DebrisGeometry.NAMES[i])] = false;
            snapshot = new SamuraiRig.Pose(snapshot.matrices(), visible);
        }
        super.die(source);
        if (!dead) return; // 尊重其他模组取消死亡；只执行一次原版掉落结算。
        if (!level().isClientSide) {
            if (state() == TRANSITION) {
                DebrisEntity.releaseDue(this, age(0));
                boolean[] visible = snapshot.visible().clone();
                for (int i = 0; i < 8; i++)
                    if (shed(i)) visible[SamuraiRig.get(2).bone(DebrisGeometry.NAMES[i])] = false;
                snapshot = new SamuraiRig.Pose(snapshot.matrices(), visible);
            }
            beginDeath(snapshot, red);
        }
    }

    private void beginDeath(SamuraiRig.Pose snapshot, float red) {
        wallApproach.clear();
        wallTraversal.cancel();
        pursuitJump.cancel();
        deathSettled = true;
        corpseHold = BossConfig.CORPSE.get();
        lockedYaw = getYRot();
        entityData.set(DEATH_SURFACE, DeathContacts.capture(this).save());
        entityData.set(
                DEATH_LANDED, terrainNavigation.support(SamuraiNavigation.point(position())));
        entityData.set(DEATH_POSE, DeathMotion.encode(snapshot, red));
        entityData.set(START, level().getGameTime());
        entityData.set(RATE, 1f);
        entityData.set(STATE, DEAD);
        terrainNavigation.cancel();
        stopMoving();
        setDeltaMovement(Vec3.ZERO);
        setTarget(null);
        phaseRequested = false;
        execution.clearContacts();
        targeting.threat.clear();
        bar.removeAllPlayers();
    }

    @Override
    public void tick() {
        oldWall = clientWall;
        Vec3 before = position();
        oldDrawGait = clientDrawGait;
        previousCycles = clientCycles;
        oldStepProgress = clientStepProgress;
        oldBodyYaw = clientBodyYaw;
        oldLookYaw = clientLookYaw;
        oldLookPitch = clientLookPitch;
        oldLookWeight = clientLookWeight;
        oldTerrainFrame = clientTerrainFrame;
        particlesThisTick = 0;
        if (!level().isClientSide && state() == DEAD) {
            setHealth(0);
            dead = true;
        }
        if (!level().isClientSide && isAlive()) clearForbiddenEffects();
        super.tick();
        if (!level().isClientSide
                && isNoGravity()
                && !wallTraversal.active()
                && !terrainNavigation.controlsGravity()) setNoGravity(false);
        if (!level().isClientSide && (state() == CHASE || state() == BACK_WALK) && isAlive()) {
            brakeMotion = position().subtract(before).multiply(1, 0, 1);
            double distance = terrainNavigation.walkDistance();
            if (distance < 1.5 && distance > .0001) {
                entityData.set(
                        CYCLES,
                        entityData.get(CYCLES)
                                + (float)
                                        (distance
                                                / (BossConfig.STRIDE.get()
                                                        * BossConfig.STRIDE_SCALE.get())));
                CombatFeedback.footsteps(this, footTracker);
            }
        }
        if (!level().isClientSide) {
            entityData.set(BODY_YAW, getYRot());
            if (isAlive()) updateHeadLook();
        }
        if (!level().isClientSide) {
            lastHorizontalTravel = position().subtract(before).horizontalDistance();
            if (state() == DRAW && clip().equals(SamuraiRig.DRAW)) {
                if (lastHorizontalTravel < .008 || lastHorizontalTravel >= 1.5) drawWalking = false;
                else if (lastHorizontalTravel > .025) drawWalking = true;
                entityData.set(
                        DRAW_GAIT,
                        Mth.approach(entityData.get(DRAW_GAIT), drawWalking ? 1f : 0f, .25f));
            }
        }
        clientWall =
                new Vec3(entityData.get(WALL_X), entityData.get(WALL_Y), entityData.get(WALL_Z));
        clientDrawGait = entityData.get(DRAW_GAIT);
        clientCycles = entityData.get(CYCLES);
        clientStepProgress = entityData.get(STEP_PROGRESS);
        clientBodyYaw = entityData.get(BODY_YAW);
        clientLookYaw = entityData.get(LOOK_YAW);
        clientLookPitch = entityData.get(LOOK_PITCH);
        clientLookWeight = entityData.get(LOOK_WEIGHT);
        clientTerrainFrame = terrainFrame();
        if (clientMotionStart != entityData.get(START)) {
            clientMotionStart = entityData.get(START);
            oldStepProgress = clientStepProgress;
            oldWall = clientWall;
        }
        if (tickCount <= 1) {
            oldDrawGait = clientDrawGait;
            oldBodyYaw = clientBodyYaw;
            oldStepProgress = clientStepProgress;
            previousCycles = clientCycles;
            oldTerrainFrame = clientTerrainFrame;
        }
        if (!level().isClientSide
                && (state() == IDLE
                        || state() == SLEEP
                        || state() == SHEATHE
                        || state() == DEAD
                        || state() == TRANSITION
                        || state() == RECOVER)) stopMoving();
    }

    @Override
    protected void tickDeath() {
        if (level().isClientSide) return;
        if (state() != DEAD) beginDeath(currentPose(0), redProgress(0)); // 兼容直接将Health写为0的来源。
        stopMoving();
        setTarget(null);
        setYRot(lockedYaw);
        yBodyRot = yHeadRot = lockedYaw;
        if (!entityData.get(DEATH_LANDED)) {
            // 半空死亡立即取消主动移动，只以原实体碰撞箱受重力落地；不传送到预设落点。
            if (onGround() || terrainNavigation.support(SamuraiNavigation.point(position()))) {
                entityData.set(DEATH_SURFACE, DeathContacts.capture(this).save());
                entityData.set(DEATH_LANDED, true);
                entityData.set(START, level().getGameTime() - 3);
            } else if (stateAge(0) > 20) {
                bar.removeAllPlayers();
                remove(RemovalReason.KILLED);
            }
            return;
        }
        deathTime = (int) (deathElapsed(0) * 20);
        if (deathTime >= 16
                && deathTime <= 40
                && (deathFeedback & 1) == 0
                && CombatFeedback.deathContact(this, false)) deathFeedback |= 1;
        if (deathTime >= 55
                && deathTime <= 76
                && (deathFeedback & 2) == 0
                && CombatFeedback.deathContact(this, true)) deathFeedback |= 2;
        if (deathTime >= DeathMotion.DURATION_TICKS + corpseHold) {
            bar.removeAllPlayers();
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        wallTraversal.save(tag);
        pursuitJump.save(tag);
        rangedBudget.save(tag, level().getGameTime());
        var death =
                state() == DEAD
                        ? new SamuraiSaveData.Death(
                                deathSettled,
                                entityData.get(DEATH_SURFACE),
                                entityData.get(DEATH_LANDED),
                                entityData.get(DEATH_POSE),
                                (int) (stateAge(0) * 20),
                                corpseHold,
                                deathFeedback,
                                lockedYaw)
                        : null;
        new SamuraiSaveData(
                        phase(),
                        phaseRequested,
                        configured,
                        entityData.get(SHED),
                        drawn() || state() == DRAW,
                        alertState.quiet(),
                        state(),
                        visualTime(0),
                        stableSide,
                        closeDefense.crowdRemaining(),
                        state() != DEAD ? DeathMotion.encode(currentPose(0), 0) : null,
                        death)
                .write(tag);
        closeDefense.save(tag);
        tag.putInt(
                "SamuraiSideCooldown",
                (int) Math.max(0, Math.min(400, nextSideSweep - level().getGameTime())));
        if (state() == DRAW) {
            tag.putString("SamuraiDrawClip", clip());
            tag.putFloat("SamuraiDrawGait", entityData.get(DRAW_GAIT));
        }
        long redStart = entityData.get(RED_START);
        tag.putLong(
                "SamuraiRedChargeRemaining",
                redStart < 0
                        ? 0
                        : Math.max(
                                0,
                                redStart + RedBladeTiming.chargeTicks() - level().getGameTime()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        pursuitJump.cancel();
        wallTraversal.cancel();
        wallOffset(Vec3.ZERO);
        wallApproach.clear();
        rangedBudget.load(tag, level().getGameTime());
        var saved = SamuraiSaveData.read(tag, BossConfig.CORPSE.get(), getYRot(), getHealth() <= 0);
        closeDefense.load(tag, saved.stompCooldown());
        terrainNavigation.cancel();
        setNoGravity(false);
        entityData.set(PHASE, saved.phase());
        configured = saved.configured();
        long chargeLeft =
                Math.min(
                        RedBladeTiming.totalTicks(),
                        Math.max(0, tag.getLong("SamuraiRedChargeRemaining")));
        entityData.set(
                RED_START,
                chargeLeft == 0
                        ? -1
                        : level().getGameTime() + chargeLeft - RedBladeTiming.chargeTicks());
        nextSideSweep =
                level().getGameTime() + Mth.clamp(tag.getInt("SamuraiSideCooldown"), 0, 400);
        nextSideDecision = 0;
        attackTracking.sample(null, level().getGameTime());
        // 活体重载不重播转场，死亡重载不调用die、不结算第二次掉落。
        entityData.set(SHED, tag.getBoolean("SamuraiDead") ? saved.shed() : phase() == 2 ? 255 : 0);
        setDrawn(phase() == 2 || saved.drawn());
        alertState.restore(saved.quiet());
        stableSide = saved.side();
        setTarget(null);
        execution.clearContacts();
        applyConfig(tag.contains("Health") || configured);
        if (saved.death() != null) {
            entityData.set(DEATH_SURFACE, saved.death().surface());
            entityData.set(DEATH_LANDED, saved.death().landed());
            dead = true;
            deathSettled = true;
            setHealth(0);
            entityData.set(STATE, DEAD);
            entityData.set(DEATH_POSE, saved.death().pose());
            entityData.set(RATE, 1f);
            int elapsed = saved.death().elapsed();
            entityData.set(START, level().getGameTime() - elapsed);
            deathTime = elapsed;
            corpseHold = saved.death().hold();
            deathFeedback = saved.death().feedback();
            lockedYaw = saved.death().yaw();
            phaseRequested = false;
            stopMoving();
            bar.removeAllPlayers();
        } else {
            phaseRequested =
                    phase() == 1
                            && (saved.phaseRequested()
                                    || getHealth() <= BossConfig.PHASE_HEALTH.get());
            enter(IDLE, idleClip());
            if (saved.loadPose() != null) {
                int savedState = saved.state();
                recoveryTicks = 10;
                cooldown = 10;
                entityData.set(RECOVERY_BLEND, .5f);
                entityData.set(MOTION_POSE, saved.loadPose());
                entityData.set(STATE, RECOVER);
                entityData.set(START, level().getGameTime());
                if (phase() == 1 && savedState == DRAW) {
                    setDrawn(false);
                    entityData.set(STATE, DRAW);
                    entityData.set(
                            CLIP,
                            SamuraiRig.DRAW_STILL.equals(tag.getString("SamuraiDrawClip"))
                                    ? SamuraiRig.DRAW_STILL
                                    : SamuraiRig.DRAW);
                    entityData.set(
                            START,
                            level().getGameTime()
                                    - Math.round(Math.min(drawLength(), saved.time()) * 20));
                    float gait =
                            tag.contains("SamuraiDrawGait")
                                    ? Mth.clamp(tag.getFloat("SamuraiDrawGait"), 0, 1)
                                    : 1;
                    entityData.set(DRAW_GAIT, clip().equals(SamuraiRig.DRAW) ? gait : 0f);
                    oldDrawGait = clientDrawGait = entityData.get(DRAW_GAIT);
                    drawWalking = gait > .5f;
                }
                if (phase() == 1 && savedState == SHEATHE) {
                    setDrawn(true);
                    entityData.set(STATE, SHEATHE);
                    entityData.set(CLIP, SamuraiRig.SHEATHE);
                    entityData.set(SHEATHE_REVERSE, Math.min(3, saved.time()));
                }
            }
            entityData.set(BODY_YAW, getYRot());
            wallTraversal.load(tag);
            if (!wallTraversal.active()) pursuitJump.load(tag);
        }
    }

    @Override
    protected void playStepSound(
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state) {}

    @Override
    public double getAttributeValue(Attribute attribute) {
        if (attribute != Attributes.ARMOR) return super.getAttributeValue(attribute);
        // 原版ARMOR属性在30处截断；仅本Boss的战斗读取扩展到40，保留其他模组的加减乘修正。
        AttributeInstance instance = getAttribute(attribute);
        if (instance == null) return 0;
        double base = instance.getBaseValue();
        for (AttributeModifier mod : instance.getModifiers(AttributeModifier.Operation.ADDITION))
            base += mod.getAmount();
        double value = base;
        for (AttributeModifier mod :
                instance.getModifiers(AttributeModifier.Operation.MULTIPLY_BASE))
            value += base * mod.getAmount();
        for (AttributeModifier mod :
                instance.getModifiers(AttributeModifier.Operation.MULTIPLY_TOTAL))
            value *= 1 + mod.getAmount();
        return Mth.clamp(value, 0, 1000);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public net.minecraft.network.protocol.Packet<
                    net.minecraft.network.protocol.game.ClientGamePacketListener>
            getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        if (isAlive()) bar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bar.removePlayer(p);
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return phase() == 2 ? GeneratedMod.ARMOR_GROAN.get() : GeneratedMod.ARMOR_SCRAPE.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }
}
