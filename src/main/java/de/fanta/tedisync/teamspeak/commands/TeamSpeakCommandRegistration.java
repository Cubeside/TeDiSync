package de.fanta.tedisync.teamspeak.commands;

import de.fanta.tedisync.teamspeak.TeamSpeakBot;
import de.iani.cubesideutils.bungee.commands.CommandRouter;
import de.iani.cubesideutils.bungee.commands.CommandRouterCommand;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginManager;

public record TeamSpeakCommandRegistration(TeamSpeakBot teamSpeakBot) {

    public static final PluginManager pM = ProxyServer.getInstance().getPluginManager();

    public void registerCommands() {
        CommandRouter commandRouter = new CommandRouter();
        pM.registerCommand(teamSpeakBot.getPlugin(), new CommandRouterCommand(commandRouter, "teamspeak", null, "ts"));
        commandRouter.addCommandMapping(new TeDiSyncTeamSpeakRegisterConfirmCommand(true, teamSpeakBot), "register", "accept");
        commandRouter.addCommandMapping(new TeDiSyncTeamSpeakRegisterConfirmCommand(false, teamSpeakBot), "register", "deny");
        commandRouter.addCommandMapping(new TeamSpeakConvertCommand(teamSpeakBot), "convert");
        commandRouter.addCommandMapping(new TeamSpeakListAccountsCommand(teamSpeakBot), "listaccounts");
        commandRouter.addCommandMapping(new TeamSpeakUnlinkCommand(teamSpeakBot), "unlink");
        commandRouter.addCommandMapping(new TeamSpeakListNewbiesCommand(teamSpeakBot), "listnewbies");
        commandRouter.addCommandMapping(new TeamSpeakLinkCommand(teamSpeakBot), "link");
    }
}
