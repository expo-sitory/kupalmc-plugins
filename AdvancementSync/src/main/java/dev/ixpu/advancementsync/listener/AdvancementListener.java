package dev.ixpu.advancementsync.listener;

import dev.ixpu.advancementsync.config.ConfigManager;
import dev.ixpu.advancementsync.database.DatabaseManager;
import dev.ixpu.advancementsync.util.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class AdvancementListener implements Listener {

    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final ConfigManager configManager;

    public AdvancementListener(DatabaseManager databaseManager, Logger logger, ConfigManager configManager) {
        this.databaseManager = databaseManager;
        this.logger = logger;
        this.configManager = configManager;
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        if (!configManager.isSyncOnUnlock()) {
            return;
        }

        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        String advancementKey = event.getAdvancement().getKey().toString();
        long completedAt = System.currentTimeMillis();

        // Log advancement unlock
        if (configManager.isDebugLoggingEnabled()) {
            logger.debug("Player %s unlocked advancement: %s", player.getName(), advancementKey);
        }

        // Store advancement asynchronously
        databaseManager.storeAdvancement(playerUUID, advancementKey, completedAt);

        if (configManager.isDebugLoggingEnabled()) {
            logger.sync("Synced advancement %s for player %s", advancementKey, player.getName());
        }
    }
}