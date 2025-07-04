package eu.byquanton.plugins.twitch_link.command;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import eu.byquanton.plugins.twitch_link.twitch.TwitchIntegration;
import eu.byquanton.plugins.twitch_link.twitch.TwitchUser;
import eu.byquanton.plugins.twitch_link.twitch.response.helix.HelixException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.StringParser;

import java.io.IOException;
import java.sql.SQLException;

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

            boolean userFollowing = twitchIntegration.getTwitchRequestUtil().isUserLive(linkedTwitchUser);

            context.sender().sendMessage(userFollowing ? "Player is currently live on Twitch" : "Player isn't currently live on twitch");
        } catch (SQLException e) {
            logError(context, "Database Error, failed to read user data. Message: " + e.getMessage());
        } catch (HelixException | InterruptedException | IOException e) {
            logError(context, "API Error, failed to fetch data. Message: " + e.getMessage());
        }
    }


    private void subscribed(CommandContext<CommandSender> context) {
        Player target = context.get("target");
        String broadcasterId = context.get("broadcasterID");

        try {
            TwitchUser linkedTwitchUser = plugin.getStorage().getLinkedTwitchUser(target.getUniqueId());

            boolean userFollowing = twitchIntegration.getTwitchRequestUtil().isUserSubscribed(linkedTwitchUser, broadcasterId);

            context.sender().sendMessage(userFollowing ? "Player is subscribed to the specified broadcaster" : "Player isn't subscribed to the specified broadcaster");
        } catch (SQLException e) {
            logError(context, "Database Error, failed to read user data. Message: " + e.getMessage());
        } catch (HelixException | InterruptedException | IOException e) {
            logError(context, "API Error, failed to fetch data. Message: " + e.getMessage());
        }
    }

    private void follows(CommandContext<CommandSender> context) {
        Player target = context.get("target");
        String broadcasterId = context.get("broadcasterID");

        try {
            TwitchUser linkedTwitchUser = plugin.getStorage().getLinkedTwitchUser(target.getUniqueId());

            boolean userFollowing = twitchIntegration.getTwitchRequestUtil().isUserFollowing(linkedTwitchUser, broadcasterId);

            context.sender().sendMessage(userFollowing ? "Player follows the specified broadcaster" : "Player isn't following the specified broadcaster");
        } catch (SQLException e) {
            logError(context, "Database Error, failed to read user data. Message: " + e.getMessage());
        } catch (HelixException | InterruptedException | IOException e) {
            logError(context, "API Error, failed to fetch data. Message: " + e.getMessage());
        }
    }


    private void logError(CommandContext<CommandSender> context, String errorMessage) {
        context.sender().sendMessage(Component.text(errorMessage).color(NamedTextColor.RED));
        plugin.getLogger().severe(errorMessage);
    }

}
