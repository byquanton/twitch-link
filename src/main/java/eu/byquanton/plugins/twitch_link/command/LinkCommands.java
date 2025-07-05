package eu.byquanton.plugins.twitch_link.command;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import eu.byquanton.plugins.twitch_link.twitch.TwitchIntegration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

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
            context.sender().sendMessage(Component.text("You are currently already authenticated"));
            // TODO: Give information and add button for removing the link
            return;
        }

        if (hasLoginInProgress.containsKey(playerUUID)) {
            context.sender().sendMessage(MiniMessage.miniMessage().deserialize("You already started a login process. Use <gray>/link abort</gray> to abort it."));
            return;
        }

        CompletableFuture<Boolean> loginFlowFuture = twitchIntegration.startLoginFlow(playerUUID, context.sender());

        hasLoginInProgress.put(playerUUID, loginFlowFuture);


        loginFlowFuture.thenAccept(aBoolean -> {
            if (aBoolean) {
                context.sender().sendMessage(Component.text("Twitch Integration was successful"));
            } else {
                context.sender().sendMessage(Component.text("Twitch Integration failed"));
            }
            hasLoginInProgress.remove(playerUUID);
        });
    }


    private void linkAbort(CommandContext<Player> context) {
        UUID playerUUID = context.sender().getUniqueId();

        if (!hasLoginInProgress.containsKey(playerUUID)) {
            context.sender().sendMessage(MiniMessage.miniMessage().deserialize("You haven't started a login process yet. Use <gray>/link</gray> to start it."));
            return;
        }
        CompletableFuture<Boolean> completableFuture = hasLoginInProgress.get(playerUUID);


        twitchIntegration.abortLoginPollingFlow(playerUUID);
        completableFuture.cancel(true);
        hasLoginInProgress.remove(playerUUID);
        context.sender().sendMessage(Component.text("Your login process was aborted."));
    }

    private void unLinkAccount(CommandContext<Player> context) {
        UUID playerUUID = context.sender().getUniqueId();

        if (twitchIntegration.isLinked(playerUUID)) {
            twitchIntegration.removeAccount(playerUUID);
            context.sender().sendMessage("Account is unlinked");
        } else {
            context.sender().sendMessage("You don't have your Account linked. Use /link to link your account");
        }
    }

}
