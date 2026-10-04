package io.github.glocation87.nature7.stats;

import io.github.glocation87.nature7.item.Icons;
import io.github.glocation87.nature7.types.GameType;
import io.github.glocation87.nature7.ui.Button;
import io.github.glocation87.nature7.ui.Menu;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

final class StatsMenu {

    private StatsMenu() {
    }

    static void open(Player viewer, String name, Map<String, PlayerStats> stats, Collection<GameType> games) {
        Menu menu = new Menu(Component.text(name + "'s stats"), 3);
        int slot = 10;
        for (GameType type : games) {
            PlayerStats entry = stats.getOrDefault(type.id(), PlayerStats.NONE);
            List<Component> lore = List.of(
                Icons.line("Games: " + entry.games()),
                Icons.line("Wins: " + entry.wins()),
                Icons.line("Kills: " + entry.kills()),
                Icons.line("Deaths: " + entry.deaths()),
                // Locale.ROOT or a german server shows 1,50
                Icons.line("K/D: " + String.format(Locale.ROOT, "%.2f", entry.killDeathRatio())));
            menu.set(slot++, new Button(Icons.of(type.icon(), type.displayName(), lore), player -> {
            }));
        }
        menu.open(viewer);
    }
}
