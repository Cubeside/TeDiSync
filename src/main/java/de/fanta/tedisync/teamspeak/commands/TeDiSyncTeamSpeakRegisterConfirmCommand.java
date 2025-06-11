package de.fanta.tedisync.teamspeak.commands;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.teamspeak.TeamSpeakUserInfo;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.sql.SQLException;
import java.util.logging.Level;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class TeDiSyncTeamSpeakRegisterConfirmCommand extends SubCommand {

    private final boolean confirm;
    private final TeamSpeakBot teamSpeakBot;

    public TeDiSyncTeamSpeakRegisterConfirmCommand(boolean confirm, TeamSpeakBot teamSpeakBot) {
        this.confirm = confirm;
        this.teamSpeakBot = teamSpeakBot;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString,
            ArgsParser args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            ChatUtil.sendErrorMessage(sender, "You are not a Player :>");
            return true;
        }

        String tsID = this.teamSpeakBot.getRequests().get(player.getUniqueId());

        if (tsID == null) {
            ChatUtil.sendErrorMessage(player, "Du hast momentan keine Anfrage.");
            return true;
        }

        ClientInfo client;
        try {
            client = this.teamSpeakBot.getAsyncApi().getClientByUId(tsID).get();
        } catch (InterruptedException e) {
            ChatUtil.sendErrorMessage(player, "TeamSpeak Client nicht gefunden.");
            return true;
        }

        try {
            TeamSpeakUserInfo userInfo = this.teamSpeakBot.getDatabase().getUserByTSID(tsID);
            if (userInfo != null) {
                ChatUtil.sendErrorMessage(player, "Dieser TeamSpeak Account ist bereits verbunden!");
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (this.confirm) {
            if (this.teamSpeakBot.getRequests().containsKey(player.getUniqueId())) {
                try {
                    this.teamSpeakBot.getDatabase().insertUser(player, tsID);
                    this.teamSpeakBot.removeFromUserInfoCache(tsID);
                    this.teamSpeakBot.getRequests().remove(player.getUniqueId());
                    this.teamSpeakBot.updateTeamSpeakGroup(player.getUniqueId(), client, null);
                    this.teamSpeakBot.updateTSDescription(tsID, player);
                    ChatUtil.sendNormalMessage(player, "Die Anfrage wurde angenommen.");
                    this.teamSpeakBot.getAsyncApi().sendPrivateMessage(client.getId(),
                            "Die Anfrage zum Verbinden wurde von " + player.getName() + " angenommen.");
                } catch (SQLException e) {
                    this.teamSpeakBot.getPlugin().getLogger().log(Level.SEVERE,
                            "Daten konnten nicht gespeichert werden.", e);
                    ChatUtil.sendErrorMessage(player, "Daten konnten nicht gespeichert werden.");
                }
            } else {
                ChatUtil.sendErrorMessage(player, "Du hast momentan keine Anfrage.");
            }
        } else {
            if (this.teamSpeakBot.getRequests().containsKey(player.getUniqueId())) {
                this.teamSpeakBot.getRequests().remove(player.getUniqueId());
                ChatUtil.sendNormalMessage(player, "Die Anfrage wurde abgelehnt.");
                this.teamSpeakBot.getAsyncApi().sendPrivateMessage(client.getId(),
                        "Die Anfrage zum Verbinden wurde von " + player.getName() + " abgelehnt.");
            } else {
                ChatUtil.sendErrorMessage(player, "Du hast momentan keine Anfrage.");
            }
        }
        return true;
    }
}

