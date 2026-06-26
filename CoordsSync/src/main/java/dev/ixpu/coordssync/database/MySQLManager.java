package dev.ixpu.coordssync.database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import static org.bukkit.Bukkit.getLogger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import dev.ixpu.coordssync.config.ConfigManager;


 // Handles connection pooling, coordinate storage, and retrieval

public class MySQLManager {
    private HikariDataSource dataSource;
    private ConfigManager config;
    private boolean connected = false;
    
    @SuppressWarnings("CallToPrintStackTrace")
    public MySQLManager(ConfigManager config) {
        this.config = config;
        try {
            initializePool();
            this.connected = true;
            Bukkit.getLogger().info("[CoordsSync] MySQL connection pool initialized successfully");
        } catch (Exception e) {
            this.connected = false;
            Bukkit.getLogger().severe(() -> "[CoordsSync] Failed to initialize MySQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Initializes HikariCP connection pool

    private void initializePool() {
        HikariConfig hikariConfig = new HikariConfig();
        
        String jdbcUrl = String.format(
            "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
            config.getDbHost(),
            config.getDbPort(),
            config.getDbName()
        );
        
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getDbUsername());
        hikariConfig.setPassword(config.getDbPassword());
        hikariConfig.setMaximumPoolSize(config.getMaxConnections());
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
        hikariConfig.setIdleTimeout(600000); // 10 minutes
        hikariConfig.setMaxLifetime(1800000); // 30 minutes
        hikariConfig.setAutoCommit(true);
        hikariConfig.setPoolName("CoordsSync-Pool");
        
        this.dataSource = new HikariDataSource(hikariConfig);
    }
    
    public boolean savePlayerCoordinates(PlayerData data) {
        if (!connected || dataSource == null) {
            return false;
        }
        
        String sql = "INSERT INTO player_coordinates " +
                     "(player_uuid, player_name, world, x, y, z, yaw, pitch, server_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "player_name=?, world=?, x=?, y=?, z=?, yaw=?, pitch=?, server_id=?, last_updated=CURRENT_TIMESTAMP";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // INSERT values
            stmt.setString(1, data.getPlayerUUID());
            stmt.setString(2, data.getPlayerName());
            stmt.setString(3, data.getWorld());
            stmt.setDouble(4, data.getX());
            stmt.setDouble(5, data.getY());
            stmt.setDouble(6, data.getZ());
            stmt.setFloat(7, data.getYaw());
            stmt.setFloat(8, data.getPitch());
            stmt.setString(9, data.getServerId());
            
            // UPDATE values (duplicate key)
            stmt.setString(10, data.getPlayerName());
            stmt.setString(11, data.getWorld());
            stmt.setDouble(12, data.getX());
            stmt.setDouble(13, data.getY());
            stmt.setDouble(14, data.getZ());
            stmt.setFloat(15, data.getYaw());
            stmt.setFloat(16, data.getPitch());
            stmt.setString(17, data.getServerId());
            
            stmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            Bukkit.getLogger().warning(() -> "[CoordsSync] Failed to save coordinates to MySQL for " + 
                    data.getPlayerName() + ": " + e.getMessage());
            return false;
        }
    }
    
    public PlayerData getPlayerCoordinates(String playerUUID) {
        if (!connected || dataSource == null) {
            return null;
        }
        
        String sql = "SELECT * FROM player_coordinates WHERE player_uuid = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPlayerData(rs);
                }
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().warning(() -> "[CoordsSync] Failed to retrieve coordinates from MySQL for UUID " + 
                    playerUUID + ": " + e.getMessage());
        }
        
        return null;
    }
    
    public List<PlayerData> getServerCoordinates(String serverId) {
        if (!connected || dataSource == null) {
            return new ArrayList<>();
        }
        
        String sql = "SELECT * FROM player_coordinates WHERE server_id = ?";
        List<PlayerData> coordinatesList = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, serverId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    coordinatesList.add(mapResultSetToPlayerData(rs));
                }
            }
            
        } catch (SQLException e) {
            getLogger().warning(() -> "[CoordsSync] Failed to retrieve coordinates for server " + 
                    serverId + ": " + e.getMessage());
        }
        
        return coordinatesList;
    }
    
    public boolean deletePlayerCoordinates(String playerUUID) {
        if (!connected || dataSource == null) {
            return false;
        }
        
        String sql = "DELETE FROM player_coordinates WHERE player_uuid = ?";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID);
            stmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            Bukkit.getLogger().warning(() -> "[CoordsSync] Failed to delete coordinates for UUID " + 
                    playerUUID + ": " + e.getMessage());
            return false;
        }
    }
    
    public boolean isConnected() {
        if (!connected || dataSource == null) {
            return false;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            return !conn.isClosed();
        } catch (SQLException e) {
            connected = false;
            return false;
        }
    }
    
    public String getPoolStats() {
        if (dataSource == null) {
            return "Database not connected";
        }
        return String.format("Active: %d, Idle: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections());
    }
    
    @SuppressWarnings("CallToPrintStackTrace")
    public void initializeTables() {
        if (!connected || dataSource == null) {
            return;
        }
        
        String createTableSQL = "CREATE TABLE IF NOT EXISTS player_coordinates (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL UNIQUE," +
                "player_name VARCHAR(16) NOT NULL," +
                "world VARCHAR(100)," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw FLOAT," +
                "pitch FLOAT," +
                "server_id VARCHAR(50)," +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "INDEX idx_uuid (player_uuid)," +
                "INDEX idx_server (server_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
            
            stmt.executeUpdate();
            Bukkit.getLogger().info("[CoordsSync] Database tables verified/created successfully");
            
        } catch (SQLException e) {
            Bukkit.getLogger().severe(() -> "[CoordsSync] Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private PlayerData mapResultSetToPlayerData(ResultSet rs) throws SQLException {
        return new PlayerData(
            rs.getString("player_uuid"),
            rs.getString("player_name"),
            rs.getString("world"),
            rs.getDouble("x"),
            rs.getDouble("y"),
            rs.getDouble("z"),
            rs.getFloat("yaw"),
            rs.getFloat("pitch"),
            rs.getString("server_id")
        );
    }
    
    // Cleans up database connection pool
    // Should be called on plugin shutdown
    
    public void close() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                connected = false;
                Bukkit.getLogger().info("[CoordsSync] MySQL connection pool closed");
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning(() -> "[CoordsSync] Error closing MySQL connection: " + e.getMessage());
        }
    }
}