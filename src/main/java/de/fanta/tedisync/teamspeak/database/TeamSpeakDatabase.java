package de.fanta.tedisync.teamspeak.database;

import de.fanta.tedisync.TeDiSync;
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
    private final String getUserByUUISQuery;

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
        getUserByUUISQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE uuid = ?";
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

    public void insertSecret(ProxiedPlayer player, String tsID) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(insertUserQuery);
            smt.setString(1, player.getUniqueId().toString());
            smt.setString(2, tsID);
            smt.setString(3, player.getName());
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
}
