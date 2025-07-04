package eu.byquanton.plugins.twitch_link;

import eu.byquanton.plugins.twitch_link.command.Commands;
import eu.byquanton.plugins.twitch_link.listener.PlayerJoinListener;
import eu.byquanton.plugins.twitch_link.storage.Storage;
import eu.byquanton.plugins.twitch_link.twitch.TwitchIntegration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class TwitchLinkPlugin extends JavaPlugin {
    private TwitchIntegration twitchIntegration;
    private Storage storage;

    @Override
    public void onEnable() {
        if (getDataFolder().mkdirs()) {
            getLogger().info("Created Data folder");
        }
        storage = new Storage(this, new File(getDataFolder(), "database.db"));
        twitchIntegration = new TwitchIntegration(this);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        Commands.registerCommands(this);
    }

    @Override
    public void onDisable() {
        twitchIntegration.shutdown();
        storage.close();
        saveConfig();
    }

    public TwitchIntegration getTwitchIntegration() {
        return twitchIntegration;
    }


    public Storage getStorage() {
        return storage;
    }
}
