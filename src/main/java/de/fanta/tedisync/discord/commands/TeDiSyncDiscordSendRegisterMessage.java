package de.fanta.tedisync.discord.commands;

import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.discord.DiscordBot;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class TeDiSyncDiscordSendRegisterMessage extends SubCommand {

    private final TeDiSync plugin;

    public TeDiSyncDiscordSendRegisterMessage(TeDiSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            ChatUtil.sendErrorMessage(sender, "You are not a Player :>");
            return false;
        }

        long channelID = plugin.getConfig().getLong("discord.registerchannel", -1);
        if (channelID == -1) {
            ChatUtil.sendErrorMessage(player, "In der Config ist kein Gültiger Register-Channel angegeben.");
            return true;
        }

        TextChannel channel = DiscordBot.getDiscordAPI().getTextChannelById(channelID);
        if (channel == null) {
            ChatUtil.sendErrorMessage(player, "Der in der Config angegebene Channel existiert nicht.");
            return true;
        }

        DiscordBot.sendRegisterMessage(channel);
        ChatUtil.sendNormalMessage(player, "Nachricht wurde erstellt.");
        return true;
    }

    @Override
    public String getRequiredPermission() {
        return "tedisync.discord.sendregistermessage";
    }
}
