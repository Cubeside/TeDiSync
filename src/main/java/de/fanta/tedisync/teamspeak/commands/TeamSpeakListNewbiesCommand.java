package de.fanta.tedisync.teamspeak.commands;

import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.ComponentUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TeamSpeakListNewbiesCommand extends SubCommand {

    private final TeamSpeakBot teamSpeakBot;

    public TeamSpeakListNewbiesCommand(TeamSpeakBot teamSpeakBot) {
        this.teamSpeakBot = teamSpeakBot;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            ChatUtil.sendErrorMessage(sender, "You are nor a Player :>");
            return true;
        }

        List<Client> clients = teamSpeakBot.getAsyncApi().getClients().getUninterruptibly();
        if (clients == null || clients.isEmpty()) {
            ChatUtil.sendNormalMessage(player, "Keine Neulinge online!");
            return true;
        }

        List<Client> newbies = new ArrayList<>();
        clients.forEach(client -> {
            List<Integer> userGroups = Arrays.stream(client.getServerGroups()).boxed().toList();
            userGroups.forEach(integer -> {
                if (Objects.equals(integer, teamSpeakBot.getNewbieGroup())) {
                    newbies.add(client);
                }
            });
        });

        if (newbies.isEmpty()) {
            ChatUtil.sendNormalMessage(player, "Keine Neulinge online!");
            return true;
        }
        ChatUtil.sendNormalMessage(player, "--- Newbies ---");
        newbies.forEach(client -> {
            ClickEvent connectClickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/teamspeak link " + client.getUniqueIdentifier());
            HoverEvent connectHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("TS Account verbinden"));

            BaseComponent component = ComponentUtil.setColor(client.getNickname() + " ", ChatUtil.GREEN);

            BaseComponent connectComponent = ComponentUtil.setColor("[✔]", ChatUtil.BLUE);
            connectComponent.setHoverEvent(connectHoverEvent);
            connectComponent.setClickEvent(connectClickEvent);
            component.addExtra(connectComponent);

            ChatUtil.sendComponent(player, component);
        });
        return true;
    }

    @Override
    public String getRequiredPermission() {
        return "teamspeak.listnewbies";
    }
}
