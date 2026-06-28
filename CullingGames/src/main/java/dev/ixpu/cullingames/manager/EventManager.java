package dev.ixpu.cullingames.manager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import dev.ixpu.cullingames.CullingGamesPlugin;
import dev.ixpu.cullingames.wave.WaveManager;

public class EventManager {

    private final CullingGamesPlugin plugin;
    private final BukkitScheduler scheduler;
    private final WaveManager waveManager;
    
    // Core state
    private volatile boolean active = false;
    private volatile boolean paused = false;
    private volatile int seconds = 0;
    private volatile String cachedTimerString = "00:00";
    private int lastFormattedSecond = -1;
    
    // Participants and points
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Double> points = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> mobKills = new ConcurrentHashMap<>();
    private final Map<UUID, Double> deathLost = new ConcurrentHashMap<>();
    
    // Cooldowns (using timestamps for efficiency)
    private final Map<String, Long> killCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> enderPearlCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> windChargeCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> tridentCooldowns = new ConcurrentHashMap<>();
    
    // Force stop confirmation
    private volatile boolean forceStopConfirmPending = false;
    private BukkitTask forceStopExpiryTask;
    
    // Tasks
    private BukkitTask timerTask;

    public EventManager(CullingGamesPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getScheduler();
        this.waveManager = new WaveManager(plugin, this);
        startCooldownCleanupTask();
    }

    // Start the event
    public void startEvent(Set<UUID> eventPlayers) {
        if (active) return;
        
        active = true;
        paused = false;
        seconds = 0;
        cachedTimerString = "00:00";
        lastFormattedSecond = 0;
        forceStopConfirmPending = false;
        
        players.clear();
        points.clear();
        mobKills.clear();
        deathLost.clear();
        killCooldowns.clear();
        
        // Initialize points for all players
        for (UUID uuid : eventPlayers) {
            players.add(uuid);
            points.put(uuid, 10.0);
            mobKills.put(uuid, 0);
        }
        
        // Start timer task
        timerTask = scheduler.runTaskTimer(plugin, () -> updateTimer(), 0L, 20L);
        
        // Start wave system
        waveManager.startWaves();
    }

    // Pause the event
    public void togglePause() {
        paused = !paused;
    }

    // Force stop with confirmation
    public void armForceStop() {
        if (!active) return;
        forceStopConfirmPending = true;
        
        if (forceStopExpiryTask != null) {
            forceStopExpiryTask.cancel();
        }
        
        forceStopExpiryTask = scheduler.runTaskLater(plugin, () -> {
            forceStopConfirmPending = false;
        }, 200L); // 10 seconds
    }

    // Confirm and execute force stop
    public boolean confirmForceStop() {
        if (!forceStopConfirmPending || !active) {
            return false;
        }
        
        String finalTimer = cachedTimerString;
        shutdown();
        
        Bukkit.broadcast(net.kyori.adventure.text.Component.text("CullingGames forcefully stopped. Final time: " + finalTimer, net.kyori.adventure.text.format.NamedTextColor.RED));
        return true;
    }

    // Shutdown the event completely
    public void shutdown() {
        active = false;
        paused = false;
        forceStopConfirmPending = false;
        seconds = 0;
        cachedTimerString = "00:00";
        
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        
        if (forceStopExpiryTask != null) {
            forceStopExpiryTask.cancel();
            forceStopExpiryTask = null;
        }
        
        waveManager.stopWaves();
        
        players.clear();
        points.clear();
        mobKills.clear();
        deathLost.clear();
        killCooldowns.clear();
        enderPearlCooldowns.clear();
        windChargeCooldowns.clear();
        tridentCooldowns.clear();
    }

    // Update timer and formatted string
    private void updateTimer() {
        if (!active || paused) return;
        
        seconds++;
        
        if (lastFormattedSecond != seconds) {
            cachedTimerString = String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
            lastFormattedSecond = seconds;
        }
    }

