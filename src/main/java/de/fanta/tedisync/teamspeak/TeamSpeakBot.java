package de.fanta.tedisync.teamspeak;

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.teamspeak.commands.TeamSpeakCommandRegistration;
import de.fanta.tedisync.teamspeak.database.TeamSpeakDatabase;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.ComponentUtil;
import de.iani.cubesideutils.bungee.sql.SQLConfigBungee;
import de.iani.cubesideutils.commands.ArgsParser;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public record TeamSpeakBot(TeDiSync plugin) {

    private static TeamSpeakDatabase database;
    private static HashMap<UUID, String> requests;
    private static TS3ApiAsync asyncApi;
    private static TS3Query query;
    private static HashMap<String, Integer> groupIDs;

    private static Integer newbieGroup;

    public void initTeamSpeakBot() {
        database = new TeamSpeakDatabase(new SQLConfigBungee(plugin.getConfig().getSection("teamspeak.database")));

        String host = plugin.getConfig().getString("teamspeak.login.host");
        int query_port = plugin.getConfig().getInt("teamspeak.login.query_port");
        int port = plugin.getConfig().getInt("teamspeak.login.port");
        String query_username = plugin.getConfig().getString("teamspeak.login.query_username");
        String query_password = plugin.getConfig().getString("teamspeak.login.query_password");
        String query_displayname = plugin.getConfig().getString("teamspeak.login.query_displayname");


        final TS3Config config = new TS3Config();
        config.setHost(host);
        config.setEnableCommunicationsLogging(true);
        config.setQueryPort(query_port);
        config.setFloodRate(TS3Query.FloodRate.UNLIMITED);
        config.setReconnectStrategy(ReconnectStrategy.exponentialBackoff());

        query = new TS3Query(config);
        query.connect();

        asyncApi = query.getAsyncApi();
        asyncApi.login(query_username, query_password);
        asyncApi.selectVirtualServerByPort(port);
        asyncApi.setNickname(query_displayname);


        asyncApi.registerAllEvents();

        new LuckPermsListener(this).createEventHandler();
        new TeamSpeakCommandRegistration(this).registerCommands();

        requests = new HashMap<>();
        groupIDs = new HashMap<>();
        Configuration rankConfig = plugin.getConfig().getSection("teamspeak.rankIDs");
        rankConfig.getKeys().forEach(s -> groupIDs.put(s, rankConfig.getInt(s)));

        newbieGroup = plugin.getConfig().getInt("teamspeak.newbieGroup");

        asyncApi.addTS3Listeners(new TS3EventAdapter() {
            @Override
            public void onTextMessage(TextMessageEvent e) {
                String message = e.getMessage();
                ArgsParser args = new ArgsParser(message.split(" "));
                if (!args.hasNext()) {
                    return;
                }

                String command = args.getNext();
                if (!command.equalsIgnoreCase("!register")) {
                    return;
                }

                ClientInfo client = asyncApi.getClientInfo(e.getInvokerId()).getUninterruptibly();

                if (client == null) {
                    return;
                }

                if (!args.hasNext()) {
                    asyncApi.sendPrivateMessage(client.getId(), "Du musst einen Spielernamen angeben! (!register NAME)");
                    return;
                }

                String playerName = args.getNext();
                ProxiedPlayer proxiedPlayer = plugin.getProxy().getPlayer(playerName);
                try {
                    if (database.getUserByTSID(client.getUniqueIdentifier()) == null) {
                        if (proxiedPlayer == null || !proxiedPlayer.isConnected()) {
                            asyncApi.sendPrivateMessage(client.getId(), "Der Spieler " + playerName + " ist nicht online.");
                        } else {
                            sendRequestToPlayer(proxiedPlayer, client);
                            asyncApi.sendPrivateMessage(client.getId(), "Eine Anfrage zum Verbinden wurde in Minecraft an " + proxiedPlayer.getName() + " geschickt.");
                        }
                    } else {
                        asyncApi.sendPrivateMessage(client.getId(), "Diese TeamSpeak-ID ist bereits mit einem Minecraft-Account verbunden");
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void onClientJoin(ClientJoinEvent e) {
                if (e.getUniqueClientIdentifier().equalsIgnoreCase("serveradmin")) {
                    return;
                }

                ClientInfo client = asyncApi.getClientByUId(e.getUniqueClientIdentifier()).getUninterruptibly();

                if (client == null) {
                    return;
                }

                try {
                    TeamSpeakUserInfo teamSpeakUserInfo = database.getUserByTSID(client.getUniqueIdentifier());
                    if (teamSpeakUserInfo != null) {
                        updateTeamSpeakGroup(teamSpeakUserInfo.uuid(), client);
                    } else {
                        removeAllTeamSpeakGroups(client.getUniqueIdentifier());
                        String message = plugin.getConfig().getString("teamspeak.message");
                        asyncApi.sendPrivateMessage(e.getClientId(), message);
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public void onClientMoved(ClientMovedEvent e) {
                /*ClientInfo client;
                try {
                    client = asyncApi.getClientInfo(e.getClientId()).get();
                } catch (InterruptedException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Client " + e.getClientId() + " konnte nicht geladen werden!", ex);
                    return;
                }

                ChannelInfo targetChannel;
                try {
                    targetChannel = asyncApi.getChannelInfo(e.getTargetChannelId()).get();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }

                plugin.getLogger().info("Client (" + client.getNickname() + " " + client.getIp() + " " + client.getUniqueIdentifier() + ") Joined Channel " + targetChannel.getName());*/
            }
        });
    }

    public void stopTeamSpeakBot() {
        if (query.isConnected()) {
            asyncApi.logout();
            query.exit();
        }
        database.disconnect();
    }

    public void updateTeamSpeakGroup(UUID uuid, ClientInfo clientInfo) {
        try {
            if (!asyncApi.isClientOnline(clientInfo.getId()).getUninterruptibly()) {
                return;
            }

            User user = LuckPermsProvider.get().getUserManager().getUser(uuid);
            String group = user != null ? user.getPrimaryGroup() : "default";
            int groupID = groupIDs.get(group);
            int[] ranks = asyncApi.getClientInfo(clientInfo.getId()).getUninterruptibly().getServerGroups();
            List<Integer> tsUserRanks = Arrays.stream(ranks).boxed().toList();

            for (Integer userRank : tsUserRanks) {
                if (groupIDs.containsValue(userRank) && userRank != groupID) {
                    asyncApi.removeClientFromServerGroup(userRank, clientInfo.getDatabaseId());
                }
            }

            if (!tsUserRanks.contains(groupID)) {
                asyncApi.addClientToServerGroup(groupID, clientInfo.getDatabaseId());
            }
        } catch (TS3CommandFailedException e) {
            int errorID = e.getError().getId();
            if (errorID == 512 || errorID == 1540) {
                plugin.getLogger().log(Level.INFO, "Error by Update User " + uuid.toString() + " (" + clientInfo.getUniqueIdentifier() + ") " + e.getError().getMessage() + " " + errorID);
            } else {
                plugin.getLogger().log(Level.INFO, "Error by Update User " + uuid.toString() + " (" + clientInfo.getUniqueIdentifier() + ") " + e.getError().getMessage() + " " + errorID, e);
            }
        }

    }

    public void removeAllTeamSpeakGroups(String id) {
        ClientInfo clientInfo = asyncApi.getClientByUId(id).getUninterruptibly();
        try {
            if (clientInfo == null || !asyncApi.isClientOnline(clientInfo.getId()).getUninterruptibly()) {
                return;
            }

            int[] ranks = asyncApi.getClientInfo(clientInfo.getId()).getUninterruptibly().getServerGroups();
            List<Integer> tsUserRanks = Arrays.stream(ranks).boxed().toList();

            for (Integer userRank : tsUserRanks) {
                if (groupIDs.containsValue(userRank)) {
                    asyncApi.removeClientFromServerGroup(userRank, clientInfo.getDatabaseId());
                }
            }
        } catch (TS3CommandFailedException e) {
            int errorID = e.getError().getId();
            if (errorID == 512 || errorID == 1540) {
                plugin.getLogger().log(Level.INFO, "Error by Remove Group from " + clientInfo + " (" + clientInfo.getUniqueIdentifier() + ") " + e.getError().getMessage() + " " + errorID);
            } else {
                plugin.getLogger().log(Level.INFO, "Error by Remove Group from " + clientInfo + " (" + clientInfo.getUniqueIdentifier() + ") " + e.getError().getMessage() + " " + errorID, e);
            }
        }

    }

    public void sendRequestToPlayer(ProxiedPlayer proxiedPlayer, ClientInfo client) {
        requests.put(proxiedPlayer.getUniqueId(), client.getUniqueIdentifier());
        ChatUtil.sendNormalMessage(proxiedPlayer, "Möchtest du deine Teamspeak ID " + ChatUtil.BLUE + client.getNickname() + ChatUtil.GREEN + " mit Minecraft verbinden?");

        ClickEvent acceptClickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/teamspeak register accept");
        HoverEvent acceptHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Annehmen"));

        ClickEvent denyClickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/teamspeak register deny");
        HoverEvent denyHoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Ablehnen"));

        BaseComponent component = ComponentUtil.setColor("", ChatColor.BLUE);

        BaseComponent acceptComponent = ComponentUtil.setColor("Annehmen", ChatUtil.GREEN);
        acceptComponent.setHoverEvent(acceptHoverEvent);
        acceptComponent.setClickEvent(acceptClickEvent);
        component.addExtra(acceptComponent);

        component.addExtra(ComponentUtil.setColor(" | ", ChatColor.DARK_GRAY));

        BaseComponent denyComponent = ComponentUtil.setColor("Ablehnen", ChatUtil.RED);
        denyComponent.setHoverEvent(denyHoverEvent);
        denyComponent.setClickEvent(denyClickEvent);
        component.addExtra(denyComponent);
        ChatUtil.sendComponent(proxiedPlayer, component);
    }

    public TeamSpeakDatabase getDatabase() {
        return database;
    }

    public HashMap<UUID, String> getRequests() {
        return requests;
    }

    public TS3ApiAsync getAsyncApi() {
        return asyncApi;
    }

    public Integer getNewbieGroup() {
        return newbieGroup;
    }
}
