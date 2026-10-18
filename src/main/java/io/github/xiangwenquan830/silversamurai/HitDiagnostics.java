package io.github.xiangwenquan830.silversamurai;

import java.util.Arrays;
import net.minecraft.world.entity.LivingEntity;

/** Per-combo diagnostics, separate from the collision and damage decisions they describe. */
final class HitDiagnostics {
    private final int[] contacts = new int[3],
            acceptedHits = new int[3],
            invulnerabilityHits = new int[3];
    private String lastContact = "尚无刀路接触";

    int[] acceptedHits() {
        return acceptedHits.clone();
    }

    String lastContact() {
        return lastContact;
    }

    void resetCounts() {
        Arrays.fill(contacts, 0);
        Arrays.fill(acceptedHits, 0);
        Arrays.fill(invulnerabilityHits, 0);
    }

    void record(
            SamuraiEntity boss,
            int strike,
            LivingEntity target,
            float before,
            float after,
            boolean accepted,
            boolean invulnerability,
            String reason) {
        contacts[strike]++;
        if (accepted && after < before) acceptedHits[strike]++;
        else if (invulnerability) invulnerabilityHits[strike]++;
        lastContact =
                "第"
                        + (strike + 1)
                        + "刀："
                        + reason
                        + "，接受="
                        + accepted
                        + "，扣血="
                        + (before - after)
                        + "，距离="
                        + String.format(java.util.Locale.ROOT, "%.2f", boss.distanceTo(target));
        if (boss.debug())
            GeneratedMod.LOG.info(
                    "[银色武士刀路] Boss={} 目标={} {} 朝向={} 俯仰={} 动画时间={}",
                    boss.getId(),
                    target.getId(),
                    lastContact,
                    boss.getYRot(),
                    boss.aimPitch(0),
                    boss.age(0));
    }

    boolean miss(int strike) {
        if (strike < 0 || contacts[strike] != 0) return false;
        lastContact = "第" + (strike + 1) + "刀：实际刀路未接触目标";
        return true;
    }

    String report() {
        return "，刀路接触="
                + Arrays.toString(contacts)
                + "，有效="
                + Arrays.toString(acceptedHits)
                + "，受伤保护="
                + Arrays.toString(invulnerabilityHits)
                + "，"
                + lastContact;
    }
}
