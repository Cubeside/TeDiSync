package de.fanta.tedisync.teamspeak.database;

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

public class TeamSpeakDatabase {

    private final SQLConfig config;
    private final SQLConnection connection;
    private final String insertUserQuery;
    private final String getUserByTSIDQuery;
    private final String getUserByTSIDANDUUIDQuery;
    private final String getUserByUUISQuery;
    private final String deleteUserbyTSIDQuery;

    public TeamSpeakDatabase(SQLConfig config) {
        this.config = config;

        try {
            this.connection = new MySQLConnection(config);
            createTablesIfNotExist();
        } catch (SQLException ex) {
            throw new RuntimeException("Could not initialize database", ex);
        }

        insertUserQuery = "INSERT INTO " + config.getTablePrefix() + "_user" + " (uuid, tsID, lastMcName) VALUE (?, ?, ?)";
        getUserByTSIDQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE tsID = ?";
        getUserByTSIDANDUUIDQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE tsID = ? AND uuid = ?";
        getUserByUUISQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE uuid = ?";
        deleteUserbyTSIDQuery = "DELETE FROM " + config.getTablePrefix() + "_user" + " WHERE `tsID` = ?";
    }

    private void createTablesIfNotExist() throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            Statement smt = connection.createStatement();
            smt.executeUpdate("CREATE TABLE IF NOT EXISTS " + config.getTablePrefix() + "_user" + " (" +
                    "`uuid` char(36)," +
                    "`tsID` varchar(64)," +
                    "`lastMcName` varchar(16)," +
                    "PRIMARY KEY (`tsID`)," +
                    "INDEX (`uuid`)" +
                    ")");
            smt.close();
            return null;
        });
    }

    public void insertUser(ProxiedPlayer player, String tsID) throws SQLException {
        insertUser(player.getUniqueId(), player.getName(), tsID);
    }

    public void insertUser(UUID uuid, String playerName, String tsID) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(insertUserQuery);
            smt.setString(1, uuid.toString());
            smt.setString(2, tsID);
            smt.setString(3, playerName);
            smt.executeUpdate();
            return null;
        });
    }


    public TeamSpeakUserInfo getUserByTSID(String tsID) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(getUserByTSIDQuery);
            statement.setString(1, tsID);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String id = rs.getString("tsID");
                String latestName = rs.getString("lastMcName");
                return new TeamSpeakUserInfo(uuid, id, latestName);
            }
            return null;
        });
    }

    public TeamSpeakUserInfo getUserByTSIDANDUUID(String tsID, UUID uuid) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(getUserByTSIDANDUUIDQuery);
            statement.setString(1, tsID);
            statement.setString(2, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                UUID tempUUID = UUID.fromString(rs.getString("uuid"));
                String id = rs.getString("tsID");
                String latestName = rs.getString("lastMcName");
                return new TeamSpeakUserInfo(tempUUID, id, latestName);
            }
            return null;
        });
    }

    public Collection<TeamSpeakUserInfo> getUsersByUUIS(UUID uuid) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            Collection<TeamSpeakUserInfo> teamSpeakUserInfos = new ArrayList<>();
            PreparedStatement statement = sqlConnection.getOrCreateStatement(getUserByUUISQuery);
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("uuid"));
                String id = rs.getString("tsID");
                String latestName = rs.getString("lastMcName");
                teamSpeakUserInfos.add(new TeamSpeakUserInfo(playerUUID, id, latestName));
            }
            return teamSpeakUserInfos;
        });
    }

    public void deleteAccountByTSID(String tsID) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(deleteUserbyTSIDQuery);

            smt.setString(1, tsID);
            smt.executeUpdate();
            return null;
        });
    }

    public void disconnect() {
        this.connection.disconnect();
    }
}
