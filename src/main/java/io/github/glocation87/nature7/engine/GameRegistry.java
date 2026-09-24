package io.github.glocation87.nature7.engine;

import java.util.Collection;
import java.util.Optional;

import io.github.glocation87.nature7.types.GameType;

//public game registry class
//exposes game types to public interface
//non-static for dependency injection and testing
public final class GameRegistry {
    private Collection<GameType> registry;

    public Optional<GameType> getRecord(String name) {
        return registry.stream().filter(gameTypeRecord -> gameTypeRecord.id().equals(name)).findFirst();
    }

    public void register(GameType game) {
        // only checks if that unique game object already exists in the registry
        //TODO if a new object of same type <game> gets inserted do a duplication check
        if (registry.contains(game)) {
            throw new IllegalArgumentException("Game type already registered: " + game.id());
        }
        registry.add(game);
    }

    public Collection<GameType> all() {
        return registry;
    }
}
