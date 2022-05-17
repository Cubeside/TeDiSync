package de.fanta.tedisync;

import com.github.theholywaffle.teamspeak3.TS3ApiAsync;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.event.ChannelCreateEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDeletedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelDescriptionEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ChannelPasswordChangedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent;
import com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent;
import com.github.theholywaffle.teamspeak3.api.event.PrivilegeKeyUsedEvent;
import com.github.theholywaffle.teamspeak3.api.event.ServerEditedEvent;
import com.github.theholywaffle.teamspeak3.api.event.TS3Listener;
import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;
import de.fanta.tedisync.utils.ChatUtil;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public record TeamSpeakTest(TeDiSync plugin) {
    public void initTeamSpeakBot() {
        final TS3Config config = new TS3Config();
        config.setHost("127.0.0.1");
        config.setEnableCommunicationsLogging(true);

        final TS3Query query = new TS3Query(config);
        query.connect();

        final TS3ApiAsync asyncApi = query.getAsyncApi();
        asyncApi.login("test", "QDjzS+3p");
        asyncApi.selectVirtualServerById(1);
        asyncApi.setNickname("fanta's test Query");

        asyncApi.registerAllEvents();
        asyncApi.addTS3Listeners(new TS3Listener() {

            @Override
            public void onTextMessage(TextMessageEvent e) {
                String message = e.getMessage();
                asyncApi.sendPrivateMessage(e.getTargetClientId(), message);
            }

            @Override
            public void onClientJoin(ClientJoinEvent e) {
                for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                    if (player.getName().equalsIgnoreCase(e.getClientNickname())) {
                        ChatUtil.sendNormalMessage(player, "Jaaa Mooooin");
                        asyncApi.addClientToServerGroup(14, e.getClientDatabaseId());
                        asyncApi.sendPrivateMessage(e.getClientDatabaseId(), "Ja hör mal");
                    }
                }
            }

            @Override
            public void onClientLeave(ClientLeaveEvent e) {

            }

            @Override
            public void onServerEdit(ServerEditedEvent e) {

            }

            @Override
            public void onChannelEdit(ChannelEditedEvent e) {

            }

            @Override
            public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {

            }

            @Override
            public void onClientMoved(ClientMovedEvent e) {

            }

            @Override
            public void onChannelCreate(ChannelCreateEvent e) {

            }

            @Override
            public void onChannelDeleted(ChannelDeletedEvent e) {

            }

            @Override
            public void onChannelMoved(ChannelMovedEvent e) {

            }

            @Override
            public void onChannelPasswordChanged(ChannelPasswordChangedEvent e) {

            }

            @Override
            public void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent e) {

            }

        });
    }
}
