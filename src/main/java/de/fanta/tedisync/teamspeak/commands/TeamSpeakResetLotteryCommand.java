package de.fanta.tedisync.teamspeak.commands;

import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;


public class TeamSpeakResetLotteryCommand extends SubCommand {

    private final TeamSpeakBot teamSpeakBot;

    public TeamSpeakResetLotteryCommand(TeamSpeakBot teamSpeakBot) {
        this.teamSpeakBot = teamSpeakBot;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString,
            ArgsParser args) {
        if (!args.getNext("").equals("RESET")) {
            ChatUtil.sendWarningMessage(sender,
                    "Um die TeamSpeak-Lotterie zurückzusetzen, verwende /teamspeak resetlottery RESET. Dies kann nicht rückgängig gemacht werden.");
            return true;
        }

        this.teamSpeakBot.resetLottery();
        ChatUtil.sendNormalMessage(sender, "TeamSpeak-Lotterie zurückgesetzt.");
        return true;
    }

    @Override
    public String getRequiredPermission() {
        return "tedisync.teamspeak.lottery";
    }

}
