package io.github.glocation87.nature7.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.glocation87.nature7.module.Timeline.Milestone;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimelineTest {

    private final List<String> fired = new ArrayList<>();

    private Milestone milestone(String label, int atSecond) {
        return new Milestone(label, atSecond, () -> fired.add(label));
    }

    @Test
    void firesMilestonesWhenTheirTimeComes() {
        Timeline timeline = new Timeline(List.of(milestone("refill", 180), milestone("deathmatch", 300)));

        timeline.advanceTo(179);
        assertEquals(List.of(), fired);
        timeline.advanceTo(180);
        assertEquals(List.of("refill"), fired);
    }

    @Test
    void firesEachMilestoneOnce() {
        Timeline timeline = new Timeline(List.of(milestone("refill", 10)));

        timeline.advanceTo(10);
        timeline.advanceTo(11);
        timeline.advanceTo(12);

        assertEquals(List.of("refill"), fired);
    }

    @Test
    void catchesUpOnEveryMissedMilestoneInOrder() {
        Timeline timeline = new Timeline(List.of(milestone("c", 30), milestone("a", 10), milestone("b", 20)));

        timeline.advanceTo(100);

        assertEquals(List.of("a", "b", "c"), fired);
    }

    @Test
    void sameSecondKeepsTheGivenOrder() {
        Timeline timeline = new Timeline(List.of(milestone("first", 5), milestone("second", 5)));

        timeline.advanceTo(5);

        assertEquals(List.of("first", "second"), fired);
    }

    @Test
    void reportsTheUpcomingMilestone() {
        Timeline timeline = new Timeline(List.of(milestone("refill", 10), milestone("end", 20)));

        assertEquals("refill", timeline.upcoming().label());
        timeline.advanceTo(10);
        assertEquals("end", timeline.upcoming().label());
        timeline.advanceTo(20);
        assertNull(timeline.upcoming());
    }

    @Test
    void aFailingActionIsNotRetried() {
        List<Milestone> milestones = List.of(new Milestone("boom", 5, () -> {
            fired.add("boom");
            throw new IllegalStateException("broken");
        }));
        Timeline timeline = new Timeline(milestones);

        assertThrows(IllegalStateException.class, () -> timeline.advanceTo(5));
        timeline.advanceTo(6);

        assertEquals(List.of("boom"), fired);
    }

    @Test
    void formatsAClock() {
        assertEquals("0:05", Timeline.clock(5));
        assertEquals("2:00", Timeline.clock(120));
        assertEquals("10:09", Timeline.clock(609));
    }
}
