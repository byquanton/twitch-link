package eu.byquanton.plugins.twitch_link.util;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Set;

public class MessageProvider {
    private YamlConfiguration config;
    private final YamlConfiguration defaultConfig;
    private final File file;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final TwitchLinkPlugin plugin;

    public MessageProvider(TwitchLinkPlugin plugin, File file, String resourceName) {
        this.plugin = plugin;
        this.file = file;

        if (!file.exists()) {
            try (InputStream inputStream = getClass().getResourceAsStream(resourceName)) {
                if (inputStream != null) {
                    Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    plugin.getLogger().severe("Resource " + resourceName + " not found in JAR.");
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to copy default messages: " + e.getMessage());
            }
        }

        this.config = YamlConfiguration.loadConfiguration(file);

        InputStream internalStream = getClass().getResourceAsStream(resourceName);
        if (internalStream != null) {
            this.defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(internalStream));
            mergeDefaults();
        } else {
            plugin.getLogger().warning("Default message resource " + resourceName + " not found.");
            this.defaultConfig = new YamlConfiguration();
        }
    }

    private void mergeDefaults() {
        boolean updated = false;

        Set<String> keys = defaultConfig.getKeys(true);
        for (String key : keys) {
            if (!config.contains(key)) {
                config.set(key, defaultConfig.get(key));
                updated = true;
            }
        }

        if (updated) {
            try {
                config.save(file);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save updated messages file: " + e.getMessage());
            }
        }
    }

    public Component getMessage(String key, TagResolver... placeholders) {
        String rawString = this.config.getString(key);
        TagResolver[] mergedPlaceholders = new TagResolver[placeholders.length + 1];
        String prefixString = this.config.getString("prefix");
        if (prefixString != null) {
            mergedPlaceholders[0] = Placeholder.parsed("prefix", prefixString);
            System.arraycopy(placeholders, 0, mergedPlaceholders, 1, placeholders.length);
        }
        return miniMessage.deserialize(rawString == null ? key : rawString, mergedPlaceholders);
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
        mergeDefaults();
    }
}
