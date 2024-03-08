package de.fanta.tedisync.discord.commands;

import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.ComponentUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.plugin.Command;

public class TeDiSyncDiscordCommand extends SubCommand {

    private final TeDiSync plugin;

    public TeDiSyncDiscordCommand(TeDiSync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        String discordLink = plugin.getConfig().getString("discord.invitelink");
        if (discordLink == null || discordLink.isEmpty()) {
            ChatUtil.sendErrorMessage(sender, "Kein Discord Link in der Config hinterlegt.");
            return true;
        }

        BaseComponent component = ComponentUtil.setColor("Discord: ", ChatColor.GREEN);
        ClickEvent URLClickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, discordLink);
        HoverEvent URLHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Discord"));
        BaseComponent acceptComponent = ComponentUtil.setColor(">> Hier klicken <<", ChatUtil.BLUE);
        acceptComponent.setHoverEvent(URLHoverEvent);
        acceptComponent.setClickEvent(URLClickEvent);
        component.addExtra(acceptComponent);
        ChatUtil.sendComponent(sender, component);

        return true;
    }
}
