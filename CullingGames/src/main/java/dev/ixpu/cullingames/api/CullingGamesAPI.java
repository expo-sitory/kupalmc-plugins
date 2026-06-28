package dev.ixpu.cullingames.api;

import java.util.UUID;

import dev.ixpu.cullingames.manager.EventManager;

public class CullingGamesAPI {

    private static EventManager manager;

    public static void setManager(EventManager eventManager) {
        manager = eventManager;
    }

    // Get a player's current points in the event

    public static double getPlayerPoints(UUID player) {
        return manager != null ? manager.getPoints(player) : 0;
    }

    // Get the number of unique players this player has killed
     
    public static int getPlayerKills(UUID player) {
        return manager != null ? manager.getPlayerKillCount(player) : 0;
    }

    // Get the number of mobs this player has killed
     
    public static int getPlayerMobKills(UUID player) {
        return manager != null ? manager.getMobKills(player) : 0;
    }

    // Get the current event status (ACTIVE, PAUSED, INACTIVE)
     
    public static String getStatus() {
        if (manager == null) {
            return "INACTIVE";
        }
        
        if (!manager.isActive()) {
            return "INACTIVE";
        }
        
        return manager.isPaused() ? "PAUSED" : "ACTIVE";
    }

    // Get the formatted timer string (HH:MM:SS)
     
    public static String getTimerFormatted() {
        return manager != null ? manager.getTimerFormatted() : "00:00";
    }

    // Check if the event is currently running
     
    public static boolean isEventActive() {
        return manager != null && manager.isActive();
    }

    // Get the EventManager instance for advanced usage
     
    public static EventManager getEventManager() {
        return manager;
    }
}