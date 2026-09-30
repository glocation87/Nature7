package io.github.glocation87.nature7.ui;

import io.github.glocation87.nature7.item.Icons;
import io.github.glocation87.nature7.map.GameMap;
import io.github.glocation87.nature7.map.MapVote;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

// holds one menu and re-renders into it after each vote so the voter sees their pick straight away
public final class MapVoteMenu {
    private static final int[] SLOTS = {11, 13, 15};

    private final MapVote vote;
    private final Menu menu = new Menu(Component.text("Vote for a map"), 3);

    private MapVoteMenu(MapVote vote) {
        this.vote = vote;
    }

    public static void open(Player viewer, MapVote vote) {
        MapVoteMenu voteMenu = new MapVoteMenu(vote);
        voteMenu.render(viewer);
        voteMenu.menu.open(viewer);
    }

    private void render(Player viewer) {
        List<GameMap> candidates = vote.candidates();
        String choice = vote.voteOf(viewer.getUniqueId());
        for (int i = 0; i < candidates.size() && i < SLOTS.length; i++) {
            GameMap map = candidates.get(i);
            boolean chosen = map.id().equals(choice);
            List<Component> lore = new ArrayList<>();
            if (!map.info().authors().isEmpty()) {
                lore.add(Icons.line("By " + String.join(", ", map.info().authors())));
            }
            lore.add(Icons.line("Votes: " + vote.votesFor(map.id())));
            lore.add(Component.empty());
            lore.add(chosen
                ? Component.text("Your vote", NamedTextColor.GREEN)
                : Component.text("Click to vote", NamedTextColor.YELLOW));
            Material icon = chosen ? Material.FILLED_MAP : Material.MAP;
            menu.set(SLOTS[i], new Button(Icons.of(icon, Component.text(map.info().name(), NamedTextColor.AQUA), lore),
                player -> choose(player, map)));
        }
    }

    private void choose(Player player, GameMap map) {
        if (!vote.cast(player.getUniqueId(), map.id())) {
            player.sendMessage(Component.text("Voting has closed", NamedTextColor.RED));
            player.closeInventory();
            return;
        }
        player.sendMessage(Component.text("You voted for " + map.info().name(), NamedTextColor.GREEN));
        render(player);
    }
}
