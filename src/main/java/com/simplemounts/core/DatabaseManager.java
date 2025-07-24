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
            mount_name TEXT,
            mount_type TEXT NOT NULL,
            mount_data TEXT NOT NULL,
            chest_inventory TEXT,
            created_at INTEGER NOT NULL,
            last_accessed INTEGER NOT NULL
        )
    """;
    
    private static final String CREATE_ACTIVE_MOUNTS_TABLE = """
        CREATE TABLE IF NOT EXISTS active_mounts (
            entity_uuid TEXT PRIMARY KEY,
            player_uuid TEXT NOT NULL,
            mount_id INTEGER NOT NULL,
            mount_name TEXT,
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
            migrateDatabaseSchema();
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
    
    private void migrateDatabaseSchema() throws SQLException {
        try (Connection conn = getConnection()) {
            // Check if we need to migrate from old schema
            boolean needsMigration = false;
            
            // Check if the old unique constraint exists
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(player_mounts)");
                boolean hasUniqueConstraint = false;
                
                // Check if we have the old schema by looking for unique constraint
                try (Statement constraintStmt = conn.createStatement()) {
                    ResultSet constraintRs = constraintStmt.executeQuery(
                        "SELECT sql FROM sqlite_master WHERE type='table' AND name='player_mounts'"
                    );
                    if (constraintRs.next()) {
                        String tableSql = constraintRs.getString("sql");
                        if (tableSql.contains("UNIQUE(player_uuid, mount_name)")) {
                            needsMigration = true;
                        }
                    }
                }
                
                if (needsMigration) {
                    plugin.getLogger().info("Migrating database schema to support duplicate mount names...");
                    
                    // Create temporary table with new schema
                    stmt.execute("""
                        CREATE TABLE player_mounts_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            player_uuid TEXT NOT NULL,
                            mount_name TEXT,
                            mount_type TEXT NOT NULL,
                            mount_data TEXT NOT NULL,
                            chest_inventory TEXT,
                            created_at INTEGER NOT NULL,
                            last_accessed INTEGER NOT NULL
                        )
                    """);
                    
                    // Copy data from old table to new table
                    stmt.execute("""
                        INSERT INTO player_mounts_new 
                        (id, player_uuid, mount_name, mount_type, mount_data, chest_inventory, created_at, last_accessed)
                        SELECT id, player_uuid, mount_name, mount_type, mount_data, chest_inventory, created_at, last_accessed
                        FROM player_mounts
                    """);
                    
                    // Drop old table and rename new table
                    stmt.execute("DROP TABLE player_mounts");
                    stmt.execute("ALTER TABLE player_mounts_new RENAME TO player_mounts");
                    
                    // Recreate indexes
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_mounts_player_uuid ON player_mounts(player_uuid)");
                    stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_mounts_mount_type ON player_mounts(mount_type)");
                    
                    plugin.getLogger().info("Database schema migration completed successfully");
                }
                
                // Check if active_mounts table needs mount_id column
                boolean needsActiveMountsMigration = false;
                ResultSet activeMountsInfo = stmt.executeQuery("PRAGMA table_info(active_mounts)");
                boolean hasMountIdColumn = false;
                
                while (activeMountsInfo.next()) {
                    if ("mount_id".equals(activeMountsInfo.getString("name"))) {
                        hasMountIdColumn = true;
                        break;
                    }
                }
                
                if (!hasMountIdColumn) {
                    plugin.getLogger().info("Adding mount_id column to active_mounts table...");
                    stmt.execute("ALTER TABLE active_mounts ADD COLUMN mount_id INTEGER DEFAULT 0");
                    // Note: Existing active mounts will have mount_id = 0, which is fine as they'll be cleaned up on restart
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
    
    public CompletableFuture<Integer> saveMountData(UUID playerUuid, String mountName, String mountType, 
                                                   String mountData, String chestInventory) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT INTO player_mounts 
                (player_uuid, mount_name, mount_type, mount_data, chest_inventory, created_at, last_accessed)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                
                long currentTime = System.currentTimeMillis();
                
                stmt.setString(1, playerUuid.toString());
                // Allow null mount names for unnamed mounts
                if (mountName != null && !mountName.trim().isEmpty()) {
                    stmt.setString(2, mountName.trim());
                } else {
                    stmt.setNull(2, java.sql.Types.VARCHAR);
                }
                stmt.setString(3, mountType);
                stmt.setString(4, mountData);
                stmt.setString(5, chestInventory);
                stmt.setLong(6, currentTime);
                stmt.setLong(7, currentTime);
                
                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            return generatedKeys.getInt(1); // Return the generated ID
                        }
                    }
                }
                return 0; // Failed to save
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save mount data", e);
                return 0; // Failed to save
            }
        });
    }
    
    public CompletableFuture<Boolean> updateMountData(int mountId, String mountType, String mountData, String chestInventory) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                UPDATE player_mounts 
                SET mount_data = ?, chest_inventory = ?, last_accessed = ?
                WHERE id = ?
            """;
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, mountData);
                stmt.setString(2, chestInventory);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.setInt(4, mountId);
                
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update mount data for mount ID: " + mountId, e);
                return false;
            }
        });
    }
    
    public CompletableFuture<MountData> getMountData(UUID playerUuid, int mountId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT id, mount_name, mount_type, mount_data, chest_inventory, created_at, last_accessed
                FROM player_mounts
                WHERE player_uuid = ? AND id = ?
            """;
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerUuid.toString());
                stmt.setInt(2, mountId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new MountData(
                            rs.getInt("id"),
                            playerUuid,
                            rs.getString("mount_name"), // Can be null
                            rs.getString("mount_type"),
                            rs.getString("mount_data"),
                            rs.getString("chest_inventory"),
                            rs.getLong("created_at"),
                            rs.getLong("last_accessed")
                        );
                    }
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get mount data by ID", e);
            }
            
            return null;
        });
    }
    
    public CompletableFuture<List<MountData>> getMountsByName(UUID playerUuid, String mountName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT id, mount_name, mount_type, mount_data, chest_inventory, created_at, last_accessed
                FROM player_mounts
                WHERE player_uuid = ? AND mount_name = ?
                ORDER BY created_at DESC
            """;
            
            List<MountData> mounts = new ArrayList<>();
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, mountName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        mounts.add(new MountData(
                            rs.getInt("id"),
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
                plugin.getLogger().log(Level.SEVERE, "Failed to get mounts by name", e);
            }
            
            return mounts;
        });
    }
    
    public CompletableFuture<List<MountData>> getPlayerMounts(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT id, mount_name, mount_type, mount_data, chest_inventory, created_at, last_accessed
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
                            rs.getInt("id"),
                            playerUuid,
                            rs.getString("mount_name"), // Can be null
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
    
    public CompletableFuture<Boolean> deleteMountData(UUID playerUuid, int mountId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM player_mounts WHERE player_uuid = ? AND id = ?";
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, playerUuid.toString());
                stmt.setInt(2, mountId);
                
                return stmt.executeUpdate() > 0;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete mount data", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> updateLastAccessed(UUID playerUuid, int mountId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE player_mounts SET last_accessed = ? WHERE player_uuid = ? AND id = ?";
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setLong(1, System.currentTimeMillis());
                stmt.setString(2, playerUuid.toString());
                stmt.setInt(3, mountId);
                
                return stmt.executeUpdate() > 0;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update last accessed", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> updateMountName(UUID playerUuid, int mountId, String newName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE player_mounts SET mount_name = ? WHERE player_uuid = ? AND id = ?";
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                // Allow null names for unnamed mounts
                if (newName != null && !newName.trim().isEmpty()) {
                    stmt.setString(1, newName.trim());
                } else {
                    stmt.setNull(1, java.sql.Types.VARCHAR);
                }
                stmt.setString(2, playerUuid.toString());
                stmt.setInt(3, mountId);
                
                return stmt.executeUpdate() > 0;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update mount name", e);
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
    
    public CompletableFuture<Boolean> addActiveMount(UUID entityUuid, UUID playerUuid, int mountId, String mountName, 
                                                    String worldName, double x, double y, double z) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO active_mounts
                (entity_uuid, player_uuid, mount_id, mount_name, world_name, x, y, z, spawned_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                
                stmt.setString(1, entityUuid.toString());
                stmt.setString(2, playerUuid.toString());
                stmt.setInt(3, mountId);
                // Allow null mount names
                if (mountName != null && !mountName.trim().isEmpty()) {
                    stmt.setString(4, mountName.trim());
                } else {
                    stmt.setNull(4, java.sql.Types.VARCHAR);
                }
                stmt.setString(5, worldName);
                stmt.setDouble(6, x);
                stmt.setDouble(7, y);
                stmt.setDouble(8, z);
                stmt.setLong(9, System.currentTimeMillis());
                
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
    
    // Production database maintenance methods
    public CompletableFuture<Boolean> performMaintenanceCleanup() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                // Clean up old mount data (older than 90 days and not accessed in 30 days)
                String cleanupOldMounts = """
                    DELETE FROM player_mounts 
                    WHERE created_at < ? AND last_accessed < ?
                """;
                
                long ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000);
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                
                try (PreparedStatement stmt = connection.prepareStatement(cleanupOldMounts)) {
                    stmt.setLong(1, ninetyDaysAgo);
                    stmt.setLong(2, thirtyDaysAgo);
                    int deletedMounts = stmt.executeUpdate();
                    
                    if (deletedMounts > 0) {
                        plugin.getLogger().info("Database maintenance: Cleaned up " + deletedMounts + " old unused mounts");
                    }
                }
                
                // Clean up active mounts for players who haven't been online in 7 days  
                String cleanupInactivePlayers = """
                    DELETE FROM active_mounts 
                    WHERE spawned_at < ?
                """;
                
                long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
                
                try (PreparedStatement stmt = connection.prepareStatement(cleanupInactivePlayers)) {
                    stmt.setLong(1, sevenDaysAgo);
                    int deletedActive = stmt.executeUpdate();
                    
                    if (deletedActive > 0) {
                        plugin.getLogger().info("Database maintenance: Cleaned up " + deletedActive + " stale active mount entries");
                    }
                }
                
                // Vacuum database to reclaim space
                try (PreparedStatement stmt = connection.prepareStatement("VACUUM")) {
                    stmt.execute();
                    plugin.getLogger().info("Database maintenance: VACUUM completed");
                }
                
                return true;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to perform database maintenance", e);
                return false;
            }
        });
    }
    
    public CompletableFuture<Integer> getDatabaseStats() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                int totalMounts = 0;
                int activeMounts = 0;
                
                // Count total mounts
                try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM player_mounts")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            totalMounts = rs.getInt(1);
                        }
                    }
                }
                
                // Count active mounts
                try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM active_mounts")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            activeMounts = rs.getInt(1);
                        }
                    }
                }
                
                plugin.getLogger().info("Database stats: " + totalMounts + " total mounts, " + activeMounts + " currently active");
                return totalMounts;
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get database stats", e);
                return -1;
            }
        });
    }
}