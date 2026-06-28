package dev.ixpu.cullingames.listener;

import dev.ixpu.cullingames.CullingGamesPlugin;
import dev.ixpu.cullingames.manager.EventManager;
import dev.ixpu.cullingames.util.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.time.Duration;

public class RespawnNotificationListener implements Listener {

    private final EventManager eventManager;
    private final CullingGamesPlugin plugin;
    private final MessageManager messageManager;

    public RespawnNotificationListener(EventManager eventManager, CullingGamesPlugin plugin, MessageManager messageManager) {
        this.eventManager = eventManager;
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if (!eventManager.isActive()) return;

        Player player = e.getPlayer();
        if (!eventManager.isParticipant(player.getUniqueId())) return;

        // Delay by 1 tick to ensure player is fully respawned
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (eventManager.hasDeathLost(player.getUniqueId())) {
                double lost = eventManager.getAndClearDeathLost(player.getUniqueId());

                Title title = Title.title(
                    Component.text("-" + (int) lost + " points lost", NamedTextColor.RED),
                    Component.empty(),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1200), Duration.ofMillis(500))
                );
                player.showTitle(title);
            }
        }, 1L);
    }
}