    // Cleanup cooldowns in batch (every 30 seconds)
    private void startCooldownCleanupTask() {
        scheduler.runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            
            killCooldowns.entrySet().removeIf(e -> e.getValue() < now);
            enderPearlCooldowns.entrySet().removeIf(e -> e.getValue() < now);
            windChargeCooldowns.entrySet().removeIf(e -> e.getValue() < now);
            tridentCooldowns.entrySet().removeIf(e -> e.getValue() < now);
        }, 0L, 30 * 20L);
    }

    // ===== PLAYER MANAGEMENT =====

    public boolean addPlayer(UUID uuid) {
        return players.add(uuid);
    }

    public boolean removePlayer(UUID uuid) {
        boolean removed = players.remove(uuid);
        if (removed) {
            points.remove(uuid);
            mobKills.remove(uuid);
            deathLost.remove(uuid);
        }
        return removed;
    }

    public boolean isParticipant(UUID uuid) {
        return players.contains(uuid);
    }

    public Set<UUID> getPlayers() {
        return new HashSet<>(players);
    }

    // ===== POINTS MANAGEMENT =====

    public double getPoints(UUID uuid) {
        return active ? points.getOrDefault(uuid, 0.0) : 0.0;
    }

    public void setPoints(UUID uuid, double amount) {
        if (isParticipant(uuid)) {
            points.put(uuid, Math.max(0, amount));
        }
    }

    public void addPoints(UUID uuid, double amount) {
        if (isParticipant(uuid)) {
            points.put(uuid, Math.max(0, points.getOrDefault(uuid, 0.0) + amount));
        }
    }

    public void subtractPoints(UUID uuid, double amount) {
        if (isParticipant(uuid)) {
            points.put(uuid, Math.max(0, points.getOrDefault(uuid, 0.0) - amount));
        }
    }

    // ===== MOB KILLS =====

    public int getMobKills(UUID uuid) {
        return active ? mobKills.getOrDefault(uuid, 0) : 0;
    }

    public void incrementMobKill(UUID uuid) {
        if (isParticipant(uuid)) {
            mobKills.put(uuid, mobKills.getOrDefault(uuid, 0) + 1);
        }
    }

    // ===== DEATH TRACKING =====

    public void setDeathLost(UUID uuid, double amount) {
        deathLost.put(uuid, amount);
    }

    public double getAndClearDeathLost(UUID uuid) {
        return deathLost.getOrDefault(uuid, 0.0);
    }

    public boolean hasDeathLost(UUID uuid) {
        return deathLost.containsKey(uuid);
    }

    // ===== KILL COOLDOWNS =====

    public boolean isKillCooldownActive(UUID killer, UUID victim) {
        String key = killer + ":" + victim;
        return killCooldowns.getOrDefault(key, 0L) > System.currentTimeMillis();
    }

    public void setKillCooldown(UUID killer, UUID victim) {
        String key = killer + ":" + victim;
        long expiry = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes
        killCooldowns.put(key, expiry);
    }

    // ===== ITEM COOLDOWNS =====

    public boolean isEnderPearlCooldownActive(UUID uuid) {
        return enderPearlCooldowns.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }

    public void setEnderPearlCooldown(UUID uuid) {
        long expiry = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
        enderPearlCooldowns.put(uuid, expiry);
    }

    public boolean isWindChargeCooldownActive(UUID uuid) {
        return windChargeCooldowns.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }

    public void setWindChargeCooldown(UUID uuid) {
        long expiry = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
        windChargeCooldowns.put(uuid, expiry);
    }

    public boolean isTridentCooldownActive(UUID uuid) {
        return tridentCooldowns.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }

    public void setTridentCooldown(UUID uuid) {
        long expiry = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
        tridentCooldowns.put(uuid, expiry);
    }

    // ===== STATE GETTERS =====

    public boolean isActive() {
        return active;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isForceStopConfirmPending() {
        return forceStopConfirmPending;
    }

    public int getSeconds() {
        return seconds;
    }

    public String getTimerFormatted() {
        return cachedTimerString;
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }

    public int getPlayerKillCount(UUID uuid) {
        // Count unique victims (entries in kill cooldown map with this killer)
        return (int) killCooldowns.keySet().stream()
            .filter(key -> key.startsWith(uuid + ":"))
            .count();
    }
}