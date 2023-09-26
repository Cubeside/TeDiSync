package de.fanta.tedisync.data;

import de.fanta.tedisync.TeDiSync;
import de.iani.cubesideutils.sql.MySQLConnection;
import de.iani.cubesideutils.sql.SQLConfig;
import de.iani.cubesideutils.sql.SQLConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class Database {

    private final TeDiSync plugin;
    private final SQLConnection connection;
    private final SQLConfig config;

    private final String insertDataQuery;
    private final String insertDiscordDataQuery;
    private final String insertTeamspeakDataQuery;
    private final String getDiscordDataQuery;
    private final String getTeamspeakDataQuery;

    public Database(SQLConfig config, TeDiSync plugin) {
        this.config = config;
        this.plugin = plugin;

        try {
            this.connection = new MySQLConnection(config.getHost(), config.getDatabase(), config.getUser(), config.getPassword());

            createTablesIfNotExist();
        } catch (SQLException ex) {
            throw new RuntimeException("Could not initialize database", ex);
        }
        insertDataQuery = "INSERT INTO " + config.getTablePrefix() + "_data" + " (uuid, discord, teamspeak) VALUE (?, ?, ?) ON DUPLICATE KEY UPDATE uuid = ?, discord = ?, teamspeak = ?";
        insertDiscordDataQuery = "INSERT INTO " + config.getTablePrefix() + "_data" + " (uuid, discord) VALUE (?, ?) ON DUPLICATE KEY UPDATE uuid = ?, discord = ?";
        insertTeamspeakDataQuery = "INSERT INTO " + config.getTablePrefix() + "_data" + " (uuid, teamspeak) VALUE (?, ?) ON DUPLICATE KEY UPDATE uuid = ?, teamspeak = ?";
        getDiscordDataQuery = "SELECT `discord` FROM " + config.getTablePrefix() + "_data WHERE `uuid` = ?";
        getTeamspeakDataQuery = "SELECT `teamspeak` FROM " + config.getTablePrefix() + "_data WHERE `uuid` = ?";
    }

    private void createTablesIfNotExist() throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            Statement smt = connection.createStatement();
            smt.executeUpdate("CREATE TABLE IF NOT EXISTS " + config.getTablePrefix() + "_data" + " (" +
                    "`uuid` char(36)," +
                    "`discord` BIGINT NULL," +
                    "`teamspeak` INT NULL," +
                    "PRIMARY KEY (`uuid`, `discord`, `teamspeak`)" +
                    ")");
            smt.close();
            return null;
        });
    }

    public void insertPlayerData(UUID uuid, long discordID, int teamspeakID) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(insertDataQuery);

            smt.setString(1, uuid.toString());
            smt.setLong(2, discordID);
            smt.setInt(3, teamspeakID);

            smt.setString(4, uuid.toString());
            smt.setLong(5, discordID);
            smt.setInt(6, teamspeakID);
            smt.executeUpdate();
            return null;
        });
    }

    public void insertDiscordPlayerData(UUID uuid, long discordID) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(insertDiscordDataQuery);

            smt.setString(1, uuid.toString());
            smt.setLong(2, discordID);

            smt.setString(3, uuid.toString());
            smt.setLong(4, discordID);
            smt.executeUpdate();
            return null;
        });
    }

    public void insertTeamSpeakPlayerData(UUID uuid, int teamspeakID) throws SQLException {
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(insertTeamspeakDataQuery);

            smt.setString(1, uuid.toString());
            smt.setLong(2, teamspeakID);

            smt.setString(3, uuid.toString());
            smt.setLong(4, teamspeakID);
            smt.executeUpdate();
            return null;
        });
    }

    public Long getDiscordData(UUID uuid) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(getDiscordDataQuery);
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                long id = rs.getLong("discord");
                if (id != 0) {
                    return id;
                }
            }
            return null;
        });
    }

    public Integer getTeamspeakData(UUID uuid) throws SQLException {
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(getTeamspeakDataQuery);
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("teamspeak");
                if (id != 0) {
                    return id;
                }
            }
            return null;
        });
    }

}
