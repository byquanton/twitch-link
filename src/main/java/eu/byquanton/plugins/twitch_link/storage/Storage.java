package eu.byquanton.plugins.twitch_link.storage;

import eu.byquanton.plugins.twitch_link.TwitchLinkPlugin;
import eu.byquanton.plugins.twitch_link.twitch.TwitchUser;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class Storage {
    private final Logger logger;
    private Connection connection;

    public Storage(TwitchLinkPlugin twitchLink, File dataBase) {
        this.logger = twitchLink.getSLF4JLogger();

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataBase.getPath());
            createTablesIfNotExists();

        } catch (ClassNotFoundException | SQLException e) {
            logger.error("Error while creating database connection", e);
        }

    }

    private void createTablesIfNotExists() throws SQLException {
        createTwitchuserTableIfNotExists();
        createMinecraftTableIfNotExists();
        logger.info("Database initialized");
    }

    private ResultSet executeQuery(@Language(value = "SQL") String query, Object... args) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
        }
        return statement.executeQuery();
    }

    private void createTwitchuserTableIfNotExists() throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS TWITCH_USERS (" +
                        "    USER_ID TEXT PRIMARY KEY NOT NULL UNIQUE," +
                        "    LOGIN TEXT NOT NULL," +
                        "    ACCESS_TOKEN TEXT NOT NULL," +
                        "    REFRESH_TOKEN TEXT NOT NULL" +
                        ");"
        );
        statement.close();
    }

    private void createMinecraftTableIfNotExists() throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS MINECRAFT_ACCOUNTS(" +
                        "    UUID               TEXT PRIMARY KEY NOT NULL UNIQUE," +
                        "    LAST_KNOWN_NAME    TEXT NULL," +
                        "    USER_ID            TEXT NULL," +
                        "    FOREIGN KEY (USER_ID) REFERENCES TWITCH_USERS(USER_ID)" +
                        ");"
        );
        statement.close();
    }

    public void createTwitchUser(String twitchUserID, String login, String accessToken, String refreshToken) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO TWITCH_USERS (USER_ID, LOGIN, ACCESS_TOKEN, REFRESH_TOKEN) VALUES (?, ?, ?, ?)"
        );
        statement.setString(1, twitchUserID);
        statement.setString(2, login);
        statement.setString(3, accessToken);
        statement.setString(4, refreshToken);
        statement.executeUpdate();
        statement.close();
    }

    public boolean updateTwitchUser(String twitchUserID, String login, String accessToken, String refreshToken) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "UPDATE TWITCH_USERS SET LOGIN = ?, ACCESS_TOKEN = ?, REFRESH_TOKEN = ? WHERE USER_ID = ?;"
        );
        statement.setString(1, login);
        statement.setString(2, accessToken);
        statement.setString(3, refreshToken);
        statement.setString(4, twitchUserID);
        int updated = statement.executeUpdate();
        statement.close();

        if (updated == 0) {
            logger.warn("No Twitch user found with USER_ID: {}", twitchUserID);
            return false;
        }
        return true;
    }

    public TwitchUser getLinkedTwitchUser(UUID uuid) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT USER_ID FROM MINECRAFT_ACCOUNTS WHERE UUID = ? AND USER_ID IS NOT NULL;"
        );
        statement.setString(1, uuid.toString());
        ResultSet rs = statement.executeQuery();
        if (rs.next()) {
            String twitchUserId = rs.getString("USER_ID");
            rs.close();
            statement.close();
            return getTwitchUserById(twitchUserId);
        }
        rs.close();
        statement.close();
        return null;
    }

    public TwitchUser getTwitchUserById(String twitchUserId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT USER_ID, LOGIN, ACCESS_TOKEN, REFRESH_TOKEN FROM TWITCH_USERS WHERE USER_ID = ?;"
        );
        statement.setString(1, twitchUserId);
        ResultSet rs = statement.executeQuery();
        if (rs.next()) {
            TwitchUser user = new TwitchUser(
                    rs.getString("USER_ID"),
                    rs.getString("LOGIN"),
                    rs.getString("ACCESS_TOKEN"),
                    rs.getString("REFRESH_TOKEN")
            );
            rs.close();
            statement.close();
            return user;
        }
        rs.close();
        statement.close();
        return null;
    }

    public java.util.Map<UUID, String> getAllLinkedAccounts() throws SQLException {
        java.util.Map<UUID, String> result = new java.util.HashMap<>();
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT UUID, USER_ID FROM MINECRAFT_ACCOUNTS WHERE USER_ID IS NOT NULL;");
        while (rs.next()) {
            UUID uuid = UUID.fromString(rs.getString("UUID"));
            String twitchUserId = rs.getString("USER_ID");
            result.put(uuid, twitchUserId);
        }
        rs.close();
        statement.close();
        return result;
    }


    public void createMinecraftPlayer(UUID uuid, String name) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO MINECRAFT_ACCOUNTS (UUID, USER_ID, LAST_KNOWN_NAME) " +
                        "VALUES (?, NULL, ?) " +
                        "ON CONFLICT(UUID) DO UPDATE SET LAST_KNOWN_NAME = excluded.LAST_KNOWN_NAME;"
        );
        statement.setString(1, uuid.toString());
        statement.setString(2, name);
        statement.executeUpdate();
        statement.close();
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            logger.error("Error while closing database connection", e);
        }
    }

    public String getLastKnownName(UUID uuid) throws SQLException {
        ResultSet resultSet = executeQuery("SELECT LAST_KNOWN_NAME FROM MINECRAFT_ACCOUNTS WHERE UUID = ?", uuid.toString());
        if (resultSet.next()) {
            return resultSet.getString("LAST_KNOWN_NAME");
        }
        resultSet.close();
        return null;
    }


    public UUID getUUIDByLastKnownName(String username) throws SQLException {
        ResultSet resultSet = executeQuery("SELECT UUID FROM MINECRAFT_ACCOUNTS WHERE LAST_KNOWN_NAME = ?", username);
        if (resultSet.next()) {
            return UUID.fromString(resultSet.getString("UUID"));
        }
        resultSet.close();
        return null;
    }

    public void linkAccounts(UUID minecraftPlayer, String twitchUserID) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("UPDATE MINECRAFT_ACCOUNTS SET USER_ID = ? WHERE UUID = ?;");
        statement.setString(1, twitchUserID);
        statement.setString(2, minecraftPlayer.toString());
        statement.executeUpdate();
        statement.close();
    }

    public void unLinkAccounts(String twitchUserID) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("UPDATE MINECRAFT_ACCOUNTS SET USER_ID = NULL WHERE USER_ID = ?;");
        statement.setString(1, twitchUserID);
        statement.executeUpdate();
        statement.close();
    }

    public void unLinkAccount(UUID minecraftPlayer) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("UPDATE MINECRAFT_ACCOUNTS SET USER_ID = NULL WHERE UUID = ?;");
        statement.setString(1, minecraftPlayer.toString());
        statement.executeUpdate();
        statement.close();
    }

    public void deleteTwitchUser(String twitchUserId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM TWITCH_USERS WHERE USER_ID = ?;"
        );
        statement.setString(1, twitchUserId);
        statement.executeUpdate();
        statement.close();
    }
}