package de.fanta.tedisync.teamspeak.commands;

import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.Pair;
import de.iani.cubesideutils.StringUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.UUID;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;


public class TeamSpeakShowLotteryTicketsCommand extends SubCommand {

    public static final String SEE_OTHER_TIMES_PERMISSION = "tedisync.teamspeak.lottery.seeother";

    private final TeamSpeakBot teamSpeakBot;

    public TeamSpeakShowLotteryTicketsCommand(TeamSpeakBot teamSpeakBot) {
        this.teamSpeakBot = teamSpeakBot;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString,
            ArgsParser args) {
        String userString;
        UUID playerId;
        if (!(sender instanceof ProxiedPlayer player) || args.hasNext()) {
            if (!sender.hasPermission(SEE_OTHER_TIMES_PERMISSION)) {
                ChatUtil.sendErrorMessage(sender,
                        "Du hast keine Berechtigung, die Aktivität anderer Spieler einzusehen.");
                return true;
            }

            userString = args.getNext(null);
            if (userString == null) {
                ChatUtil.sendWarningMessage(sender, "Bitte gib den Spieler an.");
                return true;
            }

            try {
                playerId = UUID.fromString(userString);
            } catch (IllegalArgumentException e) {
                ProxiedPlayer player = this.teamSpeakBot.getPlugin().getProxy().getPlayer(userString);
                if (player != null) {
                    playerId = player.getUniqueId();
                    userString = player.getName();
                } else {
                    ChatUtil.sendWarningMessage(sender, "Spieler \"" + userString
                            + "\" nicht gefunden (nicht-online Spieler können nur über UUID angegeben werden).");
                    return true;
                }
            }
        } else {
            playerId = player.getUniqueId();
            userString = player.getName();
        }

        Pair<Long, Integer> timeAndTickets = this.teamSpeakBot.getLotteryTimeAndTickets(playerId);
        ChatUtil.sendNormalMessage(sender,
                userString + " hat " + StringUtil.formatTimespanClassic(timeAndTickets.first())
                        + " Aktivität und damit " + timeAndTickets.second() + " Lose.");
        return true;
    }

}
