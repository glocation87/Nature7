package io.github.glocation87.nature7.games.laststanding;

import io.github.glocation87.nature7.engine.MinigameProcess;
import io.github.glocation87.nature7.engine.SessionProcess;
import io.github.glocation87.nature7.engine.States;
import io.github.glocation87.nature7.types.GameType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class LastStanding extends MinigameProcess {
    public static final GameType TYPE = new GameType(
        "last_standing",
        Component.text("Last Standing", NamedTextColor.GREEN),
        Material.IRON_SWORD,
        1,
        1,
        LastStandingMap.class,
        LastStanding::new
    );

    private static final double ARENA_X = -63.23;
    private static final double ARENA_Z = -86.377;
    private static final int ZOMBIE_COUNT = 10;
    private static final double ZOMBIE_RADIUS = 10.0;
    private static final double ZOMBIE_HEALTH = 30.0;
    private static final double ZOMBIE_DAMAGE = 5.0;
    private static final double ZOMBIE_SPEED = 0.28;
    private static final int SWORD_USES_LEFT = 15;
    private static final long NIGHT_TIME = 18000L;

    private final List<Zombie> zombies = new ArrayList<>();
    private Location arenaCenter;

    private LastStanding(SessionProcess session) {
        super(session);
    }

    @Override
    protected void onSetup() {
        arenaCenter = surfaceAt(Bukkit.getWorlds().getFirst(), ARENA_X, ARENA_Z);
        listen(EntityDamageEvent.class, this::onDamage);
        listen(PlayerDropItemEvent.class, event -> event.setCancelled(true));
    }

    @Override
    protected void onPlayerJoin(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.teleport(arenaCenter);
        }
    }

    @Override
    protected void onPlayerLeave(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.resetPlayerTime();
        }
    }

    @Override
    protected void onStart() {
        List<Player> players = session.players();
        if (players.isEmpty()) {
            return;
        }
        Player player = players.getFirst();
        player.teleport(arenaCenter);
        player.setPlayerTime(NIGHT_TIME, false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, PotionEffect.INFINITE_DURATION, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 0));
        player.setHealth(attributeValue(player, Attribute.MAX_HEALTH));

        ItemStack sword = ItemStack.of(Material.IRON_SWORD);
        sword.addEnchantment(Enchantment.SMITE, 1);
        sword.editMeta(Damageable.class, meta -> meta.setDamage(Material.IRON_SWORD.getMaxDurability() - SWORD_USES_LEFT));
        player.getInventory().addItem(sword, ItemStack.of(Material.IRON_AXE));

        World world = arenaCenter.getWorld();
        for (int i = 0; i < ZOMBIE_COUNT; i++) {
            double angle = 2 * Math.PI * i / ZOMBIE_COUNT;
            Location spawn = surfaceAt(world,
                arenaCenter.getX() + Math.cos(angle) * ZOMBIE_RADIUS,
                arenaCenter.getZ() + Math.sin(angle) * ZOMBIE_RADIUS);
            Zombie zombie = world.spawn(spawn, Zombie.class, LastStanding::powerUp);
            zombie.setTarget(player);
            zombies.add(zombie);
        }
        session.broadcastMessage(Component.text("Survive " + ZOMBIE_COUNT + " zombies!", NamedTextColor.YELLOW));
    }

    @Override
    protected void onTick(long tick) {
        int before = zombies.size();
        // Polled because the router only forwards events about players, a zombie's death event never reaches us
        zombies.removeIf(Zombie::isDead);
        if (zombies.isEmpty()) {
            session.end(session.players().stream().findFirst().orElse(null));
        } else if (zombies.size() < before) {
            session.broadcastMessage(Component.text(zombies.size() + " zombies left", NamedTextColor.YELLOW));
        }
    }

    @Override
    protected void onEnd() {
        removeZombies();
    }

    @Override
    protected void onDispose() {
        removeZombies();
        for (Player player : session.players()) {
            player.resetPlayerTime();
        }
    }

    private void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (session.state() != States.ACTIVE) {
            event.setCancelled(true);
            return;
        }
        // Cancel the killing blow so there's no death screen, no dropped items and no respawn
        if (event.getFinalDamage() >= player.getHealth()) {
            event.setCancelled(true);
            player.setGameMode(GameMode.SPECTATOR);
            session.broadcastMessage(Component.text(player.getName() + " was overwhelmed", NamedTextColor.RED));
            session.end(null);
        }
    }

    private void removeZombies() {
        for (Zombie zombie : zombies) {
            zombie.remove();
        }
        zombies.clear();
    }

    private static void powerUp(Zombie zombie) {
        zombie.setAdult();
        zombie.setShouldBurnInDay(false);
        zombie.setRemoveWhenFarAway(false);
        zombie.setLootTable(null);
        zombie.getEquipment().clear();
        setBase(zombie, Attribute.MAX_HEALTH, ZOMBIE_HEALTH);
        setBase(zombie, Attribute.ATTACK_DAMAGE, ZOMBIE_DAMAGE);
        setBase(zombie, Attribute.MOVEMENT_SPEED, ZOMBIE_SPEED);
        zombie.setHealth(ZOMBIE_HEALTH);
    }

    private static void setBase(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static double attributeValue(LivingEntity entity, Attribute attribute) {
        AttributeInstance instance = entity.getAttribute(attribute);
        return instance == null ? 20.0 : instance.getValue();
    }

    private static Location surfaceAt(World world, double x, double z) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        return new Location(world, blockX + 0.5, world.getHighestBlockYAt(blockX, blockZ) + 1, blockZ + 0.5);
    }
}
