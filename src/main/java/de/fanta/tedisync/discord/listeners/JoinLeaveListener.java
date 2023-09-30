package de.fanta.tedisync.discord.listeners;

import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.discord.DiscordBot;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class JoinLeaveListener implements Listener {

    @EventHandler
    public void onJoin(PostLoginEvent e) {
        Configuration config = TeDiSync.getPlugin().getConfig().getSection("discorduser");
        for (String id : config.getKeys()) {
            if (config.getString(id).equals(e.getPlayer().getUniqueId().toString())) {
                DiscordBot.getUserList().put(Long.valueOf(id), UUID.fromString(config.getString(id)));
                TeDiSync.getPlugin().getLogger().info(config.getString(id) + " " + id);
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent e) {
        Long id = DiscordBot.getIDFromUUD(e.getPlayer().getUniqueId());
        if (id != null) {
            DiscordBot.getUserList().remove(id);
        }
    }
}
