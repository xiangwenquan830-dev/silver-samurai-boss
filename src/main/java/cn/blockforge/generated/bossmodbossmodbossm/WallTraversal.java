package cn.blockforge.generated.bossmodbossmodbossm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/** One owner for wall locomotion, authored weapon transfer and interruption recovery. */
final class WallTraversal {
    enum Stage {
        SHEATHE,
        REACH,
        FIRST,
        CLIMB,
        TOP,
        DRAW,
        CATCH,
        HOLD,
        RELEASE,
        STAB,
        BRACE,
        EXTRACT,
        FALL,
        DROP
    }

    private final SamuraiEntity boss;
    private Stage stage;
    private WallGeometry.Contact contact;
    private Vec3 start = Vec3.ZERO, roof, previousRoot = Vec3.ZERO;
    private long started, nextAttempt;
    private int stalls, cycles;
    private boolean red, cut, armed;
    private double rise = 1.35, approach = 1;

    WallTraversal(SamuraiEntity boss) {
        this.boss = boss;
    }

    boolean active() {
        return stage != null;
    }

    String status() {
        return stage == null ? "无" : stage.name();
    }

    private long now() {
        return boss.level().getGameTime();
    }

    private String clip(Stage s) {
        return clip(s, red);
    }

    private String clip(Stage s, boolean red) {
        return switch (s) {
            case SHEATHE -> WallClips.sheathe(boss.phase());
            case REACH -> WallClips.REACH;
            case FIRST -> WallClips.FIRST;
            case CLIMB -> WallClips.LOOP;
            case TOP -> WallClips.TOP;
            case DRAW -> WallClips.draw(boss.phase());
            case CATCH -> WallClips.CATCH;
            case HOLD -> WallClips.HOLD;
            case RELEASE -> WallClips.RELEASE;
            case STAB -> red ? WallClips.RED_STAB : WallClips.STAB;
            case FALL -> WallClips.RELEASE;
            case DROP -> boss.phase() == 1 ? "armed_walk" : SamuraiRig.WALK_TWO;
            case BRACE -> red ? WallClips.RED_BRACE : WallClips.BRACE;
            case EXTRACT -> red ? WallClips.RED_EXTRACT : WallClips.EXTRACT;
        };
    }

    private Vec3 root(float t) {
        return SamuraiRig.get(boss.phase()).rootMotion(clip(stage), t);
    }

    private void enter(Stage s) {
        String name = clip(s);
        boss.wallEnter(name);
        stage = s;
        start = boss.position();
        previousRoot = root(0);
        stalls = 0;
        if (s == Stage.SHEATHE || s == Stage.REACH) {
            double distance = contact.face().subtract(start).dot(contact.forward());
            double end = s == Stage.SHEATHE ? 2.55 : 1.485;
            double authored =
                    -(root(SamuraiRig.get(boss.phase()).length(name)).z - previousRoot.z)
                            * SamuraiRig.SCALE;
            approach = authored > .001 ? Mth.clamp((distance - end) / authored, 0, 1.7) : 0;
        }
        boss.setNoGravity(
                s != Stage.SHEATHE
                        && s != Stage.REACH
                        && s != Stage.DRAW
                        && s != Stage.FALL
                        && s != Stage.DROP);
        if (s == Stage.FALL)
            boss.wallResume(SamuraiRig.get(boss.phase()).length(WallClips.RELEASE));
        if (s == Stage.REACH) boss.wallDrawn(false);
        if (s == Stage.DRAW) boss.wallDrawn(false);
    }

