package io.github.glocation87.nature7.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.glocation87.nature7.types.GameType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class GameRegistryTest {

    private static GameType type(String id) {
        return new GameType(id, Component.text(id), Material.STONE, 1, 4, Object.class, s -> null);
    }

    @Test
    void findsRegisteredTypes() {
        GameRegistry registry = new GameRegistry();
        GameType lastStanding = type("last_standing");
        registry.register(lastStanding);

        assertEquals(lastStanding, registry.find("last_standing").orElseThrow());
        assertTrue(registry.find("spleef").isEmpty());
    }

    @Test
    void rejectsDuplicateIds() {
        GameRegistry registry = new GameRegistry();
        registry.register(type("last_standing"));

        assertThrows(IllegalArgumentException.class, () -> registry.register(type("last_standing")));
    }

    @Test
    void keepsRegistrationOrder() {
        GameRegistry registry = new GameRegistry();
        registry.register(type("b"));
        registry.register(type("a"));
        registry.register(type("c"));

        var all = registry.all();
        var iterator = all.iterator();
        assertEquals("b", iterator.next().id());
        assertEquals("a", iterator.next().id());
        assertEquals("c", iterator.next().id());
    }

    @Test
    void returnsUnmodifiableCollection() {
        GameRegistry registry = new GameRegistry();
        registry.register(type("last_standing"));

        var collection = registry.all();
        assertThrows(UnsupportedOperationException.class, () -> collection.add(type("spleef")));
    }
}
