package eu.byquanton.plugins.twitch_link.twitch;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import eu.byquanton.plugins.twitch_link.storage.Storage;
import eu.byquanton.plugins.twitch_link.twitch.flow.TwitchDeviceCodeAuthentication;
import eu.byquanton.plugins.twitch_link.twitch.response.*;
import eu.byquanton.plugins.twitch_link.util.ActionExecutor;
import eu.byquanton.plugins.twitch_link.util.MessageProvider;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;


public class TwitchIntegration implements Listener {

    private final TwitchLinkPlugin plugin;
    private final Logger logger;

    private final YamlConfiguration twitchConfiguration;
    private final TwitchTokenValidation twitchTokenValidation;
    private final TwitchRequestUtil twitchRequestUtil;

    private final ScheduledExecutorService executorService;

    private final Map<String, ScheduledFuture<?>> validationTasks = new ConcurrentHashMap<>();

    private final Map<UUID, AtomicBoolean> loginPollingFlowCancellationTokens = new ConcurrentHashMap<>();

    private final Storage storage;


    public TwitchIntegration(TwitchLinkPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        this.twitchConfiguration = loadTwitchConfig();
        this.twitchTokenValidation = new TwitchTokenValidation(this);
        this.twitchRequestUtil = new TwitchRequestUtil(this);
        this.executorService = Executors.newScheduledThreadPool(4);
        this.storage = plugin.getStorage();

        loadAndValidateAllTwitchAccounts();
    }

    private YamlConfiguration loadTwitchConfig() {
        File twitchConfigFile = new File(plugin.getDataFolder(), "twitch.yml");
        if (!twitchConfigFile.exists()) {
            plugin.saveResource("twitch.yml", false);
        }
        return YamlConfiguration.loadConfiguration(twitchConfigFile);
    }


    public void shutdown() {
        validationTasks.forEach((userID, scheduledFuture) -> {
            logger.info("Shutting down validation task for user with id: " + userID);
            scheduledFuture.cancel(false);
        });

        executorService.shutdownNow();
    }

    public boolean isLinked(UUID uuid) {
        try {
            return storage.getLinkedTwitchUser(uuid) != null;
        } catch (SQLException e) {
            logger.severe("DB error while checking if linked: " + e.getMessage());
            return false;
        }
    }

