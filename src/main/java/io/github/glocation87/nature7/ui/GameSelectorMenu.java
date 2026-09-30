package io.github.glocation87.nature7.ui;

import io.github.glocation87.nature7.engine.GameRegistry;
import io.github.glocation87.nature7.engine.JoinResult;
import io.github.glocation87.nature7.engine.SessionManager;
import io.github.glocation87.nature7.engine.SessionProcess;
import io.github.glocation87.nature7.engine.States;
import io.github.glocation87.nature7.item.Icons;
import io.github.glocation87.nature7.map.MapRegistry;
import io.github.glocation87.nature7.types.GameType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

// builds a fresh menu every time it opens so the player counts are always current
public final class GameSelectorMenu {
    // second row, one column in, frames the games with empty space
    private static final int FIRST_SLOT = 10;

    private final GameRegistry registry;
    private final SessionManager sessionManager;
    private final MapRegistry maps;

    public GameSelectorMenu(GameRegistry registry, SessionManager sessionManager, MapRegistry maps) {
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.maps = maps;
    }

    public void open(Player viewer) {
        Menu menu = new Menu(Component.text("Play a game"), 3);
        int slot = FIRST_SLOT;
        for (GameType type : registry.all()) {
            menu.set(slot++, button(type));
        }
        menu.open(viewer);
    }

    private Button button(GameType type) {
        int mapCount = maps.mapsFor(type.id()).size();
        int waiting = 0;
        int playing = 0;
        for (SessionProcess session : sessionManager.getActiveSessions()) {
            if (!session.type().id().equals(type.id())) {
                continue;
            }
            if (session.state() == States.WAITING || session.state() == States.STARTING) {
                waiting += session.playerCount();
            } else if (session.state() == States.ACTIVE) {
                playing += session.playerCount();
            }
        }
        List<Component> lore = List.of(
            Icons.line(type.minPlayers() + " to " + type.maxPlayers() + " players"),
            Icons.line(mapCount + (mapCount == 1 ? " map" : " maps")),
            Icons.line("Waiting: " + waiting + "  Playing: " + playing),
            Component.empty(),
            mapCount == 0
                ? Component.text("No maps installed", NamedTextColor.RED)
                : Component.text("Click to play", NamedTextColor.GREEN));
        return new Button(Icons.of(type.icon(), type.displayName(), lore), player -> {
            player.closeInventory();
            JoinResult result = sessionManager.joinSession(player, type);
            if (result != JoinResult.JOINED) {
                player.sendMessage(result.message());
            }
        });
    }
}
