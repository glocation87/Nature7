package io.github.glocation87.nature7.games.skywars;

import io.github.glocation87.nature7.loot.LootTable;
import io.github.glocation87.nature7.loot.LootTable.Entry;
import java.util.List;
import org.bukkit.Material;

// the centre is worth fighting for, islands get the basics
final class SkyWarsLoot {

    static final LootTable ISLAND = new LootTable(List.of(
        new Entry(Material.STONE_SWORD, 1, 1, 8),
        new Entry(Material.IRON_SWORD, 1, 1, 3),
        new Entry(Material.BOW, 1, 1, 3),
        new Entry(Material.ARROW, 4, 12, 6),
        new Entry(Material.LEATHER_CHESTPLATE, 1, 1, 6),
        new Entry(Material.CHAINMAIL_LEGGINGS, 1, 1, 4),
        new Entry(Material.IRON_BOOTS, 1, 1, 3),
        new Entry(Material.IRON_HELMET, 1, 1, 3),
        new Entry(Material.COBBLESTONE, 16, 32, 12),
        new Entry(Material.OAK_PLANKS, 16, 32, 10),
        new Entry(Material.COOKED_BEEF, 2, 6, 10),
        new Entry(Material.GOLDEN_APPLE, 1, 1, 2),
        new Entry(Material.SNOWBALL, 4, 16, 5),
        new Entry(Material.EGG, 4, 16, 4),
        new Entry(Material.WATER_BUCKET, 1, 1, 3),
        new Entry(Material.ENDER_PEARL, 1, 1, 1)));

    static final LootTable CENTER = new LootTable(List.of(
        new Entry(Material.DIAMOND_SWORD, 1, 1, 4),
        new Entry(Material.IRON_SWORD, 1, 1, 6),
        new Entry(Material.BOW, 1, 1, 4),
        new Entry(Material.ARROW, 8, 24, 6),
        new Entry(Material.DIAMOND_CHESTPLATE, 1, 1, 2),
        new Entry(Material.DIAMOND_HELMET, 1, 1, 2),
        new Entry(Material.IRON_CHESTPLATE, 1, 1, 4),
        new Entry(Material.IRON_LEGGINGS, 1, 1, 4),
        new Entry(Material.GOLDEN_APPLE, 1, 3, 4),
        new Entry(Material.ENDER_PEARL, 1, 2, 3),
        new Entry(Material.LAVA_BUCKET, 1, 1, 2),
        new Entry(Material.TNT, 1, 3, 2)));

    private SkyWarsLoot() {
    }
}
