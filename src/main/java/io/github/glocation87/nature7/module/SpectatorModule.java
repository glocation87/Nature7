package io.github.glocation87.nature7.module;

import io.github.glocation87.nature7.engine.GameModule;
import io.github.glocation87.nature7.item.HotbarItems;
import io.github.glocation87.nature7.item.Icons;
import io.github.glocation87.nature7.item.ItemTags;
import io.github.glocation87.nature7.ui.Button;
import io.github.glocation87.nature7.ui.Menu;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

// flying invisible ghosts instead of vanilla spectator mode, so they keep a hotbar to teleport and leave with
public final class SpectatorModule extends GameModule {
    public static final String TELEPORTER = "spectator_teleporter";

    private final Plugin plugin;
    private final ItemTags tags;
    private final Set<UUID> spectators = new HashSet<>();

    public SpectatorModule(Plugin plugin, ItemTags tags) {
        this.plugin = plugin;
        this.tags = tags;
    }

    public boolean isSpectator(Player player) {
        return spectators.contains(player.getUniqueId());
    }

    public void spectate(Player player) {
        if (!spectators.add(player.getUniqueId())) {
            return;
        }
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setCollidable(false);
        player.setFireTicks(0);
        player.clearActivePotionEffects();
        for (Player other : session().players()) {
            if (!isSpectator(other)) {
                other.hidePlayer(plugin, player);
            }
        }
        player.getInventory().setItem(0, tags.tag(Icons.of(Material.COMPASS,
            Component.text("Teleport to a player", NamedTextColor.GOLD),
            List.of(Icons.line("Right click to pick a player"))), TELEPORTER));
        // same tag as the waiting room bed, HotbarListener already knows what to do with it
        player.getInventory().setItem(8, tags.tag(Icons.of(Material.RED_BED,
            Component.text("Leave game", NamedTextColor.GOLD),
            List.of(Icons.line("Right click to go back to the lobby"))), HotbarItems.LEAVE_GAME));
    }

    @Override
    protected void onInstall() {
        // LOWEST so a spectator is out of the picture before any game rule or the death module sees the event
        listen(EntityDamageEvent.class, EventPriority.LOWEST, event -> cancelIfSpectator(event.getEntity(), event));
        listen(EntityDamageByEntityEvent.class, EventPriority.LOWEST, event -> cancelIfSpectator(event.getDamager(), event));
        listen(EntityPickupItemEvent.class, EventPriority.LOWEST, event -> cancelIfSpectator(event.getEntity(), event));
        listen(EntityTargetLivingEntityEvent.class, EventPriority.LOWEST, event -> {
            if (event.getTarget() != null) {
                cancelIfSpectator(event.getTarget(), event);
            }
        });
        listen(BlockBreakEvent.class, EventPriority.LOWEST, event -> cancelIfSpectator(event.getPlayer(), event));
        listen(BlockPlaceEvent.class, EventPriority.LOWEST, event -> cancelIfSpectator(event.getPlayer(), event));
        listen(PlayerDropItemEvent.class, EventPriority.LOWEST, event -> cancelIfSpectator(event.getPlayer(), event));
        listen(PlayerInteractEvent.class, EventPriority.LOWEST, this::onInteract);
    }

    @Override
    protected void onPlayerLeave(Player player) {
        if (spectators.remove(player.getUniqueId())) {
            restore(player);
        }
        // or they walk back into the lobby still unable to see these people
        for (UUID uuid : spectators) {
            Player spectator = Bukkit.getPlayer(uuid);
            if (spectator != null) {
                player.showPlayer(plugin, spectator);
            }
        }
    }

    @Override
    protected void onDispose() {
        for (Player player : session().players()) {
            if (spectators.remove(player.getUniqueId())) {
                restore(player);
            }
        }
    }

    // the snapshot doesn't cover flight, so it has to be taken away here
    private void restore(Player player) {
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setCollidable(true);
        for (Player other : Bukkit.getOnlinePlayers()) {
            other.showPlayer(plugin, player);
        }
    }

    private void cancelIfSpectator(Entity entity, Cancellable event) {
        if (entity instanceof Player player && isSpectator(player)) {
            event.setCancelled(true);
        }
    }

    private void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isSpectator(player)) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND && event.getAction().isRightClick()
                && TELEPORTER.equals(tags.idOf(event.getItem()))) {
            openTeleporter(player);
        }
    }

    private void openTeleporter(Player viewer) {
        List<Player> targets = session().players().stream().filter(player -> !isSpectator(player)).toList();
        Menu menu = new Menu(Component.text("Teleport to a player"), Menu.rowsFor(targets.size()));
        for (int i = 0; i < targets.size() && i < 54; i++) {
            Player target = targets.get(i);
            ItemStack head = Icons.of(Material.PLAYER_HEAD, Component.text(target.getName(), NamedTextColor.AQUA),
                List.of(Icons.line("Click to teleport")));
            head.editMeta(SkullMeta.class, meta -> meta.setOwningPlayer(target));
            menu.set(i, new Button(head, player -> {
                // they might have died or left while the menu was open
                if (target.isOnline() && !isSpectator(target)) {
                    player.teleport(target);
                }
                player.closeInventory();
            }));
        }
        menu.open(viewer);
    }
}
