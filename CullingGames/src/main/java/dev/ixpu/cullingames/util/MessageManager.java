package dev.ixpu.cullingames.util;

import dev.ixpu.cullingames.CullingGamesPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public class MessageManager {

    private final CullingGamesPlugin plugin;
    private String messagePrefix;

    // Font conversion map for small caps style
    private static final String NORMAL = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘqʀsᴛᴜᴠᴡxʏᴢᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘqʀsᴛᴜᴠᴡxʏᴢ";

    public MessageManager(CullingGamesPlugin plugin) {
        this.plugin = plugin;
        loadPrefix();
    }

    public void loadPrefix() {
        messagePrefix = plugin.getConfigManager().getConfig().getString(
            "messages.prefix",
            "&d&lᴋᴜᴘᴀʟᴍᴄ &r&7- &fᴄᴜʟʟɪɴɢ ɢᴀᴍᴇꜱ &7| &r "
        );
    }

    // Convert text to small caps font style

    public String convertFont(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            int index = NORMAL.indexOf(c);
            if (index >= 0) {
                result.append(SMALL_CAPS.charAt(index));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    // Format a message with color codes and prefix

    private String formatMessage(String message) {
        return convertColorCodes(messagePrefix) + convertFont(message);
    }

    // Convert & color codes to actual colors

    private String convertColorCodes(String message) {
        return message.replace("&r", "")
            .replace("&0", "")
            .replace("&1", "")
            .replace("&2", "")
            .replace("&3", "")
            .replace("&4", "")
            .replace("&5", "")
            .replace("&6", "")
            .replace("&7", "")
            .replace("&8", "")
            .replace("&9", "")
            .replace("&a", "")
            .replace("&b", "")
            .replace("&c", "")
            .replace("&d", "")
            .replace("&e", "")
            .replace("&f", "")
            .replace("&l", "")
            .replace("&n", "")
            .replace("&m", "")
            .replace("&o", "")
            .replace("&k", "");
    }

    // Send a message with prefix and font conversion

    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(Component.text(formatMessage(message)));
    }

    // Broadcast a message with prefix and font conversion

    public void broadcast(String message) {
        org.bukkit.Bukkit.broadcast(Component.text(formatMessage(message)));
    }

    // Send a raw message (for broadcasts without prefix - used internally)

    public void sendRawMessage(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message));
    }

    // Get the current prefix

    public String getPrefix() {
        return messagePrefix;
    }

    // Reload prefix from config

    public void reload() {
        loadPrefix();
    }
}
