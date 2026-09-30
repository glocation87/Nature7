package io.github.glocation87.nature7.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

class MapVoteTest {

    private static final GameMap ARENA = map("arena");
    private static final GameMap CASTLE = map("castle");
    private static final GameMap DESERT = map("desert");

    private static GameMap map(String id) {
        Region bounds = new Region(new BlockPoint(0, 0, 0), new BlockPoint(1, 1, 1));
        MapInfo info = new MapInfo(id, List.of(), null, null, new Point(0, 0, 0, 0, 0), bounds);
        return new GameMap(id, "test", info, new Object(), Path.of(id));
    }

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID carol = UUID.randomUUID();

    @Test
    void mostVotesWins() {
        MapVote vote = new MapVote(List.of(ARENA, CASTLE, DESERT));
        vote.cast(alice, "castle");
        vote.cast(bob, "castle");
        vote.cast(carol, "desert");

        assertEquals(CASTLE, vote.close(RandomGenerator.getDefault()));
    }

    @Test
    void changingYourVoteMovesIt() {
        MapVote vote = new MapVote(List.of(ARENA, CASTLE));
        vote.cast(alice, "arena");
        vote.cast(alice, "castle");

        assertEquals(0, vote.votesFor("arena"));
        assertEquals(1, vote.votesFor("castle"));
    }

    @Test
    void retractingRemovesTheVote() {
        MapVote vote = new MapVote(List.of(ARENA, CASTLE));
        vote.cast(alice, "arena");
        vote.retract(alice);

        assertEquals(0, vote.votesFor("arena"));
    }

    @Test
    void rejectsMapsThatAreNotCandidates() {
        MapVote vote = new MapVote(List.of(ARENA));

        assertFalse(vote.cast(alice, "desert"));
    }

    @Test
    void closedVotesRejectNewVotes() {
        MapVote vote = new MapVote(List.of(ARENA, CASTLE));
        vote.close(RandomGenerator.getDefault());

        assertTrue(vote.isClosed());
        assertFalse(vote.cast(alice, "castle"));
    }

    @Test
    void closingTwiceKeepsTheFirstResult() {
        MapVote vote = new MapVote(List.of(ARENA, CASTLE, DESERT));
        GameMap first = vote.close(RandomGenerator.getDefault());

        assertEquals(first, vote.close(RandomGenerator.getDefault()));
    }

    @Test
    void tiesAreBrokenAmongTheLeadersOnly() {
        Set<GameMap> winners = new HashSet<>();
        RandomGenerator random = RandomGenerator.of("L64X128MixRandom");
        for (int i = 0; i < 100; i++) {
            MapVote vote = new MapVote(List.of(ARENA, CASTLE, DESERT));
            vote.cast(alice, "arena");
            vote.cast(bob, "castle");
            winners.add(vote.close(random));
        }

        assertEquals(Set.of(ARENA, CASTLE), winners);
    }

    @Test
    void withNoVotesAnyCandidateCanWin() {
        MapVote vote = new MapVote(List.of(ARENA, CASTLE));

        assertTrue(List.of(ARENA, CASTLE).contains(vote.close(RandomGenerator.getDefault())));
    }

    @Test
    void needsAtLeastOneCandidate() {
        assertThrows(IllegalArgumentException.class, () -> new MapVote(List.of()));
    }

    @Test
    void picksDistinctCandidates() {
        List<GameMap> picked = MapVote.pickCandidates(List.of(ARENA, CASTLE, DESERT), 2, RandomGenerator.getDefault());

        assertEquals(2, picked.size());
        assertEquals(2, new HashSet<>(picked).size());
    }

    @Test
    void picksEverythingWhenThereAreFewerMapsThanSlots() {
        assertEquals(1, MapVote.pickCandidates(List.of(ARENA), 3, RandomGenerator.getDefault()).size());
    }
}
