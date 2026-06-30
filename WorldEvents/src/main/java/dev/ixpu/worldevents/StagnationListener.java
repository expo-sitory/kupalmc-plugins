package dev.ixpu.worldevents;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class StagnationListener implements Listener {

    private final WorldEvents plugin;

    public StagnationListener(WorldEvents plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        if (!plugin.isStagnationEnabled()) {
            return;
        }

        // Block all XP from all sources
        event.setAmount(0);
    }
}
