package eu.byquanton.plugins.twitch_link.command;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import eu.byquanton.plugins.twitch_link.twitch.TwitchIntegration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

import java.util.ArrayList;
import java.util.UUID;

public class LinkCommands extends CommandHandler {

    private final ArrayList<UUID> hasLoginInProgress = new ArrayList<>();
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

        if (hasLoginInProgress.contains(playerUUID)) {
            context.sender().sendMessage(MiniMessage.miniMessage().deserialize("You already started a login process. Use <gray>/link abort</gray> to abort it."));
            return;
        }

        hasLoginInProgress.add(playerUUID);

        new BukkitRunnable() {
            @Override
            public void run() {
                boolean success = twitchIntegration.startLoginFlow(playerUUID, context.sender());
                if (!success) {
                    context.sender().sendMessage(Component.text("Twitch Integration failed"));
                }
                hasLoginInProgress.remove(playerUUID);
            }
        }.runTaskAsynchronously(plugin);
    }


    private void linkAbort(CommandContext<Player> context) {
        // TODO: Not Implemented
        context.sender().sendMessage("TODO: Not Implemented");
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
