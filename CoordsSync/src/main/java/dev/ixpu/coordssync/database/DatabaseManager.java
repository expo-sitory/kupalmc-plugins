package dev.ixpu.coordssync.database;

import java.util.List;

import org.bukkit.Bukkit;

import dev.ixpu.coordssync.CoordsSync;
import dev.ixpu.coordssync.config.ConfigManager;


// Unified database manager that handles both Redis and MySQL
// Implements fallback logic: tries Redis first, falls back to MySQL if Redis fails

public class DatabaseManager {
    private RedisManager redisManager;
    private MySQLManager mysqlManager;
    private ConfigManager configManager;
    private boolean useFallback;
    private boolean redisAvailable = false;
    private boolean mysqlAvailable = false;
    private CoordsSync plugin;
    
    // Constructor - initializes both Redis and MySQL managers
    public DatabaseManager(CoordsSync plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.useFallback = configManager.isUseMySQLFallback();
        
        // Initialize Redis Manager
        try {
            this.redisManager = new RedisManager(plugin, configManager);
            if (redisManager.connect()) {
                this.redisAvailable = true;
                Bukkit.getLogger().info("[CoordsSync] Redis connected successfully");
            } else {
                Bukkit.getLogger().warning("[CoordsSync] Redis connection failed");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(String.format("[CoordsSync] Error initializing Redis: %s", e.getMessage()));
            this.redisAvailable = false;
        }
        
        // Initialize MySQL Manager (if fallback is enabled)
        if (useFallback) {
            try {
                this.mysqlManager = new MySQLManager(configManager);
                this.mysqlManager.initializeTables();
                this.mysqlAvailable = mysqlManager.isConnected();
                if (mysqlAvailable) {
                    Bukkit.getLogger().info("[CoordsSync] MySQL connected successfully");
                } else {
                    Bukkit.getLogger().warning("[CoordsSync] MySQL connection failed");
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning(String.format("[CoordsSync] Error initializing MySQL: %s", e.getMessage()));
                this.mysqlAvailable = false;
            }
        } else {
            Bukkit.getLogger().info("[CoordsSync] MySQL fallback is disabled");
            this.mysqlManager = null;
            this.mysqlAvailable = false;
        }
        
        // Check if at least one database is available
        if (!redisAvailable && !mysqlAvailable) {
            Bukkit.getLogger().severe("[CoordsSync] CRITICAL: No database connection available!");
            Bukkit.getLogger().severe("[CoordsSync] Coordinates cannot be saved!");
        }
    }
    

    // Saves player coordinates - tries Redis first, falls back to MySQL

    public boolean savePlayerCoordinates(PlayerData data) {
        boolean saved = false;
        
        // Try Redis first (primary database)
        if (redisAvailable) {
            try {
                redisManager.savePlayerCoordinates(
                    data.getPlayerUUID(),
                    data.getWorld(),
                    data.getX(),
                    data.getY(),
                    data.getZ(),
                    data.getYaw(),
                    data.getPitch()
                );
                saved = true;
            } catch (Exception e) {
                Bukkit.getLogger().warning(String.format("[CoordsSync] Redis save failed for %s: %s", 
                                          data.getPlayerName(), e.getMessage()));
                redisAvailable = false;
            }
        }
        
        // Fallback to MySQL if Redis failed and fallback is enabled
        if (!saved && useFallback && mysqlAvailable) {
            try {
                boolean mysqlSaved = mysqlManager.savePlayerCoordinates(data);
                if (mysqlSaved) {
                    Bukkit.getLogger().info(String.format("[CoordsSync] Redis unavailable - saved %s to MySQL fallback",
                                           data.getPlayerName()));
                    saved = true;
                } else {
                    Bukkit.getLogger().warning(String.format("[CoordsSync] MySQL save failed for %s", data.getPlayerName()));
                    mysqlAvailable = false;
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning(String.format("[CoordsSync] MySQL fallback failed for %s: %s",
                                          data.getPlayerName(), e.getMessage()));
                mysqlAvailable = false;
            }
        }
        
        // If both databases failed, log error
        if (!saved) {
            Bukkit.getLogger().severe(String.format("[CoordsSync] Failed to save coordinates for %s - all databases unavailable!",
                                     data.getPlayerName()));
        }
        
        return saved;
    }
    
    // Retrieves player coordinates - tries Redis first, falls back to MySQL

    public PlayerData getPlayerCoordinates(String playerUUID) {
        PlayerData data = null;
        
        // Try Redis first (primary database)
        if (redisAvailable) {
            try {
                RedisManager.PlayerCoordinates coords = redisManager.loadPlayerCoordinates(playerUUID);
                if (coords != null) {
                    data = new PlayerData(
                        coords.uuid,
                        coords.uuid,
                        coords.world,
                        coords.x,
                        coords.y,
                        coords.z,
                        coords.yaw,
                        coords.pitch,
                        coords.server
                    );
                    return data;
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning(String.format("[CoordsSync] Redis retrieval failed for %s: %s",
                                          playerUUID, e.getMessage()));
                redisAvailable = false;
            }
        }
        
        // Fallback to MySQL if Redis failed and fallback is enabled
        if (data == null && useFallback && mysqlAvailable) {
            try {
                data = mysqlManager.getPlayerCoordinates(playerUUID);
                if (data != null) {
                    Bukkit.getLogger().info(String.format("[CoordsSync] Redis unavailable - retrieved %s from MySQL fallback",
                                           data.getPlayerName()));
                    return data;
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning(String.format("[CoordsSync] MySQL retrieval failed for %s: %s",
                                          playerUUID, e.getMessage()));
                mysqlAvailable = false;
            }
        }
        
        return null; // Not found in either database
    }
    

    // Retrieves all coordinates for a specific server

    public List<PlayerData> getServerCoordinates(String serverId) {
        // Use MySQL for server-wide queries (more efficient for bulk operations)
        if (useFallback && mysqlAvailable) {
            try {
                return mysqlManager.getServerCoordinates(serverId);
            } catch (Exception e) {
                Bukkit.getLogger().warning(String.format("[CoordsSync] Failed to get server coordinates: %s", e.getMessage()));
            }
        }
        
        return null;
    }
    

    // Deletes player coordinates - removes from both databases
    // Called when player quits

    public boolean deletePlayerCoordinates(String playerUUID) {
        boolean deleted = false;
        
        // Delete from Redis if available
        if (redisAvailable) {
            try {
                redisManager.deletePlayerCoordinates(playerUUID);
                deleted = true;
            } catch (Exception e) {
                Bukkit.getLogger().warning(String.format("[CoordsSync] Redis delete failed for %s", playerUUID));
                redisAvailable = false;
            }
        }
        
        // Delete from MySQL if available
        if (useFallback && mysqlAvailable) {
            try {
                mysqlManager.deletePlayerCoordinates(playerUUID);
                deleted = true;
            } catch (Exception e) {
                Bukkit.getLogger().warning(String.format("[CoordsSync] MySQL delete failed for %s", playerUUID));
                mysqlAvailable = false;
            }
        }
        
        return deleted;
    }
    

    // Checks if Redis is currently available

    public boolean isRedisAvailable() {
        if (redisManager == null) {
            return false;
        }
        
        try {
            redisAvailable = redisManager.isConnected();
            return redisAvailable;
        } catch (Exception e) {
            redisAvailable = false;
            return false;
        }
    }
    
    // Checks if MySQL is currently available

    public boolean isMysqlAvailable() {
        if (!useFallback || mysqlManager == null) {
            return false;
        }
        
        try {
            mysqlAvailable = mysqlManager.isConnected();
            return mysqlAvailable;
        } catch (Exception e) {
            mysqlAvailable = false;
            return false;
        }
    }
    

    // Gets the status of both databases

    public String getDatabaseStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Database Status:\n");
        
        if (isRedisAvailable()) {
            status.append("✓ Redis: Connected\n");
        } else {
            status.append("✗ Redis: Disconnected\n");
        }
        
        if (useFallback) {
            if (isMysqlAvailable()) {
                status.append("✓ MySQL: Connected\n");
                if (mysqlManager != null) {
                    status.append("  - ").append(mysqlManager.getPoolStats()).append("\n");
                }
            } else {
                status.append("✗ MySQL: Disconnected\n");
            }
        } else {
            status.append("⊘ MySQL: Fallback Disabled\n");
        }
        
        return status.toString();
    }
    

    // Reloads database configuration and reconnects
    // Called when /coordssync reload is executed

    public void reload() {
        Bukkit.getLogger().info("[CoordsSync] Reloading database configuration...");
        
        // Close existing connections
        shutdown();
        
        // Reinitialize databases
        try {
            this.redisManager = new RedisManager(plugin, configManager);
            redisAvailable = redisManager.connect();
        } catch (Exception e) {
            Bukkit.getLogger().warning(String.format("[CoordsSync] Error reloading Redis: %s", e.getMessage()));
            this.redisAvailable = false;
        }
        
        if (useFallback) {
            try {
                this.mysqlManager = new MySQLManager(configManager);
                this.mysqlManager.initializeTables();
                this.mysqlAvailable = mysqlManager.isConnected();
            } catch (Exception e) {
                Bukkit.getLogger().warning(String.format("[CoordsSync] Error reloading MySQL: %s", e.getMessage()));
                this.mysqlAvailable = false;
            }
        }
        
        Bukkit.getLogger().info("[CoordsSync] Database configuration reloaded");
        Bukkit.getLogger().info(getDatabaseStatus());
    }
    

    // Cleanly shuts down all database connections
    // Should be called on plugin disable

    public void shutdown() {
        Bukkit.getLogger().info("[CoordsSync] Shutting down database connections...");
        
        try {
            if (redisManager != null) {
                redisManager.disconnect();
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(String.format("[CoordsSync] Error closing Redis connection: %s", e.getMessage()));
        }
        
        try {
            if (mysqlManager != null) {
                mysqlManager.close();
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(String.format("[CoordsSync] Error closing MySQL connection: %s", e.getMessage()));
        }
        
        redisAvailable = false;
        mysqlAvailable = false;
        
        Bukkit.getLogger().info("[CoordsSync] Database connections closed");
    }
    public RedisManager getRedisManager() {
        return redisManager;
    }
    public MySQLManager getMysqlManager() {
        return mysqlManager;
    }
    public boolean isFallbackEnabled() {
        return useFallback;
    }
}