package de.fanta.tedisync.discord.commands;

import de.fanta.tedisync.TeDiSync;
import de.iani.cubesideutils.bungee.commands.CommandRouter;
import de.iani.cubesideutils.bungee.commands.CommandRouterCommand;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginManager;

public record DiscordCommandRegistration(TeDiSync plugin) {

    public static final PluginManager pM = ProxyServer.getInstance().getPluginManager();

    public void registerCommands() {
        CommandRouter commandRouter = new CommandRouter();
        pM.registerCommand(plugin, new CommandRouterCommand(commandRouter, "tedisync"));
        commandRouter.addCommandMapping(new TeDiSyncDiscordRegisterConfirmCommand(plugin, true), "register", "discord", "accept");
        commandRouter.addCommandMapping(new TeDiSyncDiscordRegisterConfirmCommand(plugin, false), "register", "discord", "deny");
    }
}
