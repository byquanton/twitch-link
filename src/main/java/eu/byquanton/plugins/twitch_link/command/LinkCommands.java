package eu.byquanton.plugins.twitch_link.command;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import eu.byquanton.plugins.twitch_link.twitch.TwitchIntegration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LinkCommands extends CommandHandler {

    private final Map<UUID, CompletableFuture<Boolean>> hasLoginInProgress = new ConcurrentHashMap<>();
    private final TwitchIntegration twitchIntegration = plugin.getTwitchIntegration();

    public LinkCommands(TwitchLinkPlugin plugin, LegacyPaperCommandManager<CommandSender> commandManager) {
        super(plugin, commandManager);
    }


    @Override
    public void register() {
        commandManager.command(commandManager.commandBuilder("link")
                .permission("twitch_link.link")
                .senderType(Player.class)
                .handler(this::linkAccount)
                .build()
        );
        commandManager.command(commandManager.commandBuilder("link")
                .literal("abort")
                .permission("twitch_link.link")
                .senderType(Player.class)
                .handler(this::linkAbort)
                .build()
        );
        commandManager.command(commandManager.commandBuilder("unlink")
                .permission("twitch_link.unlink")
                .senderType(Player.class)
                .handler(this::unLinkAccount)
                .build()
        );
    }

    private void linkAccount(CommandContext<Player> context) {
        UUID playerUUID = context.sender().getUniqueId();

        if (twitchIntegration.isLinked(playerUUID)) {
            try {
                String twitchLogin = plugin.getStorage().getLinkedTwitchUser(context.sender().getUniqueId()).login();
                context.sender().sendMessage(messageProvider.getMessage("link.already_linked", Placeholder.unparsed("twitch_login", twitchLogin)));
            } catch (SQLException e) {
                context.sender().sendMessage(messageProvider.getMessage("debug.error_database", Placeholder.unparsed("error_message", e.getMessage())));
            }
            return;
        }

        if (hasLoginInProgress.containsKey(playerUUID)) {
            context.sender().sendMessage(messageProvider.getMessage("link.already_started"));
            return;
        }

        CompletableFuture<Boolean> loginFlowFuture = twitchIntegration.startLoginFlow(playerUUID, context.sender());

        hasLoginInProgress.put(playerUUID, loginFlowFuture);

        loginFlowFuture.thenAccept(success -> {
            if (!success) {
                context.sender().sendMessage(messageProvider.getMessage("link.integration_failed"));
            }
            hasLoginInProgress.remove(playerUUID);
        });
    }


    private void linkAbort(CommandContext<Player> context) {
        UUID playerUUID = context.sender().getUniqueId();

        if (!hasLoginInProgress.containsKey(playerUUID)) {
            context.sender().sendMessage(messageProvider.getMessage("link.not_started"));
            return;
        }
        CompletableFuture<Boolean> completableFuture = hasLoginInProgress.get(playerUUID);


        twitchIntegration.abortLoginPollingFlow(playerUUID);
        completableFuture.cancel(true);
        hasLoginInProgress.remove(playerUUID);
        context.sender().sendMessage(messageProvider.getMessage("link.aborted"));
    }

    private void unLinkAccount(CommandContext<Player> context) {
        UUID playerUUID = context.sender().getUniqueId();

        if (twitchIntegration.isLinked(playerUUID)) {
            twitchIntegration.removeAccount(playerUUID);
            context.sender().sendMessage(messageProvider.getMessage("link.account_unlinked"));
        } else {
            context.sender().sendMessage(messageProvider.getMessage("link.account_not_linked"));
        }
    }

}
