package de.fanta.tedisync;

import de.fanta.tedisync.listeners.JoinLeaveListener;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginManager;

public record EventRegistration(TeDiSync plugin) {

    public static final PluginManager pM = ProxyServer.getInstance().getPluginManager();

    public void registerEvents() {
        pM.registerListener(plugin, new JoinLeaveListener());
    }
}
