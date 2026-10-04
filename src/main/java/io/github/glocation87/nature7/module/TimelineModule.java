package io.github.glocation87.nature7.module;

import io.github.glocation87.nature7.engine.GameModule;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class TimelineModule extends GameModule {
    private final Timeline timeline;
    private int elapsed;

    public TimelineModule(List<Timeline.Milestone> milestones) {
        timeline = new Timeline(milestones);
    }

    public int elapsedSeconds() {
        return elapsed;
    }

    @Override
    protected void onTick(long tick) {
        if (tick % 20 == 0) {
            elapsed = (int) (tick / 20);
            timeline.advanceTo(elapsed);
        }
    }

    @Override
    protected List<Component> sidebar() {
        Timeline.Milestone upcoming = timeline.upcoming();
        if (upcoming == null) {
            return List.of();
        }
        return List.of(Component.text(upcoming.label() + " in ", NamedTextColor.GRAY)
            .append(Component.text(Timeline.clock(upcoming.atSecond() - elapsed), NamedTextColor.WHITE)));
    }
}
