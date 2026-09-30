package io.github.glocation87.nature7.types;

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.bukkit.Material;
import net.kyori.adventure.text.Component;
import io.github.glocation87.nature7.engine.MinigameProcess;
import io.github.glocation87.nature7.engine.SessionProcess;

public record GameType(
    String id,
    Component displayName,
    Material icon,
    int minPlayers,
    int maxPlayers,
    Class<?> mapSchema,
    Function<SessionProcess, ? extends MinigameProcess> factory
) {
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9_]+$");
    public GameType {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        Objects.requireNonNull(icon, "icon cannot be null");
        Objects.requireNonNull(mapSchema, "mapSchema cannot be null");
        Objects.requireNonNull(factory, "factory cannot be null");

        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("id must match pattern: " + ID_PATTERN.pattern());
        }
        if (minPlayers < 1) {
            throw new IllegalArgumentException("minPlayers must be at least 1");
        }
        if (maxPlayers < minPlayers) {
            throw new IllegalArgumentException("maxPlayers must be greater than or equal to minPlayers");
        }
    }
}
