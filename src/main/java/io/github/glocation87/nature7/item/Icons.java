package io.github.glocation87.nature7.item;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class Icons {

    private Icons() {
    }

    public static ItemStack of(Material material, Component name, List<Component> lore) {
        ItemStack item = ItemStack.of(material);
        item.editMeta(meta -> {
            // itemName instead of displayName, it isn't italic and can't be changed in an anvil
            meta.itemName(name);
            meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        });
        return item;
    }

    public static Component line(String text) {
        return Component.text(text, NamedTextColor.GRAY);
    }
}
