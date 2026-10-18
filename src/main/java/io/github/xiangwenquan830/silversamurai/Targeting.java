package io.github.xiangwenquan830.silversamurai;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side target policy and memory. Selecting a candidate never changes an active animation.
 */
final class Targeting {
    private final SamuraiEntity boss;
    final Map<UUID, Float> threat = new HashMap<>();

    private record Failure(Vec3 position, long expires) {}

    private final Map<UUID, Failure> failures = new HashMap<>();
    private LivingEntity remembered;
    private int cursor, playerCursor;
    private long chosenAt;

    Targeting(SamuraiEntity boss) {
        this.boss = boss;
    }

    private long now() {
        return boss.level().getGameTime();
    }

    void changed() {
        chosenAt = now();
    }

    static boolean visible(SamuraiEntity boss, LivingEntity target) {
        return boss.perception().visible(target);
    }

    private int candidatesSeen, candidatesEvaluated;
    private String reason = "尚未扫描";

    String report() {
        return reason + "，候选=" + candidatesSeen + "，深入评估=" + candidatesEvaluated;
    }

    int evaluated() {
        return candidatesEvaluated;
    }

    boolean valid(LivingEntity target) {
        return target != null
                && !target.isRemoved()
                && target.level() == boss.level()
                && !CombatRules.friend(boss, target)
                && inRange(target, BossConfig.retentionRange(target));
    }

    boolean inRange(LivingEntity target, double range) {
        if (TargetGeometry.distanceSqr(boss, target) <= range * range) return true;
        double up = target.getY() - boss.getY();
        if (target == boss.getTarget()
                && up < -2
                && BossConfig.UNLIMITED_PURSUIT_DROP.get()
                && target.position().subtract(boss.position()).horizontalDistanceSqr()
                        <= range * range) return true;
        // Height is a separate climb budget. Preserve the configured horizontal radius and
        // visibility rule.
        return BossConfig.WALL_CLIMB.get()
                && up > 4.3
                && up <= BossConfig.WALL_HEIGHT.get()
                && target.position().subtract(boss.position()).horizontalDistanceSqr()
                        <= range * range;
    }

    static net.minecraft.world.phys.AABB searchBox(SamuraiEntity boss, double range) {
        var box = boss.getBoundingBox().inflate(range);
        return BossConfig.WALL_CLIMB.get()
                ? box.expandTowards(0, Math.max(0, BossConfig.WALL_HEIGHT.get() - range), 0)
                : box;
    }

    boolean playerInAcquire(net.minecraft.world.entity.player.Player player) {
        return CombatRules.enemy(boss, player, true)
                && inRange(player, BossConfig.PLAYER_ACQUIRE.get());
    }

    private boolean unavailable(LivingEntity target) {
        Failure f = failures.get(target.getUUID());
        if (f == null) return false;
        if (f.expires <= now() || f.position.distanceToSqr(target.position()) > 4) {
            failures.remove(target.getUUID());
            return false;
        }
        return true;
    }

    void failed(LivingEntity target) {
        if (target != null) {
            remembered = target;
            failures.put(
                    target.getUUID(),
                    new Failure(target.position(), now() + BossConfig.FAILED_RETRY.get()));
        }
    }

    void forgetFailure(LivingEntity target) {
        failures.remove(target.getUUID());
    }

    void obstacleChanged() {
        failures.clear();
    }

    private record Assessment(boolean immediate, double score) {}

    private Assessment assess(LivingEntity target, String clip) {
        double d = Math.sqrt(TargetGeometry.distanceSqr(boss, target)),
                dy = Math.abs(target.getY() - boss.getY());
        if (TargetGeometry.large(target) && TargetGeometry.overlapsBladeHeight(target, boss.getY()))
            dy = 0;
        // One lane query and at most one exact blade solve per candidate in this decision.
        boolean lane = CombatSpacing.lane(boss, target);
        boolean immediate =
                valid(target)
                        && d < 7
                        && lane
                        && CombatSpacing.assessLane(boss, target, clip, 0, false).attackable();
        double reachable = immediate ? -80 : !lane ? 35 + dy * 5 : d < 4.5 ? -35 : dy * 5;
        if (!immediate) reachable -= 18 * boss.terrainNavigation().approachQuality(target);
        return new Assessment(
                immediate,
                d
                        + reachable
                        - (target instanceof net.minecraft.world.entity.player.Player
                                ? BossConfig.PLAYER_PRIORITY.get()
                                : 0)
                        - Math.min(30, threat.getOrDefault(target.getUUID(), 0f) * .25));
    }

