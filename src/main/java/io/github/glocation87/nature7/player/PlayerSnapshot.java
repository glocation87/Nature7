package io.github.glocation87.nature7.player;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public record PlayerSnapshot(
        ItemStack[] inventory,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        GameMode gameMode,
        double health,
        int foodLevel,
        float saturation,
        int level,
        float exp,
        List<PotionEffect> effects) {

    public static PlayerSnapshot capture(Player player) {
        Location location = player.getLocation();
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] inventory = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            // getContents returns live mirrors, copy them so later changes can't leak in
            inventory[i] = contents[i] == null ? ItemStack.empty() : contents[i].clone();
        }
        return new PlayerSnapshot(
                inventory,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                player.getGameMode(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getLevel(),
                player.getExp(),
                List.copyOf(player.getActivePotionEffects()));
    }

    public void apply(Player player) {
        ItemStack[] contents = new ItemStack[inventory.length];
        for (int i = 0; i < inventory.length; i++) {
            // null is the only empty slot every inventory implementation agrees on, an AIR stack can block addItem
            contents[i] = inventory[i].isEmpty() ? null : inventory[i].clone();
        }
        player.getInventory().setContents(contents);
        player.teleport(location());
        player.setGameMode(gameMode);
        player.setHealth(Math.min(health, maxHealth(player)));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setLevel(level);
        player.setExp(exp);
        player.clearActivePotionEffects();
        player.addPotionEffects(effects);
    }

    public String serialize() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("inventory", Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(inventory)));
        yaml.set("world", worldName);
        yaml.set("x", x);
        yaml.set("y", y);
        yaml.set("z", z);
        yaml.set("yaw", yaw);
        yaml.set("pitch", pitch);
        yaml.set("gamemode", gameMode.name());
        yaml.set("health", health);
        yaml.set("food", foodLevel);
        yaml.set("saturation", saturation);
        yaml.set("level", level);
        yaml.set("exp", exp);
        yaml.set("effects", effects);
        return yaml.saveToString();
    }

    public static PlayerSnapshot deserialize(String data) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(data);
        String encoded = yaml.getString("inventory");
        if (encoded == null) {
            throw new InvalidConfigurationException("Snapshot has no inventory");
        }
        List<PotionEffect> effects = new ArrayList<>();
        for (Object entry : yaml.getList("effects", List.of())) {
            if (entry instanceof PotionEffect effect) {
                effects.add(effect);
            }
        }
        return new PlayerSnapshot(
                ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(encoded)),
                yaml.getString("world", ""),
                yaml.getDouble("x"),
                yaml.getDouble("y"),
                yaml.getDouble("z"),
                (float) yaml.getDouble("yaw"),
                (float) yaml.getDouble("pitch"),
                GameMode.valueOf(yaml.getString("gamemode", GameMode.SURVIVAL.name())),
                yaml.getDouble("health", 20.0),
                yaml.getInt("food", 20),
                (float) yaml.getDouble("saturation", 5.0),
                yaml.getInt("level"),
                (float) yaml.getDouble("exp"),
                List.copyOf(effects));
    }

    private Location location() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            // The original world is gone, fall back to the main world's spawn
            return Bukkit.getWorlds().getFirst().getSpawnLocation();
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    static double maxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? 20.0 : attribute.getValue();
    }
}
