package io.github.glocation87.nature7.kit;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

@ConfigSerializable
public record KitItem(@Required Material type, int amount, Map<String, Integer> enchantments) {

    public KitItem {
        amount = Math.max(1, amount);
        enchantments = enchantments == null ? Map.of() : Map.copyOf(enchantments);
    }

    public ItemStack toItemStack() {
        ItemStack item = ItemStack.of(type, amount);
        // unsafe so kits can go past the vanilla max level
        for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
            item.addUnsafeEnchantment(enchantment(entry.getKey()), entry.getValue());
        }
        return item;
    }

    private static Enchantment enchantment(String name) {
        Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(Key.key(name));
        if (enchantment == null) {
            throw new IllegalArgumentException("Unknown enchantment " + name);
        }
        return enchantment;
    }
}
