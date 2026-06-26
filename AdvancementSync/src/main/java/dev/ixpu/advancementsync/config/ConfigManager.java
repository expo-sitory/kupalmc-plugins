package dev.ixpu.advancementsync.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private final File configFile;

    // Database configurations
    private String redisHost;
    private int redisPort;
    private String redisPassword;
    private boolean redisSsl;
    private int redisTimeoutMs;

    private String mysqlHost;
    private int mysqlPort;
    private String mysqlUsername;
    private String mysqlPassword;
    private String mysqlDatabase;
    private int mysqlPoolSize;
    private String mysqlPoolName;
    private int mysqlConnectionTimeoutMs;
    private int mysqlIdleTimeoutMs;
    private int mysqlMaxLifetimeMs;

    // Sync configurations
    private int asyncTimeoutMs;
    private boolean enableDebugLogging;
    private boolean verboseLogging;

    // Features
    private boolean syncOnUnlock;
    private boolean syncOnDisconnect;
    private boolean loadOnPrelogin;

    // Logging
    private String logFile;
    private String logLevel;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void loadConfig() throws IOException {
        // Create config from default if it doesn't exist
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            createDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        // Load database configurations
        redisHost = config.getString("database.redis.host", "localhost");
        redisPort = config.getInt("database.redis.port", 6379);
        redisPassword = config.getString("database.redis.password");
        redisSsl = config.getBoolean("database.redis.ssl", false);
        redisTimeoutMs = config.getInt("database.redis.timeout-ms", 5000);

        mysqlHost = config.getString("database.mysql.host", "localhost");
        mysqlPort = config.getInt("database.mysql.port", 3306);
        mysqlUsername = config.getString("database.mysql.username", "root");
        mysqlPassword = config.getString("database.mysql.password", "password");
        mysqlDatabase = config.getString("database.mysql.database", "advancements");
        mysqlPoolSize = config.getInt("database.mysql.pool-size", 10);
        mysqlPoolName = config.getString("database.mysql.pool-name", "AdvancementSync-Pool");
        mysqlConnectionTimeoutMs = config.getInt("database.mysql.connection-timeout-ms", 30000);
        mysqlIdleTimeoutMs = config.getInt("database.mysql.idle-timeout-ms", 600000);
        mysqlMaxLifetimeMs = config.getInt("database.mysql.max-lifetime-ms", 1800000);

        // Load sync configurations
        asyncTimeoutMs = config.getInt("sync.async-timeout-ms", 5000);
        enableDebugLogging = config.getBoolean("sync.enable-debug-logging", true);
        verboseLogging = config.getBoolean("sync.verbose-logging", false);

        // Load feature flags
        syncOnUnlock = config.getBoolean("features.sync-on-unlock", true);
        syncOnDisconnect = config.getBoolean("features.sync-on-disconnect", true);
        loadOnPrelogin = config.getBoolean("features.load-on-prelogin", true);

        // Load logging configuration
        logFile = config.getString("logging.file", "plugins/AdvancementSync/logs/advancement-sync.log");
        logLevel = config.getString("logging.level", "DEBUG");
    }

    private void createDefaultConfig() throws IOException {
        try {
            // Try to load from JAR resources first
            InputStream in = plugin.getResource("config.yml");
            if (in != null) {
                configFile.createNewFile();
                Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        } catch (IOException e) {
            // Resource loading failed, create programmatically
        }

        // Create default config programmatically
        configFile.createNewFile();
        FileConfiguration defaultConfig = new YamlConfiguration();

        // Database - Redis
        defaultConfig.set("database.redis.host", "localhost");
        defaultConfig.set("database.redis.port", 6379);
        defaultConfig.set("database.redis.password", null);
        defaultConfig.set("database.redis.ssl", false);
        defaultConfig.set("database.redis.timeout-ms", 5000);

        // Database - MySQL
        defaultConfig.set("database.mysql.host", "localhost");
        defaultConfig.set("database.mysql.port", 3306);
        defaultConfig.set("database.mysql.username", "root");
        defaultConfig.set("database.mysql.password", "password");
        defaultConfig.set("database.mysql.database", "advancements");
        defaultConfig.set("database.mysql.pool-size", 10);
        defaultConfig.set("database.mysql.pool-name", "AdvancementSync-Pool");
        defaultConfig.set("database.mysql.connection-timeout-ms", 30000);
        defaultConfig.set("database.mysql.idle-timeout-ms", 600000);
        defaultConfig.set("database.mysql.max-lifetime-ms", 1800000);

        // Sync
        defaultConfig.set("sync.async-timeout-ms", 5000);
        defaultConfig.set("sync.enable-debug-logging", true);
        defaultConfig.set("sync.verbose-logging", false);

        // Features
        defaultConfig.set("features.sync-on-unlock", true);
        defaultConfig.set("features.sync-on-disconnect", true);
        defaultConfig.set("features.load-on-prelogin", true);

        // Logging
        defaultConfig.set("logging.file", "plugins/AdvancementSync/logs/advancement-sync.log");
        defaultConfig.set("logging.level", "DEBUG");

        // Save to file
        defaultConfig.save(configFile);
    }

    // Getters for Redis
    public String getRedisHost() {
        return redisHost;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public String getRedisPassword() {
        return redisPassword;
    }

    public boolean isRedisSsl() {
        return redisSsl;
    }

    public int getRedisTimeoutMs() {
        return redisTimeoutMs;
    }

    // Getters for MySQL
    public String getMysqlHost() {
        return mysqlHost;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public int getMysqlPoolSize() {
        return mysqlPoolSize;
    }

    public String getMysqlPoolName() {
        return mysqlPoolName;
    }

    public int getMysqlConnectionTimeoutMs() {
        return mysqlConnectionTimeoutMs;
    }

    public int getMysqlIdleTimeoutMs() {
        return mysqlIdleTimeoutMs;
    }

    public int getMysqlMaxLifetimeMs() {
        return mysqlMaxLifetimeMs;
    }

    // Getters for Sync
    public int getAsyncTimeoutMs() {
        return asyncTimeoutMs;
    }

    public boolean isDebugLoggingEnabled() {
        return enableDebugLogging;
    }

    public boolean isVerboseLoggingEnabled() {
        return verboseLogging;
    }

    // Getters for Features
    public boolean isSyncOnUnlock() {
        return syncOnUnlock;
    }

    public boolean isSyncOnDisconnect() {
        return syncOnDisconnect;
    }

    public boolean isLoadOnPrelogin() {
        return loadOnPrelogin;
    }

    // Getters for Logging
    public String getLogFile() {
        return logFile;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public FileConfiguration getConfig() {
        return config;
    }
}