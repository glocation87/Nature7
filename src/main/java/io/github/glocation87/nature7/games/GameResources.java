package io.github.glocation87.nature7.games;

import io.github.glocation87.nature7.item.ItemTags;
import io.github.glocation87.nature7.kit.KitRegistry;
import io.github.glocation87.nature7.stats.StatsService;
import org.bukkit.plugin.Plugin;

// what games need that has nothing to do with sessions, handed to each game's factory
public record GameResources(Plugin plugin, ItemTags tags, KitRegistry kits, StatsService stats) {
}
