package eu.byquanton.plugins.twitch_link.listener;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import eu.byquanton.plugins.twitch_link.twitch.TwitchRequestUtil;
import eu.byquanton.plugins.twitch_link.twitch.TwitchUser;
import eu.byquanton.plugins.twitch_link.util.MessageProvider;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import eu.byquanton.plugins.twitch_link.util.ActionExecutor;

public class PlayerJoinListener implements Listener {

    private final TwitchLinkPlugin plugin;

    public PlayerJoinListener(TwitchLinkPlugin plugin) {
        this.plugin = plugin;
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
                                plugin.getTwitchIntegration().logError(player, messageProvider.getMessage("debug.error_timeout"));
                            } else {
                                plugin.getTwitchIntegration().logError(player, messageProvider.getMessage("debug.error_api", Placeholder.unparsed("error_message", throwable.getMessage())));
                            }
                        } else {
                            List<ConfigurationSection> subActions = ActionExecutor.getConfigList(plugin.getConfig(), "join.subscriber.actions");
                            List<ConfigurationSection> subElseActions = ActionExecutor.getConfigList(plugin.getConfig(), "join.subscriber.else_actions");
                            plugin.getActionExecutor().executeConfiguredActions(player, twitchUser.login(), subscribed ? subActions : subElseActions);
                        }
                    });
                }
                if (plugin.getConfig().getBoolean("join.follower.enabled")) {
                    CompletableFuture<Boolean> isFollower = requestUtil.isUserFollowing(twitchUser, broadcaster);
                    isFollower.orTimeout(5, TimeUnit.SECONDS).whenComplete((follower, throwable) -> {
                        if (throwable != null) {
                            if (throwable instanceof TimeoutException) {
                                plugin.getTwitchIntegration().logError(player, messageProvider.getMessage("debug.error_timeout"));
                            } else {
                                plugin.getTwitchIntegration().logError(player, messageProvider.getMessage("debug.error_api", Placeholder.unparsed("error_message", throwable.getMessage())));
                            }
                        } else {
                            List<ConfigurationSection> followerActions = ActionExecutor.getConfigList(plugin.getConfig(), "join.follower.actions");
                            List<ConfigurationSection> followerElseActions = ActionExecutor.getConfigList(plugin.getConfig(), "join.follower.else_actions");
                            plugin.getActionExecutor().executeConfiguredActions(player, twitchUser.login(), follower ? followerActions : followerElseActions);
                        }
                    });
                }

            }

            if (plugin.getConfig().getBoolean("join.live.enabled")) {
                CompletableFuture<Boolean> isLive = requestUtil.isUserLive(twitchUser);

                isLive.orTimeout(5, TimeUnit.SECONDS).whenComplete((live, throwable) -> {
                    if (throwable != null) {
                        if (throwable instanceof TimeoutException) {
                            plugin.getTwitchIntegration().logError(player, messageProvider.getMessage("debug.error_timeout"));
                        } else {
                            plugin.getTwitchIntegration().logError(player, messageProvider.getMessage("debug.error_api", Placeholder.unparsed("error_message", throwable.getMessage())));
                        }
                    } else {
                        List<ConfigurationSection> liveActions = ActionExecutor.getConfigList(plugin.getConfig(), "join.live.actions");
                        List<ConfigurationSection> liveElseActions = ActionExecutor.getConfigList(plugin.getConfig(), "join.live.else_actions");
                        plugin.getActionExecutor().executeConfiguredActions(player, twitchUser.login(), live ? liveActions : liveElseActions);
                    }
                });
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Error fetching linked Twitch data: " + e.getMessage());
        }
    }

}