package io.github.glocation87.nature7.ui;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

// the menu is its own inventory holder, that's how MenuListener recognises it (never by title)
public final class Menu implements InventoryHolder {
    private final Inventory inventory;
    private final Map<Integer, Button> buttons = new HashMap<>();

    public Menu(Component title, int rows) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("A menu has 1 to 6 rows, got " + rows);
        }
        // passing this early is fine, the inventory only stores it and Menu is final
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    // whole rows that fit this many buttons, between 1 and 6
    public static int rowsFor(int buttons) {
        return Math.clamp((buttons + 8) / 9, 1, 6);
    }

    public void set(int slot, Button button) {
        buttons.put(slot, button);
        inventory.setItem(slot, button.icon());
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    void click(Player player, int slot) {
        Button button = buttons.get(slot);
        if (button != null) {
            button.onClick().accept(player);
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
