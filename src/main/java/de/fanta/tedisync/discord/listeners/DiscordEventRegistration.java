package de.fanta.tedisync.discord.listeners;

import de.fanta.tedisync.TeDiSync;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginManager;

public record DiscordEventRegistration(TeDiSync plugin) {

    public static final PluginManager pM = ProxyServer.getInstance().getPluginManager();

    public void registerEvents() {
        pM.registerListener(plugin, new JoinLeaveListener());
    }
}
