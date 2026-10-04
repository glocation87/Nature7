package io.github.glocation87.nature7.module;

import io.github.glocation87.nature7.engine.GameModule;
import io.github.glocation87.nature7.stats.StatsDelta;
import io.github.glocation87.nature7.stats.StatsService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class StatsModule extends GameModule {
    private static final class Tally {
        int kills;
        int deaths;
    }

    private final StatsService stats;
    private final Map<UUID, Tally> tallies = new LinkedHashMap<>();

    public StatsModule(StatsService stats) {
        this.stats = stats;
    }

    public int kills(Player player) {
        Tally tally = tallies.get(player.getUniqueId());
        return tally == null ? 0 : tally.kills;
    }

    // for kills that aren't players, like zombies in Last Standing
    public void creditKill(Player player) {
        Tally tally = tallies.get(player.getUniqueId());
        if (tally != null) {
            tally.kills++;
        }
    }

    @Override
    protected void onInstall() {
        listen(GameDeathEvent.class, this::onDeath);
    }

    @Override
    protected void onStart() {
        for (Player player : session().players()) {
            tallies.put(player.getUniqueId(), new Tally());
        }
    }

    // everyone who started gets a row, people who quit included, so quitting counts as a loss
    @Override
    protected void onEnd() {
        List<StatsDelta> deltas = new ArrayList<>();
        for (Map.Entry<UUID, Tally> entry : tallies.entrySet()) {
            Tally tally = entry.getValue();
            int wins = session().isWinner(entry.getKey()) ? 1 : 0;
            deltas.add(new StatsDelta(entry.getKey(), 1, wins, tally.kills, tally.deaths));
        }
        stats.add(session().type().id(), deltas);
    }

    private void onDeath(GameDeathEvent event) {
        Tally victim = tallies.get(event.getPlayer().getUniqueId());
        if (victim != null) {
            victim.deaths++;
        }
        Player killer = event.getKiller();
        if (killer != null) {
            creditKill(killer);
        }
    }
}
