package io.github.glocation87.nature7.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class LootTable {

    public record Entry(Material type, int min, int max, int weight) {

        public Entry {
            Objects.requireNonNull(type, "type");
            if (weight < 1) {
                throw new IllegalArgumentException("Weight must be positive for " + type);
            }
            if (min < 1 || max < min) {
                throw new IllegalArgumentException("Invalid amount range " + min + "-" + max + " for " + type);
            }
        }
    }

    public record Drop(Material type, int amount) {
    }

    private final List<Entry> entries;
    private final int totalWeight;

    public LootTable(List<Entry> entries) {
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("A loot table needs at least one entry");
        }
        this.entries = List.copyOf(entries);
        totalWeight = entries.stream().mapToInt(Entry::weight).sum();
    }

    // roll under the total, walk down the weights, whichever entry takes it below zero wins
    public Entry pick(RandomGenerator random) {
        int roll = random.nextInt(totalWeight);
        for (Entry entry : entries) {
            roll -= entry.weight();
            if (roll < 0) {
                return entry;
            }
        }
        throw new IllegalStateException("Unreachable, the roll is always below the total weight");
    }

    public List<Drop> roll(RandomGenerator random, int rolls) {
        List<Drop> drops = new ArrayList<>(rolls);
        for (int i = 0; i < rolls; i++) {
            Entry entry = pick(random);
            drops.add(new Drop(entry.type(), random.nextInt(entry.min(), entry.max() + 1)));
        }
        return drops;
    }

    public List<ItemStack> items(RandomGenerator random, int rolls) {
        return roll(random, rolls).stream().map(drop -> ItemStack.of(drop.type(), drop.amount())).toList();
    }
}
