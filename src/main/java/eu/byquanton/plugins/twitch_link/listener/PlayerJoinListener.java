package eu.byquanton.plugins.twitch_link.listener;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;

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

        // TODO: Remove/Make configurable
        boolean linked = plugin.getTwitchIntegration().isLinked(player.getUniqueId());
        try {
            player.sendMessage("[Twitch Link] Your account is currently "+(linked ? "linked to " + plugin.getStorage().getLinkedTwitchUser(player.getUniqueId()).login() : "not linked"));
        } catch (SQLException e) {
            plugin.getLogger().severe("Database Error: "+ e.getMessage());
        }

    }


}
