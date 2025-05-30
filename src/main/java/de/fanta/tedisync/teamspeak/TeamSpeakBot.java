package de.fanta.tedisync.teamspeak;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.teamspeak.commands.TeamSpeakCommandRegistration;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.ComponentUtil;
import de.iani.cubesideutils.bungee.sql.SQLConfigBungee;
import de.iani.cubesideutils.commands.ArgsParser;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import org.jetbrains.annotations.Nullable;

public class TeamSpeakBot {
    private final TeDiSync plugin;

    private volatile boolean stopping;

    private static TeamSpeakDatabase database;
    private static ConcurrentHashMap<UUID, String> requests;
    private static TS3ApiAsync asyncApi;
    private static TS3Query query;
    private static ConcurrentHashMap<String, Integer> groupIDs;

    private static Integer newbieGroup;
    private static Collection<Integer> ignoreGroups;

    public TeamSpeakBot(TeDiSync plugin) {
        this.plugin = plugin;
        database = new TeamSpeakDatabase(new SQLConfigBungee(plugin.getConfig().getSection("teamspeak.database")));

        new TeamSpeakCommandRegistration(this).registerCommands();
        plugin.getProxy().getPluginManager().registerListener(plugin, new BungeeListener(this));

        requests = new ConcurrentHashMap<>();
        groupIDs = new ConcurrentHashMap<>();
        Configuration rankConfig = plugin.getConfig().getSection("teamspeak.rankIDs");
        rankConfig.getKeys().forEach(s -> groupIDs.put(s, rankConfig.getInt(s)));

        newbieGroup = plugin.getConfig().getInt("teamspeak.newbieGroup");
        ignoreGroups = plugin.getConfig().getIntList("teamspeak.ignoreGroups");

        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            connect();
        }, 0, TimeUnit.MINUTES);
    }

    public void connect() {
        // if (query != null && query.isConnected()) {
        // try {
        // query.getAsyncApi().logout();
        // query.exit();
        // } catch (Exception ignored) {
        // }
        // }
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
        config.setLoginCredentials(query_username, query_password);
        config.setConnectionHandler(new ConnectionHandler() {

            @Override
            public void onDisconnect(TS3Query ts3Query) {
                plugin.getLogger().info("TeamSpeakBot:onDisconnect");
            }

            @Override
            public void onConnect(TS3Api api) {
                plugin.getLogger().info("TeamSpeakBot:onConnect");
                api.selectVirtualServerByPort(port);
                api.setNickname(query_displayname);
                api.registerAllEvents();

            }
        });

        while (!stopping && query == null) {
            query = new TS3Query(config);
            try {
                query.connect();
                asyncApi = query.getAsyncApi();

                initTeamSpeakBotListener();
                plugin.getLogger().info("Established a connection to the teamspeak server!");
            } catch (TS3ConnectionFailedException e) {
                plugin.getLogger().warning("Could not connect to the teamspeak server!");
                query = null;
                asyncApi = null;
                try {
                    Thread.sleep(20000L);
                } catch (InterruptedException e1) {
                    // ignore
                }
            }
        }
    }

    public void initTeamSpeakBotListener() {
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

                asyncApi.getClientInfo(e.getInvokerId()).onSuccess(client -> {
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
                });
            }

            @Override
            public void onClientJoin(ClientJoinEvent e) {
                if (e.getUniqueClientIdentifier().equalsIgnoreCase("serveradmin")) {
                    return;
                }

                try {
                    asyncApi.getClientByUId(e.getUniqueClientIdentifier()).onSuccess(client -> {
                        if (client == null) {
                            return;
                        }

                        try {
                            TeamSpeakUserInfo teamSpeakUserInfo = database.getUserByTSID(client.getUniqueIdentifier());
                            if (teamSpeakUserInfo != null) {
                                updateTeamSpeakGroup(teamSpeakUserInfo.uuid(), client, null);
                            } else {
                                removeAllTeamSpeakGroups(client.getUniqueIdentifier());
                            }

                            boolean hasGroup = false;
                            for (int serverGroup : client.getServerGroups()) {
                                if (groupIDs.containsValue(serverGroup)) {
                                    hasGroup = true;
                                    break;
                                }
                            }
                            if (!hasGroup && !hasIgnoreGroup(client)) {
                                String message = plugin.getConfig().getString("teamspeak.message");
                                asyncApi.sendPrivateMessage(e.getClientId(), message);
                            }
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                } catch (TS3CommandFailedException ex) {
                    int errorID = ex.getError().getId();
                    if (errorID != 512 && errorID != 1540) {
                        plugin.getLogger().log(Level.INFO, "Error by Join User from" + " (" + e.getClientId() + ") " + ex.getError().getMessage() + " " + errorID, e);
                    }
                }
            }
        });
    }

    public void stopTeamSpeakBot() {
        stopping = true;
        if (query.isConnected()) {
            asyncApi.logout();
            query.exit();
        }
        database.disconnect();
    }

    public void updateTeamSpeakGroup(UUID uuid, ClientInfo clientInfo, @Nullable User lpUser) {
        try {
            asyncApi.isClientOnline(clientInfo.getId()).onSuccess(isOnline -> {
                User user = lpUser;
                if (!isOnline) {
                    return;
                }
                if (user == null) {
                    if (LuckPermsProvider.get().getUserManager().isLoaded(uuid)) {
                        user = LuckPermsProvider.get().getUserManager().getUser(uuid);
                    } else {
                        try {
                            user = LuckPermsProvider.get().getUserManager().loadUser(uuid).get();
                        } catch (ExecutionException | InterruptedException e) {
                            plugin.getLogger().log(Level.SEVERE, "Error by Loading User " + uuid);
                        }
                    }
                }

                ArrayList<Group> userGroups = new ArrayList<>(user.getInheritedGroups(QueryOptions.builder(QueryMode.NON_CONTEXTUAL).flag(Flag.RESOLVE_INHERITANCE, true).build()));
                userGroups.sort((a, b) -> Integer.compare(b.getWeight().orElse(0), a.getWeight().orElse(0)));
                String group = "default";
                for (Group g : userGroups) {
                    if (groupIDs.containsKey(g.getName())) {
                        group = g.getName();
                        break;
                    }
                }
                int groupID = groupIDs.get(group);
                int[] ranks = clientInfo.getServerGroups();
                List<Integer> tsUserRanks = Arrays.stream(ranks).boxed().toList();

                for (Integer userRank : tsUserRanks) {
                    if (groupIDs.containsValue(userRank) && userRank != groupID) {
                        asyncApi.removeClientFromServerGroup(userRank, clientInfo.getDatabaseId());
                    }
                }

                if (!tsUserRanks.contains(groupID)) {
                    asyncApi.addClientToServerGroup(groupID, clientInfo.getDatabaseId());
                }
            });
        } catch (TS3CommandFailedException e) {
            int errorID = e.getError().getId();
            if (errorID != 512 && errorID != 1540) {
                plugin.getLogger().log(Level.INFO, "Error by Update User " + uuid.toString() + " (" + clientInfo.getUniqueIdentifier() + ") " + e.getError().getMessage() + " " + errorID, e);
            }
        }

    }

    public void removeAllTeamSpeakGroups(String id) {

        try {
            asyncApi.getClientByUId(id).onSuccess(clientInfo -> {
                if (clientInfo == null) {
                    return;
                }

                asyncApi.isClientOnline(clientInfo.getId()).onSuccess(isOnline -> {
                    if (!isOnline) {
                        return;
                    }

                    int[] ranks = clientInfo.getServerGroups();
                    List<Integer> tsUserRanks = Arrays.stream(ranks).boxed().toList();

                    for (Integer userRank : tsUserRanks) {
                        if (groupIDs.containsValue(userRank)) {
                            asyncApi.removeClientFromServerGroup(userRank, clientInfo.getDatabaseId());
                        }
                    }
                });
            });


        } catch (TS3CommandFailedException e) {
            int errorID = e.getError().getId();
            if (errorID != 512 && errorID != 1540) {
                plugin.getLogger().log(Level.INFO, "Error by Remove Group from" + " (" + id + ") " + e.getError().getMessage() + " " + errorID, e);
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

    public void updateTSDescription(String tsID, ProxiedPlayer player) {
        TeamSpeakUserInfo teamSpeakUserInfo = null;
        try {
            teamSpeakUserInfo = database.getUserByTSIDANDUUID(tsID, player.getUniqueId());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while Loading TeamSpeakUserInfo", e);
        }
        if (teamSpeakUserInfo != null) {
            updateTSDescription(teamSpeakUserInfo, player);
        }
    }

    public void updateTSDescription(TeamSpeakUserInfo teamSpeakUserInfo, ProxiedPlayer player) {
        if (teamSpeakUserInfo != null) {
            asyncApi.isClientOnline(teamSpeakUserInfo.tsID()).onSuccess(isOnline -> {
                if (!isOnline) {
                    return;
                }
                String lastName = teamSpeakUserInfo.latestName();

                try {
                    asyncApi.getClientByUId(teamSpeakUserInfo.tsID()).onSuccess(clientInfo -> {
                        if (clientInfo != null) {
                            String description = clientInfo.getDescription();
                            if (lastName != null) {
                                if (lastName.equals(player.getName())) {
                                    if (!description.contains(player.getName())) {
                                        description = player.getName() + ((description.isEmpty() || description.isBlank()) ? "" : " | " + description);
                                        setDescription(clientInfo, description);
                                    }
                                } else {
                                    description = description.replace(lastName, player.getName());
                                    setDescription(clientInfo, description);
                                    try {
                                        database.updateMcName(teamSpeakUserInfo.uuid(), teamSpeakUserInfo.tsID(), player.getName());
                                    } catch (SQLException ex) {
                                        plugin.getLogger().log(Level.SEVERE, "Error while Update McName", ex);
                                    }
                                }
                            } else {
                                if (!description.contains(player.getName())) {
                                    description = player.getName() + ((description.isEmpty() || description.isBlank()) ? "" : " | " + description);
                                    setDescription(clientInfo, description);
                                }
                                try {
                                    database.updateMcName(teamSpeakUserInfo.uuid(), teamSpeakUserInfo.tsID(), player.getName());
                                } catch (SQLException ex) {
                                    plugin.getLogger().log(Level.SEVERE, "Error while Update McName", ex);
                                }
                            }
                        }
                    });
                } catch (TS3CommandFailedException ignored) {
                }
            });
        }
    }

    private void setDescription(ClientInfo clientInfo, String description) {
        asyncApi.editClient(clientInfo.getId(), Collections.singletonMap(ClientProperty.CLIENT_DESCRIPTION, (description.length() > 200 ? description.substring(0, 200) : description)));
    }

    public TeamSpeakDatabase getDatabase() {
        return database;
    }

    public ConcurrentHashMap<UUID, String> getRequests() {
        return requests;
    }

    public TS3ApiAsync getAsyncApi() {
        return asyncApi;
    }

    public Integer getNewbieGroup() {
        return newbieGroup;
    }

    public boolean hasIgnoreGroup(ClientInfo clientInfo) {
        boolean hasIgnoreGroup = false;
        for (int serverGroup : clientInfo.getServerGroups()) {
            if (ignoreGroups.contains(serverGroup)) {
                return true;
            }
        }

        return hasIgnoreGroup;
    }

    public TeDiSync getPlugin() {
        return plugin;
    }
}
