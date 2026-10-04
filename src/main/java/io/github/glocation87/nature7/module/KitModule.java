package io.github.glocation87.nature7.module;

import io.github.glocation87.nature7.engine.GameModule;
import io.github.glocation87.nature7.item.Icons;
import io.github.glocation87.nature7.item.ItemTags;
import io.github.glocation87.nature7.kit.Kit;
import io.github.glocation87.nature7.kit.KitItem;
import io.github.glocation87.nature7.ui.Button;
import io.github.glocation87.nature7.ui.Menu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

public final class KitModule extends GameModule {
    public static final String KIT_SELECTOR = "kit_selector";

    private final List<Kit> kits;
    private final ItemTags tags;
    private final Map<UUID, Kit> chosen = new HashMap<>();

    public KitModule(List<Kit> kits, ItemTags tags) {
        this.kits = List.copyOf(kits);
        this.tags = tags;
    }

    // players who never open the menu get the first kit
    public @Nullable Kit kitOf(Player player) {
        Kit kit = chosen.get(player.getUniqueId());
        if (kit != null) {
            return kit;
        }
        return kits.isEmpty() ? null : kits.getFirst();
    }

    public void apply(Player player) {
        Kit kit = kitOf(player);
        if (kit != null) {
            kit.apply(player);
        }
    }

    @Override
    protected void onInstall() {
        listen(PlayerInteractEvent.class, this::onInteract);
    }

    // slot 1, the engine already put the vote paper in 0 and the bed in 8
    @Override
    protected void onPlayerJoin(Player player) {
        if (!kits.isEmpty()) {
            ItemStack selector = Icons.of(Material.CHEST, Component.text("Choose a kit", NamedTextColor.GOLD),
                List.of(Icons.line("Right click to pick your kit")));
            player.getInventory().setItem(1, tags.tag(selector, KIT_SELECTOR));
        }
    }

    @Override
    protected void onPlayerLeave(Player player) {
        chosen.remove(player.getUniqueId());
    }

    @Override
    protected void onStart() {
        for (Player player : session().players()) {
            apply(player);
        }
    }

    private void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()
                || !KIT_SELECTOR.equals(tags.idOf(event.getItem()))) {
            return;
        }
        event.setCancelled(true);
        openMenu(event.getPlayer());
    }

    private void openMenu(Player viewer) {
        int rows = Menu.rowsFor(kits.size());
        Menu menu = new Menu(Component.text("Choose a kit"), rows);
        Kit current = kitOf(viewer);
        for (int i = 0; i < kits.size() && i < rows * 9; i++) {
            Kit kit = kits.get(i);
            List<Component> lore = new ArrayList<>();
            for (KitItem item : kit.items()) {
                lore.add(Icons.line(item.amount() + " x " + item.type().getKey().getKey().replace('_', ' ')));
            }
            lore.add(Component.empty());
            lore.add(kit == current
                ? Component.text("Selected", NamedTextColor.GREEN)
                : Component.text("Click to select", NamedTextColor.YELLOW));
            menu.set(i, new Button(Icons.of(kit.icon(), Component.text(kit.name(), NamedTextColor.AQUA), lore), player -> {
                chosen.put(player.getUniqueId(), kit);
                player.sendMessage(Component.text("You picked the " + kit.name() + " kit", NamedTextColor.GREEN));
                player.closeInventory();
            }));
        }
        menu.open(viewer);
    }
}
