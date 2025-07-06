package eu.byquanton.plugins.twitch_link.command;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import eu.byquanton.plugins.twitch_link.util.MessageProvider;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public abstract class CommandHandler {
    protected final TwitchLinkPlugin plugin;
    protected final MessageProvider messageProvider;
    protected final LegacyPaperCommandManager<CommandSender> commandManager;

    protected CommandHandler(TwitchLinkPlugin plugin, LegacyPaperCommandManager<CommandSender> commandManager) {
        this.plugin = plugin;
        this.messageProvider = plugin.getMessageProvider();
        this.commandManager = commandManager;
    }

    public abstract void register();
}
