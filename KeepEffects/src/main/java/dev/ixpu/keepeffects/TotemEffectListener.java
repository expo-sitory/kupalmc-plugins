package dev.ixpu.keepeffects;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

public class TotemEffectListener implements Listener {

    private final JavaPlugin plugin;

    public TotemEffectListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTotemUse(EntityResurrectEvent event) {
        // Only track players
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Store all active effects before the totem activates
        List<PotionEffect> activeEffects = new ArrayList<>(player.getActivePotionEffects());

        // Schedule effect reapplication after the totem effect (1 tick delay)
        // This is needed because the totem clears effects after this event fires
        player.getServer().getScheduler().scheduleSyncDelayedTask(
            plugin,
            () -> {
                for (PotionEffect effect : activeEffects) {
                    player.addPotionEffect(effect);
                }
            },
            1L // 1 tick delay
        );
    }
}