    private void loadAndValidateAllTwitchAccounts() {
        try {
            Map<UUID, String> uuidToTwitchId = storage.getAllLinkedAccounts();
            for (Map.Entry<UUID, String> entry : uuidToTwitchId.entrySet()) {
                UUID uuid = entry.getKey();
                try {
                    TwitchUser twitchUser = storage.getLinkedTwitchUser(uuid);
                    if (twitchUser == null) {
                        logger.warning("No Twitch user found in DB for uuid: " + uuid);
                        removeAccount(uuid);
                        continue;
                    }

                    restoreToken(uuid);
                    validationTasks.put(twitchUser.twitchUserId(), executorService.scheduleAtFixedRate(() -> restoreToken(uuid), 1, 1, TimeUnit.HOURS));
                } catch (SQLException e) {
                    logger.severe("DB error while loading Twitch user for uuid " + uuid + ": " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.severe("DB error while loading all linked accounts: " + e.getMessage());
        }
    }

    public CompletableFuture<Boolean> startLoginFlow(Player player, Audience audience) {
        UUID uuid = player.getUniqueId();
        TwitchDeviceCodeAuthentication codeAuthentication = new TwitchDeviceCodeAuthentication(this);
        MessageProvider messageProvider = plugin.getMessageProvider();

        AtomicBoolean cancelToken = new AtomicBoolean(false);
        loginPollingFlowCancellationTokens.put(uuid, cancelToken);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return codeAuthentication.startDeviceAuthorizationFlow();
            } catch (Exception e) {
                audience.sendMessage(messageProvider.getMessage("twitch.error_contacting_twitch", Placeholder.unparsed("error_message", e.getMessage())));
                return null;
            }
        }).thenApply(authorizationResponse -> {
            if (authorizationResponse == null) return CompletableFuture.completedFuture(false);

            audience.sendMessage(messageProvider.getMessage(
                    "twitch.device_code",
                    Placeholder.unparsed("user_code", authorizationResponse.userCode()),
                    Placeholder.parsed("verification_uri", authorizationResponse.verificationUri())
            ));


            CompletableFuture<TokenResponse> tokenFuture = new CompletableFuture<>();

            Runnable pollToken = new Runnable() {
                @Override
                public void run() {
                    if (cancelToken.get()) {
                        tokenFuture.completeExceptionally(new InterruptedException("Login flow was cancelled."));
                        return;
                    }
                    try {
                        TokenResponse tokenResponse = codeAuthentication.obtainToken(authorizationResponse.deviceCode());
                        tokenFuture.complete(tokenResponse);
                    } catch (TwitchAPIException e) {
                        if ("authorization_pending".equals(e.getMessage())) {
                            audience.sendActionBar(messageProvider.getMessage("twitch.waiting_for_authorization"));
                            // Schedule next poll after 5 seconds
                            executorService.schedule(this, 5, TimeUnit.SECONDS);
                        } else {
                            audience.sendMessage(messageProvider.getMessage(
                                    "twitch.twitch_api_error",
                                    Placeholder.unparsed("error_message", e.getMessage())
                            ));
                            tokenFuture.completeExceptionally(e);
                        }
                    } catch (IOException | InterruptedException e) {
                        audience.sendMessage(messageProvider.getMessage(
                                "twitch.authorization_failed",
                                Placeholder.unparsed("error_message", e.getMessage())
                        ));
                        tokenFuture.completeExceptionally(e);
                    }
                }
            };

            // Start polling immediately
            executorService.submit(pollToken);

            executorService.schedule(() -> {
                if (!tokenFuture.isDone()) {
                    tokenFuture.completeExceptionally(new TimeoutException("Authorization timed out after 2 minutes."));
                    audience.sendMessage(messageProvider.getMessage("twitch.authorization_timed_out"));
                    abortLoginPollingFlow(uuid);
                }
            }, 2, TimeUnit.MINUTES);

            return tokenFuture.join();
        }).thenCompose(tokenResponseObject -> {
            loginPollingFlowCancellationTokens.remove(uuid);
            if (tokenResponseObject == null) return CompletableFuture.completedFuture(false);

            TokenResponse tokenResponse = (TokenResponse) tokenResponseObject;

            TokenValidationResponse validationResponse;
            try {
                validationResponse = twitchTokenValidation.validate(tokenResponse.accessToken());
            } catch (Exception e) {
                audience.sendMessage(messageProvider.getMessage(
                        "twitch.token_validation_failed",
                        Placeholder.unparsed("error_message", e.getMessage())
                ));
                return CompletableFuture.completedFuture(false);
            }

            String twitchUserId = validationResponse.userId();
            String twitchLogin = validationResponse.login();

            try {
                UUID linkedMinecraftUUID = storage.getLinkedMinecraftUUID(twitchUserId);

                if (linkedMinecraftUUID != null) {
                    audience.sendMessage(messageProvider.getMessage("link.already_linked_other_player", Placeholder.unparsed("twitch_login", twitchLogin)));
                    return CompletableFuture.completedFuture(false);
                }
            } catch (SQLException e) {
                audience.sendMessage(messageProvider.getMessage("debug.error_database", Placeholder.unparsed("error_message", e.getMessage())));
                logger.severe("DB error while checking if user is already in database: " + e.getMessage());
                return CompletableFuture.completedFuture(false);
            }

            try {
                storage.createTwitchUser(twitchUserId, twitchLogin, tokenResponse.accessToken(), tokenResponse.refreshToken());
                storage.linkAccounts(uuid, twitchUserId);
            } catch (SQLException e) {
                audience.sendMessage(messageProvider.getMessage("twitch.db_save_error"));
                logger.severe("DB error while saving Twitch user: " + e.getMessage());
                return CompletableFuture.completedFuture(false);
            }

            validationTasks.put(twitchUserId, executorService.scheduleAtFixedRate(() -> restoreToken(uuid), 1, 1, TimeUnit.HOURS));

            audience.sendMessage(plugin.getMessageProvider().getMessage(
                    "twitch.logged_in_as",
                    Placeholder.unparsed("twitch_login", twitchLogin)
            ));

            return CompletableFuture.completedFuture(true);
        }).thenApply(success -> {
            if (success) {
                try {
                    TwitchUser twitchUser = plugin.getStorage().getLinkedTwitchUser(player.getUniqueId());
                    String broadcaster = plugin.getConfig().getString("broadcaster_id", "");

                    CompletableFuture<Boolean> isSub = getTwitchRequestUtil().isUserSubscribed(twitchUser, broadcaster);
                    isSub.orTimeout(5, TimeUnit.SECONDS).whenComplete((subscribed, throwable) -> {
                        if (throwable != null) {
                            if (throwable instanceof TimeoutException) {
                                logError(player, messageProvider.getMessage("debug.error_timeout"));
                            } else {
                                logError(player, messageProvider.getMessage("debug.error_api", Placeholder.unparsed("error_message", throwable.getMessage())));
                            }
                        } else {
                            if (subscribed) {
                                plugin.getActionExecutor().executeConfiguredActions(player, twitchUser.login(), ActionExecutor.getConfigList(plugin.getConfig(), "link.subscriber.actions"));
                            }
                        }
                    });

                    CompletableFuture<Boolean> isFollower = getTwitchRequestUtil().isUserFollowing(twitchUser, broadcaster);
                    isFollower.orTimeout(5, TimeUnit.SECONDS).whenComplete((follower, throwable) -> {
                        if (throwable != null) {
                            if (throwable instanceof TimeoutException) {
                                plugin.getTwitchIntegration().logError(player, messageProvider.getMessage("debug.error_timeout"));
                            } else {
                                plugin.getTwitchIntegration().logError(player, messageProvider.getMessage("debug.error_api", Placeholder.unparsed("error_message", throwable.getMessage())));
                            }
                        } else {
                            if (follower) {
                                plugin.getActionExecutor().executeConfiguredActions(player, twitchUser.login(), ActionExecutor.getConfigList(plugin.getConfig(), "link.follower.actions"));
                            }
                        }
                    });


                } catch (Exception e) {
                    audience.sendMessage(plugin.getMessageProvider().getMessage("debug.error_database", Placeholder.unparsed("error_message", e.getMessage())));
                }
            }
            return success;
        });
    }

