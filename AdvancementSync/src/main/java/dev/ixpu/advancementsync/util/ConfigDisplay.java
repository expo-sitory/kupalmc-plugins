package dev.ixpu.advancementsync.util;

import org.bukkit.plugin.java.JavaPlugin;

import dev.ixpu.advancementsync.config.ConfigManager;

public class ConfigDisplay {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Logger logger;

    public ConfigDisplay(JavaPlugin plugin, ConfigManager configManager, Logger logger) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logger = logger;
    }

    public void displayConfiguration() {
        String separator = "═══════════════════════════════════";
        String version = plugin.getPluginMeta().getVersion();
        
        logger.info(separator);
        logger.info("    ADVANCEMENTSYNC CONFIG");
        logger.info(separator);
        
        // Database Configuration
        logger.info("Database Configuration:");
        logger.info(String.format("    MySQL Host: %s:%d", 
            configManager.getMysqlHost(), 
            configManager.getMysqlPort()));
        logger.info(String.format("    Database: %s", configManager.getMysqlDatabase()));
        logger.info(String.format("    Connection Pool Size: %d", configManager.getMysqlPoolSize()));
        
        // Redis Configuration
        logger.info("Redis Configuration:");
        logger.info(String.format("    Redis Host: %s:%d", 
            configManager.getRedisHost(), 
            configManager.getRedisPort()));
        logger.info(String.format("    Redis SSL: %s", configManager.isRedisSsl()));
        
        // Toggles
        logger.info("Toggles:");
        logger.info(String.format("    Log Level: %s", configManager.getLogLevel()));
        logger.info(String.format("    Debug Logs: %s", configManager.isDebugLoggingEnabled()));
        logger.info(String.format("    Verbose Logs: %s", configManager.isVerboseLoggingEnabled()));
        
        // Features
        logger.info("Features:");
        logger.info(String.format("    Sync on Unlock: %s", configManager.isSyncOnUnlock()));
        logger.info(String.format("    Sync on Disconnect: %s", configManager.isSyncOnDisconnect()));
        logger.info(String.format("    Load on Pre-Login: %s", configManager.isLoadOnPrelogin()));
        
        // Plugin Info
        logger.info("Plugin Info:");
        logger.info(String.format("    Version: %s", version));
        logger.info("    Plugin by Ixpu | https://github.com/expo-sitory");
        
        logger.info(separator);
        logger.info("");
    }
}