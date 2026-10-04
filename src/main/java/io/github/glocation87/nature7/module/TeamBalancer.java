package io.github.glocation87.nature7.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;

public final class TeamBalancer {

    private TeamBalancer() {
    }

    public static List<List<UUID>> split(List<UUID> players, int teamCount, RandomGenerator random) {
        if (teamCount < 1) {
            throw new IllegalArgumentException("Need at least one team, got " + teamCount);
        }
        List<UUID> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled, random);
        List<List<UUID>> teams = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            teams.add(new ArrayList<>());
        }
        // dealt out like cards, so sizes never differ by more than one
        for (int i = 0; i < shuffled.size(); i++) {
            teams.get(i % teamCount).add(shuffled.get(i));
        }
        return teams.stream().map(List::copyOf).toList();
    }
}
