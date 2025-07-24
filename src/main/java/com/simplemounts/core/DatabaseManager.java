package com.simplemounts.core;

import com.simplemounts.SimpleMounts;
import com.simplemounts.data.MountData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {
    
    private final SimpleMounts plugin;
    private HikariDataSource dataSource;
    
    private static final String CREATE_PLAYER_MOUNTS_TABLE = """
        CREATE TABLE IF NOT EXISTS player_mounts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            player_uuid TEXT NOT NULL,
            mount_name TEXT NOT NULL,
            mount_type TEXT NOT NULL,
            mount_data TEXT NOT NULL,
            chest_inventory TEXT,
            created_at INTEGER NOT NULL,
            last_accessed INTEGER NOT NULL,
            UNIQUE(player_uuid, mount_name)
        )
    """;
    
    private static final String CREATE_ACTIVE_MOUNTS_TABLE = """
        CREATE TABLE IF NOT EXISTS active_mounts (
            entity_uuid TEXT PRIMARY KEY,
            player_uuid TEXT NOT NULL,
            mount_name TEXT NOT NULL,
            world_name TEXT NOT NULL,
            x REAL NOT NULL,
            y REAL NOT NULL,
            z REAL NOT NULL,
            spawned_at INTEGER NOT NULL
        )
    """;
    
    private static final String CREATE_PLUGIN_CONFIG_TABLE = """
        CREATE TABLE IF NOT EXISTS plugin_config (
            key TEXT PRIMARY KEY,
            value TEXT NOT NULL
        )
    """;
    
    private static final String CREATE_INDEXES = """
        CREATE INDEX IF NOT EXISTS idx_player_mounts_player_uuid ON player_mounts(player_uuid);
        CREATE INDEX IF NOT EXISTS idx_player_mounts_mount_type ON player_mounts(mount_type);
        CREATE INDEX IF NOT EXISTS idx_active_mounts_player_uuid ON active_mounts(player_uuid);
        CREATE INDEX IF NOT EXISTS idx_active_mounts_world_name ON active_mounts(world_name);
    """;
    
    public DatabaseManager(SimpleMounts plugin) {
        this.plugin = plugin;
    }
    
    public boolean initialize() {
        try {
            setupDataSource();
            createTables();
            plugin.getLogger().info("Database initialized successfully");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }
    
    private void setupDataSource() throws SQLException {
        File dbFile = new File(plugin.getDataFolder(), "mounts.db");
        
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        dataSource = new HikariDataSource(config);
    }
    
    private void createTables() throws SQLException {
        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement()) {
            
            stmt.execute(CREATE_PLAYER_MOUNTS_TABLE);
            stmt.execute(CREATE_ACTIVE_MOUNTS_TABLE);
            stmt.execute(CREATE_PLUGIN_CONFIG_TABLE);
            
            for (String index : CREATE_INDEXES.split(";")) {
                if (!index.trim().isEmpty()) {
                    stmt.execute(index.trim());
                }
            }
        }
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public void closeConnection() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
    
    public CompletableFuture<Boolean> saveMountData(UUID playerUuid, String mountName, String mountType, 
                                                   String mountData, String chestInventory) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO player_mounts 
                (player_uuid, mount_name, mount_type, mount_data, chest_inventory, created_at, last_accessed)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                long currentTime = System.currentTimeMillis();
                
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, mountName);
                stmt.setString(3, mountType);
                stmt.setString(4, mountData);
                stmt.setString(5, chestInventory);
                stmt.setLong(6, currentTime);
                stmt.setLong(7, currentTime);
                
                return stmt.executeUpdate() > 0;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save mount data", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<MountData> getMountData(UUID playerUuid, String mountName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT mount_type, mount_data, chest_inventory, created_at, last_accessed
                FROM player_mounts
                WHERE player_uuid = ? AND mount_name = ?
            """;
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, mountName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new MountData(
                            playerUuid,
                            mountName,
                            rs.getString("mount_type"),
                            rs.getString("mount_data"),
                            rs.getString("chest_inventory"),
                            rs.getLong("created_at"),
                            rs.getLong("last_accessed")
                        );
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get mount data", e);
            }
            
            return null;
        });
    }
    
    public CompletableFuture<List<MountData>> getPlayerMounts(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT mount_name, mount_type, mount_data, chest_inventory, created_at, last_accessed
                FROM player_mounts
                WHERE player_uuid = ?
                ORDER BY last_accessed DESC
            """;
            
            List<MountData> mounts = new ArrayList<>();
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        mounts.add(new MountData(
                            playerUuid,
                            rs.getString("mount_name"),
                            rs.getString("mount_type"),
                            rs.getString("mount_data"),
                            rs.getString("chest_inventory"),
                            rs.getLong("created_at"),
                            rs.getLong("last_accessed")
                        ));
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player mounts", e);
            }
            
            return mounts;
        });
    }
    
    public CompletableFuture<Boolean> deleteMountData(UUID playerUuid, String mountName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM player_mounts WHERE player_uuid = ? AND mount_name = ?";
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, mountName);
                
                return stmt.executeUpdate() > 0;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete mount data", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> updateLastAccessed(UUID playerUuid, String mountName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE player_mounts SET last_accessed = ? WHERE player_uuid = ? AND mount_name = ?";
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setLong(1, System.currentTimeMillis());
                stmt.setString(2, playerUuid.toString());
                stmt.setString(3, mountName);
                
                return stmt.executeUpdate() > 0;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update last accessed", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Integer> getPlayerMountCount(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM player_mounts WHERE player_uuid = ?";
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player mount count", e);
            }
            
            return 0;
        });
    }
    
    public CompletableFuture<Integer> getPlayerMountCountByType(UUID playerUuid, String mountType) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM player_mounts WHERE player_uuid = ? AND mount_type = ?";
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, mountType);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player mount count by type", e);
            }
            
            return 0;
        });
    }
    
    public CompletableFuture<Boolean> addActiveMount(UUID entityUuid, UUID playerUuid, String mountName, 
                                                    String worldName, double x, double y, double z) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO active_mounts
                (entity_uuid, player_uuid, mount_name, world_name, x, y, z, spawned_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, entityUuid.toString());
                stmt.setString(2, playerUuid.toString());
                stmt.setString(3, mountName);
                stmt.setString(4, worldName);
                stmt.setDouble(5, x);
                stmt.setDouble(6, y);
                stmt.setDouble(7, z);
                stmt.setLong(8, System.currentTimeMillis());
                
                return stmt.executeUpdate() > 0;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add active mount", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> removeActiveMount(UUID entityUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM active_mounts WHERE entity_uuid = ?";
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, entityUuid.toString());
                
                return stmt.executeUpdate() > 0;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove active mount", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<List<UUID>> getPlayerActiveMounts(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT entity_uuid FROM active_mounts WHERE player_uuid = ?";
            
            List<UUID> activeUuids = new ArrayList<>();
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        activeUuids.add(UUID.fromString(rs.getString("entity_uuid")));
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player active mounts", e);
            }
            
            return activeUuids;
        });
    }
    
    public CompletableFuture<Boolean> cleanupOrphanedMounts() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM active_mounts WHERE spawned_at < ?";
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 hours ago
                stmt.setLong(1, cutoffTime);
                
                int deleted = stmt.executeUpdate();
                if (deleted > 0) {
                    plugin.getLogger().info("Cleaned up " + deleted + " orphaned active mount entries");
                }
                
                return true;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to cleanup orphaned mounts", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> setConfigValue(String key, String value) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT OR REPLACE INTO plugin_config (key, value) VALUES (?, ?)";
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, key);
                stmt.setString(2, value);
                
                return stmt.executeUpdate() > 0;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set config value", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<String> getConfigValue(String key) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT value FROM plugin_config WHERE key = ?";
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, key);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("value");
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get config value", e);
            }
            
            return null;
        });
    }
}