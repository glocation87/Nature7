package io.github.glocation87.nature7.engine;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

// sidebar + boss bar for one session
final class SessionHud {
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final BossBar bossBar;
    private int lineCount;

    SessionHud(Component title) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("nature7", Criteria.DUMMY, title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());
        bossBar = BossBar.bossBar(title, 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
    }

    void show(Player player) {
        player.setScoreboard(scoreboard);
        player.showBossBar(bossBar);
    }

    void hide(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.hideBossBar(bossBar);
    }

    // entries never change, only their display text does, that's what stops the sidebar flickering
    void lines(List<Component> lines) {
        for (int i = 0; i < lines.size(); i++) {
            Score score = objective.getScore(entry(i));
            score.setScore(lines.size() - i);
            score.customName(lines.get(i));
        }
        for (int i = lines.size(); i < lineCount; i++) {
            scoreboard.resetScores(entry(i));
        }
        lineCount = lines.size();
    }

    void bossBar(Component name, float progress) {
        bossBar.name(name);
        // adventure throws outside 0..1
        bossBar.progress(Math.clamp(progress, 0.0f, 1.0f));
    }

    private static String entry(int index) {
        return "line" + index;
    }
}
