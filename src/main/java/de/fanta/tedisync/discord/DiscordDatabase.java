package de.fanta.tedisync.discord;

import de.iani.cubesideutils.sql.MySQLConnection;
import de.iani.cubesideutils.sql.SQLConfig;
import de.iani.cubesideutils.sql.SQLConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class DiscordDatabase {

    private final SQLConfig config;
    private final SQLConnection connection;
    private final String insertUserQuery;
    private final String getUserByDCIDQuery;
    private final String getUserByDCIDANDUUIDQuery;
    private final String getUserByUUIDQuery;
    private final String deleteUserbyDCIDQuery;

    public DiscordDatabase(SQLConfig config) {
        this.config = config;

        try {
            this.connection = new MySQLConnection(config);
            createTablesIfNotExist();
        } catch (SQLException ex) {
            throw new RuntimeException("Could not initialize Discord database", ex);
        }

        insertUserQuery = "INSERT INTO " + config.getTablePrefix() + "_user" + " (uuid, dcID) VALUE (?, ?)";
        getUserByDCIDQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE dcID = ?";
        getUserByDCIDANDUUIDQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE dcID = ? AND uuid = ?";
        getUserByUUIDQuery = "SELECT * FROM " + config.getTablePrefix() + "_user" + " WHERE uuid = ?";
        deleteUserbyDCIDQuery = "DELETE FROM " + config.getTablePrefix() + "_user" + " WHERE `dcID` = ?";
    }

    private void createTablesIfNotExist() throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            Statement smt = connection.createStatement();
            smt.executeUpdate("CREATE TABLE IF NOT EXISTS " + config.getTablePrefix() + "_user" + " (" +
                    "`uuid` char(36)," +
                    "`dcID` BIGINT(64)," +
                    "PRIMARY KEY (`uuid`)" +
                    ")");
            smt.close();
            return null;
        });
    }

    public void insertUser(UUID uuid, long dcID) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(insertUserQuery);
            smt.setString(1, uuid.toString());
            smt.setLong(2, dcID);
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
                String id = rs.getString("dcID");
                return new DiscordUserInfo(uuid, id);
            }
            return null;
        });
    }

    public DiscordUserInfo getUserByDCIDANDUUID(long dcID, UUID uuid) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(getUserByDCIDANDUUIDQuery);
            statement.setLong(1, dcID);
            statement.setString(2, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                UUID tempUUID = UUID.fromString(rs.getString("uuid"));
                String id = rs.getString("dcID");
                return new DiscordUserInfo(tempUUID, id);
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
                String id = rs.getString("dcID");
                return new DiscordUserInfo(playerUUID, id);
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
