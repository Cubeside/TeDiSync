package de.fanta.tedisync.teamspeak.database;

import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.fanta.tedisync.teamspeak.TeamSpeakUserInfo;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;

public class BungeeListener implements Listener {

    private final TeamSpeakBot teamSpeakBot;

    public BungeeListener(TeamSpeakBot teamSpeakBot) {
        this.teamSpeakBot = teamSpeakBot;
    }

    @EventHandler
    public void onPlayerJoin(PostLoginEvent e) {
        try {
            Collection<TeamSpeakUserInfo> teamSpeakUserInfos = teamSpeakBot.getDatabase().getUsersByUUIS(e.getPlayer().getUniqueId());
            teamSpeakUserInfos.forEach(teamSpeakUserInfo -> teamSpeakBot.updateTSDescription(teamSpeakUserInfo, e.getPlayer()));
        } catch (SQLException ex) {
            teamSpeakBot.plugin().getLogger().log(Level.SEVERE, "Error while load user Infos", ex);
        }
    }
}
