package dev.ixpu.advancementsync.database;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import dev.ixpu.advancementsync.config.ConfigManager;
import dev.ixpu.advancementsync.util.Logger;

public class DatabaseManager {

    private final RedisManager redisManager;
    private final MySQLManager mysqlManager;
    private final ConfigManager configManager;
    private final Gson gson = new Gson();
    private Logger logger;

    public DatabaseManager(RedisManager redisManager, MySQLManager mysqlManager, ConfigManager configManager) {
        this.redisManager = redisManager;
        this.mysqlManager = mysqlManager;
        this.configManager = configManager;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    
    // Store an advancement for a player
    
    public void storeAdvancement(String playerUUID, String advancementKey, long completedAt) {
        // Store in Redis first (primary)
        boolean redisSuccess = redisManager.setAdvancements(playerUUID, buildAdvancementJson(advancementKey, completedAt));
        
        if (!redisSuccess && logger != null) {
            logger.debug("Failed to store in Redis, falling back to MySQL");
        }

        // Async MySQL write as fallback
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50); // Small delay to avoid race conditions
                mysqlManager.storeAdvancement(playerUUID, advancementKey, completedAt);
                
                if (logger != null) {
                    logger.sync("Stored advancement %s for player %s in MySQL", advancementKey, playerUUID);
                }
            } catch (InterruptedException e) {
                if (logger != null) {
                    logger.debug("MySQL write interrupted: " + e.getMessage());
                }
            }
        });
    }

    
    // Load all advancements for a player from cache/database
    
    public Map<String, Long> loadAdvancements(String playerUUID) {
        final Map<String, Long> advancements = new HashMap<>();

        // Try Redis first
        String redisData = redisManager.getAdvancements(playerUUID);
        if (redisData != null) {
            try {
                JsonObject json = gson.fromJson(redisData, JsonObject.class);
                if (json != null && json.has("advancements")) {
                    json.getAsJsonObject("advancements").entrySet().forEach(entry ->
                        advancements.put(entry.getKey(), entry.getValue().getAsLong())
                    );
                }
                if (logger != null) {
                    logger.debug("Loaded %d advancements from Redis for player %s", advancements.size(), playerUUID);
                }
                return advancements;
            } catch (RuntimeException e) {
                if (logger != null) {
                    logger.debug("Failed to parse Redis data, falling back to MySQL: " + e.getMessage());
                }
            }
        }

        // Fallback to MySQL
        advancements.putAll(mysqlManager.getPlayerAdvancements(playerUUID));
        if (!advancements.isEmpty()) {
            // Update Redis cache
            updateRedisCache(playerUUID, advancements);
            if (logger != null) {
                logger.debug("Loaded %d advancements from MySQL for player %s", advancements.size(), playerUUID);
            }
        }

        return advancements;
    }


    // Sync advancements for a player (assurance check on disconnect)
  
    public void syncPlayerAdvancements(String playerUUID) {
        Player player = Bukkit.getPlayer(UUID.fromString(playerUUID));
        if (player == null) {
            if (logger != null) {
                logger.debug("Player %s not found for sync", playerUUID);
            }
            return;
        }

        Map<String, Long> currentAdvancements = new HashMap<>();
        long now = System.currentTimeMillis();

        // Collect all completed advancements
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            if (player.getAdvancementProgress(advancement).isDone()) {
                currentAdvancements.put(advancement.getKey().toString(), now);
            }
        }

        // Get stored advancements
        Map<String, Long> storedAdvancements = loadAdvancements(playerUUID);

        // Find missing advancements and store them
        int syncedCount = 0;
        for (String advKey : currentAdvancements.keySet()) {
            if (!storedAdvancements.containsKey(advKey)) {
                storeAdvancement(playerUUID, advKey, now);
                syncedCount++;
            }
        }

        if (logger != null) {
            logger.sync("Assurance check for player %s: synced %d new advancements", playerUUID, syncedCount);
        }
    }


    //  Apply stored advancements to a player

    public void applyAdvancements(String playerUUID) {
        Player player = Bukkit.getPlayer(UUID.fromString(playerUUID));
        if (player == null) {
            if (logger != null) {
                logger.debug("Player %s not found for advancement application", playerUUID);
            }
            return;
        }

        Map<String, Long> advancements = loadAdvancements(playerUUID);
        if (advancements.isEmpty()) {
            if (logger != null) {
                logger.debug("No advancements to apply for player %s", playerUUID);
            }
            return;
        }

        int appliedCount = 0;
        for (String advKey : advancements.keySet()) {
            try {
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(advKey);
                if (key != null) {
                    Advancement advancement = Bukkit.getAdvancement(key);
                    if (advancement != null) {
                        org.bukkit.advancement.AdvancementProgress progress = player.getAdvancementProgress(advancement);
                        // Only award if not already done to prevent duplicate notifications
                        if (!progress.isDone()) {
                            progress.awardCriteria("*");
                        }
                        appliedCount++;
                    }
                }
            } catch (RuntimeException e) {
                if (logger != null) {
                    logger.debug("Failed to apply advancement %s: %s", advKey, e.getMessage());
                }
            }
        }

        if (logger != null) {
            logger.sync("Applied %d advancements to player %s", appliedCount, playerUUID);
        }
    }


    // Rollback advancements to a specific timestamp
 
    public void rollbackAdvancements(String playerUUID, long timestamp) {
        Player player = Bukkit.getPlayer(UUID.fromString(playerUUID));
        if (player == null) {
            if (logger != null) {
                logger.debug("Player %s not found for rollback", playerUUID);
            }
            return;
        }

        // Get advancements completed after the timestamp
        Map<String, Long> advancementsToRemove = mysqlManager.getAdvancementsAfter(playerUUID, timestamp);

        int revokedCount = 0;
        for (String advKey : advancementsToRemove.keySet()) {
            try {
                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(advKey);
                if (key != null) {
                    Advancement advancement = Bukkit.getAdvancement(key);
                    if (advancement != null) {
                        player.getAdvancementProgress(advancement).revokeCriteria("*");
                        mysqlManager.deleteAdvancement(playerUUID, advKey);
                        revokedCount++;
                    }
                }
            } catch (RuntimeException e) {
                if (logger != null) {
                    logger.debug("Failed to revoke advancement %s: %s", advKey, e.getMessage());
                }
            }
        }

        // Update Redis cache
        Map<String, Long> remaining = loadAdvancements(playerUUID);
        updateRedisCache(playerUUID, remaining);

        if (logger != null) {
            logger.sync("Rolled back %d advancements for player %s to timestamp %d", revokedCount, playerUUID, timestamp);
        }
    }


    // Reset all advancements for a player
 
    public void resetAdvancements(String playerUUID) {
        Player player = Bukkit.getPlayer(UUID.fromString(playerUUID));
        if (player == null) {
            if (logger != null) {
                logger.debug("Player %s not found for reset", playerUUID);
            }
            return;
        }

        int revokedCount = 0;
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            if (player.getAdvancementProgress(advancement).isDone()) {
                player.getAdvancementProgress(advancement).revokeCriteria("*");
                revokedCount++;
            }
        }

        // Clear from database
        mysqlManager.deletePlayerAdvancements(playerUUID);
        redisManager.deleteAdvancements(playerUUID);

        if (logger != null) {
            logger.sync("Reset %d advancements for player %s", revokedCount, playerUUID);
        }
    }


    // Update Redis cache with advancement data

    private void updateRedisCache(String playerUUID, Map<String, Long> advancements) {
        try {
            JsonObject json = new JsonObject();
            JsonObject advancementsJson = new JsonObject();
            
            for (Map.Entry<String, Long> entry : advancements.entrySet()) {
                advancementsJson.addProperty(entry.getKey(), entry.getValue());
            }
            
            json.add("advancements", advancementsJson);
            json.addProperty("timestamp", System.currentTimeMillis());
            
            redisManager.setAdvancements(playerUUID, json.toString());
        } catch (RuntimeException e) {
            if (logger != null) {
                logger.debug("Failed to update Redis cache: " + e.getMessage());
            }
        }
    }


    // Build advancement JSON for storage

    private String buildAdvancementJson(String advancementKey, long completedAt) {
        try {
            JsonObject json = new JsonObject();
            JsonObject advancements = new JsonObject();
            advancements.addProperty(advancementKey, completedAt);
            
            json.add("advancements", advancements);
            json.addProperty("timestamp", System.currentTimeMillis());
            
            return json.toString();
        } catch (RuntimeException e) {
            return "{}";
        }
    }

  
    // Check database connectivity

    public boolean isConnected() {
        return redisManager.isConnected() || mysqlManager.testConnection();
    }

    // Getters
    public RedisManager getRedisManager() {
        return redisManager;
    }

    public MySQLManager getMySQLManager() {
        return mysqlManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}