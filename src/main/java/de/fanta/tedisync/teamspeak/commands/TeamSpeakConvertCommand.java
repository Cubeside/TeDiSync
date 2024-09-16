package de.fanta.tedisync.teamspeak.commands;

import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.teamspeak.TeamSpeakUserInfo;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.bungee.sql.SQLConfigBungee;
import de.iani.cubesideutils.commands.ArgsParser;
import de.iani.cubesideutils.sql.MySQLConnection;
import de.iani.cubesideutils.sql.SQLConfig;
import de.iani.cubesideutils.sql.SQLConnection;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class TeamSpeakConvertCommand extends SubCommand {
    private final TeamSpeakBot teamSpeakBot;

    public TeamSpeakConvertCommand(TeamSpeakBot teamSpeakBot) {
        this.teamSpeakBot = teamSpeakBot;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        SQLConfig sqlConfig = new SQLConfigBungee(teamSpeakBot.getPlugin().getConfig().getSection("teamspeak.oldDatabase"));

        SQLConnection connection;
        try {
            connection = new MySQLConnection(sqlConfig);
        } catch (SQLException ex) {
            throw new RuntimeException("Could not initialize database", ex);
        }

        String selectOldUserData = "SELECT * FROM " + sqlConfig.getTablePrefix();

        try {
            Collection<TeamSpeakUserInfo> teamSpeakUserInfos = connection.runCommands((connection2, sqlConnection) -> {
                Collection<TeamSpeakUserInfo> tempTeamSpeakUserInfos = new ArrayList<>();
                PreparedStatement statement = sqlConnection.getOrCreateStatement(selectOldUserData);
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    UUID playerUUID = UUID.fromString(rs.getString("uuid"));
                    String id = rs.getString("id");
                    tempTeamSpeakUserInfos.add(new TeamSpeakUserInfo(playerUUID, id, null));
                }
                return tempTeamSpeakUserInfos;
            });

            teamSpeakUserInfos.forEach(teamSpeakUserInfo -> {
                try {
                    if (teamSpeakBot.getDatabase().getUserByTSIDANDUUID(teamSpeakUserInfo.tsID(), teamSpeakUserInfo.uuid()) == null && teamSpeakBot.getDatabase().getUserByTSID(teamSpeakUserInfo.tsID()) == null) {
                        teamSpeakBot.getDatabase().insertUser(teamSpeakUserInfo.uuid(), null, teamSpeakUserInfo.tsID());
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            ChatUtil.sendNormalMessage(sender, "Datenbank convertiert!");
            connection.disconnect();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }


    @Override
    public String getRequiredPermission() {
        return "tedisync.teamspeak.convert";
    }
}
