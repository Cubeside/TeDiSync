package de.fanta.tedisync.discord.commands;

import de.fanta.tedisync.TeDiPlayer;
import de.fanta.tedisync.discord.DiscordBot;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class TeDiSyncDiscordRegisterConfirmCommand extends SubCommand {
    private final boolean confirm;

    public TeDiSyncDiscordRegisterConfirmCommand(boolean confirm) {
        this.confirm = confirm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            ChatUtil.sendErrorMessage(sender, "You are not a Player :>");
            return true;
        }

        if (confirm) {
            if (DiscordBot.getRequests().containsKey(player.getUniqueId())) {
                User user = DiscordBot.getRequests().get(player.getUniqueId());

                TeDiPlayer teDiPlayer = new TeDiPlayer(player, user.getIdLong(), null);
                if (teDiPlayer.saveToDatabase()) {
                    ChatUtil.sendNormalMessage(player, "Anfrage wurde Angenommen.");
                    EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Anfrage angenommen");
                    embedBuilder.setColor(ChatUtil.GREEN.getColor());
                    embedBuilder.setDescription("Die anfrage zum Verbinden wurde von " + player.getName() + " angenommen.");
                    user.openPrivateChannel().complete().sendMessageEmbeds(embedBuilder.build()).queue();
                } else {
                    ChatUtil.sendErrorMessage(player, "Daten konnten nicht gespeichert werden.");
                    EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Fehler");
                    embedBuilder.setColor(ChatUtil.RED.getColor());
                    embedBuilder.setDescription("Daten konnten nicht gespeichert werden.");
                    user.openPrivateChannel().complete().sendMessageEmbeds(embedBuilder.build()).queue();
                }
            } else {
                ChatUtil.sendErrorMessage(player, "Du hast keine Anfrage");
            }
        } else {
            if (DiscordBot.getRequests().containsKey(player.getUniqueId())) {
                User user = DiscordBot.getRequests().get(player.getUniqueId());
                DiscordBot.getRequests().remove(player.getUniqueId());
                ChatUtil.sendNormalMessage(player, "Anfrage wurde Abgelehnt");

                EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Anfrage abgelehnt");
                embedBuilder.setColor(ChatUtil.RED.getColor());
                embedBuilder.setDescription("Die anfrage zum Verbinden wurde von " + player.getName() + " abgelehnt.");
                user.openPrivateChannel().complete().sendMessageEmbeds(embedBuilder.build()).queue();
            } else {
                ChatUtil.sendErrorMessage(player, "Du hast keine Anfrage");
            }
        }
        return true;
    }
}

