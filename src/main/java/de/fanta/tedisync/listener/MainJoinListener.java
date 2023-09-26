package de.fanta.tedisync.listener;

import de.fanta.tedisync.TeDiPlayer;
import de.fanta.tedisync.TeDiSync;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.SQLException;
import java.util.logging.Level;

public class MainJoinListener implements Listener {

    private final TeDiSync plugin;

    public MainJoinListener(TeDiSync plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onJoin(PostLoginEvent e) {
        long discordID;
        int teamspeakID;
        try {
            discordID = plugin.getDatabase().getDiscordData(e.getPlayer().getUniqueId());
            teamspeakID = plugin.getDatabase().getTeamspeakData(e.getPlayer().getUniqueId());
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "User could not be loaded.", ex);
            return;
        }

        TeDiPlayer teDiPlayer = new TeDiPlayer(e.getPlayer(), discordID, teamspeakID);
        plugin.getTeDiPlayerMap().put(teDiPlayer.getPlayer().getUniqueId(), teDiPlayer);
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent e) {
        plugin.getTeDiPlayerMap().remove(e.getPlayer().getUniqueId());
    }
}
