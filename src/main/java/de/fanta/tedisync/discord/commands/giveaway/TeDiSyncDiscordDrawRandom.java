package de.fanta.tedisync.discord.commands.giveaway;

import de.fanta.tedisync.discord.DiscordBot;
import de.fanta.tedisync.discord.Giveaway;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.ComponentUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Collection;

public class TeDiSyncDiscordDrawRandom extends SubCommand {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            ChatUtil.sendErrorMessage(sender, "You are not a Player :>");
            return true;
        }

        if (!args.hasNext()) {
            ChatUtil.sendErrorMessage(player, "Du musst einen Namen für das Gewinnspiel angeben.");
            return true;
        }

        String giveawayName = args.getNext();
        Giveaway giveaway = DiscordBot.getGiveaways().get(giveawayName.toLowerCase());
        if (giveaway == null) {
            ChatUtil.sendErrorMessage(player, "Ein Gewinnspiel mit dem Namen " + ChatUtil.BLUE + giveawayName + ChatUtil.RED + " existiert nicht.");
            return true;
        }

        User user = DiscordBot.getDiscordAPI().retrieveUserById(giveaway.drawRandom()).complete();
        if (DiscordBot.getDiscordIdToUUID().containsKey(user.getIdLong())) {
            ClickEvent infoClickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/playerinfo " + DiscordBot.getDiscordIdToUUID().get(user.getIdLong()));
            HoverEvent infoHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Info"));

            BaseComponent component = ComponentUtil.setColor("Gewinner: " + user.getName() + "(" + user.getEffectiveName() + ") " + "Lose: " + giveaway.getEntryCount(user.getIdLong()), ChatUtil.GREEN);

            BaseComponent infoComponent = ComponentUtil.setColor(" \uD83D\uDEC8", ChatUtil.ORANGE);
            infoComponent.setHoverEvent(infoHoverEvent);
            infoComponent.setClickEvent(infoClickEvent);
            component.addExtra(infoComponent);
            ChatUtil.sendComponent(player, component);
        } else {
            ChatUtil.sendNormalMessage(player, "Gewinner: " + user.getName() + "(" + user.getEffectiveName() + ") " + "Lose: " + giveaway.getEntryCount(user.getIdLong()));
        }

        return true;
    }

    @Override
    public Collection<String> onTabComplete(CommandSender sender, Command command, String alias, ArgsParser args) {
        return DiscordBot.getGiveaways().keySet();
    }

    @Override
    public String getRequiredPermission() {
        return "tedisync.discord.giveaway.drawrandom";
    }
}
