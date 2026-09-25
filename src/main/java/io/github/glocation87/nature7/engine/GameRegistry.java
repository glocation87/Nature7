package io.github.glocation87.nature7.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.github.glocation87.nature7.types.GameType;

//public game registry class
//exposes game types to public interface
//non-static for dependency injection and testing
public final class GameRegistry {
    private final Map<String, GameType> registry = new LinkedHashMap<>();

    public Optional<GameType> find(String id) {
        return Optional.ofNullable(registry.get(id));
    }

    public void register(GameType game) {
        // duplicates are detected by id, two different GameType objects can describe the same game
        if (registry.putIfAbsent(game.id(), game) != null) {
            throw new IllegalArgumentException("Game type already registered: " + game.id());
        }
    }

    public Collection<GameType> all() {
        return Collections.unmodifiableCollection(registry.values());
    }
}
