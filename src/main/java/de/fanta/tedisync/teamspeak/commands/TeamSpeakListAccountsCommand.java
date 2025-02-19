package de.fanta.tedisync.teamspeak.commands;

import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.teamspeak.TeamSpeakUserInfo;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.ComponentUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import java.sql.SQLException;
import java.util.Collection;
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
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            ChatUtil.sendErrorMessage(sender, "You are nor a Player :>");
            return true;
        }

        try {
            Collection<TeamSpeakUserInfo> teamSpeakUserInfos = teamSpeakBot.getDatabase().getUsersByUUID(player.getUniqueId());
            if (teamSpeakUserInfos.isEmpty()) {
                ChatUtil.sendNormalMessage(player, "Du hast keine verbundenen TeamSpeak Accounts");
            } else {
                ChatUtil.sendNormalMessage(player, "--- Verbundene TeamSpeak Accounts ---");
                teamSpeakUserInfos.forEach(teamSpeakUserInfo -> {
                    ClickEvent deleteClickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/teamspeak unlink " + teamSpeakUserInfo.tsID());
                    HoverEvent deleteHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("TS Account entfernen"));

                    BaseComponent component = ComponentUtil.setColor(teamSpeakUserInfo.tsID() + " ", ChatUtil.GREEN);

                    BaseComponent deleteComponent = ComponentUtil.setColor("[X]", ChatUtil.RED);
                    deleteComponent.setHoverEvent(deleteHoverEvent);
                    deleteComponent.setClickEvent(deleteClickEvent);
                    component.addExtra(deleteComponent);

                    ChatUtil.sendComponent(player, component);
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        return true;
    }
}
