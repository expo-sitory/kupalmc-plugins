package dev.ixpu.advancementsync.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import dev.ixpu.advancementsync.config.ConfigManager;

public class MySQLManager {

    private final HikariDataSource dataSource;
    private final ConfigManager configManager;

    public MySQLManager(ConfigManager configManager) throws SQLException {
        this.configManager = configManager;
        this.dataSource = initializePool();
        initializeDatabase();
    }

    private HikariDataSource initializePool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + configManager.getMysqlHost() + ":" + 
            configManager.getMysqlPort() + "/" + configManager.getMysqlDatabase());
        config.setUsername(configManager.getMysqlUsername());
        config.setPassword(configManager.getMysqlPassword());
        config.setMaximumPoolSize(configManager.getMysqlPoolSize());
        config.setPoolName(configManager.getMysqlPoolName());
        config.setConnectionTimeout(configManager.getMysqlConnectionTimeoutMs());
        config.setIdleTimeout(configManager.getMysqlIdleTimeoutMs());
        config.setMaxLifetime(configManager.getMysqlMaxLifetimeMs());
        config.setAutoCommit(true);
        config.setLeakDetectionThreshold(60000);

        return new HikariDataSource(config);
    }

    private void initializeDatabase() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS advancements_data (
                id INT AUTO_INCREMENT PRIMARY KEY,
                player_uuid CHAR(36) NOT NULL,
                advancement_key VARCHAR(255) NOT NULL,
                completed_at BIGINT NOT NULL,
                last_updated BIGINT NOT NULL,
                UNIQUE KEY unique_player_advancement (player_uuid, advancement_key),
                KEY idx_player_uuid (player_uuid),
                KEY idx_last_updated (last_updated)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
            stmt.execute();
        }
    }


    // Get a connection from the pool

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }


    // Store advancement data in MySQL

    public boolean storeAdvancement(String playerUUID, String advancementKey, long completedAt) {
        String sql = """
            INSERT INTO advancements_data (player_uuid, advancement_key, completed_at, last_updated)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE 
                completed_at = VALUES(completed_at),
                last_updated = VALUES(last_updated)
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, advancementKey);
            stmt.setLong(3, completedAt);
            stmt.setLong(4, System.currentTimeMillis());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }


    // Get all advancements for a player from MySQL
 
    public Map<String, Long> getPlayerAdvancements(String playerUUID) {
        Map<String, Long> advancements = new HashMap<>();
        String sql = "SELECT advancement_key, completed_at FROM advancements_data WHERE player_uuid = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    advancements.put(rs.getString("advancement_key"), rs.getLong("completed_at"));
                }
            }
        } catch (SQLException e) {
            return advancements;
        }

        return advancements;
    }


    // Get advancements completed after a specific timestamp

    public Map<String, Long> getAdvancementsAfter(String playerUUID, long timestamp) {
        Map<String, Long> advancements = new HashMap<>();
        String sql = "SELECT advancement_key, completed_at FROM advancements_data " +
                    "WHERE player_uuid = ? AND completed_at > ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID);
            stmt.setLong(2, timestamp);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    advancements.put(rs.getString("advancement_key"), rs.getLong("completed_at"));
                }
            }
        } catch (SQLException e) {
            return advancements;
        }

        return advancements;
    }


    // Delete all advancements for a player

    public boolean deletePlayerAdvancements(String playerUUID) {
        String sql = "DELETE FROM advancements_data WHERE player_uuid = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }


    // Delete specific advancement for a player

    public boolean deleteAdvancement(String playerUUID, String advancementKey) {
        String sql = "DELETE FROM advancements_data WHERE player_uuid = ? AND advancement_key = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, advancementKey);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }


    // Get advancement count for a player

    public int getAdvancementCount(String playerUUID) {
        String sql = "SELECT COUNT(*) as count FROM advancements_data WHERE player_uuid = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUUID);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            return 0;
        }

        return 0;
    }


    // Test connection

    public boolean testConnection() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
            stmt.execute();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    // Shutdown the connection pool

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}