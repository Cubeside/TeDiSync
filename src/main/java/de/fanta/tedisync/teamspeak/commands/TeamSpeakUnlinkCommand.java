package de.fanta.tedisync.teamspeak.commands;

import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.teamspeak.TeamSpeakUserInfo;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.SQLException;

public class TeamSpeakUnlinkCommand extends SubCommand {

    private final String DELETE_OTHER_ACCOUNTS_PERMISSION = "teamspeak.admin.deleteotheraccounts";
    private final TeamSpeakBot teamSpeakBot;

    public TeamSpeakUnlinkCommand(TeamSpeakBot teamSpeakBot) {
        this.teamSpeakBot = teamSpeakBot;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            ChatUtil.sendErrorMessage(sender, "You are nor a Player :>");
            return true;
        }

        if (!args.hasNext()) {
            ChatUtil.sendErrorMessage(player, "Du musst eine TeamSpeak ID eingeben!");
            return true;
        }

        try {
            TeamSpeakUserInfo teamSpeakUserInfo = teamSpeakBot.getDatabase().getUserByTSID(args.getNext());
            if (teamSpeakUserInfo == null) {
                ChatUtil.sendErrorMessage(player, "TeamSpeak ID nicht gefunden!");
                return true;
            }

            if (teamSpeakUserInfo.uuid().equals(player.getUniqueId())) {
                teamSpeakBot.getDatabase().deleteAccountByTSID(teamSpeakUserInfo.tsID());
                teamSpeakBot.removeAllTeamSpeakGroups(teamSpeakUserInfo.tsID());
                ChatUtil.sendNormalMessage(player, "Du hast den Account " + teamSpeakUserInfo.tsID() + " entfernt!");
            } else if (player.hasPermission(DELETE_OTHER_ACCOUNTS_PERMISSION)) {
                teamSpeakBot.getDatabase().deleteAccountByTSID(teamSpeakUserInfo.tsID());
                ProxiedPlayer proxiedPlayer = teamSpeakBot.getPlugin().getProxy().getPlayer(teamSpeakUserInfo.uuid());
                teamSpeakBot.removeAllTeamSpeakGroups(teamSpeakUserInfo.tsID());
                ChatUtil.sendNormalMessage(player, "Du hast den Account " + teamSpeakUserInfo.tsID() + " von " + (proxiedPlayer != null ? proxiedPlayer.getName() : teamSpeakUserInfo.uuid()) + " entfernt!");
            } else {
                ChatUtil.sendErrorMessage(player, "Du kannst nur eigene Accounts entfernen!");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
