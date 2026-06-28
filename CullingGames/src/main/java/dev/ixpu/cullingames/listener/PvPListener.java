package dev.ixpu.cullingames.listener;

import dev.ixpu.cullingames.manager.EventManager;
import dev.ixpu.cullingames.util.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.time.Duration;

public class PvPListener implements Listener {

    private final EventManager eventManager;
    private final MessageManager messageManager;

    public PvPListener(EventManager eventManager, MessageManager messageManager) {
        this.eventManager = eventManager;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!eventManager.isActive()) return;

        Player victim = e.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return;
        if (!eventManager.isParticipant(killer.getUniqueId())) return;
        if (!eventManager.isParticipant(victim.getUniqueId())) return;
        if (killer.getUniqueId().equals(victim.getUniqueId())) return;

        double stolen = eventManager.getPoints(victim.getUniqueId());
        boolean onCooldown = eventManager.isKillCooldownActive(killer.getUniqueId(), victim.getUniqueId());

        double killerGains;
        String messageType;

        if (onCooldown) {
            // On cooldown: only steal points, no bonus
            killerGains = stolen;
            messageType = "no_bonus";
        } else {
            // Not on cooldown: steal points + 10 bonus
            killerGains = stolen + 10;
            messageType = "bonus";
            eventManager.setKillCooldown(killer.getUniqueId(), victim.getUniqueId());
        }

        // Update points
        eventManager.addPoints(killer.getUniqueId(), killerGains);
        eventManager.setDeathLost(victim.getUniqueId(), stolen);
        eventManager.setPoints(victim.getUniqueId(), 0);

        // Send titles
        if ("no_bonus".equals(messageType)) {
            Title title = Title.title(
                Component.text("+" + (int) stolen + " (cooldown)", NamedTextColor.YELLOW),
                Component.empty(),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(500))
            );
            killer.showTitle(title);

            String broadcast = killer.getName() + " eliminated " + victim.getName() + " (no bonus)";
            killer.getWorld().getPlayers().forEach(p -> messageManager.sendRawMessage(p, broadcast));
        } else {
            Title title = Title.title(
                Component.text("+" + (int) (stolen + 10) + " (+10 bonus)", NamedTextColor.GREEN),
                Component.empty(),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(500))
            );
            killer.showTitle(title);

            String broadcast = killer.getName() + " eliminated " + victim.getName() + " (+10 bonus)";
            killer.getWorld().getPlayers().forEach(p -> messageManager.sendRawMessage(p, broadcast));
        }
    }
}
