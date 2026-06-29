package dev.ixpu.cullingames.listener;

import dev.ixpu.cullingames.manager.EventManager;
import dev.ixpu.cullingames.util.MessageManager;
import dev.ixpu.cullingames.wave.WaveManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.entity.PiglinBrute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTransformEvent;

import java.time.Duration;

public class WaveMobListener implements Listener {

    private final EventManager eventManager;
    private final MessageManager messageManager;

    public WaveMobListener(EventManager eventManager, MessageManager messageManager) {
        this.eventManager = eventManager;
        this.messageManager = messageManager;
    }

    @EventHandler
    public void onWaveMobDeath(EntityDeathEvent e) {
        if (!eventManager.isActive()) return;

        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        if (!eventManager.isParticipant(killer.getUniqueId())) return;

        WaveManager waveManager = eventManager.getWaveManager();
        
        if (!waveManager.isWaveMob(e.getEntity().getUniqueId())) {
            return;
        }

        int points = waveManager.getPointsForMob(e.getEntity().getUniqueId());
        
        eventManager.addPoints(killer.getUniqueId(), points);
        eventManager.incrementMobKill(killer.getUniqueId());

        Title title = Title.title(
            Component.text("+" + points + " wave kill", NamedTextColor.LIGHT_PURPLE),
            Component.empty(),
            Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(500))
        );
        killer.showTitle(title);
    }

    @EventHandler
    public void onPiglinBruteTransform(EntityTransformEvent event) {
        if (!(event.getEntity() instanceof PiglinBrute)) return;
        
        WaveManager waveManager = eventManager.getWaveManager();
        if (waveManager.isWaveMob(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}