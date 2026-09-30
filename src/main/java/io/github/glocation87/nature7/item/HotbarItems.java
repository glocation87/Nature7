package io.github.glocation87.nature7.item;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class HotbarItems {
    // compile time constants so HotbarListener can switch on them
    public static final String GAME_SELECTOR = "game_selector";
    public static final String MAP_VOTE = "map_vote";
    public static final String LEAVE_GAME = "leave_game";

    private final ItemTags tags;

    public HotbarItems(ItemTags tags) {
        this.tags = tags;
    }

    public void giveLobbyItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setItem(4, item(Material.COMPASS, GAME_SELECTOR, "Play a game", "Right click to pick a minigame"));
    }

    public void giveWaitingItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setItem(0, item(Material.PAPER, MAP_VOTE, "Vote for a map", "Right click to vote"));
        inventory.setItem(8, item(Material.RED_BED, LEAVE_GAME, "Leave game", "Right click to go back to the lobby"));
    }

    private ItemStack item(Material material, String id, String name, String... lore) {
        ItemStack item = Icons.of(
            material,
            Component.text(name, NamedTextColor.GOLD),
            Arrays.stream(lore).map(Icons::line).toList());
        return tags.tag(item, id);
    }
}
