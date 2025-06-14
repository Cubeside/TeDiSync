package de.fanta.tedisync.teamspeak;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.ClientProperty;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3ConnectionFailedException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ClientInfo;
import de.fanta.tedisync.TeDiSync;
import de.fanta.tedisync.teamspeak.commands.TeamSpeakCommandRegistration;
import de.fanta.tedisync.utils.ChatUtil;
import de.iani.cubesideutils.ComponentUtil;
import de.iani.cubesideutils.Pair;
import de.iani.cubesideutils.RandomUtil;
import de.iani.cubesideutils.bungee.sql.SQLConfigBungee;
import de.iani.cubesideutils.commands.ArgsParser;
import de.iani.cubesideutils.plugin.api.UtilsApi;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
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
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.config.Configuration;
import org.jetbrains.annotations.Nullable;

public class TeamSpeakBot {

    public static final String TEAMSPEAK_ACTIVITY_KEY = "TeDiSync_TeamspeakActivity";

    private final TeDiSync plugin;

    private volatile boolean stopping;

    private TeamSpeakDatabase database;
    private ConcurrentHashMap<UUID, String> requests;
    private TS3ApiAsync asyncApi;
    private TS3Query query;
    private ConcurrentHashMap<String, Integer> groupIDs;

    private Object activityLock = new Object();
    private Map<Integer, String> clientIdCache;
    private Map<String, TeamSpeakUserInfo> userInfoCache;
    private Set<String> nonLinkedCache;
    private Map<UUID, Long> temporaryActiveTime;
    private long lastActivityCheck;
    private ScheduledTask controlTask;
    private ScheduledTask tidyUserCacheTask;

    private Integer newbieGroup;
    private Collection<Integer> ignoreGroups;

    private long activityControlPeriodMs;
    private Set<Integer> activityExcludingChannels;
    private Set<Integer> lotteryChannels;
    private long timePerLotteryTicket;
    private int maxLotteryTicketsByTime;
    private int lotteryChannelTickets;

    public TeamSpeakBot(TeDiSync plugin) {
        this.plugin = plugin;
        this.database = new TeamSpeakDatabase(new SQLConfigBungee(plugin.getConfig().getSection("teamspeak.database")));

        new TeamSpeakCommandRegistration(this).registerCommands();
        plugin.getProxy().getPluginManager().registerListener(plugin, new BungeeListener(this));

        this.requests = new ConcurrentHashMap<>();
        this.groupIDs = new ConcurrentHashMap<>();
        Configuration rankConfig = plugin.getConfig().getSection("teamspeak.rankIDs");
        rankConfig.getKeys().forEach(s -> this.groupIDs.put(s, rankConfig.getInt(s)));

        this.clientIdCache = new ConcurrentHashMap<>();
        this.userInfoCache = new ConcurrentHashMap<>();
        this.nonLinkedCache = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.temporaryActiveTime = new ConcurrentHashMap<>();

        this.newbieGroup = plugin.getConfig().getInt("teamspeak.newbieGroup");
        this.ignoreGroups = plugin.getConfig().getIntList("teamspeak.ignoreGroups");

        this.activityControlPeriodMs = plugin.getConfig().getLong("teamspeak.activityControlPeriodMs");
        this.activityExcludingChannels =
                new LinkedHashSet<>(plugin.getConfig().getIntList("teamspeak.activityExcludingChannels"));
        this.lotteryChannels = new LinkedHashSet<>(plugin.getConfig().getIntList("teamspeak.lotteryChannels"));
        this.timePerLotteryTicket = plugin.getConfig().getLong("teamspeak.msPerLotteryTicket");
        this.maxLotteryTicketsByTime = plugin.getConfig().getInt("teamspeak.maxLotteryTicketsByTime");
        this.lotteryChannelTickets = plugin.getConfig().getInt("teamspeak.lotteryChannelTickets");

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
        String host = this.plugin.getConfig().getString("teamspeak.login.host");
        int query_port = this.plugin.getConfig().getInt("teamspeak.login.query_port");
        int port = this.plugin.getConfig().getInt("teamspeak.login.port");
        String query_username = this.plugin.getConfig().getString("teamspeak.login.query_username");
        String query_password = this.plugin.getConfig().getString("teamspeak.login.query_password");
        String query_displayname = this.plugin.getConfig().getString("teamspeak.login.query_displayname");

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
                TeamSpeakBot.this.plugin.getLogger().info("TeamSpeakBot:onDisconnect");
            }

