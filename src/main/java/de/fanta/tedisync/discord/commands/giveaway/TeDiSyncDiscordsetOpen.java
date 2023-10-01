package de.fanta.tedisync.discord.commands.giveaway;

import de.fanta.tedisync.discord.DiscordBot;
import de.fanta.tedisync.discord.Giveaway;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class TeDiSyncDiscordsetOpen extends SubCommand {
    private final boolean open;

    public TeDiSyncDiscordsetOpen(boolean open) {
        this.open = open;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            ChatUtil.sendErrorMessage(sender, "You are not a Player :>");
            return false;
        }

        if (!DiscordBot.getUserEditGiveaway().containsKey(player.getUniqueId())) {
            ChatUtil.sendErrorMessage(player, "Du bearbeitest momentan kein Gewinnspiel.");
            return true;
        }
        Giveaway giveaway = DiscordBot.getGiveaways().get(DiscordBot.getUserEditGiveaway().get(player.getUniqueId()));
        if (giveaway == null) {
            ChatUtil.sendErrorMessage(player, "Das Giveaway, welches du aktuell bearbeitest existiert nicht.");
            return true;
        }

        if (open) {
            if (giveaway.isOpen()) {
                ChatUtil.sendNormalMessage(player, "Das Gewinnspiel ist bereits geöffnet.");
                return true;
            }
            if (giveaway.setOpen(true)) {
                giveaway.sendGiveawayToDiscordChannel();
                ChatUtil.sendNormalMessage(player, "Das Gewinnspiel wurde geöffnet und veröffentlicht.");
            } else {
                ChatUtil.sendErrorMessage(player, "Das Gewinnspiel konnte nicht geöffnet werden.");
            }

        } else {
            if (!giveaway.isOpen()) {
                ChatUtil.sendNormalMessage(player, "Das Gewinnspiel ist bereits geschlossen.");
                return true;
            }
            if (!giveaway.setOpen(false)) {
                giveaway.deleteGiveawayFromDiscordChannel();
                ChatUtil.sendNormalMessage(player, "Das Gewinnspiel wurde geschlossen und aus dem Discord entfernt.");
            } else {
                ChatUtil.sendErrorMessage(player, "Das Gewinnspiel konnte nicht geschlossen werden.");
            }
        }
        giveaway.saveToConfig();
        return true;
    }

    @Override
    public String getRequiredPermission() {
        return "tedisync.discord.giveaway.setopen";
    }
}

