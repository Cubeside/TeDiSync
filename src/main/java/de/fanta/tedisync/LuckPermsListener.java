package de.fanta.tedisync;

import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.teamspeak.TeamSpeakUserInfo;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.md_5.bungee.api.ProxyServer;

import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public record LuckPermsListener(TeamSpeakBot teamSpeakBot) {

    public void createEventHandler() {
        LuckPermsProvider.get().getEventBus().subscribe(teamSpeakBot.plugin(), UserDataRecalculateEvent.class, event -> {
            if (!teamSpeakBot.getUpdatePlayers().add(event.getUser().getUniqueId())) {
                return;
            }

            ProxyServer.getInstance().getScheduler().schedule(teamSpeakBot.plugin(), () -> {
                teamSpeakBot.getUpdatePlayers().remove(event.getUser().getUniqueId());
                try {
                    //Update TeamSpeak group
                    Collection<TeamSpeakUserInfo> teamSpeakUserInfos = teamSpeakBot.getDatabase().getUsersByUUIS(event.getUser().getUniqueId());
                    teamSpeakUserInfos.forEach(teamSpeakUserInfo -> {
                        if (teamSpeakBot.getAsyncApi().isClientOnline(teamSpeakUserInfo.tsID()).getUninterruptibly()) {
                            try {
                                ClientInfo clientInfo = teamSpeakBot.getAsyncApi().getClientByUId(teamSpeakUserInfo.tsID()).getUninterruptibly();
                                if (clientInfo != null) {
                                    teamSpeakBot.updateTeamSpeakGroup(event.getUser().getUniqueId(), clientInfo);
                                }
                            } catch (TS3CommandFailedException ignored) {
                            }
                        }
                    });

                    //TODO Update Discord Group
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, 100, TimeUnit.MILLISECONDS);
        });
    }
}
