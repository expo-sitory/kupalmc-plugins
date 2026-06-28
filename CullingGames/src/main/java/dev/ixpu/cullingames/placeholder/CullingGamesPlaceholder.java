package dev.ixpu.cullingames.placeholder;

import dev.ixpu.cullingames.api.CullingGamesAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for CullingGames
 * Properly discoverable by PlaceholderAPI and other plugins (TAB, etc)
 * 
 * Placeholders:
 * - %cg_points% - Player's current points
 * - %cg_playerkills% - Player kills count
 * - %cg_mobkills% - Mob kills count
 * - %cg_status% - Event status (active/inactive)
 * - %cg_timer% - Formatted timer (MM:SS)
 * 
 * Usage in TAB scoreboard:
 * header: "CullingGames"
 * footer: "Points: %cg_points% | Kills: %cg_mobkills%"
 */
public class CullingGamesPlaceholder extends PlaceholderExpansion {

    @Override
    @NotNull
    public String getIdentifier() {
        return "cg";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "ixpu";
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        return switch (params.toLowerCase()) {
            case "points" -> String.valueOf(CullingGamesAPI.getPlayerPoints(player.getUniqueId()));
            case "playerkills" -> String.valueOf(CullingGamesAPI.getPlayerKills(player.getUniqueId()));
            case "mobkills" -> String.valueOf(CullingGamesAPI.getPlayerMobKills(player.getUniqueId()));
            case "status" -> CullingGamesAPI.getStatus();
            case "timer" -> CullingGamesAPI.getTimerFormatted();
            default -> null;
        };
    }
}