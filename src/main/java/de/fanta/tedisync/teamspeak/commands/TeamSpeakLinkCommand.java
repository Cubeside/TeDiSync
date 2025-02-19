package de.fanta.tedisync.teamspeak.commands;

import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.teamspeak.TeamSpeakUserInfo;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class TeamSpeakLinkCommand extends SubCommand {
    private final TeamSpeakBot teamSpeakBot;

    public TeamSpeakLinkCommand(TeamSpeakBot teamSpeakBot) {
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

        String tsID = args.getNext();

        if (!args.hasNext()) {
            ChatUtil.sendErrorMessage(player, "Du musst einen Spieler eingeben!");
            return true;
        }

        String name = args.getNext();
        try {
            teamSpeakBot.getAsyncApi().getClientByUId(tsID).onSuccess(clientInfo -> {
                if (clientInfo == null) {
                    ChatUtil.sendErrorMessage(player, "TeamSpeak ID nicht gefunden!");
                    return;
                }

                try {
                    TeamSpeakUserInfo userInfo = teamSpeakBot.getDatabase().getUserByTSID(tsID);
                    if (userInfo != null) {
                        ChatUtil.sendErrorMessage(player, "Dieser TeamSpeak Account ist bereits verbunden!");
                        return;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                ProxiedPlayer proxiedPlayer = teamSpeakBot.getPlugin().getProxy().getPlayer(name);
                if (proxiedPlayer != null && proxiedPlayer.isConnected()) {
                    teamSpeakBot.sendRequestToPlayer(proxiedPlayer, clientInfo);
                    ChatUtil.sendNormalMessage(player, "Eine Anfrage wurde an " + proxiedPlayer.getName() + " gesendet!");
                } else {
                    ChatUtil.sendErrorMessage(player, "Spieler " + name + " ist nicht online!");
                }
            });
        } catch (TS3CommandFailedException e) {
            if (e.getError().getId() == 1540) {
                ChatUtil.sendErrorMessage(player, "Ungültige ID");
            } else {
                teamSpeakBot.getPlugin().getLogger().log(Level.SEVERE, "Error by loading client!", e);
            }
            return true;
        }

        return true;
    }

    @Override
    public String getRequiredPermission() {
        return "teamspeak.link.other";
    }

    @Override
    public Collection<String> onTabComplete(CommandSender sender, Command command, String alias, ArgsParser args) {
        int i = args.remaining();
        if (i == 1) {
            return Collections.emptyList();
        }

        if (i == 2) {
            ArrayList<String> li = new ArrayList<>();
            for (ProxiedPlayer player : teamSpeakBot.getPlugin().getProxy().getPlayers()) {
                li.add(player.getName());
            }
            return li;
        }
        return new ArrayList<>();
    }
}
