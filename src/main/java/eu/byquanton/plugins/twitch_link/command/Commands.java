package eu.byquanton.plugins.twitch_link.command;

import com.google.common.collect.ImmutableList;
import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;


public class Commands {
    public static void registerCommands(TwitchLinkPlugin plugin) {
        final LegacyPaperCommandManager<CommandSender> manager = new LegacyPaperCommandManager<>(
                plugin,
                ExecutionCoordinator.simpleCoordinator(),
                SenderMapper.identity()
        );

        if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            manager.registerBrigadier();
        } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions();
        }

        ImmutableList.of(
                new LinkCommands(plugin, manager),
                new DebugCommands(plugin, manager)
        ).forEach(CommandHandler::register);
    }
}
