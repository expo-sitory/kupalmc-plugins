package dev.ixpu.advancementsync.command;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import dev.ixpu.advancementsync.AdvancementSync;
import dev.ixpu.advancementsync.config.ConfigManager;
import dev.ixpu.advancementsync.database.DatabaseManager;
import dev.ixpu.advancementsync.util.Logger;

public class AdvancementSyncCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final ConfigManager configManager;
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AdvancementSyncCommand(JavaPlugin plugin, DatabaseManager databaseManager, Logger logger, ConfigManager configManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.logger = logger;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only allow OP players to use this command
        if (!sender.isOp()) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        return switch (subcommand) {
            case "reset" -> handleReset(sender, args);
            case "rollback" -> handleRollback(sender, args);
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            case "help" -> {
                sendHelpMessage(sender);
                yield true;
            }
            default -> {
                sender.sendMessage(String.format("§cUnknown subcommand: %s", subcommand));
                sendHelpMessage(sender);
                yield true;
            }
        };
    }

    // Handle /advsync reset <player>

    private boolean handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /advsync reset <player>");
            return true;
        }

        String playerName = args[1];
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);
        String playerUUID = targetPlayer.getUniqueId().toString();

        // Execute reset asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(
            plugin,
            () -> {
                Player onlinePlayer = Bukkit.getPlayer(targetPlayer.getUniqueId());
                if (onlinePlayer != null) {
                    databaseManager.resetAdvancements(playerUUID);
                    sender.sendMessage(String.format("§aSuccessfully reset all advancements for player %s", playerName));
                    onlinePlayer.sendMessage(String.format("§eYour advancements have been reset by %s", sender.getName()));
                } else {
                    sender.sendMessage(String.format("§cPlayer %s is not online. Cannot reset advancements.", playerName));
                }
            }
        );

        return true;
    }

    // Handle /advsync rollback <player> <time>
    // Time format: 1h, 30m, 1d, etc.

    private boolean handleRollback(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /advsync rollback <player> <time>");
            sender.sendMessage("§cTime format: 1h, 30m, 1d, etc.");
            return true;
        }

        String playerName = args[1];
        String timeStr = args[2];

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);

        // Parse time
        long timeMs = parseTime(timeStr);
        if (timeMs < 0) {
            sender.sendMessage(String.format("§cInvalid time format: %s", timeStr));
            sender.sendMessage("§cValid formats: 1h, 30m, 1d, etc.");
            return true;
        }

        long rollbackTimestamp = System.currentTimeMillis() - timeMs;
        String playerUUID = targetPlayer.getUniqueId().toString();

        // Execute rollback asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(
            plugin,
            () -> {
                Player onlinePlayer = Bukkit.getPlayer(targetPlayer.getUniqueId());
                if (onlinePlayer != null) {
                    databaseManager.rollbackAdvancements(playerUUID, rollbackTimestamp);
                    
                    LocalDateTime rollbackTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(rollbackTimestamp),
                        ZoneId.systemDefault()
                    );
                    
                    sender.sendMessage(String.format("§aRolled back advancements for %s to %s", 
                        playerName, rollbackTime.format(timeFormat)));
                    onlinePlayer.sendMessage(String.format("§eYour advancements have been rolled back by %s", sender.getName()));
                } else {
                    sender.sendMessage(String.format("§cPlayer %s is not online. Cannot rollback advancements.", playerName));
                }
            }
        );

        return true;
    }

    // Handle /advsync status

    private boolean handleStatus(CommandSender sender) {
        String separator = "§8" + "═".repeat(35);
        String online = String.valueOf(Bukkit.getOnlinePlayers().size());
        
        sender.sendMessage("");
        sender.sendMessage(separator);
        sender.sendMessage("§b§l  ADVANCEMENT SYNC STATUS");
        sender.sendMessage(separator);
        
        // Plugin Status
        sender.sendMessage("§7Plugin Status: §a✓ ENABLED");
        sender.sendMessage(String.format("§7Redis Connection: %s", 
            databaseManager.getRedisManager().isConnected() ? "§a✓ CONNECTED" : "§c✗ DISCONNECTED"));
        sender.sendMessage(String.format("§7MySQL Connection: %s", 
            databaseManager.getMySQLManager().testConnection() ? "§a✓ CONNECTED" : "§c✗ DISCONNECTED"));
        sender.sendMessage("");
        
        // Database Configuration
        sender.sendMessage("§bDatabase Configuration:");
        sender.sendMessage(String.format("§7  • MySQL Host: §f%s:%d", configManager.getMysqlHost(), configManager.getMysqlPort()));
        sender.sendMessage(String.format("§7  • Database: §f%s", configManager.getMysqlDatabase()));
        sender.sendMessage(String.format("§7  • Pool Size: §f%d", configManager.getMysqlPoolSize()));
        sender.sendMessage("");
        
        // Redis Configuration
        sender.sendMessage("§bRedis Configuration:");
        sender.sendMessage(String.format("§7  • Redis Host: §f%s:%d", configManager.getRedisHost(), configManager.getRedisPort()));
        sender.sendMessage(String.format("§7  • Redis Port: §f%d", configManager.getRedisPort()));
        sender.sendMessage("");

        
        // Server Stats
        sender.sendMessage("§bServer Info:");
        sender.sendMessage(String.format("§7  • Online Players: §f%s", online));
        sender.sendMessage("");
        sender.sendMessage("Plugin by Ixpu | https://github.com/expo-sitory");
        sender.sendMessage(separator);
        sender.sendMessage("");
        
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        try {
            sender.sendMessage("§eReloading AdvancementSync configuration...");
            configManager.loadConfig();
            sender.sendMessage("§aConfiguration reloaded successfully!");
            logger.info(String.format("Configuration reloaded by %s", sender.getName()));
            
            // Display updated configuration
            AdvancementSync advancementSync = (AdvancementSync) plugin;
            if (advancementSync.getConfigDisplay() != null) {
                advancementSync.getConfigDisplay().displayConfiguration();
            }
            
            return true;
        } catch (IOException e) {
            sender.sendMessage(String.format("§cFailed to reload configuration: %s", e.getMessage()));
            logger.error("Error reloading configuration", e);
            return true;
        }
    }

    // Parse time string to milliseconds
    // Supports: 1s, 30m, 1h, 1d, etc.

    private long parseTime(String timeStr) {
        try {
            if (timeStr.endsWith("ms")) {
                return Long.parseLong(timeStr.substring(0, timeStr.length() - 2));
            } else if (timeStr.endsWith("s")) {
                return Long.parseLong(timeStr.substring(0, timeStr.length() - 1)) * 1000;
            } else if (timeStr.endsWith("m")) {
                return Long.parseLong(timeStr.substring(0, timeStr.length() - 1)) * 60 * 1000;
            } else if (timeStr.endsWith("h")) {
                return Long.parseLong(timeStr.substring(0, timeStr.length() - 1)) * 60 * 60 * 1000;
            } else if (timeStr.endsWith("d")) {
                return Long.parseLong(timeStr.substring(0, timeStr.length() - 1)) * 24 * 60 * 60 * 1000;
            } else {
                return -1;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Send help message

    private void sendHelpMessage(CommandSender sender) {
        String separator = "§8" + "═".repeat(35);
 
        sender.sendMessage("");
        sender.sendMessage(separator);
        sender.sendMessage("§b§l  AdvancementSync Commands");
        sender.sendMessage(separator);
        sender.sendMessage("§b/advsync reset <player> - §fReset all advancements for a player");
        sender.sendMessage("§b/advsync rollback <player> <time> - §fRollback advancements to a specific time");
        sender.sendMessage("§b/advsync reload - §fReload feature configuration without restarting");
        sender.sendMessage("§b/advsync status - §fCheck plugin database connection status");
        sender.sendMessage("");
        sender.sendMessage("§bTime format: 1s, 30m, 1h, 1d");
        sender.sendMessage(separator);
    }
}