package eu.byquanton.plugins.twitch_link;

import eu.byquanton.plugins.twitch_link.command.Commands;
import eu.byquanton.plugins.twitch_link.listener.PlayerJoinListener;
import eu.byquanton.plugins.twitch_link.storage.Storage;
import eu.byquanton.plugins.twitch_link.twitch.TwitchIntegration;
import eu.byquanton.plugins.twitch_link.util.ActionExecutor;
import eu.byquanton.plugins.twitch_link.util.MessageProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class TwitchLinkPlugin extends JavaPlugin {

    private MessageProvider messageProvider;
    private Storage storage;
    private TwitchIntegration twitchIntegration;
    private ActionExecutor actionExecutor;


    @Override
    public void onEnable() {
        if (getDataFolder().mkdirs()) {
            getLogger().info("Created Data folder");
        }
        saveDefaultConfig();

        messageProvider = new MessageProvider(this, new File(getDataFolder(), "messages.yml"), "/messages.yml");
        storage = new Storage(this, new File(getDataFolder(), "database.db"));
        actionExecutor = new ActionExecutor(this);
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


    public MessageProvider getMessageProvider() {
        return messageProvider;
    }

    public TwitchIntegration getTwitchIntegration() {
        return twitchIntegration;
    }

    public Storage getStorage() {
        return storage;
    }

    public ActionExecutor getActionExecutor() {
        return actionExecutor;
    }
}
