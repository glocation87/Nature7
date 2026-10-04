package io.github.glocation87.nature7.stats;

import java.util.UUID;

// what one player added in one match
public record StatsDelta(UUID player, int games, int wins, int kills, int deaths) {
}
