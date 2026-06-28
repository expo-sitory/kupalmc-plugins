package dev.ixpu.cullingames;

import dev.ixpu.cullingames.api.CullingGamesAPI;
import dev.ixpu.cullingames.command.CullingGamesCommand;
import dev.ixpu.cullingames.command.ForcejoinallCommand;
import dev.ixpu.cullingames.config.ConfigManager;
import dev.ixpu.cullingames.listener.*;
import dev.ixpu.cullingames.manager.EventManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CullingGamesPlugin extends JavaPlugin {

    private static CullingGamesPlugin instance;
    private EventManager eventManager;
    private ConfigManager configManager;
    private dev.ixpu.cullingames.util.MessageManager messageManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize config
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize message manager
        messageManager = new dev.ixpu.cullingames.util.MessageManager(this);
        
        // Initialize event manager
        eventManager = new EventManager(this);
        CullingGamesAPI.setManager(eventManager);
        
        // Register commands
        getCommand("cgames").setExecutor(new CullingGamesCommand(eventManager, messageManager));
        getCommand("cgames").setTabCompleter(new CullingGamesCommand(eventManager, messageManager));
        getCommand("cgamesforcejoinall").setExecutor(new ForcejoinallCommand(eventManager, messageManager));
        
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new PvPListener(eventManager, messageManager), this);
        Bukkit.getPluginManager().registerEvents(new MovementLockListener(eventManager, messageManager), this);
        Bukkit.getPluginManager().registerEvents(new RespawnNotificationListener(eventManager, this, messageManager), this);
        Bukkit.getPluginManager().registerEvents(new ItemCooldownListener(eventManager, messageManager), this);
        Bukkit.getPluginManager().registerEvents(new WaveMobListener(eventManager, messageManager), this);
        
        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new dev.ixpu.cullingames.placeholder.CullingGamesPlaceholder().register();
                getLogger().info("PlaceholderAPI expansion registered");
            } catch (Exception e) {
                getLogger().warning(() -> "Failed to register PlaceholderAPI expansion: " + e.getMessage());
            }
        }
        
        getLogger().info("CullingGames enabled!");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.shutdown();
        }
        getLogger().info("CullingGames disabled!");
    }

    public static CullingGamesPlugin getInstance() {
        return instance;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public dev.ixpu.cullingames.util.MessageManager getMessageManager() {
        return messageManager;
    }
}