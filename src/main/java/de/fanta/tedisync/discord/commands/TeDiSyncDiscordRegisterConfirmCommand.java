package de.fanta.tedisync.discord.commands;

import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.discord.DiscordBot;
import de.fanta.tedisync.discord.DiscordUserInfo;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.bungee.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.sql.SQLException;
import java.util.logging.Level;

public class TeDiSyncDiscordRegisterConfirmCommand extends SubCommand {
    private final boolean confirm;
    private final DiscordBot discordBot;

    public TeDiSyncDiscordRegisterConfirmCommand(DiscordBot discordBot, boolean confirm) {
        this.discordBot = discordBot;
        this.confirm = confirm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            ChatUtil.sendErrorMessage(sender, "You are not a Player :>");
            return true;
        }

        User user = DiscordBot.getRequests().get(player.getUniqueId());
        if (confirm) {
            if (DiscordBot.getRequests().containsKey(player.getUniqueId())) {
                if (DiscordBot.saveUser(player.getUniqueId(), user.getIdLong())) {
                    ChatUtil.sendNormalMessage(player, "Die Anfrage wurde angenommen.");
                    EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Anfrage angenommen");
                    embedBuilder.setColor(ChatUtil.GREEN.getColor());
                    embedBuilder.setDescription("Die Anfrage zum Verbinden wurde von " + player.getName() + " angenommen.");
                    user.openPrivateChannel().complete().sendMessageEmbeds(embedBuilder.build()).queue();

                    try {
                        DiscordUserInfo discordUserInfo = DiscordBot.getDatabase().getUsersByUUID(player.getUniqueId());
                        discordBot.updateDiscordGroup(player.getUniqueId(), discordUserInfo);
                    } catch (SQLException e) {
                        TeDiSync.getPlugin().getLogger().log(Level.SEVERE, "Error while get user", e);
                    }
                } else {
                    ChatUtil.sendErrorMessage(player, "Daten konnten nicht gespeichert werden.");
                    EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Fehler");
                    embedBuilder.setColor(ChatUtil.RED.getColor());
                    embedBuilder.setDescription("Daten konnten nicht gespeichert werden.");
                    user.openPrivateChannel().complete().sendMessageEmbeds(embedBuilder.build()).queue();
                }
            } else {
                ChatUtil.sendErrorMessage(player, "Du hast momentan keine Anfrage.");
            }
        } else {
            if (DiscordBot.getRequests().containsKey(player.getUniqueId())) {
                DiscordBot.getRequests().remove(player.getUniqueId());
                ChatUtil.sendNormalMessage(player, "Die Anfrage wurde abgelehnt.");

                EmbedBuilder embedBuilder = new EmbedBuilder().setTitle("Anfrage abgelehnt");
                embedBuilder.setColor(ChatUtil.RED.getColor());
                embedBuilder.setDescription("Die Anfrage zum Verbinden wurde von " + player.getName() + " abgelehnt.");
                user.openPrivateChannel().complete().sendMessageEmbeds(embedBuilder.build()).queue();
            } else {
                ChatUtil.sendErrorMessage(player, "Du hast momentan keine Anfrage.");
            }
        }
        return true;
    }
}

