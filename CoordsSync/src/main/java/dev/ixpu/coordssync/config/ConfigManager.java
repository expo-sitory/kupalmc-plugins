package dev.ixpu.coordssync.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    
    private String redisHost;
    private int redisPort;
    private String redisPassword;
    private String serverId;
    private boolean syncAlertsEnabled;
    private boolean teleportOnJoin;
    private boolean periodicSaveEnabled;
    
    private String dbHost;
    private int dbPort;
    private String dbName;
    private String dbUsername;
    private String dbPassword;
    private int maxConnections;
    private int minIdle;
    private int connectionTimeout;
    private boolean useMySQLFallback;
    private boolean databaseEnabled;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        // Create plugins folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");

        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            try {
                InputStream defaultConfig = plugin.getResource("config.yml");
                if (defaultConfig != null) {
                    Files.copy(defaultConfig, configFile.toPath());
                } else {
                    createDefaultConfig();
                }
            } catch (IOException e) {
                plugin.getLogger().severe(String.format("Could not create config file: %s", e.getMessage()));
                createDefaultConfig();
            }
        }

        // Load the config
        config = YamlConfiguration.loadConfiguration(configFile);

        // Load Redis configuration
        this.redisHost = config.getString("redis.host", "localhost");
        this.redisPort = config.getInt("redis.port", 6379);
        this.redisPassword = config.getString("redis.password", "");
        
        // Load server identification
        this.serverId = config.getString("server-id", "server-1");
        
        // Load feature flags
        this.syncAlertsEnabled = config.getBoolean("features.sync-alerts", true);
        this.teleportOnJoin = config.getBoolean("features.teleport-on-join", true);
        this.periodicSaveEnabled = config.getBoolean("features.periodic-save", true);

        // Log Redis and general configuration
        plugin.getLogger().info("══════════════════════════════════════════════════");
        plugin.getLogger().info("                  COORDSSYNC CONFIG");
        plugin.getLogger().info("══════════════════════════════════════════════════");         
        loadDatabaseConfig();
        plugin.getLogger().info("Redis Configuration:");
        plugin.getLogger().info(String.format("    Redis Host: %s", redisHost));
        plugin.getLogger().info(String.format("    Redis Port: %d", redisPort));
        plugin.getLogger().info("Server Configuration:");
        plugin.getLogger().info(String.format("    Server ID: %s", serverId));
        plugin.getLogger().info("Feature Flags:");
        plugin.getLogger().info(String.format("    Teleport on Join: %s", teleportOnJoin ? "ENABLED" : "DISABLED"));
        plugin.getLogger().info(String.format("    Periodic Save: %s", periodicSaveEnabled ? "ENABLED" : "DISABLED"));
        plugin.getLogger().info(String.format("    Sync Alerts: %s", syncAlertsEnabled ? "ENABLED" : "DISABLED"));
        plugin.getLogger().info("══════════════════════════════════════════════════");
    }

    public void loadDatabaseConfig() {
        FileConfiguration dbConfig = YamlConfiguration.loadConfiguration(configFile);
        
        // Load database settings
        dbHost = dbConfig.getString("database.host", "localhost");
        dbPort = dbConfig.getInt("database.port", 3306);
        dbName = dbConfig.getString("database.database", "coordssync");
        dbUsername = dbConfig.getString("database.username", "root");
        dbPassword = dbConfig.getString("database.password", "");
        databaseEnabled = dbConfig.getBoolean("database.enabled", false);
        
        // Load connection pool settings
        maxConnections = dbConfig.getInt("database.pool.max-connections", 10);
        minIdle = dbConfig.getInt("database.pool.min-idle", 2);
        connectionTimeout = dbConfig.getInt("database.pool.connection-timeout", 5000);
        
        // Load MySQL fallback feature flag
        useMySQLFallback = dbConfig.getBoolean("features.use-mysql-fallback", true);
        
        // Log database configuration
        plugin.getLogger().info("Database Configuration:");
        plugin.getLogger().info(String.format("    MySQL Host: %s", dbHost));
        plugin.getLogger().info(String.format("    MySQL Port: %d", dbPort));
        plugin.getLogger().info(String.format("    Database Name: %s", dbName));
        plugin.getLogger().info(String.format("    Database Enabled: %s", databaseEnabled ? "YES" : "NO"));
        plugin.getLogger().info(String.format("    MySQL Fallback: %s", useMySQLFallback ? "ENABLED" : "DISABLED"));
        plugin.getLogger().info(String.format("    Connection Pool - Max: %d, Min Idle: %d", maxConnections, minIdle));
    }

    public void reloadConfig() {
        plugin.getLogger().info("[ConfigManager] Reloading configuration...");
        loadConfig();
    }

    private void createDefaultConfig() {
        try {
            String defaultContent = 
                """
                # CoordsSync Configuration
                # Multi-server coordinate synchronization plugin
                
                # Redis connection settings
                redis:
                  # Redis server host
                  host: localhost
                  # Redis server port
                  port: 6379
                  # Redis password (leave empty if no password)
                  password: ""
                
                # Unique identifier for this server
                # Used to identify which server a player is on
                server-id: server-1
                
                # Features
                features:
                  # Teleport player to last location when joining
                  teleport-on-join: true
                  # Save positions periodically
                  periodic-save: true
                  # Log [Sync] alerts when receiving coordinate updates from other servers
                  sync-alerts: true
                  # Enable MySQL database as fallback when Redis is unavailable
                  use-mysql-fallback: true
                
                # Database configuration
                database:
                  enabled: false
                  host: localhost
                  port: 3306
                  database: coordssync
                  username: root
                  password: ""
                  pool:
                    max-connections: 10
                    min-idle: 2
                    connection-timeout: 5000
                """;

            Files.write(configFile.toPath(), defaultContent.getBytes());
            plugin.getLogger().info("[ConfigManager] Created default config.yml");
        } catch (IOException e) {
            plugin.getLogger().severe(String.format("[ConfigManager] Could not create default config: %s", e.getMessage()));
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
            plugin.getLogger().info("[ConfigManager] Configuration saved successfully");
        } catch (IOException e) {
            plugin.getLogger().severe(String.format("[ConfigManager] Could not save config: %s", e.getMessage()));
        }
    }
    
    // ==================== Redis Configuration Getters ====================
    
    public String getRedisHost() {
        return redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public String getRedisPassword() {
        return redisPassword;
    }

    // ==================== Server Configuration Getters ====================
    
    public String getServerId() {
        return serverId;
    }

    // ==================== Feature Flag Getters ====================
    
    public boolean isTeleportOnJoin() {
        return teleportOnJoin;
    }

    public boolean isPeriodicSaveEnabled() {
        return periodicSaveEnabled;
    }

    public boolean isSyncAlertsEnabled() {
        return syncAlertsEnabled;
    }

    // ==================== Database Configuration Getters ====================
    
    public String getDbHost() { 
        return dbHost; 
    }
    
    public int getDbPort() { 
        return dbPort; 
    }
    
    public String getDbName() { 
        return dbName; 
    }
    
    public String getDbUsername() { 
        return dbUsername; 
    }
    
    public String getDbPassword() { 
        return dbPassword; 
    }
    
    public boolean isDatabaseEnabled() {
        return databaseEnabled;
    }
    
    public int getMaxConnections() { 
        return maxConnections; 
    }
    
    public int getMinIdle() { 
        return minIdle; 
    }
    
    public int getConnectionTimeout() { 
        return connectionTimeout; 
    }
    
    public boolean isUseMySQLFallback() { 
        return useMySQLFallback; 
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
}