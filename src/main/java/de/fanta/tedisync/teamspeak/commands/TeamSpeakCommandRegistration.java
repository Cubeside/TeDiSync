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
        pM.registerCommand(this.teamSpeakBot.getPlugin(),
                new CommandRouterCommand(commandRouter, "teamspeak", null, "ts"));
        commandRouter.addCommandMapping(new TeDiSyncTeamSpeakRegisterConfirmCommand(true, this.teamSpeakBot),
                "register", "accept");
        commandRouter.addCommandMapping(new TeDiSyncTeamSpeakRegisterConfirmCommand(false, this.teamSpeakBot),
                "register", "deny");
        commandRouter.addCommandMapping(new TeamSpeakConvertCommand(this.teamSpeakBot), "convert");
        commandRouter.addCommandMapping(new TeamSpeakListAccountsCommand(this.teamSpeakBot), "listaccounts");
        commandRouter.addCommandMapping(new TeamSpeakUnlinkCommand(this.teamSpeakBot), "unlink");
        commandRouter.addCommandMapping(new TeamSpeakListNewbiesCommand(this.teamSpeakBot), "listnewbies");
        commandRouter.addCommandMapping(new TeamSpeakLinkCommand(this.teamSpeakBot), "link");
        commandRouter.addCommandMapping(new TeamSpeakDrawLotteryCommand(this.teamSpeakBot), "drawlottery");
        commandRouter.addCommandMapping(new TeamSpeakResetLotteryCommand(this.teamSpeakBot), "resetlottery");
        commandRouter.addCommandMapping(new TeamSpeakListChannelsCommand(this.teamSpeakBot), "listchannels");
    }
}
