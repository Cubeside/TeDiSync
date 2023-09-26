package de.fanta.tedisync;

import de.fanta.tedisync.discord.DiscordBot;
import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.logging.Level;

public class TeDiPlayer {

    private final Long discordUserID;
    private final Integer teamSpeakUserID;
    private final ProxiedPlayer player;

    public TeDiPlayer(ProxiedPlayer player, @Nullable Long discordUserID, @Nullable Integer teamSpeakUserID) {
        this.player = player;
        this.discordUserID = discordUserID;
        this.teamSpeakUserID = teamSpeakUserID;
    }

    public Long getDiscordID() {
        return discordUserID;
    }

    public User getDiscordUser() {
        if (discordUserID != null) {
            return DiscordBot.getDiscordUser(discordUserID);
        }
        return null;
    }

    public Integer getTeamSpeakID() {
        return teamSpeakUserID;
    }

    public ProxiedPlayer getPlayer() {
        return player;
    }

    public boolean saveToDatabase() {
        try {
            TeDiSync.getPlugin().getDatabase().insertPlayerData(player.getUniqueId(), discordUserID != null ? discordUserID : -1, teamSpeakUserID != null ? teamSpeakUserID : -1);
            return true;
        } catch (SQLException e) {
            TeDiSync.getPlugin().getLogger().log(Level.SEVERE, "data could not be saved", e);
            return false;
        }
    }
}
