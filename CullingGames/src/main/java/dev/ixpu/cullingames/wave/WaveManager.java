package dev.ixpu.cullingames.wave;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import dev.ixpu.cullingames.CullingGamesPlugin;
import dev.ixpu.cullingames.config.ConfigManager;
import dev.ixpu.cullingames.manager.EventManager;

public class WaveManager {

    private final CullingGamesPlugin plugin;
    private final EventManager eventManager;
    private final ConfigManager configManager;
    
    private List<WaveDefinition> waves = new ArrayList<>();
    private int currentWaveIndex = 0;
    private BukkitTask waveTask;
    private BukkitTask cleanupTask;
    
    private final Set<UUID> spawnedWaveMobs = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> waveMobPoints = new ConcurrentHashMap<>();

    public WaveManager(CullingGamesPlugin plugin, EventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.configManager = plugin.getConfigManager();
        this.waves = configManager.loadWaves();
    }

    public void startWaves() {
        if (waves.isEmpty()) {
            plugin.getLogger().warning("No waves configured!");
            return;
        }
        
        currentWaveIndex = 0;
        scheduleNextWave();
        startCleanupTask();
    }

    public void stopWaves() {
        if (waveTask != null) {
            waveTask.cancel();
            waveTask = null;
        }
        
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        
        despawnAllWaveMobs();
        spawnedWaveMobs.clear();
        waveMobPoints.clear();
        currentWaveIndex = 0;
    }

    private void scheduleNextWave() {
        if (currentWaveIndex >= waves.size()) {
            Bukkit.broadcast(net.kyori.adventure.text.Component.text("All waves completed!", net.kyori.adventure.text.format.NamedTextColor.GREEN));
            return;
        }
        
        WaveDefinition wave = waves.get(currentWaveIndex);
        int waveDurationTicks = (int) (configManager.getWaveDuration() * 20L);
        
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("[Wave " + (currentWaveIndex + 1) + "] " + wave.getName() + " starting!", net.kyori.adventure.text.format.NamedTextColor.YELLOW));
        
        spawnWaveMobs(wave);
        
        waveTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            currentWaveIndex++;
            scheduleNextWave();
        }, waveDurationTicks);
    }

    private void spawnWaveMobs(WaveDefinition wave) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        if (onlinePlayers.isEmpty()) {
            return;
        }
        
        // Spawn main wave mobs
        for (MobSpawn mobSpawn : wave.getMobs()) {
            for (int i = 0; i < mobSpawn.getAmount(); i++) {
                // Stagger spawns over wave duration
                long delay = (long) ((configManager.getWaveDuration() / mobSpawn.getAmount()) * 20 * i);
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Player target = onlinePlayers.get(new Random().nextInt(onlinePlayers.size()));
                    spawnMobAtLocation(mobSpawn, target.getLocation());
                }, delay);
            }
        }
        
        // Spawn general baby creepers (10% chance)
        int totalMobs = wave.getMobs().stream().mapToInt(MobSpawn::getAmount).sum();
        int babyCreepersToSpawn = Math.max(1, (int) (totalMobs * 0.1));
        
        MobSpawnData babyCreepData = configManager.getBabyCreeper();
        if (babyCreepData != null) {
            MobSpawn babyCreeper = new MobSpawn(babyCreepData);
            for (int i = 0; i < babyCreepersToSpawn; i++) {
                long delay = (long) (Math.random() * configManager.getWaveDuration() * 20);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Player target = onlinePlayers.get(new Random().nextInt(onlinePlayers.size()));
                    spawnMobAtLocation(babyCreeper, target.getLocation());
                }, delay);
            }
        }
    }

    private void spawnMobAtLocation(MobSpawn mobSpawn, Location loc) {
        if (!eventManager.isActive()) return;
        
        try {
            // Spawn using MobBuilder instead of command execution
            org.bukkit.entity.LivingEntity entity = MobBuilder.spawnMob(loc, mobSpawn.getMobData());
            
            if (entity != null) {
                // Register the mob for tracking and points
                registerWaveMob(entity.getUniqueId(), mobSpawn.getPointValue());
            } else {
                plugin.getLogger().warning(() -> "Failed to spawn mob: " + mobSpawn.getMobTypeId());
            }
        } catch (Exception e) {
            plugin.getLogger().warning(() -> "Failed to spawn mob: " + e.getMessage());
        }
    }

    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Remove dead/invalid entities from tracking
            spawnedWaveMobs.removeIf(uuid -> {
                Entity e = Bukkit.getEntity(uuid);
                return e == null || !e.isValid() || e.isDead();
            });
            
            // Sync wave mob points map
            waveMobPoints.entrySet().removeIf(e -> !spawnedWaveMobs.contains(e.getKey()));
        }, 0L, 30 * 20L); // Every 30 seconds
    }

    public void registerWaveMob(UUID entityUUID, int pointValue) {
        spawnedWaveMobs.add(entityUUID);
        waveMobPoints.put(entityUUID, pointValue);
    }

    public boolean isWaveMob(UUID entityUUID) {
        return spawnedWaveMobs.contains(entityUUID);
    }

    public int getPointsForMob(UUID entityUUID) {
        return waveMobPoints.getOrDefault(entityUUID, 0);
    }

    public void despawnAllWaveMobs() {
        for (UUID uuid : new ArrayList<>(spawnedWaveMobs)) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
    }
}