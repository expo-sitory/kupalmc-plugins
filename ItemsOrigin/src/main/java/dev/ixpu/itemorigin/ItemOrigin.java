package dev.ixpu.itemorigin;

import org.bukkit.plugin.java.JavaPlugin;

public class ItemOrigin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register listener for items picked up
        getServer().getPluginManager().registerEvents(new ItemOriginListener(), this);
        
        // Register listener for items crafted
        getServer().getPluginManager().registerEvents(new ItemOriginCraftingListener(), this);
        
        getLogger().info("ItemOrigin plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ItemOrigin plugin has been disabled!");
    }
}