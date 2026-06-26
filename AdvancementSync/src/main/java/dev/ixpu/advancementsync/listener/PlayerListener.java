package dev.ixpu.advancementsync.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import dev.ixpu.advancementsync.config.ConfigManager;
import dev.ixpu.advancementsync.database.DatabaseManager;
import dev.ixpu.advancementsync.util.Logger;

public class PlayerListener implements Listener {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final ConfigManager configManager;

    public PlayerListener(JavaPlugin plugin, DatabaseManager databaseManager, Logger logger, ConfigManager configManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.logger = logger;
        this.configManager = configManager;
    }

    // Load and apply advancements on player join

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!configManager.isLoadOnPrelogin()) {
            return;
        }

        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();

        if (configManager.isDebugLoggingEnabled()) {
            logger.debug("Loading advancements for player %s on join", player.getName());
        }

        // Load advancements asynchronously without blocking the join
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Load advancements from database
                var advancements = databaseManager.loadAdvancements(playerUUID);

                if (!advancements.isEmpty()) {
                    // Apply advancements on main thread
                    Bukkit.getScheduler().runTask(plugin, () -> databaseManager.applyAdvancements(playerUUID));

                    if (configManager.isDebugLoggingEnabled()) {
                        logger.debug("Applied %d advancements for player %s", advancements.size(), player.getName());
                    }
                } else if (configManager.isDebugLoggingEnabled()) {
                    logger.debug("No advancements found for player %s", player.getName());
                }
            } catch (RuntimeException e) {
                logger.error("Error loading advancements for player " + player.getName(), e);
            }
        });
    }
}