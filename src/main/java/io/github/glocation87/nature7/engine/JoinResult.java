package io.github.glocation87.nature7.engine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

// expected outcomes of joining, returned instead of thrown so the command and the menu handle them the same way
public enum JoinResult {
    JOINED,
    ALREADY_IN_GAME,
    NO_MAPS;

    public Component message() {
        return switch (this) {
            case JOINED -> Component.empty();
            case ALREADY_IN_GAME -> Component.text("You are already in a game, leave it first", NamedTextColor.RED);
            case NO_MAPS -> Component.text("That game has no maps installed yet", NamedTextColor.RED);
        };
    }
}
