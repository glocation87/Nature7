package io.github.glocation87.nature7.ui;

import java.util.Objects;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public record Button(ItemStack icon, Consumer<Player> onClick) {

    public Button {
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(onClick, "onClick");
    }
}
