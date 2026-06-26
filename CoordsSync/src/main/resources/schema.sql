-- Create database
CREATE DATABASE IF NOT EXISTS coordssync;
USE coordssync;

-- Player coordinates table
CREATE TABLE IF NOT EXISTS player_coordinates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL UNIQUE,
    player_name VARCHAR(16) NOT NULL,
    world VARCHAR(100),
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL,
    z DOUBLE NOT NULL,
    yaw FLOAT,
    pitch FLOAT,
    server_id VARCHAR(50),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uuid (player_uuid),
    INDEX idx_server (server_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Server status table (optional, for tracking server health)
CREATE TABLE IF NOT EXISTS server_status (
    server_id VARCHAR(50) PRIMARY KEY,
    last_heartbeat TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status ENUM('online', 'offline') DEFAULT 'online'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;