package de.fanta.tedisync.teamspeak;

import de.iani.cubesideutils.sql.MySQLConnection;
import de.iani.cubesideutils.sql.SQLConfig;
import de.iani.cubesideutils.sql.SQLConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class TeamSpeakDatabase {

    private final SQLConfig config;
    private final SQLConnection connection;

    private final String insertUserQuery;
    private final String getUserByTSIDQuery;
    private final String getUserByTSIDANDUUIDQuery;
    private final String getUserByUUIDQuery;
    private final String deleteUserbyTSIDQuery;
    private final String updateNameQuery;

    private final String addActiveTimeQuery;
    private final String getActiveTimeQuery;
    private final String getActiveTimesQuery;
    private final String clearActiveTimeQuery;
    private final String clearActiveTimesQuery;

    public TeamSpeakDatabase(SQLConfig config) {
        this.config = config;

        try {
            this.connection = new MySQLConnection(config);
            createTablesIfNotExist();
        } catch (SQLException ex) {
            throw new RuntimeException("Could not initialize TeamSpeak database", ex);
        }

        this.insertUserQuery =
                "INSERT INTO " + config.getTablePrefix() + "_user" + " (uuid, tsID, lastMcName) VALUE (?, ?, ?)";
        this.getUserByTSIDQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE tsID = ?";
        this.getUserByTSIDANDUUIDQuery =
                "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE tsID = ? AND uuid = ?";
        this.getUserByUUIDQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE uuid = ?";
        this.deleteUserbyTSIDQuery = "DELETE FROM " + config.getTablePrefix() + "_user" + " WHERE `tsID` = ?";
        this.updateNameQuery =
                "UPDATE " + config.getTablePrefix() + "_user" + " SET `lastMcName` = ? WHERE tsID = ? AND uuid = ?";

        this.addActiveTimeQuery = "INSERT INTO `" + config.getTablePrefix() + "_activeTimes`"
                + " (uuid, time) VALUES (?, ?) ON DUPLICATE KEY UPDATE time = time + ?";
        this.getActiveTimeQuery = "SELECT time FROM `" + config.getTablePrefix() + "_activeTimes`" + " WHERE uuid = ?";
        this.getActiveTimesQuery = "SELECT uuid, time FROM `" + config.getTablePrefix() + "_activeTimes`";
        this.clearActiveTimeQuery =
                "DELETE FROM `" + this.config.getTablePrefix() + "_activeTimes`" + " WHERE uuid = ?";
        this.clearActiveTimesQuery = "DELETE FROM `" + this.config.getTablePrefix() + "_activeTimes`" + " WHERE 1";
    }

    private void createTablesIfNotExist() throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            Statement smt = connection.createStatement();
            smt.executeUpdate("CREATE TABLE IF NOT EXISTS " + this.config.getTablePrefix() + "_user" + " ("
                    + "`uuid` char(36)," + "`tsID` varchar(64)," + "`lastMcName` varchar(16)," + "PRIMARY KEY (`tsID`),"
                    + "INDEX (`uuid`)" + ")");
            smt.close();
            return null;
        });
    }

    public void insertUser(ProxiedPlayer player, String tsID) throws SQLException {
        insertUser(player.getUniqueId(), player.getName(), tsID);
    }

    public void insertUser(UUID uuid, String playerName, String tsID) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(this.insertUserQuery);
            smt.setString(1, uuid.toString());
            smt.setString(2, tsID);
            smt.setString(3, playerName);
            smt.executeUpdate();
            return null;
        });
    }


    public TeamSpeakUserInfo getUserByTSID(String tsID) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(this.getUserByTSIDQuery);
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
            PreparedStatement statement = sqlConnection.getOrCreateStatement(this.getUserByTSIDANDUUIDQuery);
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

    public Collection<TeamSpeakUserInfo> getUsersByUUID(UUID uuid) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            Collection<TeamSpeakUserInfo> teamSpeakUserInfos = new ArrayList<>();
            PreparedStatement statement = sqlConnection.getOrCreateStatement(this.getUserByUUIDQuery);
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
            PreparedStatement smt = sqlConnection.getOrCreateStatement(this.deleteUserbyTSIDQuery);

            smt.setString(1, tsID);
            smt.executeUpdate();
            return null;
        });
    }

    public void updateMcName(UUID uuid, String tsID, String newName) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(this.updateNameQuery);

            smt.setString(1, newName);
            smt.setString(2, tsID);
            smt.setString(3, uuid.toString());
            smt.executeUpdate();
            return null;
        });
    }

    public void addActiveTime(UUID uuid, long time) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(this.addActiveTimeQuery);

            smt.setString(1, uuid.toString());
            smt.setLong(2, time);
            smt.setLong(3, time);
            smt.executeUpdate();
            return null;
        });
    }

    public long getActiveTime(UUID uuid) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(this.getActiveTimeQuery);
            smt.setString(1, uuid.toString());

            ResultSet rs = smt.executeQuery();
            return rs.first() ? rs.getLong(1) : 0;
        });
    }

    public Map<UUID, Long> getActiveTimes() throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(this.getActiveTimesQuery);
            ResultSet rs = smt.executeQuery();
            Map<UUID, Long> result = new LinkedHashMap<>();
            while (!rs.next()) {
                result.put(UUID.fromString(rs.getString(1)), rs.getLong(2));
            }
            return result;
        });
    }

    public void clearActiveTime(UUID uuid) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(this.clearActiveTimeQuery);

            smt.setString(1, uuid.toString());
            smt.executeUpdate();
            return null;
        });
    }

    public void clearActiveTimes() throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(this.clearActiveTimesQuery);

            smt.executeUpdate();
            return null;
        });
    }

    public void disconnect() {
        this.connection.disconnect();
    }
}
