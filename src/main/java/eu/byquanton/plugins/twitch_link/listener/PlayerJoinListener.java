package eu.byquanton.plugins.twitch_link.listener;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import eu.byquanton.plugins.twitch_link.twitch.TwitchRequestUtil;
import eu.byquanton.plugins.twitch_link.twitch.TwitchUser;
import eu.byquanton.plugins.twitch_link.util.MessageProvider;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PlayerJoinListener implements Listener {

    private final TwitchLinkPlugin plugin;
    private final GlobalRegionScheduler globalScheduler;

    public PlayerJoinListener(TwitchLinkPlugin plugin) {
        this.plugin = plugin;
        this.globalScheduler = plugin.getServer().getGlobalRegionScheduler();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            plugin.getStorage().createMinecraftPlayer(player.getUniqueId(), player.getName());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to insert player to the database");
        }

        boolean linked = plugin.getTwitchIntegration().isLinked(player.getUniqueId());
        MessageProvider messageProvider = plugin.getMessageProvider();

        if (plugin.getConfig().getBoolean("join.message", true)) {
            if (linked) {
                try {
                    TwitchUser twitchUser = plugin.getStorage().getLinkedTwitchUser(player.getUniqueId());
                    String twitchLogin = twitchUser.login();
                    player.sendMessage(messageProvider.getMessage("join.linked", Placeholder.unparsed("twitch_login", twitchLogin)));
                } catch (SQLException e) {
                    player.sendMessage(messageProvider.getMessage("debug.error_database", Placeholder.unparsed("error_message", e.getMessage())));
                }
            } else {
                player.sendMessage(messageProvider.getMessage("join.not_linked"));
            }
        }

        if (!linked) return;

        try {
            TwitchUser twitchUser = plugin.getStorage().getLinkedTwitchUser(player.getUniqueId());
            TwitchRequestUtil requestUtil = plugin.getTwitchIntegration().getTwitchRequestUtil();
            String broadcaster = plugin.getConfig().getString("broadcaster_id", "");

            if (!broadcaster.isEmpty()) {
                if (plugin.getConfig().getBoolean("join.subscriber.enabled")) {
                    CompletableFuture<Boolean> isSub = requestUtil.isUserSubscribed(twitchUser, broadcaster);

                    isSub.orTimeout(5, TimeUnit.SECONDS).whenComplete((subscribed, throwable) -> {
                        if (throwable != null) {
                            if (throwable instanceof TimeoutException) {
                                logError(player, messageProvider.getMessage("debug.error_timeout"));
                            } else {
                                logError(player, messageProvider.getMessage("debug.error_api", Placeholder.unparsed("error_message", throwable.getMessage())));
                            }
                        } else {
                            List<ConfigurationSection> subActions = getConfigList(plugin.getConfig(), "join.subscriber.actions");
                            List<ConfigurationSection> subElseActions = getConfigList(plugin.getConfig(), "join.subscriber.else_actions");
                            executeConfiguredActions(player, twitchUser.login(), subscribed ? subActions : subElseActions);
                        }
                    });
                }
                if (plugin.getConfig().getBoolean("join.follower.enabled")) {
                    CompletableFuture<Boolean> isFollower = requestUtil.isUserFollowing(twitchUser, broadcaster);
                    isFollower.orTimeout(5, TimeUnit.SECONDS).whenComplete((follower, throwable) -> {
                        if (throwable != null) {
                            if (throwable instanceof TimeoutException) {
                                logError(player, messageProvider.getMessage("debug.error_timeout"));
                            } else {
                                logError(player, messageProvider.getMessage("debug.error_api", Placeholder.unparsed("error_message", throwable.getMessage())));
                            }
                        } else {
                            List<ConfigurationSection> followerActions = getConfigList(plugin.getConfig(), "join.follower.actions");
                            List<ConfigurationSection> followerElseActions = getConfigList(plugin.getConfig(), "join.follower.else_actions");
                            executeConfiguredActions(player, twitchUser.login(), follower ? followerActions : followerElseActions);
                        }
                    });
                }

            }

            if (plugin.getConfig().getBoolean("join.live.enabled")) {
                CompletableFuture<Boolean> isLive = requestUtil.isUserLive(twitchUser);

                isLive.orTimeout(5, TimeUnit.SECONDS).whenComplete((live, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof TimeoutException) {
                            logError(player, messageProvider.getMessage("debug.error_timeout"));
                        } else {
                            logError(player, messageProvider.getMessage("debug.error_api", Placeholder.unparsed("error_message", throwable.getMessage())));
                        }
                    } else {
                        List<ConfigurationSection> liveActions = getConfigList(plugin.getConfig(), "join.live.actions");
                        List<ConfigurationSection> liveElseActions = getConfigList(plugin.getConfig(), "join.live.else_actions");
                        executeConfiguredActions(player, twitchUser.login(), live ? liveActions : liveElseActions);
                    }
                });
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error fetching linked Twitch data: " + e.getMessage());
        }
    }

    private void executeConfiguredActions(Player player, String twitchLogin, List<ConfigurationSection> actions) {
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
                        globalScheduler.execute(plugin, () -> Bukkit.broadcast(component));
                    } else {
                        globalScheduler.execute(plugin, () -> player.sendMessage(component));
                    }
                }
                case "command" -> {
                    String command = action.getString("command", "")
                            .replace("<player_name>", player.getName())
                            .replace("<twitch_name>", twitchLogin);
                    if (command.isEmpty()) return;
                    String executor = action.getString("executor", "console");
                    if (executor.equalsIgnoreCase("player")) {
                        globalScheduler.execute(plugin, () -> player.performCommand(command));
                    } else {
                        globalScheduler.execute(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
                    }
                }
            }
        });
    }

    private List<ConfigurationSection> getConfigList(ConfigurationSection config, String path) {
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


    private void logError(CommandSender sender, Component errorMessage) {
        sender.sendMessage(errorMessage);
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(errorMessage);
        plugin.getLogger().severe(plainMessage);
    }
}