    boolean tryStart() {
        if (active() || !BossConfig.WALL_CLIMB.get() || now() < nextAttempt || !boss.isAlive())
            return false;
        int state = boss.state();
        if (state == SamuraiEntity.TRANSITION
                || state == SamuraiEntity.DEAD
                || state == SamuraiEntity.DRAW
                || state == SamuraiEntity.SHEATHE) return false;
        boolean falling =
                !boss.onGround() && boss.getDeltaMovement().y < -.18 && boss.fallDistance > .8f;
        if (!falling && !ActionRules.canBeginAttack(state)) return false;
        var target = boss.getTarget();
        if (!boss.targetValid(target)) {
            if (!falling) return false;
            target = null;
        }
        if (!falling && (target == null || target.getY() < boss.getY() + 2.5)) return tryDrop();
        if (!falling
                && target != null
                && Targeting.visible(boss, target)
                && boss.canStartAttack(target)) return false;
        nextAttempt = now() + 8;
        Vec3 toward =
                target != null
                        ? target.position()
                        : boss.position()
                                .add(
                                        -Math.sin(Math.toRadians(boss.getYRot())),
                                        0,
                                        Math.cos(Math.toRadians(boss.getYRot())));
        WallGeometry.Contact c = WallGeometry.find(boss, toward, falling ? 5.2 : 4.3);
        if (c == null
                || boss.wallApproach().blocked(c)
                || !WallGeometry.fits(boss, c.stand(boss.getY()))) return false;
        if (!falling
                && !boss.weaponsDrawn()
                && c.face().subtract(boss.position()).dot(c.forward()) > 2.2) return false;
        Vec3 top = WallGeometry.roof(boss, c, boss.getY());
        if (!falling && top == null) return false;
        float yaw = (float) Math.toDegrees(Math.atan2(c.forward().z, c.forward().x)) - 90;
        boss.facePoint(boss.position().add(c.forward()), falling ? 24 : 12);
        if (Math.abs(Mth.wrapDegrees(yaw - boss.getYRot())) > 8) return false;
        contact = c;
        roof = top;
        armed = boss.weaponsDrawn();
        red = boss.redReady();
        cut = false;
        cycles = 0;
        started = now();
        boss.terrainNavigation().cancel();
        boss.setYRot(yaw);
        boss.yBodyRot = boss.yHeadRot = yaw;
        enter(falling ? (armed ? Stage.STAB : Stage.CATCH) : (armed ? Stage.SHEATHE : Stage.REACH));
        nextAttempt = now() + 30;
        return true;
    }

    boolean ready() {
        return !active()
                && now() >= nextAttempt
                && boss.onGround()
                && ActionRules.canBeginAttack(boss.state());
    }

    boolean startAt(WallGeometry.Contact c, Vec3 landing) {
        if (!ready()) return false;
        Vec3 top = landing;
        if (top == null
                || boss.wallApproach().blocked(c)
                || !WallGeometry.routeClear(boss, c, boss.getY(), top)
                || !WallGeometry.fits(boss, top)
                || !boss.terrainNavigation().support(SamuraiNavigation.point(top))
                || !WallGeometry.grips(boss, c, boss.getY())) return false;
        contact = c;
        roof = top;
        armed = boss.weaponsDrawn();
        red = boss.redReady();
        cut = false;
        cycles = 0;
        started = now();
        boss.terrainNavigation().cancel();
        float yaw = (float) Math.toDegrees(Math.atan2(c.forward().z, c.forward().x)) - 90;
        boss.setYRot(yaw);
        boss.yBodyRot = boss.yHeadRot = yaw;
        enter(armed ? Stage.SHEATHE : Stage.REACH);
        nextAttempt = now() + 30;
        return true;
    }

    private Vec3 localWorld(Vec3 local) {
        return SamuraiRig.world(local.scale(SamuraiRig.SCALE), Vec3.ZERO, boss.getYRot());
    }

