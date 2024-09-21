package de.fanta.tedisync.discord;

import de.iani.cubesideutils.sql.MySQLConnection;
import de.iani.cubesideutils.sql.SQLConfig;
import de.iani.cubesideutils.sql.SQLConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class DiscordDatabase {

    private final SQLConfig config;
    private final SQLConnection connection;
    private final String insertUserQuery;
    private final String getUserByDCIDQuery;
    private final String getUserByUUIDQuery;
    private final String getNotificationUser;
    private final String updateNotificationUser;
    private final String deleteUserbyDCIDQuery;

    public DiscordDatabase(SQLConfig config) {
        this.config = config;

        try {
            this.connection = new MySQLConnection(config);
            createTablesIfNotExist();
        } catch (SQLException ex) {
            throw new RuntimeException("Could not initialize Discord database", ex);
        }

        insertUserQuery = "INSERT INTO " + config.getTablePrefix() + "_user" + " (uuid, dcID, notification) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE dcID = VALUES(dcID), notification = VALUES(notification)";
        getUserByDCIDQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE dcID = ?";
        getUserByUUIDQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE uuid = ?";
        getNotificationUser = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE notification = 1";
        updateNotificationUser = "UPDATE " + config.getTablePrefix() + "_user" + " SET notification = ? WHERE uuid = ?";
        deleteUserbyDCIDQuery = "DELETE FROM " + config.getTablePrefix() + "_user" + " WHERE `dcID` = ?";
    }

    private void createTablesIfNotExist() throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            Statement smt = connection.createStatement();
            smt.executeUpdate("CREATE TABLE IF NOT EXISTS " + config.getTablePrefix() + "_user" + " (" +
                    "`uuid` char(36)," +
                    "`dcID` BIGINT(64)," +
                    "`notification` BOOLEAN," +
                    "PRIMARY KEY (`uuid`)" +
                    ")");
            smt.close();
            return null;
        });
    }

    public void insertUser(UUID uuid, long dcID, boolean notification) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(insertUserQuery);
            smt.setString(1, uuid.toString());
            smt.setLong(2, dcID);
            smt.setBoolean(3, notification);
            smt.executeUpdate();
            return null;
        });
    }

    public Collection<DiscordUserInfo> getGivewayNotificationUser() throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            Collection<DiscordUserInfo> discordUserInfos = new ArrayList<>();
            PreparedStatement statement = sqlConnection.getOrCreateStatement(getNotificationUser);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("uuid"));
                long id = rs.getLong("dcID");
                boolean latestName = rs.getBoolean("notification");
                discordUserInfos.add(new DiscordUserInfo(playerUUID, id, latestName));
            }
            return discordUserInfos;
        });
    }

    public void updateNotificationUser(UUID uuid, boolean notification) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(updateNotificationUser);
            smt.setBoolean(1, notification);
            smt.setString(2, uuid.toString());
            smt.executeUpdate();
            return null;
        });
    }

    public DiscordUserInfo getUserByDCID(long dcID) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(getUserByDCIDQuery);
            statement.setLong(1, dcID);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long id = rs.getLong("dcID");
                boolean notification = rs.getBoolean("notification");
                return new DiscordUserInfo(uuid, id, notification);
            }
            return null;
        });
    }

    public DiscordUserInfo getUsersByUUID(UUID uuid) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(getUserByUUIDQuery);
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("uuid"));
                long id = rs.getLong("dcID");
                boolean notification = rs.getBoolean("notification");
                return new DiscordUserInfo(playerUUID, id, notification);
            }
            return null;
        });
    }

    public void deleteAccountByDCID(long dcID) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(deleteUserbyDCIDQuery);
            smt.setLong(1, dcID);
            smt.executeUpdate();
            return null;
        });
    }

    public void disconnect() {
        this.connection.disconnect();
    }
}
