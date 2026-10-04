package io.github.glocation87.nature7.kit;

import java.util.List;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public record Kit(String name, Material icon, List<KitItem> items, @Nullable KitArmor armor) {

    public Kit {
        Objects.requireNonNull(name, "name is missing");
        Objects.requireNonNull(icon, "icon is missing");
        items = items == null ? List.of() : List.copyOf(items);
    }

    public void apply(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (KitItem item : items) {
            inventory.addItem(item.toItemStack());
        }
        if (armor != null) {
            inventory.setHelmet(stack(armor.helmet()));
            inventory.setChestplate(stack(armor.chestplate()));
            inventory.setLeggings(stack(armor.leggings()));
            inventory.setBoots(stack(armor.boots()));
        }
    }

    // building each item once means a bad enchantment shows up when the server starts, not mid match
    void validate() {
        for (KitItem item : items) {
            item.toItemStack();
        }
    }

    private static @Nullable ItemStack stack(@Nullable Material material) {
        return material == null ? null : ItemStack.of(material);
    }
}
