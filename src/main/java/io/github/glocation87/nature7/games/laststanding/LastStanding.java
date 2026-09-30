package io.github.glocation87.nature7.games.laststanding;

import io.github.glocation87.nature7.engine.MinigameProcess;
import io.github.glocation87.nature7.engine.SessionProcess;
import io.github.glocation87.nature7.engine.States;
import io.github.glocation87.nature7.map.Point;
import io.github.glocation87.nature7.types.GameType;
import io.github.glocation87.nature7.world.MapInstance;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

// one player against a horde of buffed zombies, kill them all to win
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

    private static final int ZOMBIE_COUNT = 10;
    private static final double ZOMBIE_HEALTH = 30.0;
    private static final double ZOMBIE_DAMAGE = 5.0;
    private static final double ZOMBIE_SPEED = 0.28;
    private static final int SWORD_USES_LEFT = 15;
    private static final long NIGHT_TIME = 18000L;

    private final List<Zombie> zombies = new ArrayList<>();

    private LastStanding(SessionProcess session) {
        super(session);
    }

    @Override
    protected void onSetup() {
        listen(EntityDamageEvent.class, this::onDamage);
        listen(EntityDeathEvent.class, this::onDeath);
        listen(PlayerMoveEvent.class, this::onMove);
        listen(PlayerDropItemEvent.class, event -> event.setCancelled(true));
        listen(BlockBreakEvent.class, event -> event.setCancelled(true));
        listen(BlockPlaceEvent.class, event -> event.setCancelled(true));
    }

    @Override
    protected void onStart() {
        List<Player> players = session.players();
        if (players.isEmpty()) {
            return;
        }
        MapInstance map = session.map();
        List<Point> spawns = map.data(LastStandingMap.class).spawns();
        // the match world is ours alone, so night can be set on the world itself
        map.world().setTime(NIGHT_TIME);

        Player player = players.getFirst();
        player.teleport(map.location(spawns.getFirst()));
        player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, PotionEffect.INFINITE_DURATION, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, 0));
        player.setHealth(attributeValue(player, Attribute.MAX_HEALTH));

        ItemStack sword = ItemStack.of(Material.IRON_SWORD);
        sword.addEnchantment(Enchantment.SMITE, 1);
        sword.editMeta(Damageable.class, meta -> meta.setDamage(Material.IRON_SWORD.getMaxDurability() - SWORD_USES_LEFT));
        player.getInventory().addItem(sword, ItemStack.of(Material.IRON_AXE));

        // spawn 0 is the player's, zombies take the rest and double up if the map has fewer than 10
        for (int i = 0; i < ZOMBIE_COUNT; i++) {
            Point spawn = spawns.get(1 + i % (spawns.size() - 1));
            Zombie zombie = map.world().spawn(map.location(spawn), Zombie.class, LastStanding::powerUp);
            zombie.setTarget(player);
            zombies.add(zombie);
        }
        session.broadcastMessage(Component.text("Survive " + ZOMBIE_COUNT + " zombies!", NamedTextColor.YELLOW));
    }

    @Override
    protected void onEnd() {
        for (Zombie zombie : zombies) {
            zombie.remove();
        }
        zombies.clear();
    }

    @Override
    protected List<Component> sidebar() {
        return List.of(Component.text("Zombies left: ", NamedTextColor.GRAY)
            .append(Component.text(zombies.size(), NamedTextColor.WHITE)));
    }

    // zombies aren't players, this only arrives because the router finds the session by the match world
    private void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie) || !zombies.remove(zombie)) {
            return;
        }
        event.setDroppedExp(0);
        if (session.state() != States.ACTIVE) {
            return;
        }
        if (zombies.isEmpty()) {
            session.end(session.players().stream().findFirst().orElse(null));
        } else {
            String left = zombies.size() == 1 ? "1 zombie left" : zombies.size() + " zombies left";
            session.broadcastMessage(Component.text(left, NamedTextColor.YELLOW));
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
            lose(player, "was overwhelmed");
        }
    }

    private void onMove(PlayerMoveEvent event) {
        // fires on every head turn, skip anything that didn't change block before doing real work
        if (!event.hasChangedBlock() || session.state() != States.ACTIVE) {
            return;
        }
        if (!session.map().info().bounds().contains(event.getTo())) {
            lose(event.getPlayer(), "fell out of the arena");
        }
    }

    private void lose(Player player, String reason) {
        if (session.state() != States.ACTIVE) {
            return;
        }
        MapInstance map = session.map();
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(map.location(map.info().spectatorSpawn()));
        session.broadcastMessage(Component.text(player.getName() + " " + reason, NamedTextColor.RED));
        session.end(null);
    }

    private static void powerUp(Zombie zombie) {
        zombie.setAdult();
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
}
