package dev.ixpu.coordssync.database;

public class PlayerData {
    private String playerUUID;
    private String playerName;
    private String world;
    private double x, y, z;
    private float yaw, pitch;
    private String serverId;
    private long lastUpdated;
    
    // Constructor
    public PlayerData(String playerUUID, String playerName, String world,
                     double x, double y, double z, float yaw, float pitch,
                     String serverId) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.serverId = serverId;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getPlayerUUID() { return playerUUID; }
    public void setPlayerUUID(String playerUUID) { this.playerUUID = playerUUID; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }
    
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
    
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}