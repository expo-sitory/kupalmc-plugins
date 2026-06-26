package dev.ixpu.coordssync;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import dev.ixpu.coordssync.command.CommandManager;
import dev.ixpu.coordssync.config.ConfigManager;
import dev.ixpu.coordssync.database.DatabaseManager;
import dev.ixpu.coordssync.listener.PlayerListener;


// CoordsSync - Multi-server player coordinate synchronization plugin
// Syncs player locations across multiple servers using Redis with MySQL fallback

public class CoordsSync extends JavaPlugin {

    private DatabaseManager databaseManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        
        // Load configuration
        this.configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize database manager (Redis + MySQL fallback)
        this.databaseManager = new DatabaseManager(this);

        // Check if any database is available
        if (!databaseManager.isRedisAvailable() && !databaseManager.isMysqlAvailable()) {
            getLogger().severe("[CoordsSync] CRITICAL: No database connection available!");
            getLogger().severe("[CoordsSync] Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Register event listeners
        Bukkit.getPluginManager().registerEvents(
            new PlayerListener(this, databaseManager),
            this
        );

        // Register command executor
        CommandManager commandManager = new CommandManager(this, databaseManager);
        var coordsCommand = getCommand("coordssync");
        if (coordsCommand != null) {
            coordsCommand.setExecutor(commandManager);
        }

        long duration = System.currentTimeMillis() - startTime;
        @SuppressWarnings("deprecation")
        String version = getDescription().getVersion();
        getLogger().info(String.format("[CoordsSync] v%s enabled successfully! (%dms)", version, duration));
        getLogger().info(databaseManager.getDatabaseStatus());
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("[CoordsSync] Plugin disabled");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}