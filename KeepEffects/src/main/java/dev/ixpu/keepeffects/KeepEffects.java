package dev.ixpu.keepeffects;

import org.bukkit.plugin.java.JavaPlugin;

public class KeepEffects extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new TotemEffectListener(this), this);
        getLogger().info("KeepEffects plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("KeepEffects plugin has been disabled!");
    }
}
