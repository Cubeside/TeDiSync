package de.fanta.tedisync.teamspeak.commands;

import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.ChatUtilAdventure;
import de.iani.cubesideutils.bungee.ChatUtilsBungee;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;


public class TeamSpeakDrawLotteryCommand extends SubCommand {

    private final TeamSpeakBot teamSpeakBot;

    public TeamSpeakDrawLotteryCommand(TeamSpeakBot teamSpeakBot) {
        this.teamSpeakBot = teamSpeakBot;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString,
            ArgsParser args) {
        UUID playerId = this.teamSpeakBot.drawLottery();
        if (playerId == null) {
            ChatUtil.sendWarningMessage(sender, "Keine Spieler mit Losen vorhanden.");
            return true;
        }

        ProxiedPlayer player = this.teamSpeakBot.getPlugin().getProxy().getPlayer(playerId);
        Component playerComponent = Component.text(player == null ? playerId.toString() : player.getName())
                .hoverEvent(HoverEvent.showText(Component.text("PlayerInfo anzeigen")))
                .clickEvent(ClickEvent.runCommand("/security playerinfo " + playerId));
        Component message = Component.text("Gewinner: ").append(playerComponent).color(NamedTextColor.GREEN);
        new ChatUtilAdventure.AdventureComponentMsg(message).send(new ChatUtilsBungee.CommandSenderWrapper(sender));
        return true;
    }

    @Override
    public String getRequiredPermission() {
        return "tedisync.teamspeak.lottery";
    }

}
