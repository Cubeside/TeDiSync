package de.fanta.tedisync.discord;

import de.fanta.tedisync.teamspeak.TeamSpeakUserInfo;
import de.iani.cubesideutils.sql.MySQLConnection;
import de.iani.cubesideutils.sql.SQLConfig;
import de.iani.cubesideutils.sql.SQLConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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
    private final String getUserByDCIDANDUUIDQuery;
    private final String getUserByUUIDQuery;
    private final String deleteUserbyDCIDQuery;
    private final String updateNameQuery;

    public DiscordDatabase(SQLConfig config) {
        this.config = config;

        try {
            this.connection = new MySQLConnection(config);
            createTablesIfNotExist();
        } catch (SQLException ex) {
            throw new RuntimeException("Could not initialize Discord database", ex);
        }

        insertUserQuery = "INSERT INTO " + config.getTablePrefix() + "_user" + " (uuid, dcID, lastMcName) VALUE (?, ?, ?)";
        getUserByDCIDQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE dcID = ?";
        getUserByDCIDANDUUIDQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE dcID = ? AND uuid = ?";
        getUserByUUIDQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE uuid = ?";
        deleteUserbyDCIDQuery = "DELETE FROM " + config.getTablePrefix() + "_user" + " WHERE `dcID` = ?";
        updateNameQuery = "UPDATE " + config.getTablePrefix() + "_user" + " SET `lastMcName` = ? WHERE dcID = ? AND uuid = ?";
    }

    private void createTablesIfNotExist() throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            Statement smt = connection.createStatement();
            smt.executeUpdate("CREATE TABLE IF NOT EXISTS " + config.getTablePrefix() + "_user" + " (" +
                    "`uuid` char(36)," +
                    "`dcID` BIGINT(64)," +
                    "`lastMcName` varchar(16)," +
                    "PRIMARY KEY (`dcID`)," +
                    "INDEX (`uuid`)" +
                    ")");
            smt.close();
            return null;
        });
    }

    public void insertUser(ProxiedPlayer player, long dcID) throws SQLException {
        insertUser(player.getUniqueId(), player.getName(), dcID);
    }

    public void insertUser(UUID uuid, String playerName, long dcID) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(insertUserQuery);
            smt.setString(1, uuid.toString());
            smt.setLong(2, dcID);
            smt.setString(3, playerName);
            smt.executeUpdate();
            return null;
        });
    }


    public TeamSpeakUserInfo getUserByDCID(long dcID) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(getUserByDCIDQuery);
            statement.setLong(1, dcID);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String id = rs.getString("dcID");
                String latestName = rs.getString("lastMcName");
                return new TeamSpeakUserInfo(uuid, id, latestName);
            }
            return null;
        });
    }

    public TeamSpeakUserInfo getUserByDCIDANDUUID(long dcID, UUID uuid) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(getUserByDCIDANDUUIDQuery);
            statement.setLong(1, dcID);
            statement.setString(2, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                UUID tempUUID = UUID.fromString(rs.getString("uuid"));
                String id = rs.getString("dcID");
                String latestName = rs.getString("lastMcName");
                return new TeamSpeakUserInfo(tempUUID, id, latestName);
            }
            return null;
        });
    }

    public Collection<TeamSpeakUserInfo> getUsersByUUID(UUID uuid) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            Collection<TeamSpeakUserInfo> teamSpeakUserInfos = new ArrayList<>();
            PreparedStatement statement = sqlConnection.getOrCreateStatement(getUserByUUIDQuery);
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("uuid"));
                String id = rs.getString("dcID");
                String latestName = rs.getString("lastMcName");
                teamSpeakUserInfos.add(new TeamSpeakUserInfo(playerUUID, id, latestName));
            }
            return teamSpeakUserInfos;
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

    public void updateMcName(UUID uuid, long dcID, String newName) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(updateNameQuery);

            smt.setString(1, newName);
            smt.setLong(2, dcID);
            smt.setString(3, uuid.toString());
            smt.executeUpdate();
            return null;
        });
    }

    public void disconnect() {
        this.connection.disconnect();
    }
}
