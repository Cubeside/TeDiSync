package de.fanta.tedisync.teamspeak;

import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;

import java.sql.SQLException;
import java.util.Collection;

public record LuckPermsListener(TeamSpeakBot teamSpeakBot) {

    public void createEventHandler() {
        LuckPermsProvider.get().getEventBus().subscribe(teamSpeakBot.plugin(), UserDataRecalculateEvent.class, event -> {
            try {
                Collection<TeamSpeakUserInfo> teamSpeakUserInfos = teamSpeakBot.getDatabase().getUsersByUUIS(event.getUser().getUniqueId());
                teamSpeakUserInfos.forEach(teamSpeakUserInfo -> {
                    ClientInfo clientInfo = null;
                    if (teamSpeakBot.getAsyncApi().isClientOnline(teamSpeakUserInfo.tsID()).getUninterruptibly()) {
                        try {
                            clientInfo = teamSpeakBot.getAsyncApi().getClientByUId(teamSpeakUserInfo.tsID()).getUninterruptibly();
                        } catch (TS3CommandFailedException ignored) {
                        }
                    }

                    if (clientInfo != null) {
                        teamSpeakBot.updateTeamSpeakGroup(event.getUser().getUniqueId(), clientInfo);
                    }

                });
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
