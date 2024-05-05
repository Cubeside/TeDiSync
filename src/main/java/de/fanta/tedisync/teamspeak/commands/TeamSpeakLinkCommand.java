package de.fanta.tedisync.teamspeak.commands;

import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.protocol.packet.Chat;

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

        ClientInfo clientInfo = teamSpeakBot.getAsyncApi().getClientByUId(tsID).getUninterruptibly();
        if (clientInfo == null) {
            ChatUtil.sendErrorMessage(player, "TeamSpeak ID nicht gefunden!");
            return true;
        }

        ProxiedPlayer proxiedPlayer = teamSpeakBot.plugin().getProxy().getPlayer(name);
        if (proxiedPlayer != null && proxiedPlayer.isConnected()) {
            teamSpeakBot.sendRequestToPlayer(proxiedPlayer, clientInfo);
            ChatUtil.sendNormalMessage(player, "Eine anfrage wurde an " + proxiedPlayer.getName() + " gesendet!");
        } else {
            ChatUtil.sendErrorMessage(player, "Spieler " + name + " ist nicht Online!");
        }

        return true;
    }

    @Override
    public String getRequiredPermission() {
        return "teamspeak.link.other";
    }
}
