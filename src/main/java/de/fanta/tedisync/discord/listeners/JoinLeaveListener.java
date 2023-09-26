package de.fanta.tedisync.discord.listeners;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class JoinLeaveListener implements Listener {

    @EventHandler
    public void onJoin(PostLoginEvent e) {
        System.out.println(e.getPlayer().getDisplayName() + " Joined");
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent e) {
        System.out.println(e.getPlayer().getDisplayName() + " Leaved");
    }
}
