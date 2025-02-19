package de.fanta.tedisync.utils;

import de.fanta.tedisync.TeDiSync;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class ChatUtil {

    public static final ChatColor GREEN = ChatColor.of("#52ff9d");
    public static final ChatColor ORANGE = ChatColor.of("#ffac4d");
    public static final ChatColor RED = ChatColor.of("#ff6b6b");
    public static final ChatColor BLUE = ChatColor.of("#87f7ea");

    private ChatUtil() {
        // prevent instances
    }

    public static void sendMessage(CommandSender sender, String colors, String message) {
        sender.sendMessage(TextComponent.fromLegacy(TeDiSync.PREFIX + " " + colors + message));
    }

    public static void sendComponent(CommandSender sender, BaseComponent message) {
        TextComponent prefixComponent = new TextComponent();
        prefixComponent.addExtra(new TextComponent(TextComponent.fromLegacy(TeDiSync.PREFIX + " ")));
        prefixComponent.addExtra(message);
        sender.sendMessage(prefixComponent);
    }

    public static void sendNormalMessage(CommandSender sender, String message) {
        sendMessage(sender, GREEN.toString(), message);
    }

    public static void sendWarningMessage(CommandSender sender, String message) {
        sendMessage(sender, ORANGE.toString(), message);
    }

    public static void sendErrorMessage(CommandSender sender, String message) {
        sendMessage(sender, RED.toString(), message);
    }

    public static void sendDebugMessage(CommandSender sender, String message) {
        if (sender != null && sender.hasPermission("fanta.debug")) {
            sendMessage(sender, ChatColor.of("#FF04F7").toString(), message);
        }
    }

    public static void sendBrodcastMessage(String message) {
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            sendMessage(player, GREEN.toString(), message);
        }
    }
}
