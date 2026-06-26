package dev.ixpu.coordssync.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import dev.ixpu.coordssync.CoordsSync;
import dev.ixpu.coordssync.config.ConfigManager;
import dev.ixpu.coordssync.database.DatabaseManager;

public class CommandManager implements CommandExecutor {
    
    private final CoordsSync plugin;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;

    public CommandManager(CoordsSync plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("coordssync")) {
            return false;
        }

        // Check if player has permission
        if (!sender.hasPermission("coordssync.admin")) {
            sender.sendMessage("§c✗ You don't have permission to use this command!");
            return true;
        }

        // Handle no arguments
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        return switch (subcommand) {
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleReload(CommandSender sender) {
        try {
            sender.sendMessage("§e⟳ Reloading CoordsSync configuration...");
            
            // Reload configuration
            configManager.reloadConfig();
            
            // Reload database connections
            databaseManager.reload();
            
            sender.sendMessage("§a✓ Configuration reloaded successfully!");
            
            return true;
        } catch (Exception e) {
            sender.sendMessage("§c✗ Failed to reload configuration: " + e.getMessage());
            plugin.getLogger().warning(String.format("Error reloading config: %s", e.getMessage()));
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Exception:", e);
            return true;
        }
    }

    private boolean handleStatus(CommandSender sender) {
        try {
            String separator = "§8" + "═".repeat(35);
            
            sender.sendMessage("");
            sender.sendMessage(separator);
            sender.sendMessage("§b§l    COORDS SYNC STATUS");
            sender.sendMessage(separator);
            
            // Plugin status
            boolean pluginEnabled = plugin.isEnabled();
            sender.sendMessage("§7Plugin Status: " + (pluginEnabled ? "§a✓ ENABLED" : "§c✗ DISABLED"));
            
            // Redis connection status
            boolean redisConnected = databaseManager.isRedisAvailable();
            sender.sendMessage("§7Redis Connection: " + (redisConnected ? "§a✓ CONNECTED" : "§c✗ DISCONNECTED"));
            
            // MySQL connection status
            boolean mysqlConnected = databaseManager.isMysqlAvailable();
            sender.sendMessage("§7MySQL Connection: " + (mysqlConnected ? "§a✓ CONNECTED" : "§c✗ DISCONNECTED"));
            
            // Configuration details
            sender.sendMessage("§7");
            sender.sendMessage("§bRedis Configuration:");
            sender.sendMessage("§7  • Server ID: §f" + configManager.getServerId());
            sender.sendMessage("§7  • Redis Host: §f" + configManager.getRedisHost());
            sender.sendMessage("§7  • Redis Port: §f" + configManager.getRedisPort());
            
            // Online players
            sender.sendMessage("§7");
            sender.sendMessage("§bServer Info:");
            sender.sendMessage("§7  • Online Players: §f" + plugin.getServer().getOnlinePlayers().size());
            sender.sendMessage("");
            sender.sendMessage("Plugin by Ixpu | https://github.com/expo-sitory");
            sender.sendMessage(separator);
            
            return true;
        } catch (Exception e) {
            sender.sendMessage("§c✗ Failed to retrieve status: " + e.getMessage());
            plugin.getLogger().warning(String.format("Error retrieving status: %s", e.getMessage()));
            return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        String separator = "§8" + "═".repeat(35);

        sender.sendMessage(separator);
        sender.sendMessage("§b§l      CoordsSync Commands");
        sender.sendMessage(separator);
        sender.sendMessage("§b/coordssync reload §f- Reload configuration from config.yml");
        sender.sendMessage("§b/coordssync status §f- Show plugin database connection status");
        sender.sendMessage(separator);
    }
}