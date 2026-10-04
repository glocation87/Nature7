package io.github.glocation87.nature7.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.glocation87.nature7.map.MapLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurateException;

class KitRegistryTest {

    @TempDir
    Path temp;

    private final List<String> errors = new ArrayList<>();

    private List<Kit> parse(String yaml) throws IOException, ConfigurateException {
        Path file = temp.resolve("kits.yml");
        Files.writeString(file, yaml);
        return KitRegistry.parse(MapLoader.yaml(file).load(), errors::add);
    }

    @Test
    void parsesKitsWithItemsArmorAndEnchantments() throws Exception {
        List<Kit> kits = parse("""
            kits:
              archer:
                name: Archer
                icon: BOW
                items:
                  - {type: BOW, enchantments: {power: 2}}
                  - {type: ARROW, amount: 16}
                armor:
                  chestplate: LEATHER_CHESTPLATE
            """);

        Kit archer = kits.getFirst();
        assertEquals("Archer", archer.name());
        assertEquals(Material.BOW, archer.icon());
        assertEquals(Map.of("power", 2), archer.items().getFirst().enchantments());
        assertEquals(16, archer.items().get(1).amount());
        assertEquals(Material.LEATHER_CHESTPLATE, archer.armor().chestplate());
        assertNull(archer.armor().helmet());
    }

    @Test
    void amountDefaultsToOne() throws Exception {
        List<Kit> kits = parse("""
            kits:
              basic:
                name: Basic
                icon: STONE_SWORD
                items:
                  - {type: STONE_SWORD}
            """);

        assertEquals(1, kits.getFirst().items().getFirst().amount());
    }

    @Test
    void skipsABrokenKitAndKeepsTheRest() throws Exception {
        List<Kit> kits = parse("""
            kits:
              broken:
                name: Broken
                icon: NOT_A_MATERIAL
              fine:
                name: Fine
                icon: APPLE
            """);

        assertEquals(1, kits.size());
        assertEquals("Fine", kits.getFirst().name());
        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().startsWith("broken"), errors.getFirst());
    }

    @Test
    void reportsMissingRequiredFields() throws Exception {
        List<Kit> kits = parse("""
            kits:
              nameless:
                icon: APPLE
            """);

        assertTrue(kits.isEmpty());
        assertTrue(errors.getFirst().contains("name is missing"), errors.getFirst());
    }

    @Test
    void bundledKitFilesParseCleanly() throws Exception {
        for (String game : List.of("skywars", "capture_the_flag")) {
            Path file = Path.of("src/main/resources/kits/" + game + ".yml");
            List<Kit> kits = KitRegistry.parse(MapLoader.yaml(file).load(), errors::add);
            assertTrue(kits.size() >= 3, game + " has " + kits.size() + " kits");
        }
        assertTrue(errors.isEmpty(), errors.toString());
    }
}