            @Override
            public void onConnect(TS3Api api) {
                TeamSpeakBot.this.plugin.getLogger().info("TeamSpeakBot:onConnect");
                api.selectVirtualServerByPort(port);
                api.setNickname(query_displayname);
                api.registerAllEvents();

            }
        });

        while (!this.stopping && this.query == null) {
            this.query = new TS3Query(config);
            try {
                this.query.connect();
                this.asyncApi = this.query.getAsyncApi();

                initTeamSpeakBotListener();
                initActivityControl();
                this.plugin.getLogger().info("Established a connection to the teamspeak server!");
            } catch (TS3ConnectionFailedException e) {
                this.plugin.getLogger().log(Level.WARNING, "Could not connect to the teamspeak server!", e);
                this.query = null;
                this.asyncApi = null;
                try {
                    Thread.sleep(20000L);
                } catch (InterruptedException e1) {
                    // ignore
                }
            }
        }
    }

    public void initTeamSpeakBotListener() {
        this.asyncApi.addTS3Listeners(new TS3EventAdapter() {

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

                TeamSpeakBot.this.asyncApi.getClientInfo(e.getInvokerId()).onSuccess(client -> {
                    if (client == null) {
                        return;
                    }

                    if (!args.hasNext()) {
                        TeamSpeakBot.this.asyncApi.sendPrivateMessage(client.getId(),
                                "Du musst einen Spielernamen angeben! (!register NAME)");
                        return;
                    }

                    String playerName = args.getNext();
                    ProxiedPlayer proxiedPlayer = TeamSpeakBot.this.plugin.getProxy().getPlayer(playerName);
                    try {
                        if (TeamSpeakBot.this.database.getUserByTSID(client.getUniqueIdentifier()) == null) {
                            if (proxiedPlayer == null || !proxiedPlayer.isConnected()) {
                                TeamSpeakBot.this.asyncApi.sendPrivateMessage(client.getId(),
                                        "Der Spieler " + playerName + " ist nicht online.");
                            } else {
                                sendRequestToPlayer(proxiedPlayer, client);
                                TeamSpeakBot.this.asyncApi.sendPrivateMessage(client.getId(),
                                        "Eine Anfrage zum Verbinden wurde in Minecraft an " + proxiedPlayer.getName()
                                                + " geschickt.");
                            }
                        } else {
                            TeamSpeakBot.this.asyncApi.sendPrivateMessage(client.getId(),
                                    "Diese TeamSpeak-ID ist bereits mit einem Minecraft-Account verbunden");
                        }
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            }

            @Override
            public void onClientJoin(ClientJoinEvent e) {
                TeamSpeakBot.this.clientIdCache.put(e.getClientId(), e.getUniqueClientIdentifier());

                if (e.getUniqueClientIdentifier().equalsIgnoreCase("serveradmin")) {
                    return;
                }

                try {
                    TeamSpeakBot.this.asyncApi.getClientByUId(e.getUniqueClientIdentifier()).onSuccess(client -> {
                        if (client == null) {
                            return;
                        }

                        try {
                            TeamSpeakUserInfo teamSpeakUserInfo =
                                    TeamSpeakBot.this.database.getUserByTSID(client.getUniqueIdentifier());
                            if (teamSpeakUserInfo != null) {
                                TeamSpeakBot.this.userInfoCache.put(e.getUniqueClientIdentifier(), teamSpeakUserInfo);
                                updateTeamSpeakGroup(teamSpeakUserInfo.uuid(), client, null);
                            } else {
                                TeamSpeakBot.this.userInfoCache.remove(client.getUniqueIdentifier());
                                TeamSpeakBot.this.nonLinkedCache.add(client.getUniqueIdentifier());
                                removeAllTeamSpeakGroups(client.getUniqueIdentifier());
                            }

                            boolean hasGroup = false;
                            for (int serverGroup : client.getServerGroups()) {
                                if (TeamSpeakBot.this.groupIDs.containsValue(serverGroup)) {
                                    hasGroup = true;
                                    break;
                                }
                            }
                            if (!hasGroup && !hasIgnoreGroup(client)) {
                                String message = TeamSpeakBot.this.plugin.getConfig().getString("teamspeak.message");
                                TeamSpeakBot.this.asyncApi.sendPrivateMessage(e.getClientId(), message);
                            }
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                } catch (TS3CommandFailedException ex) {
                    int errorID = ex.getError().getId();
                    if (errorID != 512 && errorID != 1540) {
                        TeamSpeakBot.this.plugin.getLogger().log(Level.INFO, "Error by Join User from" + " ("
                                + e.getClientId() + ") " + ex.getError().getMessage() + " " + errorID, e);
                    }
                }
            }

            @Override
            public void onClientLeave(ClientLeaveEvent e) {
                String clientId = TeamSpeakBot.this.clientIdCache.remove(e.getClientId());
                if (clientId.equalsIgnoreCase("serveradmin")) {
                    return;
                }

                TeamSpeakUserInfo info = TeamSpeakBot.this.userInfoCache.remove(clientId);
                if (info == null) {
                    return;
                }

                Long activeTimeLeft = TeamSpeakBot.this.temporaryActiveTime.remove(info.uuid());
                if (activeTimeLeft != null && activeTimeLeft > 0) {
                    try {
                        TeamSpeakBot.this.database.addActiveTime(info.uuid(), activeTimeLeft);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
    }

    public void stopTeamSpeakBot() {
        this.stopping = true;
        synchronized (this.activityLock) {
            if (this.query.isConnected()) {
                this.asyncApi.logout();
                this.query.exit();
            }
            try {
                for (Entry<UUID, Long> entry : this.temporaryActiveTime.entrySet()) {
                    this.database.addActiveTime(entry.getKey(), entry.getValue());
                }
                this.temporaryActiveTime.clear();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                this.database.disconnect();
            }
        }
    }

    private void initActivityControl() {
        this.lastActivityCheck = System.currentTimeMillis();
        this.controlTask = this.plugin.getProxy().getScheduler().schedule(this.plugin, () -> {
            synchronized (this.activityLock) {
                if (this.stopping) {
                    this.controlTask.cancel();
                    this.tidyUserCacheTask.cancel();
                } else {
                    checkClientActivities();
                }
            }
        }, 0, this.activityControlPeriodMs, TimeUnit.MILLISECONDS);

        this.tidyUserCacheTask = this.plugin.getProxy().getScheduler().schedule(this.plugin, () -> {
            if (this.stopping) {
                this.controlTask.cancel();
                this.tidyUserCacheTask.cancel();
            } else {
                tidyUserInfoCache();
            }
        }, 5, 5, TimeUnit.MINUTES);

        try {
            List<Client> clients = this.asyncApi.getClients().get();
            for (Client client : clients) {
                this.clientIdCache.put(client.getId(), client.getUniqueIdentifier());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkClientActivities() {
        long now = System.currentTimeMillis();
        long elapsed = now - this.lastActivityCheck;
        this.lastActivityCheck = now;

        try {
            Set<UUID> cashMachineActiveUsers = new LinkedHashSet<>();
            Set<UUID> lotteryActiveUsers = new LinkedHashSet<>();
            List<Client> clients = this.asyncApi.getClients().get();
            List<Channel> channels = this.asyncApi.getChannels().get();

            Map<Integer, Channel> channelsByIds =
                    channels.stream().collect(Collectors.toMap(Channel::getId, Function.identity()));

            for (Client client : clients) {
                TeamSpeakUserInfo info = getUserInfoCached(client.getUniqueIdentifier());
                if (info == null) {
                    continue;
                }
                Channel channel = channelsByIds.get(client.getChannelId());

                boolean cashMachineActive = !(client.isOutputMuted() || !client.isOutputHardware()
                        || this.activityExcludingChannels.contains(client.getChannelId()));
                boolean lotteryActive = cashMachineActive && !client.isAway() && !channel.hasPassword();

                if (cashMachineActive) {
                    cashMachineActiveUsers.add(info.uuid());
                }
                if (lotteryActive && lotteryActiveUsers.add(info.uuid())) {
                    long newTime = this.temporaryActiveTime.compute(info.uuid(),
                            (id, oldTime) -> oldTime == null ? elapsed : oldTime + elapsed);
                    if (newTime >= 5 * 60 * 1000) {
                        this.database.addActiveTime(info.uuid(), newTime);
                        this.temporaryActiveTime.put(info.uuid(), 0L);
                    }
                }
            }
            UtilsApi.getInstance().setGeneralData(TEAMSPEAK_ACTIVITY_KEY,
                    cashMachineActiveUsers.stream().map(UUID::toString).collect(Collectors.joining(",")));

        } catch (InterruptedException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void tidyUserInfoCache() {
        try {
            this.nonLinkedCache.clear();
            Iterator<TeamSpeakUserInfo> it = this.userInfoCache.values().iterator();
            while (it.hasNext()) {
                TeamSpeakUserInfo info = it.next();
                Client client = this.asyncApi.getClientByUId(info.tsID()).get();
                if (client == null) {
                    it.remove();
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeFromUserInfoCache(String tsId) {
        this.userInfoCache.remove(tsId);
        this.nonLinkedCache.remove(tsId);
    }

    public void userUnlinked(TeamSpeakUserInfo info) {
        removeFromUserInfoCache(info.tsID());
        removeFromUserInfoCache(info.tsID());
    }

    public void updateTeamSpeakGroup(UUID uuid, ClientInfo clientInfo, @Nullable User lpUser) {
        try {
            this.asyncApi.isClientOnline(clientInfo.getId()).onSuccess(isOnline -> {
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
                            this.plugin.getLogger().log(Level.SEVERE, "Error by Loading User " + uuid);
                        }
                    }
                }

                ArrayList<Group> userGroups = new ArrayList<>(user.getInheritedGroups(
                        QueryOptions.builder(QueryMode.NON_CONTEXTUAL).flag(Flag.RESOLVE_INHERITANCE, true).build()));
                userGroups.sort((a, b) -> Integer.compare(b.getWeight().orElse(0), a.getWeight().orElse(0)));
                String group = "default";
                for (Group g : userGroups) {
                    if (this.groupIDs.containsKey(g.getName())) {
                        group = g.getName();
                        break;
                    }
                }
                int groupID = this.groupIDs.get(group);
                int[] ranks = clientInfo.getServerGroups();
                List<Integer> tsUserRanks = Arrays.stream(ranks).boxed().toList();

                for (Integer userRank : tsUserRanks) {
                    if (this.groupIDs.containsValue(userRank) && userRank != groupID) {
                        this.asyncApi.removeClientFromServerGroup(userRank, clientInfo.getDatabaseId());
                    }
                }

                if (!tsUserRanks.contains(groupID)) {
                    this.asyncApi.addClientToServerGroup(groupID, clientInfo.getDatabaseId());
                }
            });
        } catch (TS3CommandFailedException e) {
            int errorID = e.getError().getId();
            if (errorID != 512 && errorID != 1540) {
                this.plugin.getLogger()
                        .log(Level.INFO, "Error by Update User " + uuid.toString() + " ("
                                + clientInfo.getUniqueIdentifier() + ") " + e.getError().getMessage() + " " + errorID,
                                e);
            }
        }

    }

    public void removeAllTeamSpeakGroups(String id) {

        try {
            this.asyncApi.getClientByUId(id).onSuccess(clientInfo -> {
                if (clientInfo == null) {
                    return;
                }

                this.asyncApi.isClientOnline(clientInfo.getId()).onSuccess(isOnline -> {
                    if (!isOnline) {
                        return;
                    }

                    int[] ranks = clientInfo.getServerGroups();
                    List<Integer> tsUserRanks = Arrays.stream(ranks).boxed().toList();

                    for (Integer userRank : tsUserRanks) {
                        if (this.groupIDs.containsValue(userRank)) {
                            this.asyncApi.removeClientFromServerGroup(userRank, clientInfo.getDatabaseId());
                        }
                    }
                });
            });


        } catch (TS3CommandFailedException e) {
            int errorID = e.getError().getId();
            if (errorID != 512 && errorID != 1540) {
                this.plugin.getLogger().log(Level.INFO,
                        "Error by Remove Group from" + " (" + id + ") " + e.getError().getMessage() + " " + errorID, e);
            }
        }
    }

    public void sendRequestToPlayer(ProxiedPlayer proxiedPlayer, ClientInfo client) {
        this.requests.put(proxiedPlayer.getUniqueId(), client.getUniqueIdentifier());
        ChatUtil.sendNormalMessage(proxiedPlayer, "Möchtest du deine Teamspeak ID " + ChatUtil.BLUE
                + client.getNickname() + ChatUtil.GREEN + " mit Minecraft verbinden?");

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
            teamSpeakUserInfo = this.database.getUserByTSIDANDUUID(tsID, player.getUniqueId());
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Error while Loading TeamSpeakUserInfo", e);
        }
        if (teamSpeakUserInfo != null) {
            updateTSDescription(teamSpeakUserInfo, player);
        }
    }

    public void updateTSDescription(TeamSpeakUserInfo teamSpeakUserInfo, ProxiedPlayer player) {
        if (teamSpeakUserInfo != null) {
            this.asyncApi.isClientOnline(teamSpeakUserInfo.tsID()).onSuccess(isOnline -> {
                if (!isOnline) {
                    return;
                }
                String lastName = teamSpeakUserInfo.latestName();

                try {
                    this.asyncApi.getClientByUId(teamSpeakUserInfo.tsID()).onSuccess(clientInfo -> {
                        if (clientInfo != null) {
                            String description = clientInfo.getDescription();
                            if (lastName != null) {
                                if (lastName.equals(player.getName())) {
                                    if (!description.contains(player.getName())) {
                                        description = player.getName()
                                                + ((description.isEmpty() || description.isBlank()) ? ""
                                                        : " | " + description);
                                        setDescription(clientInfo, description);
                                    }
                                } else {
                                    description = description.replace(lastName, player.getName());
                                    setDescription(clientInfo, description);
                                    try {
                                        this.database.updateMcName(teamSpeakUserInfo.uuid(), teamSpeakUserInfo.tsID(),
                                                player.getName());
                                    } catch (SQLException ex) {
                                        this.plugin.getLogger().log(Level.SEVERE, "Error while Update McName", ex);
                                    }
                                }
                            } else {
                                if (!description.contains(player.getName())) {
                                    description =
                                            player.getName() + ((description.isEmpty() || description.isBlank()) ? ""
                                                    : " | " + description);
                                    setDescription(clientInfo, description);
                                }
                                try {
                                    this.database.updateMcName(teamSpeakUserInfo.uuid(), teamSpeakUserInfo.tsID(),
                                            player.getName());
                                } catch (SQLException ex) {
                                    this.plugin.getLogger().log(Level.SEVERE, "Error while Update McName", ex);
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
        this.asyncApi.editClient(clientInfo.getId(), Collections.singletonMap(ClientProperty.CLIENT_DESCRIPTION,
                (description.length() > 200 ? description.substring(0, 200) : description)));
    }

    public UUID drawLottery() {
        synchronized (this.activityLock) {
            try {
                Map<UUID, Long> activeTimes = this.database.getActiveTimes();
                Map<UUID, Integer> ticketsByUser = new LinkedHashMap<>(activeTimes.size());
                for (Entry<UUID, Long> entry : this.temporaryActiveTime.entrySet()) {
                    activeTimes.merge(entry.getKey(), entry.getValue(), (l1, l2) -> l1 + l2);
                }

                for (Entry<UUID, Long> entry : activeTimes.entrySet()) {
                    int tickets =
                            (int) Math.min(entry.getValue() / this.timePerLotteryTicket, this.maxLotteryTicketsByTime);
                    ticketsByUser.put(entry.getKey(), tickets);
                }

                System.out.println("Tickets from time:" + ticketsByUser);

                for (Client client : this.asyncApi.getClients().get()) {
                    TeamSpeakUserInfo info = getUserInfoCached(client.getUniqueIdentifier());
                    if (info == null) {
                        continue;
                    }
                    if (this.lotteryChannels.contains(client.getChannelId())) {
                        ticketsByUser.compute(info.uuid(), (id, tickets) -> tickets == null ? this.lotteryChannelTickets
                                : tickets + this.lotteryChannelTickets);
                    }
                }

                System.out.println("Tickets total:" + ticketsByUser);

                int totalTickets = ticketsByUser.values().stream().mapToInt(Integer::intValue).sum();
                if (totalTickets == 0) {
                    return null;
                }

                int winnerTicket = RandomUtil.SHARED_SECURE_RANDOM.nextInt(totalTickets);
                int ticketNr = 0;
                for (Entry<UUID, Integer> user : ticketsByUser.entrySet()) {
                    ticketNr += user.getValue();
                    if (ticketNr > winnerTicket) {
                        return user.getKey();
                    }
                }
                throw new RuntimeException("winning ticket not found, should not happen");
            } catch (InterruptedException | SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void resetLottery() {
        synchronized (this.activityLock) {
            try {
                this.temporaryActiveTime.clear();
                this.database.clearActiveTimes();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private TeamSpeakUserInfo getUserInfoCached(String tsId) {
        return this.userInfoCache.computeIfAbsent(tsId, id -> {
            if (this.nonLinkedCache.contains(id)) {
                return null;
            }
            try {
                TeamSpeakUserInfo result = this.database.getUserByTSID(id);
                if (result == null) {
                    this.nonLinkedCache.add(id);
                }
                return result;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Pair<Long, Integer> getLotteryTimeAndTickets(UUID playerId) {
        try {
            synchronized (this.activityLock) {
                long time = getDatabase().getActiveTime(playerId) + this.temporaryActiveTime.getOrDefault(playerId, 0L);
                int tickets = (int) Math.min(time / this.timePerLotteryTicket, this.maxLotteryTicketsByTime);
                return new Pair<>(time, tickets);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public TeamSpeakDatabase getDatabase() {
        return this.database;
    }

    public ConcurrentHashMap<UUID, String> getRequests() {
        return this.requests;
    }

    public TS3ApiAsync getAsyncApi() {
        return this.asyncApi;
    }

    public Integer getNewbieGroup() {
        return this.newbieGroup;
    }

    public boolean hasIgnoreGroup(ClientInfo clientInfo) {
        boolean hasIgnoreGroup = false;
        for (int serverGroup : clientInfo.getServerGroups()) {
            if (this.ignoreGroups.contains(serverGroup)) {
                return true;
            }
        }

        return hasIgnoreGroup;
    }

    public TeDiSync getPlugin() {
        return this.plugin;
    }
}
