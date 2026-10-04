package io.github.glocation87.nature7.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TeamBalancerTest {

    private static List<UUID> players(int count) {
        List<UUID> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            players.add(UUID.randomUUID());
        }
        return players;
    }

    @ParameterizedTest
    @CsvSource({"8, 2", "7, 2", "10, 4", "3, 4", "0, 2"})
    void teamSizesDifferByAtMostOne(int playerCount, int teamCount) {
        List<List<UUID>> teams = TeamBalancer.split(players(playerCount), teamCount, RandomGenerator.getDefault());

        int smallest = teams.stream().mapToInt(List::size).min().orElseThrow();
        int largest = teams.stream().mapToInt(List::size).max().orElseThrow();
        assertEquals(teamCount, teams.size());
        assertTrue(largest - smallest <= 1, "sizes " + smallest + " and " + largest);
    }

    @Test
    void everyPlayerIsOnExactlyOneTeam() {
        List<UUID> players = players(9);
        List<List<UUID>> teams = TeamBalancer.split(players, 3, RandomGenerator.getDefault());

        Set<UUID> assigned = new HashSet<>();
        int total = 0;
        for (List<UUID> team : teams) {
            assigned.addAll(team);
            total += team.size();
        }
        assertEquals(new HashSet<>(players), assigned);
        assertEquals(players.size(), total);
    }

    @Test
    void doesNotModifyTheInput() {
        List<UUID> players = players(6);
        List<UUID> copy = List.copyOf(players);

        TeamBalancer.split(players, 2, RandomGenerator.getDefault());

        assertEquals(copy, players);
    }

    @Test
    void needsAtLeastOneTeam() {
        assertThrows(IllegalArgumentException.class, () -> TeamBalancer.split(players(2), 0, RandomGenerator.getDefault()));
    }
}
