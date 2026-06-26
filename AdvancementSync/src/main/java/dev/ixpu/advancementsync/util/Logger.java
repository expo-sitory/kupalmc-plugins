package dev.ixpu.advancementsync.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.bukkit.plugin.java.JavaPlugin;

public class Logger {

    private final JavaPlugin plugin;
    private final String prefix = "[AdvancementSync] ";
    private boolean debugEnabled;
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Logger(JavaPlugin plugin) {
        this.plugin = plugin;
        this.debugEnabled = true;
    }

    public void setDebugEnabled(boolean enabled) {
        this.debugEnabled = enabled;
    }

    public void info(String message) {
        plugin.getLogger().info(message);
    }

    public void warn(String message) {
        plugin.getLogger().warning(String.format("%s%s", prefix, message));
    }

    public void error(String message) {
        plugin.getLogger().severe(String.format("%s%s", prefix, message));
    }

    public void error(String message, Throwable throwable) {
        plugin.getLogger().severe(String.format("%s%s", prefix, message));
        throwable.printStackTrace(System.err);
    }

    public void debug(String message) {
        if (debugEnabled) {
            plugin.getLogger().info(String.format("%s[DEBUG] %s", prefix, message));
        }
    }

    public void debug(String message, Object... args) {
        if (debugEnabled) {
            plugin.getLogger().info(String.format("%s[DEBUG] %s", prefix, String.format(message, args)));
        }
    }

    public void sync(String message) {
        if (debugEnabled) {
            plugin.getLogger().info(String.format("%s[SYNC] %s", prefix, message));
        }
    }

    public void sync(String message, Object... args) {
        if (debugEnabled) {
            plugin.getLogger().info(String.format("%s[SYNC] %s", prefix, String.format(message, args)));
        }
    }

    public void logToFile(String message) {
        try {
            String logFile = String.format("%s/logs/advancement-sync.log", plugin.getDataFolder());
            Files.createDirectories(Paths.get(String.format("%s/logs", plugin.getDataFolder())));
            
            String timestamp = LocalDateTime.now().format(timeFormat);
            String logEntry = String.format("[%s] %s%n", timestamp, message);
            
            Files.write(Paths.get(logFile), logEntry.getBytes(), 
                java.nio.file.StandardOpenOption.CREATE, 
                java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            error("Failed to write to log file", e);
        }
    }
}