    private boolean tryDrop() {
        var target = boss.getTarget();
        if (!boss.onGround()
                || !boss.weaponsDrawn()
                || target == null
                || target.getY() > boss.getY() - 2
                || now() < nextAttempt) return false;
        nextAttempt = now() + 8;
        Vec3 toward = target.position().subtract(boss.position()).multiply(1, 0, 1);
        if (toward.lengthSqr() < .04)
            toward =
                    new Vec3(
                            -Math.sin(Math.toRadians(boss.getYRot())),
                            0,
                            Math.cos(Math.toRadians(boss.getYRot())));
        toward = toward.normalize();
        Vec3 edge = null, landing = null, forward = null;
        double bottom =
                BossConfig.UNLIMITED_PURSUIT_DROP.get()
                        ? boss.level().getMinBuildHeight()
                        : Math.max(
                                boss.level().getMinBuildHeight(),
                                boss.getY() - BossConfig.PURSUIT_DROP.get());
        // First solid surface only; never search through floors or descend into the void.
        search:
        for (int angle : new int[] {0, 35, -35, 70, -70, 110, -110, 180}) {
            Vec3 direction = toward.yRot((float) Math.toRadians(angle));
            for (double distance = .5; distance <= 24; distance += .5) {
                Vec3 candidate = boss.position().add(direction.scale(distance));
                if (!WallGeometry.fits(boss, candidate)) break;
                if (boss.terrainNavigation().support(SamuraiNavigation.point(candidate))) continue;
                var hit =
                        boss.level()
                                .clip(
                                        new net.minecraft.world.level.ClipContext(
                                                candidate,
                                                new Vec3(candidate.x, bottom, candidate.z),
                                                net.minecraft.world.level.ClipContext.Block
                                                        .COLLIDER,
                                                net.minecraft.world.level.ClipContext.Fluid.NONE,
                                                boss));
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                        || hit.getDirection() != net.minecraft.core.Direction.UP) break;
                Vec3 floor = hit.getLocation();
                if (!DropIntent.useful(boss.position(), floor, target.position())
                        || boss.getY() - floor.y < 1.5
                        || floor.distanceTo(target.position())
                                >= boss.position().distanceTo(target.position()) - 1
                        || !WallGeometry.fits(boss, floor)
                        || !boss.terrainNavigation().support(SamuraiNavigation.point(floor))) break;
                boolean clear = true;
                for (double d = .2; d <= distance + .001; d += .2)
                    if (!WallGeometry.fits(boss, boss.position().add(direction.scale(d)))) {
                        clear = false;
                        break;
                    }
                if (!clear) break;
                for (double y = candidate.y; y > floor.y; y -= .5)
                    if (!WallGeometry.fits(boss, new Vec3(candidate.x, y, candidate.z))) {
                        clear = false;
                        break;
                    }
                if (clear) {
                    edge = candidate;
                    landing = floor;
                    forward = direction;
                    break search;
                }
                break; // Never cross an unchecked void gap to reach a more distant landing.
            }
        }
        if (edge == null) return false;
        // Commit the checked departure before turning; ordinary chase must not undo it.
        contact = new WallGeometry.Contact(edge, forward);
        roof = landing;
        armed = true;
        red = boss.redReady();
        cut = false;
        started = now();
        boss.terrainNavigation().cancel();
        enter(Stage.DROP);
        return true;
    }

    private boolean moveTo(Vec3 destination) {
        Vec3 delta = destination.subtract(boss.position());
        if (delta.length() > 1.1) return false;
        int n = Math.max(1, (int) Math.ceil(delta.length() / .12));
        Vec3 step = delta.scale(1.0 / n);
        for (int i = 0; i < n; i++) {
            if (!WallGeometry.fits(boss, boss.position().add(step))) return false;
            boss.move(MoverType.SELF, step);
        }
        return boss.position().distanceToSqr(destination) < .0025;
    }

    void tick() {
        if (stage == null) {
            boss.wallFinish();
            return;
        }
        if (now() - started > BossConfig.WALL_TIME.get()) {
            abort();
            return;
        }
        if (stage == Stage.FALL) {
            boss.setNoGravity(false);
            if (boss.onGround()) {
                enter(Stage.DRAW);
                return;
            }
            if (boss.stateAge(0) > 1.2f && boss.getDeltaMovement().y < -.18) {
                var c = WallGeometry.find(boss, boss.position().add(contact.forward()), 2.2);
                if (c != null
                        && !boss.wallApproach().blocked(c)
                        && WallGeometry.fits(boss, c.stand(boss.getY()))) {
                    contact = c;
                    enter(Stage.CATCH);
                }
            }
            return;
        }
        if (stage == Stage.DROP) {
            boss.setNoGravity(false);
            // Keep falling until touchdown; an early exit would trigger wall catching.
            if (boss.onGround() && boss.getY() < start.y - .5) {
                finish();
                nextAttempt = now() + 2;
                return;
            }
            var target = boss.getTarget();
            if (boss.onGround() && (!boss.targetValid(target) || target.getY() > boss.getY() - 2)) {
                finish();
                return;
            }
            if (boss.onGround()
                    && (roof == null
                            || !DropIntent.useful(boss.position(), roof, target.position())
                            || !WallGeometry.fits(boss, roof)
                            || !boss.terrainNavigation().support(SamuraiNavigation.point(roof)))) {
                finish();
                return;
            }
            Vec3 remaining = contact.face().subtract(boss.position()).multiply(1, 0, 1);
            if (remaining.lengthSqr() > .0025) {
                boss.facePoint(contact.face(), 18);
                float wanted = (float) Math.toDegrees(Math.atan2(remaining.z, remaining.x)) - 90;
                if (Math.abs(Mth.wrapDegrees(wanted - boss.getYRot())) > 20) return;
                Vec3 d = remaining.normalize().scale(Math.min(.24, remaining.length()));
                if (WallGeometry.fits(boss, boss.position().add(d))) boss.move(MoverType.SELF, d);
                else {
                    finish();
                    return;
                }
            }
            return;
        }
        boolean groundPhase = stage == Stage.SHEATHE || stage == Stage.REACH || stage == Stage.DRAW;
        if (groundPhase) boss.setDeltaMovement(0, boss.getDeltaMovement().y, 0);
        else {
            boss.setDeltaMovement(Vec3.ZERO);
            boss.fallDistance = 0;
        }
        if (stage == Stage.TOP
                && (roof == null
                        || !boss.terrainNavigation().support(SamuraiNavigation.point(roof)))) {
            abort();
            return;
        }
        float t = boss.stateAge(0), length = SamuraiRig.get(boss.phase()).length(clip(stage));
        boolean hold = stage == Stage.HOLD || stage == Stage.BRACE;
        if (hold) t = 0;
        Vec3 r = root(Math.min(t, length)), r0 = root(0), delta = r.subtract(r0), dest = start;
        if (stage == Stage.SHEATHE || stage == Stage.REACH) {
            dest = start.add(localWorld(new Vec3(delta.x, 0, delta.z * approach)));
        } else if (stage == Stage.FIRST || stage == Stage.CLIMB) {
            double p = Math.min(1, t / length);
            double y = stage == Stage.FIRST ? Math.max(0, delta.y * SamuraiRig.SCALE) : rise * p;
            dest = start.add(0, y, 0);
        } else if (stage == Stage.TOP) {
            // The full standing collision box goes up before going over the lip. The authored
            // articulated body follows its knee/hip arc with a bounded visual root offset.
            double p = Math.min(1, t / length), h = roof.y - start.y;
            double up = SamuraiRig.smooth((float) (p / .42));
            double over = SamuraiRig.smooth((float) ((p - .42) / .48));
            dest =
                    new Vec3(
                            Mth.lerp(over, start.x, roof.x),
                            start.y + h * up,
                            Mth.lerp(over, start.z, roof.z));
        } else if (stage != Stage.DRAW && !hold) {
            dest = start.add(localWorld(delta));
            if (stage == Stage.CATCH || stage == Stage.STAB) {
                double p = SamuraiRig.smooth(Math.min(1, t / (stage == Stage.CATCH ? .75f : .5f)));
                Vec3 stand = contact.stand(dest.y);
                // Braced swords use authored root z=+2; empty hands use z=-2.8.
                // Preserve the 4.8-model-unit difference so planted boots do not sink into the
                // wall.
                if (stage == Stage.STAB)
                    stand = stand.subtract(contact.forward().scale(4.8 * SamuraiRig.SCALE));
                dest =
                        new Vec3(
                                Mth.lerp(p, start.x, stand.x),
                                dest.y,
                                Mth.lerp(p, start.z, stand.z));
            }
        }
        if (stage == Stage.DRAW) dest = boss.position();
        if ((stage == Stage.SHEATHE || stage == Stage.REACH)
                && !boss.terrainNavigation().safeStep(boss.position(), dest)) {
            abort();
            return;
        }
        boolean moved = moveTo(dest);
        if (!moved && boss.terrainNavigation().support(SamuraiNavigation.point(boss.position()))) {
            if (stage == Stage.STAB) {
                enter(Stage.BRACE);
                return;
            }
            if (stage == Stage.EXTRACT || stage == Stage.RELEASE) {
                moved = moveTo(new Vec3(dest.x, boss.getY(), dest.z));
                if (moved) start = start.add(0, boss.getY() - dest.y, 0);
            }
        }
        if (!moved) {
            stalls++;
            if (stalls >= 5) {
                abort();
                return;
            }
        } else stalls = 0;
        boss.setNoGravity(stage != Stage.SHEATHE && stage != Stage.REACH && stage != Stage.DRAW);
        if (!groundPhase) boss.setDeltaMovement(Vec3.ZERO);
        Vec3 offset = Vec3.ZERO;
        if (stage == Stage.TOP) {
            Vec3 end = root(length).subtract(r0);
            double py = Math.abs(end.y) > .01 ? delta.y / end.y : 0,
                    pz = Math.abs(end.z) > .01 ? delta.z / end.z : 0;
            Vec3 authored =
                    new Vec3(
                                    Mth.lerp(pz, start.x, roof.x),
                                    Mth.lerp(py, start.y, roof.y),
                                    Mth.lerp(pz, start.z, roof.z))
                            .add(localWorld(new Vec3(delta.x, 0, 0)));
            Vec3 world = authored.subtract(boss.position());
            if (world.length() > 2.7) {
                abort();
                return;
            }
            offset = SamuraiRig.local(world, boss.getYRot()).scale(1 / SamuraiRig.SCALE);
        }
        boss.wallOffset(offset);
        if (stage == Stage.STAB && red && !cut && t >= .42f) {
            cut = true;
            BladeCombat.wallContact(boss);
        }
        if (stage == Stage.STAB && t > .65f && !WallGeometry.grips(boss, contact, boss.getY())) {
            finish();
            return;
        }
        if ((stage == Stage.FIRST
                        || stage == Stage.CLIMB
                        || stage == Stage.HOLD
                        || stage == Stage.BRACE)
                && !WallGeometry.grips(boss, contact, boss.getY())) {
            // One nearby replacement, then an explicit release; never hang from deleted blocks.
            var replacement = WallGeometry.alternative(boss, contact);
            if (replacement != null) {
                contact = replacement;
                enter(armed ? Stage.STAB : Stage.CATCH);
                return;
            }
            enter(armed ? Stage.EXTRACT : Stage.RELEASE);
            return;
        }
        if (hold) {
            if (boss.stateAge(0) < .4f) return;
            advance();
        } else if (t >= length - .0001f) advance();
    }

    private void climbNext() {
        if (roof == null || !boss.terrainNavigation().support(SamuraiNavigation.point(roof)))
            roof = WallGeometry.roof(boss, contact, boss.getY() - 3.5);
        if (roof == null || ++cycles > 200) {
            enter(Stage.RELEASE);
            return;
        }
        double left = roof.y - 35.75 * SamuraiRig.SCALE - boss.getY();
        if (left <= .08) {
            enter(Stage.TOP);
            return;
        }
        rise = Math.min(1.35, left);
        enter(Stage.CLIMB);
    }

    private void advance() {
        switch (stage) {
            case SHEATHE -> enter(Stage.REACH);
            case REACH -> {
                armed = false;
                if (roof != null && roof.y - boss.getY() <= 5.25) enter(Stage.TOP);
                else enter(Stage.FIRST);
            }
            case FIRST, CLIMB -> climbNext();
            case TOP -> {
                if (roof == null
                        || !boss.terrainNavigation()
                                .support(SamuraiNavigation.point(boss.position()))) {
                    abort();
                    return;
                }
                enter(Stage.DRAW);
            }
            case DRAW -> {
                boss.wallDrawn(true);
                finish();
            }
            case CATCH -> enter(Stage.HOLD);
            case HOLD -> {
                roof = WallGeometry.roof(boss, contact, boss.getY());
                if (roof != null) enter(Stage.FIRST);
                else enter(Stage.RELEASE);
            }
            case STAB -> enter(Stage.BRACE);
            case BRACE -> enter(Stage.EXTRACT);
            case RELEASE -> enter(Stage.FALL);
            case EXTRACT -> finish();
            case FALL, DROP -> throw new IllegalStateException("Continuous gravity stage");
        }
    }

    void abort() {
        if (stage == Stage.SHEATHE
                || stage == Stage.REACH
                || stage == Stage.FIRST
                || stage == Stage.CLIMB
                || stage == Stage.TOP) boss.wallApproach().failed(contact);
        if (!armed && stage != Stage.FALL) {
            enter(Stage.FALL);
            nextAttempt = now() + 40;
        } else finish();
    }

    private void finish() {
        stage = null;
        nextAttempt = now() + 40;
        boss.setNoGravity(false);
        boss.wallFinish();
    }

    void cancel() {
        stage = null;
        boss.setNoGravity(false);
    }

    void save(CompoundTag tag) {
        if (stage == null) {
            tag.remove("SamuraiWall");
            return;
        }
        CompoundTag w = new CompoundTag();
        w.putString("Stage", stage.name());
        w.putBoolean("Armed", armed);
        w.putBoolean("Red", red);
        w.putBoolean("Cut", cut);
        w.putLong("Elapsed", Math.min(6000, now() - started));
        w.putFloat("Time", boss.stateAge(0));
        put(w, "Offset", boss.wallOffset());
        put(w, "Start", start);
        put(w, "Face", contact.face());
        put(w, "Forward", contact.forward());
        if (roof != null) put(w, "Roof", roof);
        w.putDouble("Rise", rise);
        w.putDouble("Approach", approach);
        w.putInt("Cycles", cycles);
        tag.put("SamuraiWall", w);
    }

    void load(CompoundTag tag) {
        cancel();
        boss.wallOffset(Vec3.ZERO);
        if (!tag.contains("SamuraiWall", net.minecraft.nbt.Tag.TAG_COMPOUND) || !boss.isAlive())
            return;
        CompoundTag w = tag.getCompound("SamuraiWall");
        try {
            Stage saved = Stage.valueOf(w.getString("Stage"));
            Vec3 f = get(w, "Forward"), face = get(w, "Face"), origin = get(w, "Start");
            boolean dropping = saved == Stage.DROP, falling = dropping || saved == Stage.FALL;
            double horizontalLimit = falling ? 26 : 16,
                    verticalLimit = falling ? boss.level().getHeight() : 24;
            Vec3 originDelta = origin.subtract(boss.position()),
                    faceDelta = face.subtract(boss.position());
            if (f.y != 0
                    || Math.abs(f.length() - 1) > .001
                    || originDelta.horizontalDistanceSqr() > horizontalLimit * horizontalLimit
                    || Math.abs(originDelta.y) > verticalLimit
                    || (falling
                            ? faceDelta.horizontalDistanceSqr() > 676
                                    || Math.abs(faceDelta.y) > boss.level().getHeight()
                            : faceDelta.length() > 16))
                throw new IllegalArgumentException("Invalid wall frame");
            Vec3 landing = hasVector(w, "Roof") ? get(w, "Roof") : null;
            if ((saved == Stage.TOP || dropping) && landing == null
                    || landing != null
                            && landing.distanceTo(boss.position())
                                    > (dropping ? boss.level().getHeight() + 16 : 150))
                throw new IllegalArgumentException("Invalid landing");
            double loadedRise = finiteClamp(w.getDouble("Rise"), 0, 1.35),
                    loadedApproach = finiteClamp(w.getDouble("Approach"), 0, 1.7);
            float time =
                    (float)
                            finiteClamp(
                                    w.getFloat("Time"),
                                    0,
                                    dropping || saved == Stage.FALL
                                            ? 5
                                            : SamuraiRig.get(boss.phase())
                                                    .length(clip(saved, w.getBoolean("Red"))));
            // Offset is optional for older saves; all new saves preserve the TOP pose.
            Vec3 offset = hasVector(w, "Offset") ? get(w, "Offset") : Vec3.ZERO;
            if (offset.length() * SamuraiRig.SCALE > 2.701
                    || saved != Stage.TOP && offset.lengthSqr() > .000001)
                throw new IllegalArgumentException("Invalid visual offset");
            // Validate the entire frame before changing animation, weapon state or gravity.
            contact = new WallGeometry.Contact(face, f);
            roof = landing;
            armed = w.getBoolean("Armed");
            red = w.getBoolean("Red");
            cut = w.getBoolean("Cut");
            cycles = Mth.clamp(w.getInt("Cycles"), 0, 200);
            started = now() - Math.max(0, Math.min(w.getLong("Elapsed"), 6000));
            enter(saved);
            start = origin;
            rise = loadedRise;
            approach = loadedApproach;
            boss.wallResume(time);
            boss.wallOffset(offset);
            boss.wallDrawn(
                    switch (saved) {
                        case SHEATHE -> armed;
                        case STAB, BRACE, EXTRACT, DROP -> true;
                        default -> false;
                    });
        } catch (RuntimeException invalid) {
            cancel();
            boss.wallFinish();
        }
    }

    private static boolean hasVector(CompoundTag tag, String key) {
        return tag.contains(key + "X") || tag.contains(key + "Y") || tag.contains(key + "Z");
    }

    private static double finiteClamp(double v, double lo, double hi) {
        if (!Double.isFinite(v)) throw new IllegalArgumentException("Nonfinite wall state");
        return Mth.clamp(v, lo, hi);
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    private static void put(CompoundTag t, String k, Vec3 v) {
        t.putDouble(k + "X", v.x);
        t.putDouble(k + "Y", v.y);
        t.putDouble(k + "Z", v.z);
    }

    private static Vec3 get(CompoundTag t, String k) {
        for (String axis : new String[] {"X", "Y", "Z"})
            if (!t.contains(k + axis, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC))
                throw new IllegalArgumentException("Missing wall coordinate");
        Vec3 value = new Vec3(t.getDouble(k + "X"), t.getDouble(k + "Y"), t.getDouble(k + "Z"));
        if (!finite(value)) throw new IllegalArgumentException("Nonfinite wall coordinate");
        return value;
    }
}
