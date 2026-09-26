package io.github.glocation87.nature7.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.glocation87.nature7.engine.GameRegistry;
import io.github.glocation87.nature7.engine.SessionIndex;
import io.github.glocation87.nature7.engine.SessionManager;
import io.github.glocation87.nature7.engine.SessionProcess;
import io.github.glocation87.nature7.types.GameType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public final class NatureCommand {
    private final GameRegistry registry;
    private final SessionManager sessionManager;
    private final SessionIndex index;

    public NatureCommand(GameRegistry registry, SessionManager sessionManager, SessionIndex index) {
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.index = index;
    }

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("nature7")
            .then(Commands.literal("join")
                .then(Commands.argument("game", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        for (GameType type : registry.all()) {
                            if (type.id().startsWith(builder.getRemainingLowerCase())) {
                                builder.suggest(type.id());
                            }
                        }
                        return builder.buildFuture();
                    })
                    .executes(this::join)))
            .then(Commands.literal("leave").executes(this::leave))
            .then(Commands.literal("list").executes(this::list))
            .build();
    }

    private int list(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (sessionManager.getActiveSessions().isEmpty()) {
            sender.sendMessage(Component.text("No active sessions", NamedTextColor.GRAY));
            return Command.SINGLE_SUCCESS;
        }
        for (SessionProcess session : sessionManager.getActiveSessions()) {
            sender.sendMessage(Component.text()
                    .append(session.type().displayName())
                    .append(Component.text(" " + session.state() + " " + session.playerCount() + "/" + session.type().maxPlayers(), NamedTextColor.GRAY)));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int join(CommandContext<CommandSourceStack> ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(ctx, "game");
        Optional<GameType> type = registry.find(id);
        if (type.isEmpty()) {
            player.sendMessage(Component.text("Unknown minigame: " + id, NamedTextColor.RED));
            return 0;
        }
        if (index.getSession(player).isPresent()) {
            player.sendMessage(Component.text("You are already in a game, use /nature7 leave first", NamedTextColor.RED));
            return 0;
        }
        if (sessionManager.joinSession(player, type.get()).isEmpty()) {
            player.sendMessage(Component.text("Could not join " + id + " right now", NamedTextColor.RED));
            return 0;
        }
        return Command.SINGLE_SUCCESS;
    }

    private int leave(CommandContext<CommandSourceStack> ctx) {
        Player player = requirePlayer(ctx);
        if (player == null) {
            return 0;
        }
        if (index.getSession(player).isEmpty()) {
            player.sendMessage(Component.text("You are not in a game", NamedTextColor.RED));
            return 0;
        }
        sessionManager.leaveSession(player);
        return Command.SINGLE_SUCCESS;
    }

    private static @Nullable Player requirePlayer(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getExecutor() instanceof Player player) {
            return player;
        }
        ctx.getSource().getSender().sendMessage(Component.text("Only players can use this", NamedTextColor.RED));
        return null;
    }
}
