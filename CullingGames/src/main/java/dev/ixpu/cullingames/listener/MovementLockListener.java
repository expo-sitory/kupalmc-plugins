package dev.ixpu.cullingames.listener;

import dev.ixpu.cullingames.manager.EventManager;
import dev.ixpu.cullingames.util.MessageManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MovementLockListener implements Listener {

    private final EventManager eventManager;
    private final MessageManager messageManager;

    public MovementLockListener(EventManager eventManager, MessageManager messageManager) {
        this.eventManager = eventManager;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        // Early exit for most cases
        if (!eventManager.isActive() || !eventManager.isPaused()) return;
        
        if (!eventManager.isParticipant(e.getPlayer().getUniqueId())) return;

        // Only cancel if they actually moved (not just rotation)
        if (e.getFrom().getX() == e.getTo().getX() &&
            e.getFrom().getY() == e.getTo().getY() &&
            e.getFrom().getZ() == e.getTo().getZ()) {
            return;
        }

        e.setCancelled(true);
    }
}
