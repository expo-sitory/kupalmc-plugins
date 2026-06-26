package dev.ixpu.advancementsync;

import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import dev.ixpu.advancementsync.command.AdvancementSyncCommand;
import dev.ixpu.advancementsync.config.ConfigManager;
import dev.ixpu.advancementsync.database.DatabaseManager;
import dev.ixpu.advancementsync.database.MySQLManager;
import dev.ixpu.advancementsync.database.RedisManager;
import dev.ixpu.advancementsync.listener.AdvancementListener;
import dev.ixpu.advancementsync.listener.PlayerListener;
import dev.ixpu.advancementsync.util.ConfigDisplay;
import dev.ixpu.advancementsync.util.Logger;

public class AdvancementSync extends JavaPlugin {

    private static AdvancementSync instance;
    private ConfigManager configManager;
    private RedisManager redisManager;
    private MySQLManager mySQLManager;
    private DatabaseManager databaseManager;
    private Logger logger;
    private ConfigDisplay configDisplay;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize logger
        this.logger = new Logger(this);
        logger.info("=================================");
        logger.info("AdvancementSync v" + getPluginMeta().getVersion());
        logger.info("Initializing advancement synchronization...");
        logger.info("=================================");

        // Create plugin directory
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Load configuration
        try {
            this.configManager = new ConfigManager(this);
            configManager.loadConfig();
            logger.info("Configuration loaded successfully");
            
            // Initialize config display
            this.configDisplay = new ConfigDisplay(this, configManager, logger);
            configDisplay.displayConfiguration();
        } catch (IOException | RuntimeException e) {
            logger.error("Failed to load configuration", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize databases
        try {
            this.redisManager = new RedisManager(configManager);
            this.mySQLManager = new MySQLManager(configManager);
            this.databaseManager = new DatabaseManager(redisManager, mySQLManager, configManager);
            
            logger.info("Redis connection pool initialized");
            logger.info("MySQL connection pool initialized");
        } catch (SQLException | RuntimeException e) {
            logger.error("Failed to initialize database connections", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register event listeners
        try {
            getServer().getPluginManager().registerEvents(
                new AdvancementListener(databaseManager, logger, configManager), 
                this
            );
            getServer().getPluginManager().registerEvents(
                new PlayerListener(this, databaseManager, logger, configManager), 
                this
            );
            logger.info("Event listeners registered");
        } catch (Exception e) {
            logger.error("Failed to register event listeners", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        try {
            PluginCommand advSyncCommand = getCommand("advsync");
            if (advSyncCommand == null) {
                logger.error("Failed to register commands: 'advsync' command is not defined in plugin.yml");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            advSyncCommand.setExecutor(new AdvancementSyncCommand(this, databaseManager, logger, configManager));
            logger.info("Commands registered");
        } catch (Exception e) {
            logger.error("Failed to register commands", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        logger.info("=================================");
        logger.info("AdvancementSync enabled successfully!");
        logger.info("=================================");
    }

    @Override
    public void onDisable() {
        logger.info("=================================");
        logger.info("AdvancementSync shutting down...");
        logger.info("=================================");

        try {
            if (redisManager != null) {
                redisManager.shutdown();
                logger.info("Redis connection pool closed");
            }
            if (mySQLManager != null) {
                mySQLManager.shutdown();
                logger.info("MySQL connection pool closed");
            }
        } catch (Exception e) {
            logger.error("Error during shutdown", e);
        }

        logger.info("AdvancementSync disabled");
    }

    // Getters
    public static AdvancementSync getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public MySQLManager getMySQLManager() {
        return mySQLManager;
    }

    public Logger getLoggerUtil() {
        return logger;
    }

    public ConfigDisplay getConfigDisplay() {
        return configDisplay;
    }
}