    LivingEntity select(String chosenClip) {
        failures.entrySet().removeIf(e -> e.getValue().expires <= now());
        threat.entrySet().removeIf(e -> e.getValue() < .1f);
        if (threat.size() > 64) threat.clear();
        String clip = boss.phase() == 2 ? SamuraiRig.COMBO_TWO : chosenClip;
        Map<LivingEntity, Assessment> decisions = new IdentityHashMap<>();
        java.util.function.Function<LivingEntity, Assessment> evaluate =
                e -> decisions.computeIfAbsent(e, t -> assess(t, clip));
        LivingEntity current = boss.getTarget(), best = null;
        double score = Double.MAX_VALUE, range = BossConfig.ACQUIRE.get();
        if (current == null
                && valid(remembered)
                && visible(boss, remembered)
                && !unavailable(remembered)) {
            best = remembered;
            score = evaluate.apply(best).score + 8;
        }
        if (remembered != null && !valid(remembered)) remembered = null;
        List<LivingEntity> candidates =
                boss.senses().nearby(range).stream()
                        .filter(e -> CombatRules.enemy(boss, e, true) && inRange(e, range))
                        .toList();
        List<LivingEntity> players = new ArrayList<>();
        for (var player : boss.senses().players()) if (playerInAcquire(player)) players.add(player);
        candidatesSeen =
                candidates.size() + (int) players.stream().filter(e -> !inRange(e, range)).count();
        candidatesEvaluated = 0;
        // Nearest 32 plus a rotating slice: O(n log 32), with no full-crowd sort or starvation.
        Comparator<LivingEntity> nearest =
                Comparator.<LivingEntity>comparingDouble(e -> TargetGeometry.distanceSqr(boss, e))
                        .thenComparingInt(LivingEntity::getId);
        PriorityQueue<LivingEntity> close = new PriorityQueue<>(nearest.reversed());
        for (LivingEntity e : candidates) {
            close.offer(e);
            if (close.size() > 32) close.poll();
        }
        List<LivingEntity> near = new ArrayList<>(close);
        near.sort(nearest);
        // Reserve player slots before the mob crowd slice, keeping the same 64-candidate bound.
        players.sort(nearest);
        Set<LivingEntity> bounded = new LinkedHashSet<>();
        for (int i = 0; i < Math.min(4, players.size()); i++) bounded.add(players.get(i));
        for (int i = 0; i < Math.min(4, players.size()); i++)
            bounded.add(players.get(Math.floorMod(playerCursor + i, players.size())));
        playerCursor = players.isEmpty() ? 0 : (playerCursor + 4) % players.size();
        bounded.addAll(near);
        int count = candidates.size(), slice = Math.min(64 - bounded.size(), count);
        for (int i = 0; i < slice; i++)
            bounded.add(candidates.get(Math.floorMod(cursor + i, count)));
        cursor = count == 0 ? 0 : (cursor + slice) % count;
        for (LivingEntity e : bounded) {
            if (!unavailable(e) && visible(boss, e)) {
                candidatesEvaluated++;
                double value = evaluate.apply(e).score;
                if (value < score) {
                    score = value;
                    best = e;
                }
            }
        }
        reason = best == null ? "没有可见且可重试的候选" : best == current ? "保持现有目标" : "评估更合适的目标";
        if (best == null || best == current) return current;
        if (!valid(current) || unavailable(current)) return best;
        Assessment existing = evaluate.apply(current), replacement = evaluate.apply(best);
        long held = now() - chosenAt;
        if (held >= 6 && replacement.immediate && !existing.immediate
                || held >= 12 && score + 12 < existing.score) return best;
        return current;
    }
}
