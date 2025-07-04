package eu.byquanton.plugins.twitch_link.command;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public abstract class CommandHandler {
    protected final TwitchLinkPlugin plugin;
    protected final LegacyPaperCommandManager<CommandSender> commandManager;

    protected CommandHandler(TwitchLinkPlugin plugin, LegacyPaperCommandManager<CommandSender> commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public abstract void register();
}
