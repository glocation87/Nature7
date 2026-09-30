package io.github.glocation87.nature7.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.Nullable;

// plain java, no bukkit, randomness comes in as a parameter so tests can control it
public final class MapVote {
    private final List<GameMap> candidates;
    private final Map<UUID, String> votes = new HashMap<>();
    // null while voting is open, doubles as the closed flag
    private @Nullable GameMap winner;

    public MapVote(List<GameMap> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("A vote needs at least one map");
        }
        this.candidates = List.copyOf(candidates);
    }

    public static List<GameMap> pickCandidates(List<GameMap> maps, int count, RandomGenerator random) {
        List<GameMap> shuffled = new ArrayList<>(maps);
        Collections.shuffle(shuffled, random);
        return List.copyOf(shuffled.subList(0, Math.min(count, shuffled.size())));
    }

    public List<GameMap> candidates() {
        return candidates;
    }

    public boolean isClosed() {
        return winner != null;
    }

    public GameMap winner() {
        if (winner == null) {
            throw new IllegalStateException("The vote is still open");
        }
        return winner;
    }

    public boolean cast(UUID voter, String mapId) {
        if (isClosed() || candidates.stream().noneMatch(map -> map.id().equals(mapId))) {
            return false;
        }
        votes.put(voter, mapId);
        return true;
    }

    public void retract(UUID voter) {
        if (!isClosed()) {
            votes.remove(voter);
        }
    }

    public int votesFor(String mapId) {
        int count = 0;
        for (String vote : votes.values()) {
            if (vote.equals(mapId)) {
                count++;
            }
        }
        return count;
    }

    public @Nullable String voteOf(UUID voter) {
        return votes.get(voter);
    }

    public GameMap close(RandomGenerator random) {
        if (winner != null) {
            return winner;
        }
        int best = -1;
        List<GameMap> leaders = new ArrayList<>();
        for (GameMap map : candidates) {
            int count = votesFor(map.id());
            if (count > best) {
                best = count;
                leaders.clear();
            }
            if (count == best) {
                leaders.add(map);
            }
        }
        // random among the tied leaders, always taking the first one is a bug players notice
        winner = leaders.get(random.nextInt(leaders.size()));
        return winner;
    }
}