    public void abortLoginPollingFlow(UUID uuid) {
        AtomicBoolean token = loginPollingFlowCancellationTokens.get(uuid);
        if (token != null) {
            token.set(true);
        }
    }


    public void restoreToken(UUID uuid) {
        try {
            TwitchUser twitchUser = storage.getLinkedTwitchUser(uuid);
            if (twitchUser == null) return;
            TokenValidationResponse validation = twitchTokenValidation.validate(twitchUser.accessToken());
            logger.info("Validated session for Twitch user " + validation.login());
        } catch (TwitchAPIException e) {
            if (e.getResponse().status() == 401) {
                logger.warning("Token expired. Attempting refresh for user: " + uuid);
                refreshToken(uuid);
            } else {
                logger.warning("Twitch API error for user " + uuid + ": " + e.getMessage());
                removeAccount(uuid);
            }
        } catch (IOException | InterruptedException | SQLException ex) {
            logger.severe("Error validating token for user " + uuid + ": " + ex.getMessage());
            removeAccount(uuid);
        }
    }


    private void refreshToken(UUID uuid) {
        try {
            TwitchUser twitchUser = storage.getLinkedTwitchUser(uuid);
            if (twitchUser == null) return;
            TokenRefreshResponse refreshed = twitchTokenValidation.refresh(twitchUser.refreshToken());
            try {
                boolean updated = storage.updateTwitchUser(twitchUser.twitchUserId(), twitchUser.login(), refreshed.accessToken(), refreshed.refreshToken());
                if (!updated) {
                    logger.severe("Failed to update tokens in DB for user " + uuid);
                }
            } catch (SQLException e) {
                logger.severe("Failed to update tokens in DB for user " + uuid + ": " + e.getMessage());
            }
            logger.info("Refreshed token for user " + uuid);
        } catch (Exception e) {
            logger.severe("Failed to refresh token for user " + uuid + ": " + e.getMessage());
            removeAccount(uuid);
        }
    }


    public void removeAccount(UUID uuid) {
        try {
            TwitchUser twitchUser = storage.getLinkedTwitchUser(uuid);
            if (twitchUser != null) {
                OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);
                if (offlinePlayer.isOnline()) {
                    plugin.getActionExecutor().executeConfiguredActions(offlinePlayer.getPlayer(), twitchUser.login(), ActionExecutor.getConfigList(plugin.getConfig(), "unlink.actions"));
                } else {
                    plugin.getActionExecutor().executeConfiguredActions(plugin.getStorage().getLastKnownName(uuid), twitchUser.login(), ActionExecutor.getConfigList(plugin.getConfig(), "unlink.actions"));
                }
                storage.unLinkAccount(uuid);
                removeTwitchUser(twitchUser.twitchUserId());
            }
        } catch (Exception e) {
            logger.severe("Error while removing account for user " + uuid + ": " + e.getMessage());
        }
        logger.info("Removed invalid Twitch account for user " + uuid);
    }


    private void removeTwitchUser(String twitchUserId) {
        try {
            validationTasks.get(twitchUserId).cancel(false);
            storage.deleteTwitchUser(twitchUserId);
            logger.info("Deleted Twitch user from DB: " + twitchUserId);
        } catch (SQLException e) {
            logger.severe("Failed to delete Twitch user from DB: " + twitchUserId + ", error: " + e.getMessage());
        }
    }


    public void logError(CommandSender sender, Component errorMessage) {
        sender.sendMessage(errorMessage);
        String plainMessage = PlainTextComponentSerializer.plainText().serialize(errorMessage);
        plugin.getLogger().severe(plainMessage);
    }

    public TwitchRequestUtil getTwitchRequestUtil() {
        return twitchRequestUtil;
    }

    public String getOauthEndpoint() {
        return twitchConfiguration.getString("oauth_endpoint", "https://id.twitch.tv/oauth2/");
    }

    public String getHelixEndpoint() {
        return twitchConfiguration.getString("helix_endpoint", "https://api.twitch.tv/helix/");
    }

    public String getClientID() {
        return twitchConfiguration.getString("client_id");
    }

    public String getScopes() {
        return twitchConfiguration.getString("scopes");
    }
}