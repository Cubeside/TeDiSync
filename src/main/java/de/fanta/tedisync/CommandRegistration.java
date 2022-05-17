package de.fanta.tedisync;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginManager;

public record CommandRegistration(TeDiSync plugin) {

    public static final PluginManager pM = ProxyServer.getInstance().getPluginManager();

    public void registerCommands() {

    }
}
