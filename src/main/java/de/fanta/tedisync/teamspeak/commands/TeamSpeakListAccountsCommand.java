package de.fanta.tedisync.teamspeak.commands;

import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.teamspeak.TeamSpeakUserInfo;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.ComponentUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class TeamSpeakListAccountsCommand extends SubCommand {

    private final TeamSpeakBot teamSpeakBot;

    public TeamSpeakListAccountsCommand(TeamSpeakBot teamSpeakBot) {
        this.teamSpeakBot = teamSpeakBot;
    }

    @Override
    public Collection<String> onTabComplete(CommandSender sender, Command command, String alias, ArgsParser args) {
        if (args.remaining() == 1 && sender.hasPermission(TeamSpeakUnlinkCommand.DELETE_OTHER_ACCOUNTS_PERMISSION)) {
            return teamSpeakBot.getPlugin().getProxy().getPlayers().stream().map(p -> p.getName()).toList();
        }
        return List.of();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        ProxiedPlayer otherPlayer = null;
        if (!(sender instanceof ProxiedPlayer player)) {
            if (!args.hasNext()) {
                ChatUtil.sendErrorMessage(sender, "You are nor a Player :>");
                return true;
            }
        } else {
            otherPlayer = player;
        }

        String name = null;
        if (args.hasNext() && sender.hasPermission(TeamSpeakUnlinkCommand.DELETE_OTHER_ACCOUNTS_PERMISSION)) {
            name = args.getNext();
            otherPlayer = teamSpeakBot.getPlugin().getProxy().getPlayer(name);
        }
        if (otherPlayer == null) {
            ChatUtil.sendErrorMessage(sender, "Spieler " + name + " ist nicht online!");
            return true;
        }

        try {
            Collection<TeamSpeakUserInfo> teamSpeakUserInfos = teamSpeakBot.getDatabase().getUsersByUUID(otherPlayer.getUniqueId());
            if (teamSpeakUserInfos.isEmpty()) {
                if (otherPlayer == sender) {
                    ChatUtil.sendNormalMessage(sender, "Du hast keine verbundenen TeamSpeak Accounts");
                } else {
                    ChatUtil.sendNormalMessage(sender, otherPlayer.getName() + " hat keine verbundenen TeamSpeak Accounts");
                }
            } else {
                if (otherPlayer == sender) {
                    ChatUtil.sendNormalMessage(sender, "--- Verbundene TeamSpeak Accounts ---");
                } else {
                    ChatUtil.sendNormalMessage(sender, "--- Mit " + otherPlayer.getName() + " verbundene TeamSpeak Accounts ---");
                }
                teamSpeakUserInfos.forEach(teamSpeakUserInfo -> {
                    ClickEvent deleteClickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/teamspeak unlink " + teamSpeakUserInfo.tsID());
                    HoverEvent deleteHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("TS Account entfernen"));

                    BaseComponent component = ComponentUtil.setColor(teamSpeakUserInfo.tsID() + " ", ChatUtil.GREEN);

                    BaseComponent deleteComponent = ComponentUtil.setColor("[X]", ChatUtil.RED);
                    deleteComponent.setHoverEvent(deleteHoverEvent);
                    deleteComponent.setClickEvent(deleteClickEvent);
                    component.addExtra(deleteComponent);

                    ChatUtil.sendComponent(sender, component);
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
