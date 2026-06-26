package dev.ixpu.coordssync.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import dev.ixpu.coordssync.CoordsSync;
import dev.ixpu.coordssync.database.DatabaseManager;
import dev.ixpu.coordssync.database.PlayerData;


// Listener for player events - handles coordinate synchronization
// Tracks player movements and saves coordinates to database

public class PlayerListener implements Listener {

    private final CoordsSync plugin;
    private final DatabaseManager databaseManager;

    // Throttle player move events to avoid spamming Redis
    private final Map<String, Long> lastUpdateTime = new HashMap<>();
    private static final long MOVE_SYNC_INTERVAL = 5000; // 5 seconds

    public PlayerListener(CoordsSync plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;

        // Start periodic saves
        startPeriodicSave();
    }


    // Handle player join - teleport to last location if available

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        // Run async to avoid blocking main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = databaseManager.getPlayerCoordinates(uuid);

            if (data == null) {
                plugin.getLogger().info(String.format("[CoordsSync] No saved location for %s", player.getName()));
                return;
            }

            // Teleport to saved location on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                teleportPlayerToLocation(player, data);
            });
        });
    }


    // Handle player quit - save current location

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        // Save coordinates async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Location loc = player.getLocation();
                if (loc == null || loc.getWorld() == null) {
                    plugin.getLogger().warning(String.format("[CoordsSync] Invalid location for %s", player.getName()));
                    return;
                }
                PlayerData data = new PlayerData(
                    uuid,
                    player.getName(),
                    loc.getWorld().getName(),
                    loc.getX(),
                    loc.getY(),
                    loc.getZ(),
                    loc.getYaw(),
                    loc.getPitch(),
                    plugin.getConfigManager().getServerId()
                );

                if (databaseManager.savePlayerCoordinates(data)) {
                    plugin.getLogger().info(String.format("[CoordsSync] Saved location for %s", player.getName()));
                }
            } catch (Exception e) {
                plugin.getLogger().warning(String.format("[CoordsSync] Failed to save coordinates for %s: %s", 
                                          player.getName(), e.getMessage()));
            }
        });

        // Clean up throttle timer
        lastUpdateTime.remove(uuid);
    }

    // Handle player teleport - save new location

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        Location to = event.getTo();

        // Save location async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerData data = new PlayerData(
                    uuid,
                    player.getName(),
                    to.getWorld().getName(),
                    to.getX(),
                    to.getY(),
                    to.getZ(),
                    to.getYaw(),
                    to.getPitch(),
                    plugin.getConfigManager().getServerId()
                );

                databaseManager.savePlayerCoordinates(data);
                lastUpdateTime.put(uuid, System.currentTimeMillis());
            } catch (Exception e) {
                plugin.getLogger().warning(String.format("[CoordsSync] Failed to save teleport coordinates: %s", 
                                          e.getMessage()));
            }
        });
    }

    // Handle player movement - save location with throttling
    // Only syncs if player moves at least one block

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // Only sync if player moved to a different block
        if (from.getBlockX() == to.getBlockX() &&
            from.getBlockY() == to.getBlockY() &&
            from.getBlockZ() == to.getBlockZ()) {
            return; // Player just rotated, didn't move blocks
        }

        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        // Check throttle - only update every 5 seconds
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(uuid);

        if (lastUpdate != null && currentTime - lastUpdate < MOVE_SYNC_INTERVAL) {
            return; // Too soon, skip this update
        }

        // Save location async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerData data = new PlayerData(
                    uuid,
                    player.getName(),
                    to.getWorld().getName(),
                    to.getX(),
                    to.getY(),
                    to.getZ(),
                    to.getYaw(),
                    to.getPitch(),
                    plugin.getConfigManager().getServerId()
                );

                databaseManager.savePlayerCoordinates(data);
            } catch (Exception e) {
                // Silently fail on move events to avoid spam
            }
        });

        lastUpdateTime.put(uuid, currentTime);
    }

    // Periodic save of all online player positions (every 30 seconds)
    // Ensures positions are saved even if players don't move

    private void startPeriodicSave() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String uuid = player.getUniqueId().toString();
                Location loc = player.getLocation();
                
                if (loc == null || loc.getWorld() == null) {
                    continue;
                }

                // Copy location data before async task
                String worldName = loc.getWorld().getName();
                double x = loc.getX();
                double y = loc.getY();
                double z = loc.getZ();
                float yaw = loc.getYaw();
                float pitch = loc.getPitch();

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        PlayerData data = new PlayerData(
                            uuid,
                            player.getName(),
                            worldName,
                            x,
                            y,
                            z,
                            yaw,
                            pitch,
                            plugin.getConfigManager().getServerId()
                        );

                        databaseManager.savePlayerCoordinates(data);
                    } catch (Exception e) {
                        // Silently fail on periodic saves to avoid spam
                    }
                });
            }
        }, 600L, 600L); // 30 seconds
    }

 
    // Teleports the player to their saved location

    private void teleportPlayerToLocation(Player player, PlayerData data) {
        World world = Bukkit.getWorld(data.getWorld());

        // If world doesn't exist, use fallback
        if (world == null) {
            plugin.getLogger().warning(String.format("[CoordsSync] World '%s' not found for %s", 
                                      data.getWorld(), player.getName()));

            // Try to use first available world
            List<World> worlds = Bukkit.getWorlds();
            if (worlds.isEmpty()) {
                plugin.getLogger().severe("[CoordsSync] CRITICAL: No worlds available!");
                player.sendMessage("§c✗ No worlds available on this server. Contact an administrator.");
                return;
            }

            world = worlds.get(0);
            player.sendMessage(String.format("§e⚠ Your world '%s' doesn't exist on this server.", data.getWorld()));
            player.sendMessage(String.format("§e⚠ Teleported to %s instead.", world.getName()));
        }

        try {
            Location location = new Location(
                world,
                data.getX(),
                data.getY(),
                data.getZ(),
                data.getYaw(),
                data.getPitch()
            );

            player.teleport(location);
            player.sendMessage("§a✓ Teleported to your last location!");
            plugin.getLogger().info(String.format("[CoordsSync] Teleported %s to %s", 
                                   player.getName(), world.getName()));
        } catch (Exception e) {
            plugin.getLogger().severe(String.format("[CoordsSync] Failed to teleport %s: %s", 
                                     player.getName(), e.getMessage()));
            player.sendMessage("§c✗ Failed to teleport you to your last location.");
        }
    }
}