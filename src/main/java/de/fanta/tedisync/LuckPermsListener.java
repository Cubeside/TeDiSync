package de.fanta.tedisync;

import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.fanta.tedisync.discord.DiscordBot;
import de.fanta.tedisync.discord.DiscordUserInfo;
import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.teamspeak.TeamSpeakUserInfo;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.md_5.bungee.api.ProxyServer;

import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public record LuckPermsListener(TeDiSync plugin, TeamSpeakBot teamSpeakBot, DiscordBot discordBot) {

    public void createEventHandler() {
        LuckPermsProvider.get().getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, event -> {
            if (!plugin.getUpdatePlayers().add(event.getUser().getUniqueId())) {
                return;
            }

            ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
                plugin.getUpdatePlayers().remove(event.getUser().getUniqueId());
                try {
                    //Update TeamSpeak group
                    if (teamSpeakBot != null) {
                        Collection<TeamSpeakUserInfo> teamSpeakUserInfos = teamSpeakBot.getDatabase().getUsersByUUID(event.getUser().getUniqueId());
                        teamSpeakUserInfos.forEach(teamSpeakUserInfo -> {
                            teamSpeakBot.getAsyncApi().isClientOnline(teamSpeakUserInfo.tsID()).onSuccess(isOnline -> {
                                if (!isOnline) {
                                    return;
                                }

                                try {
                                    teamSpeakBot.getAsyncApi().getClientByUId(teamSpeakUserInfo.tsID()).onSuccess(clientInfo -> {
                                        if (clientInfo != null) {
                                            teamSpeakBot.updateTeamSpeakGroup(event.getUser().getUniqueId(), clientInfo, event.getUser());
                                        }
                                    });
                                } catch (TS3CommandFailedException ignored) {
                                }
                            });
                        });
                    }

                    //Update Discord group
                    if (discordBot != null) {
                        DiscordUserInfo discordUserInfo = DiscordBot.getDatabase().getUsersByUUID(event.getUser().getUniqueId());
                        discordBot.updateDiscordGroup(event.getUser().getUniqueId(), discordUserInfo, event.getUser());
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, 100, TimeUnit.MILLISECONDS);
        });
    }
}
