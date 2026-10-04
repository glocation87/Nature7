package io.github.glocation87.nature7.stats;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.glocation87.nature7.engine.GameRegistry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

// /n7 stats [player]
public final class StatsCommand {
    private final StatsService stats;
    private final GameRegistry registry;
    private final Executor mainThread;

    public StatsCommand(StatsService stats, GameRegistry registry, Executor mainThread) {
        this.stats = stats;
        this.registry = registry;
        this.mainThread = mainThread;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("stats")
            .executes(ctx -> ctx.getSource().getExecutor() instanceof Player player
                ? show(ctx, player.getUniqueId(), player.getName())
                : 0)
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (online.getName().toLowerCase(Locale.ROOT).startsWith(builder.getRemainingLowerCase())) {
                            builder.suggest(online.getName());
                        }
                    }
                    return builder.buildFuture();
                })
                .executes(this::showOther));
    }

    // getOfflinePlayerIfCached never asks mojang, the plain getOfflinePlayer(name) can block the main thread
    private int showOther(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "player");
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(name);
        if (target == null) {
            ctx.getSource().getSender().sendMessage(Component.text("No player called " + name + " has played here", NamedTextColor.RED));
            return 0;
        }
        return show(ctx, target.getUniqueId(), target.getName() == null ? name : target.getName());
    }

    private int show(CommandContext<CommandSourceStack> ctx, UUID target, String name) {
        if (!(ctx.getSource().getExecutor() instanceof Player viewer)) {
            return 0;
        }
        UUID viewerId = viewer.getUniqueId();
        stats.load(target).thenAcceptAsync(loaded -> {
            Player online = Bukkit.getPlayer(viewerId);
            if (online != null) {
                StatsMenu.open(online, name, loaded, registry.all());
            }
        }, mainThread);
        return Command.SINGLE_SUCCESS;
    }
}
