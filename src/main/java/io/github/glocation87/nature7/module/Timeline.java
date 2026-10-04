package io.github.glocation87.nature7.module;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

// "at second T do X", simpler than a state machine when the phases don't change the rules
public final class Timeline {

    public record Milestone(String label, int atSecond, Runnable action) {

        public Milestone {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(action, "action");
            if (atSecond < 0) {
                throw new IllegalArgumentException("A milestone cannot be in the past: " + atSecond);
            }
        }
    }

    private final List<Milestone> milestones;
    private int next;

    public Timeline(List<Milestone> milestones) {
        // stable sort, same second keeps the given order
        this.milestones = milestones.stream().sorted(Comparator.comparingInt(Milestone::atSecond)).toList();
    }

    // fires everything that's due, so a lag spike doesn't skip anything
    public void advanceTo(int second) {
        while (next < milestones.size() && milestones.get(next).atSecond() <= second) {
            // step past it first, a throwing action then fails once instead of every tick
            milestones.get(next++).action().run();
        }
    }

    public @Nullable Milestone upcoming() {
        return next < milestones.size() ? milestones.get(next) : null;
    }

    public static String clock(int seconds) {
        return "%d:%02d".formatted(seconds / 60, seconds % 60);
    }
}
