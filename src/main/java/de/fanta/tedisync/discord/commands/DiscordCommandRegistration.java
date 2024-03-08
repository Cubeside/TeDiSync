package de.fanta.tedisync.discord.commands;

import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.discord.commands.giveaway.*;
import de.iani.cubesideutils.bungee.commands.CommandRouter;
import de.iani.cubesideutils.bungee.commands.CommandRouterCommand;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginManager;

public record DiscordCommandRegistration(TeDiSync plugin) {

    public static final PluginManager pM = ProxyServer.getInstance().getPluginManager();

    public void registerCommands() {
        CommandRouter commandRouter = new CommandRouter();
        pM.registerCommand(plugin, new CommandRouterCommand(commandRouter, "discord"));
        commandRouter.addCommandMapping(new TeDiSyncDiscordCommand(plugin));
        commandRouter.addCommandMapping(new TeDiSyncDiscordSendRegisterMessage(plugin), "sendregistermessage");
        commandRouter.addCommandMapping(new TeDiSyncDiscordRegisterConfirmCommand(true), "register", "accept");
        commandRouter.addCommandMapping(new TeDiSyncDiscordRegisterConfirmCommand(false), "register", "deny");

        commandRouter.addCommandMapping(new TeDiSyncDiscordCreateGiveaway(), "giveaway", "create");
        commandRouter.addCommandMapping(new TeDiSyncDiscordInfo(), "giveaway", "info");
        commandRouter.addCommandMapping(new TeDiSyncDiscordEdit(), "giveaway", "edit");
        commandRouter.addCommandMapping(new TeDiSyncDiscordDrawRandom(), "giveaway", "drawrandom");
        commandRouter.addCommandMapping(new TeDiSyncDiscordDelete(), "giveaway", "delete");

        commandRouter.addCommandMapping(new TeDiSyncDiscordSetMessage(), "giveaway", "setmessage");
        commandRouter.addCommandMapping(new TeDiSyncDiscordSetTitle(), "giveaway", "settitle");
        commandRouter.addCommandMapping(new TeDiSyncDiscordSetColor(), "giveaway", "setcolor");
        commandRouter.addCommandMapping(new TeDiSyncDiscordSetButtonText(), "giveaway", "setbuttontext");
        commandRouter.addCommandMapping(new TeDiSyncDiscordSave(), "giveaway", "save");
        commandRouter.addCommandMapping(new TeDiSyncDiscordsetOpen(true), "giveaway", "setopen", "true");
        commandRouter.addCommandMapping(new TeDiSyncDiscordsetOpen(false), "giveaway", "setopen", "false");
        commandRouter.addCommandMapping(new TeDiSyncDiscordSetEnterMultiple(true), "giveaway", "setentermultiple", "true");
        commandRouter.addCommandMapping(new TeDiSyncDiscordSetEnterMultiple(false), "giveaway", "setentermultiple", "false");
    }
}
