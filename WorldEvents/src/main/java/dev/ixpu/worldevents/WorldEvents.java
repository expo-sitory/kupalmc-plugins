package dev.ixpu.worldevents;

import org.bukkit.plugin.java.JavaPlugin;

public final class WorldEvents extends JavaPlugin {

    private static WorldEvents instance;
    private VillagerTradeListener villagerListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Register Inflation listener
        villagerListener = new VillagerTradeListener(this);
        getServer().getPluginManager().registerEvents(villagerListener, this);

        // Register Stagnation listener
        getServer().getPluginManager().registerEvents(new StagnationListener(this), this);

        getCommand("worldevents").setExecutor(new WorldEventsCommandExecutor(this));

        getLogger().info("WorldEvents plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("WorldEvents plugin disabled!");
    }

    public static WorldEvents getInstance() {
        return instance;
    }

    public boolean isInflationEnabled() {
        return getConfig().getBoolean("Inflation.enabled", true);
    }

    public boolean isHeroDiscountEnabled() {
        return getConfig().getBoolean("Inflation.village-hero-discount", false);
    }

    public boolean isStagnationEnabled() {
        return getConfig().getBoolean("Stagnation.enabled", false);
    }

    public boolean allowXpBottlesForMending() {
        return getConfig().getBoolean("Stagnation.allow-xp-bottles-for-mending", false);
    }

    public boolean allowXpBottlesForXpGain() {
        return getConfig().getBoolean("Stagnation.allow-xp-bottles-for-xp-gain", false);
    }

    public void clearRecipeCache() {
        if (villagerListener != null) {
            villagerListener.clearCache();
        }
    }
}