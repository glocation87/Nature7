package io.github.glocation87.nature7.loot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.glocation87.nature7.loot.LootTable.Drop;
import io.github.glocation87.nature7.loot.LootTable.Entry;
import java.util.List;
import java.util.random.RandomGenerator;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class LootTableTest {

    // always rolls the same number, so a test can aim at the exact weight boundaries
    private static RandomGenerator fixed(int roll) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return roll;
            }

            @Override
            public int nextInt(int bound) {
                return roll;
            }

            @Override
            public int nextInt(int origin, int bound) {
                return origin;
            }
        };
    }

    private final LootTable table = new LootTable(List.of(
        new Entry(Material.STONE, 1, 1, 1),
        new Entry(Material.DIRT, 1, 1, 3)));

    @Test
    void rollsMapOntoWeightRanges() {
        assertEquals(Material.STONE, table.pick(fixed(0)).type());
        assertEquals(Material.DIRT, table.pick(fixed(1)).type());
        assertEquals(Material.DIRT, table.pick(fixed(3)).type());
    }

    @Test
    void heavierEntriesComeUpMoreOften() {
        RandomGenerator random = RandomGenerator.of("L64X128MixRandom");
        int dirt = 0;
        int rolls = 10_000;
        for (int i = 0; i < rolls; i++) {
            if (table.pick(random).type() == Material.DIRT) {
                dirt++;
            }
        }
        double share = dirt / (double) rolls;
        assertTrue(share > 0.72 && share < 0.78, "dirt share was " + share);
    }

    @Test
    void amountsStayWithinTheEntryRange() {
        LootTable arrows = new LootTable(List.of(new Entry(Material.ARROW, 4, 12, 1)));
        for (Drop drop : arrows.roll(RandomGenerator.getDefault(), 200)) {
            assertTrue(drop.amount() >= 4 && drop.amount() <= 12, "amount " + drop.amount());
        }
    }

    @Test
    void rejectsInvalidEntries() {
        assertThrows(IllegalArgumentException.class, () -> new Entry(Material.STONE, 1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Entry(Material.STONE, 5, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> new LootTable(List.of()));
    }
}
