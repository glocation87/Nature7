package io.github.glocation87.nature7.stats;

public record PlayerStats(int games, int wins, int kills, int deaths) {
    public static final PlayerStats NONE = new PlayerStats(0, 0, 0, 0);

    public double killDeathRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }
}
