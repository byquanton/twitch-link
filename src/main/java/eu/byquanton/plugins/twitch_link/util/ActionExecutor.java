package eu.byquanton.plugins.twitch_link.util;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ActionExecutor {

    private final TwitchLinkPlugin plugin;
    private final GlobalRegionScheduler globalRegionScheduler;

    public ActionExecutor(TwitchLinkPlugin plugin) {
        this.plugin = plugin;
        this.globalRegionScheduler = plugin.getServer().getGlobalRegionScheduler();
    }

    public void executeConfiguredActions(Player player, String twitchLogin, List<ConfigurationSection> actions) {
        if (actions == null) return;
        actions.forEach(action -> {
            String type = action.getString("type");
            if (type == null) return;

            String requiredPermission = action.getString("required_permission", "");

            if (!requiredPermission.isEmpty()) {
                boolean isNegated = requiredPermission.startsWith("!");

                String permission = isNegated
                        ? requiredPermission.substring(1)
                        : requiredPermission;

                boolean hasPermission = player.hasPermission(permission);

                boolean permissionCheckPassed = (isNegated && hasPermission) || (!isNegated && !hasPermission);
                if (permissionCheckPassed) {
                    return;
                }
            }

            switch (type.toLowerCase()) {
                case "message" -> {
                    Component component = MiniMessage.miniMessage().deserialize(action.getString("message", ""), Placeholder.unparsed("player_name", player.getName()), Placeholder.unparsed("twitch_name", twitchLogin));
                    if (action.getBoolean("broadcast", false)) {
                        globalRegionScheduler.execute(plugin, () -> Bukkit.broadcast(component));
                    } else {
                        globalRegionScheduler.execute(plugin, () -> player.sendMessage(component));
                    }
                }
                case "command" -> {
                    String command = action.getString("command", "")
                            .replace("<player_name>", player.getName())
                            .replace("<twitch_name>", twitchLogin);
                    if (command.isEmpty()) return;
                    String executor = action.getString("executor", "console");
                    if (executor.equalsIgnoreCase("player")) {
                        globalRegionScheduler.execute(plugin, () -> player.performCommand(command));
                    } else {
                        globalRegionScheduler.execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
                    }
                }
            }
        });
    }

    public void executeConfiguredActions(String playerName, String twitchLogin, List<ConfigurationSection> actions) {
        if (actions == null) return;
        actions.forEach(action -> {
            String type = action.getString("type");
            if (type == null) return;

            switch (type.toLowerCase()) {
                case "message" -> {
                    if (action.getBoolean("broadcast", false)) {
                        Component component = MiniMessage.miniMessage().deserialize(
                                action.getString("message", ""),
                                Placeholder.unparsed("player_name", playerName),
                                Placeholder.unparsed("twitch_name", twitchLogin)
                        );
                        globalRegionScheduler.execute(plugin, () -> Bukkit.broadcast(component));
                    }
                    // Can't send direct message to offline player
                }
                case "command" -> {
                    String command = action.getString("command", "")
                            .replace("<player_name>", playerName)
                            .replace("<twitch_name>", twitchLogin);
                    if (command.isEmpty()) return;
                    // Force console executor, since player is offline
                    globalRegionScheduler.execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
                }
            }
        });
    }


    public static List<ConfigurationSection> getConfigList(Configuration config, String path) {
        List<?> rawList = config.getList(path);
        if (rawList == null) return null;

        List<ConfigurationSection> result = new LinkedList<>();
        for (Object item : rawList) {
            if (item instanceof Map<?, ?> map) {
                MemoryConfiguration section = new MemoryConfiguration();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String key) {
                        section.set(key, entry.getValue());
                    }
                }
                result.add(section);
            }
        }
        return result;
    }


}
