package io.github.glocation87.nature7.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

// marks our UI items with a hidden id in the PDC, survives renaming unlike checking display names
public final class ItemTags {
    private final NamespacedKey key;

    public ItemTags(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "item");
    }

    public ItemStack tag(ItemStack item, String id) {
        item.editPersistentDataContainer(data -> data.set(key, PersistentDataType.STRING, id));
        return item;
    }

    public @Nullable String idOf(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        return item.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public boolean isTagged(@Nullable ItemStack item) {
        return idOf(item) != null;
    }
}
