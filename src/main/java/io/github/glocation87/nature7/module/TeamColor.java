package io.github.glocation87.nature7.module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public enum TeamColor {
    RED("Red", NamedTextColor.RED, Material.RED_WOOL, Material.RED_BANNER),
    BLUE("Blue", NamedTextColor.BLUE, Material.BLUE_WOOL, Material.BLUE_BANNER),
    GREEN("Green", NamedTextColor.GREEN, Material.LIME_WOOL, Material.LIME_BANNER),
    YELLOW("Yellow", NamedTextColor.YELLOW, Material.YELLOW_WOOL, Material.YELLOW_BANNER);

    private final String displayName;
    private final NamedTextColor textColor;
    private final Material wool;
    private final Material banner;

    TeamColor(String displayName, NamedTextColor textColor, Material wool, Material banner) {
        this.displayName = displayName;
        this.textColor = textColor;
        this.wool = wool;
        this.banner = banner;
    }

    public Component displayName() {
        return Component.text(displayName, textColor);
    }

    public NamedTextColor textColor() {
        return textColor;
    }

    public Material wool() {
        return wool;
    }

    public Material banner() {
        return banner;
    }
}
