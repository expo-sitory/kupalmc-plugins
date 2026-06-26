package dev.ixpu.advancementsync.database;

import java.util.Set;

import dev.ixpu.advancementsync.config.ConfigManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisManager {

    private final JedisPool jedisPool;
    private final ConfigManager configManager;

    public RedisManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.jedisPool = initializePool();
    }

    private JedisPool initializePool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(2);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);

        String password = configManager.getRedisPassword();
        
        if (password != null && !password.isEmpty()) {
            return new JedisPool(
                poolConfig,
                configManager.getRedisHost(),
                configManager.getRedisPort(),
                configManager.getRedisTimeoutMs(),
                password
            );
        } else {
            return new JedisPool(
                poolConfig,
                configManager.getRedisHost(),
                configManager.getRedisPort(),
                configManager.getRedisTimeoutMs()
            );
        }
    }


    // Get advancement data from Redis cache

    public String getAdvancements(String playerUUID) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get("advancements:" + playerUUID);
        } catch (Exception e) {
            return null;
        }
    }

    // Store advancement data in Redis cache

    public boolean setAdvancements(String playerUUID, String advancementData) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set("advancements:" + playerUUID, advancementData);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Delete advancement data from Redis cache

    public boolean deleteAdvancements(String playerUUID) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("advancements:" + playerUUID);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Check if Redis is connected and accessible

    public boolean isConnected() {
        try (Jedis jedis = jedisPool.getResource()) {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            return false;
        }
    }

    // Get all advancement keys from Redis for a specific pattern

    public Set<String> getAllAdvancementKeys() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.keys("advancements:*");
        } catch (Exception e) {
            return Set.of();
        }
    }

    // Exists check

    public boolean exists(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Shutdown the Redis connection pool
     */
    public void shutdown() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}