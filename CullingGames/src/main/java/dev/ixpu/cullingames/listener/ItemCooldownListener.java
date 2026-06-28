package dev.ixpu.cullingames.listener;

import dev.ixpu.cullingames.manager.EventManager;
import dev.ixpu.cullingames.util.MessageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ItemCooldownListener implements Listener {

    private final EventManager eventManager;
    private final MessageManager messageManager;
    private static final Material WIND_CHARGE = getMaterialSafely("WIND_CHARGE");
    private static final Material MACE = getMaterialSafely("MACE");

    public ItemCooldownListener(EventManager eventManager, MessageManager messageManager) {
        this.eventManager = eventManager;
        this.messageManager = messageManager;
    }

    private static Material getMaterialSafely(String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (!eventManager.isActive()) return;

        Player player = e.getPlayer();
        if (!eventManager.isParticipant(player.getUniqueId())) return;

        ItemStack item = e.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        Material type = item.getType();

        // End crystal - disabled
        if (type == Material.END_CRYSTAL) {
            e.setCancelled(true);
            messageManager.sendMessage(player, "End crystals are disabled in CullingGames");
            return;
        }

        // Ender pearl - 5 min cooldown
        if (type == Material.ENDER_PEARL) {
            if (eventManager.isEnderPearlCooldownActive(player.getUniqueId())) {
                e.setCancelled(true);
                messageManager.sendMessage(player, "Ender pearl cooldown active");
                return;
            }
            eventManager.setEnderPearlCooldown(player.getUniqueId());
            messageManager.sendMessage(player, "Ender pearl cooldown started (5 min)");
            return;
        }

        // Wind charge - 5 min cooldown (1.20.5+)
        if (WIND_CHARGE != null && type == WIND_CHARGE) {
            if (eventManager.isWindChargeCooldownActive(player.getUniqueId())) {
                e.setCancelled(true);
                messageManager.sendMessage(player, "Wind charge cooldown active");
                return;
            }
            eventManager.setWindChargeCooldown(player.getUniqueId());
            messageManager.sendMessage(player, "Wind charge cooldown started (5 min)");
            return;
        }

        // Trident - 5 min cooldown
        if (type == Material.TRIDENT) {
            if (eventManager.isTridentCooldownActive(player.getUniqueId())) {
                e.setCancelled(true);
                messageManager.sendMessage(player, "Trident cooldown active");
                return;
            }
            eventManager.setTridentCooldown(player.getUniqueId());
            messageManager.sendMessage(player, "Trident cooldown started (5 min)");
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!eventManager.isActive()) return;

        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;

        if (!eventManager.isParticipant(victim.getUniqueId())) return;
        if (!eventManager.isParticipant(attacker.getUniqueId())) return;

        // If paused: cancel all damage to participants
        if (eventManager.isPaused()) {
            e.setCancelled(true);
            return;
        }

        // Check if attacker is holding a mace (1.20.5+)
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (MACE != null && weapon.getType() == MACE) {
            e.setCancelled(true);
            messageManager.sendMessage(attacker, "Maces are disabled in CullingGames");
        }
    }
}
