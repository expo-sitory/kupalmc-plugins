package dev.ixpu.worldevents;

import org.bukkit.entity.ExperienceOrb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StagnationListener implements Listener {

    private final WorldEvents plugin;

    public StagnationListener(WorldEvents plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!plugin.isStagnationEnabled()) {
            return;
        }

        // Check if XP orbs are allowed
        if (plugin.allowXpBottlesForMending() || plugin.allowXpBottlesForXpGain()) {
            // XP bottles are at least partially allowed, don't block all pickups
            return;
        }

        // Prevent XP orbs from being picked up if no exceptions are set
        if (event.getEntity() instanceof ExperienceOrb) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerExpChangeStagnation(PlayerExpChangeEvent event) {
        if (!plugin.isStagnationEnabled()) {
            return;
        }

        // Check if this XP gain should be blocked
        boolean shouldBlock = true;

        // Allow XP if mending bottles are enabled and player has mending items
        if (plugin.allowXpBottlesForMending() && hasPlayerMendingItems(event.getPlayer())) {
            shouldBlock = false;
        }

        // Allow XP if regular XP gain is enabled
        if (plugin.allowXpBottlesForXpGain()) {
            shouldBlock = false;
        }

        if (shouldBlock) {
            event.setAmount(0);
        }
    }

    private boolean hasPlayerMendingItems(org.bukkit.entity.Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (hasMending(item)) {
                return true;
            }
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (hasMending(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMending(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasEnchants() && meta.hasConflictingEnchant(
            org.bukkit.enchantments.Enchantment.MENDING
        );
    }
}