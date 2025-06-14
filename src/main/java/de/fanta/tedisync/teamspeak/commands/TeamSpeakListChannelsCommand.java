package de.fanta.tedisync.teamspeak.commands;

import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class TeamSpeakListChannelsCommand extends SubCommand {

    private final TeamSpeakBot teamSpeakBot;

    public TeamSpeakListChannelsCommand(TeamSpeakBot teamSpeakBot) {
        this.teamSpeakBot = teamSpeakBot;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        teamSpeakBot.getAsyncApi().getChannels().onSuccess(list -> {
            for (Channel channel : list) {
                ChatUtil.sendNormalMessage(sender, "Channel '" + channel.getName() + "' (" + channel.getId() + ") TalkPower: " + channel.getNeededTalkPower() + " Clients: " + channel.getTotalClients());
            }
        });

        return true;
    }

    @Override
    public String getRequiredPermission() {
        return "tedisync.teamspeak.listchannels";
    }
}
