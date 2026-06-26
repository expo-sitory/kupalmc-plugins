package dev.ixpu.coordssync.database;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import dev.ixpu.coordssync.config.ConfigManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

public class RedisManager {
    
    private JedisPool jedisPool;
    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final Gson gson;
    private Thread pubSubThread;
    private CoordsSyncSubscriber subscriber;
    private volatile boolean connected = false;

    public RedisManager(JavaPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.gson = new Gson();
    }

    @SuppressWarnings("deprecation")
    public boolean connect() {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestOnReturn(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMinEvictableIdleTimeMillis(60000);
            poolConfig.setTimeBetweenEvictionRunsMillis(30000);

            String host = config.getRedisHost();
            int port = config.getRedisPort();
            String password = config.getRedisPassword();

            // Create JedisPool with proper constructor
            if (password != null && !password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, 5000, password);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, 5000);
            }

            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                String pong = jedis.ping();
                plugin.getLogger().info(String.format("Redis connection successful! Response: %s", pong));
            }
            
            connected = true;
            
            // Start Pub/Sub listener in background thread
            startPubSubListener();
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe(String.format("Failed to connect to Redis: %s", e.getMessage()));
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Exception:", e);
            return false;
        }
    }

    public void disconnect() {
        connected = false;
        
        // Stop pub/sub subscriber
        if (subscriber != null) {
            try {
                subscriber.unsubscribe();
            } catch (Exception e) {
                plugin.getLogger().warning(String.format("Error unsubscribing from Pub/Sub: %s", e.getMessage()));
            }
        }
        
        // Wait for pub/sub thread to finish
        if (pubSubThread != null) {
            try {
                pubSubThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Close connection pool
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    @SuppressWarnings("ThreadSleep")
    private void reconnectDelay() throws InterruptedException {
        Thread.sleep(5000);
    }

    private void startPubSubListener() {
        subscriber = new CoordsSyncSubscriber(plugin, config);
        pubSubThread = new Thread(() -> {
            while (connected && !Thread.currentThread().isInterrupted()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    plugin.getLogger().info("Subscribing to coordinate sync channel...");
                    jedis.subscribe(subscriber, "coordssync:sync");
                } catch (Exception e) {
                    if (connected) {
                        plugin.getLogger().warning("Pub/Sub connection lost, reconnecting in 5 seconds...");
                        try {
                            reconnectDelay();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }, "CoordsSync-PubSub");
        pubSubThread.setDaemon(true);
        pubSubThread.start();
    }

    public void savePlayerCoordinates(String uuid, String world, double x, double y, double z, float yaw, float pitch) {
        try (Jedis jedis = jedisPool.getResource()) {
            JsonObject json = new JsonObject();
            json.addProperty("uuid", uuid);
            json.addProperty("world", world);
            json.addProperty("x", x);
            json.addProperty("y", y);
            json.addProperty("z", z);
            json.addProperty("yaw", yaw);
            json.addProperty("pitch", pitch);
            json.addProperty("timestamp", System.currentTimeMillis());
            json.addProperty("server", config.getServerId());

            String key = "coordssync:player:" + uuid;
            jedis.setex(key, 86400, json.toString()); // 24 hour expiration
            
            // Publish update to other servers
            publishCoordinateUpdate(json.toString());
        } catch (Exception e) {
            plugin.getLogger().warning(String.format("Failed to save coordinates for %s: %s", uuid, e.getMessage()));
        }
    }


    private void publishCoordinateUpdate(String jsonData) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish("coordssync:sync", jsonData);
        } catch (Exception e) {
            plugin.getLogger().warning(String.format("Failed to publish coordinate update: %s", e.getMessage()));
        }
    }

    public PlayerCoordinates loadPlayerCoordinates(String uuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "coordssync:player:" + uuid;
            String data = jedis.get(key);

            if (data == null) {
                return null;
            }

            return gson.fromJson(data, PlayerCoordinates.class);
        } catch (Exception e) {
            plugin.getLogger().warning(String.format("Failed to load coordinates for %s: %s", uuid, e.getMessage()));
            return null;
        }
    }

    public void deletePlayerCoordinates(String uuid) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "coordssync:player:" + uuid;
            jedis.del(key);
        } catch (Exception e) {
            plugin.getLogger().warning(String.format("Failed to delete coordinates for %s: %s", uuid, e.getMessage()));
        }
    }


    public static class PlayerCoordinates {
        public String uuid;
        public String world;
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
        public long timestamp;
        public String server;

        public PlayerCoordinates() {}

        public PlayerCoordinates(String uuid, String world, double x, double y, double z, float yaw, float pitch) {
            this.uuid = uuid;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public boolean isConnected() {
        if (jedisPool == null || jedisPool.isClosed()) {
            return false;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static class CoordsSyncSubscriber extends JedisPubSub {
        private final JavaPlugin plugin;
        private final ConfigManager config;

        public CoordsSyncSubscriber(JavaPlugin plugin, ConfigManager config) {
            this.plugin = plugin;
            this.config = config;
        }

        @Override
        public void onMessage(String channel, String message) {
            // Handle incoming coordinate updates from other servers
            try {
                Gson gson = new Gson();
                PlayerCoordinates coords = gson.fromJson(message, PlayerCoordinates.class);
                
                // Log [Sync] alert only if enabled in config
                if (config.isSyncAlertsEnabled()) {
                    plugin.getLogger().info(String.format("[Sync] Received coordinate update for %s from server %s at %s (%d, %d, %d)",
                        coords.uuid, coords.server, coords.world, (int)coords.x, (int)coords.y, (int)coords.z));
                }
            
            } catch (RuntimeException e) {
                plugin.getLogger().warning(String.format("Failed to process coordinate sync message: %s", e.getMessage()));
            }
        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            plugin.getLogger().info(String.format("Successfully subscribed to channel: %s", channel));
        }

        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {
            plugin.getLogger().info(String.format("Unsubscribed from channel: %s", channel));
        }

        @Override
        public void onPSubscribe(String pattern, int subscribedChannels) {}

        @Override
        public void onPUnsubscribe(String pattern, int subscribedChannels) {}

        @Override
        public void onPMessage(String pattern, String channel, String message) {}
    }
}