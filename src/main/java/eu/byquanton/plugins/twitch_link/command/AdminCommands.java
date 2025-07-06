package eu.byquanton.plugins.twitch_link.command;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.paper.LegacyPaperCommandManager;


public class AdminCommands extends CommandHandler {


    public AdminCommands(TwitchLinkPlugin plugin, LegacyPaperCommandManager<CommandSender> commandManager) {
        super(plugin, commandManager);
    }


    @Override
    public void register() {
        commandManager.command(commandManager.commandBuilder("link")
                .literal("reload")
                .permission("twitch_link.admin")
                .handler(this::reload)
                .build()
        );
    }

    private void reload(CommandContext<CommandSender> context) {
        plugin.reloadConfig();
        context.sender().sendMessage("Reloaded Config");
        plugin.getMessageProvider().reload();
        context.sender().sendMessage("Reloaded Message Provider");
    }


}
