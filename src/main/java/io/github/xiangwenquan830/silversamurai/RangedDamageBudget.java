package io.github.xiangwenquan830.silversamurai;

import java.util.ArrayDeque;
import net.minecraft.nbt.*;

/** Sliding window over final damage; bounded to twenty aggregated tick entries. */
final class RangedDamageBudget {
    private record Entry(long tick, float damage) {}

    private final ArrayDeque<Entry> entries = new ArrayDeque<>();

    float accept(long tick, float amount, double cap) {
        while (!entries.isEmpty()
                && (tick - entries.peekFirst().tick >= 20 || tick < entries.peekFirst().tick))
            entries.removeFirst();
        if (!Float.isFinite(amount) || amount <= 0) return 0;
        if (cap <= 0) return amount;
        double spent = entries.stream().mapToDouble(Entry::damage).sum();
        float accepted = (float) Math.min(amount, Math.max(0, cap - spent));
        if (accepted > 0) {
            if (!entries.isEmpty() && entries.peekLast().tick == tick)
                accepted += entries.removeLast().damage;
            entries.addLast(new Entry(tick, accepted));
        }
        return (float) Math.min(amount, Math.max(0, cap - spent));
    }

    void save(CompoundTag tag, long now) {
        ListTag list = new ListTag();
        for (Entry e : entries)
            if (now - e.tick >= 0 && now - e.tick < 20) {
                CompoundTag t = new CompoundTag();
                t.putInt("Age", (int) (now - e.tick));
                t.putFloat("Damage", e.damage);
                list.add(t);
            }
        tag.put("SamuraiRangedBudget", list);
    }

    void load(CompoundTag tag, long now) {
        entries.clear();
        ListTag list = tag.getList("SamuraiRangedBudget", 10);
        for (int i = 0; i < Math.min(20, list.size()); i++) {
            CompoundTag t = list.getCompound(i);
            int age = t.getInt("Age");
            float d = t.getFloat("Damage");
            if (age >= 0 && age < 20 && Float.isFinite(d) && d > 0)
                entries.add(new Entry(now - age, d));
        }
    }
}
