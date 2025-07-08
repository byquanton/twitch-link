package eu.byquanton.plugins.twitch_link.command;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import eu.byquanton.plugins.twitch_link.twitch.TwitchIntegration;
import eu.byquanton.plugins.twitch_link.twitch.TwitchUser;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.StringParser;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DebugCommands extends CommandHandler {

    private final TwitchIntegration twitchIntegration = plugin.getTwitchIntegration();

    public DebugCommands(TwitchLinkPlugin plugin, LegacyPaperCommandManager<CommandSender> commandManager) {
        super(plugin, commandManager);
    }


    @Override
    public void register() {
        commandManager.command(commandManager.commandBuilder("link")
                .literal("debug")
                .literal("follows")
                .permission("twitch_link.debug")
                .required("target", PlayerParser.playerParser())
                .required("broadcasterID", StringParser.stringParser())
                .handler(this::follows)
                .build()
        );
        commandManager.command(commandManager.commandBuilder("link")
                .literal("debug")
                .literal("subscribed")
                .permission("twitch_link.debug")
                .required("target", PlayerParser.playerParser())
                .required("broadcasterID", StringParser.stringParser())
                .handler(this::subscribed)
                .build()
        );
        commandManager.command(commandManager.commandBuilder("link")
                .literal("debug")
                .literal("live")
                .permission("twitch_link.debug")
                .required("target", PlayerParser.playerParser())
                .handler(this::isLive)
                .build()
        );
    }

    private void isLive(CommandContext<CommandSender> context) {
        Player target = context.get("target");

        try {
            TwitchUser linkedTwitchUser = plugin.getStorage().getLinkedTwitchUser(target.getUniqueId());

            twitchIntegration.getTwitchRequestUtil().isUserLive(linkedTwitchUser).whenComplete((userLive, throwable) -> {
                if (throwable != null) {
                    if (throwable instanceof TimeoutException) {
                        twitchIntegration.logError(context.sender(), messageProvider.getMessage("debug.error_timeout"));
                    } else {
                        twitchIntegration.logError(context.sender(), messageProvider.getMessage("debug.error_api", Placeholder.unparsed("error_message", throwable.getMessage())));
                    }
                } else {
                    context.sender().sendMessage(
                            userLive ?
                                    plugin.getMessageProvider().getMessage("debug.is_live_true") :
                                    plugin.getMessageProvider().getMessage("debug.is_live_false")
                    );
                }
            });
        } catch (SQLException e) {
            twitchIntegration.logError(context.sender(), messageProvider.getMessage("debug.error_database", Placeholder.unparsed("error_message", e.getMessage())));
        }
    }


    private void subscribed(CommandContext<CommandSender> context) {
        Player target = context.get("target");
        String broadcasterId = context.get("broadcasterID");

        try {
            TwitchUser linkedTwitchUser = plugin.getStorage().getLinkedTwitchUser(target.getUniqueId());

            twitchIntegration.getTwitchRequestUtil().isUserSubscribed(linkedTwitchUser, broadcasterId).whenComplete((userSubscribed, throwable) -> {
                if (throwable != null) {
                    if (throwable instanceof TimeoutException) {
                        twitchIntegration.logError(context.sender(), messageProvider.getMessage("debug.error_timeout"));
                    } else {
                        twitchIntegration.logError(context.sender(), messageProvider.getMessage("debug.error_api", Placeholder.unparsed("error_message", throwable.getMessage())));
                    }
                } else {
                    context.sender().sendMessage(
                            userSubscribed ?
                                    plugin.getMessageProvider().getMessage("debug.is_subscribed_true") :
                                    plugin.getMessageProvider().getMessage("debug.is_subscribed_false")
                    );
                }
            });

        } catch (SQLException e) {
            twitchIntegration.logError(context.sender(), messageProvider.getMessage("debug.error_database", Placeholder.unparsed("error_message", e.getMessage())));
        }
    }

    private void follows(CommandContext<CommandSender> context) {
        Player target = context.get("target");
        String broadcasterId = context.get("broadcasterID");

        try {
            TwitchUser linkedTwitchUser = plugin.getStorage().getLinkedTwitchUser(target.getUniqueId());

            twitchIntegration.getTwitchRequestUtil().isUserFollowing(linkedTwitchUser, broadcasterId).whenComplete((userFollowing, throwable) -> {
                if (throwable != null) {
                    if (throwable instanceof TimeoutException) {
                        twitchIntegration.logError(context.sender(), messageProvider.getMessage("debug.error_timeout"));
                    } else {
                        twitchIntegration.logError(context.sender(), messageProvider.getMessage("debug.error_api", Placeholder.unparsed("error_message", throwable.getMessage())));
                    }
                } else {
                    context.sender().sendMessage(
                            userFollowing ?
                                messageProvider.getMessage("debug.follows_true") :
                                messageProvider.getMessage("debug.follows_false")
                    );
                }
            });
        } catch (SQLException e) {
            twitchIntegration.logError(context.sender(), messageProvider.getMessage("debug.error_database", Placeholder.unparsed("error_message", e.getMessage())));
        }
    }

}
