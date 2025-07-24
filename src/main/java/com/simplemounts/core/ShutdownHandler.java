package com.simplemounts.core;

import com.simplemounts.SimpleMounts;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class ShutdownHandler {
    
    private final SimpleMounts plugin;
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private final AtomicBoolean emergencyShutdown = new AtomicBoolean(false);
    
    public ShutdownHandler(SimpleMounts plugin) {
        this.plugin = plugin;
    }
    
    public void handleShutdown() {
        if (!plugin.getConfigManager().enableShutdownHandler()) {
            return;
        }
        
        if (!shutdownInProgress.compareAndSet(false, true)) {
            return; // Already shutting down
        }
        
        if (plugin.getConfigManager().logShutdownOperations()) {
            plugin.getLogger().info("Starting graceful shutdown process...");
        }
        
        long startTime = System.currentTimeMillis();
        int timeoutSeconds = plugin.getConfigManager().getShutdownStorageTimeout();
        
        try {
            if (plugin.getConfigManager().autoStoreOnShutdown()) {
                storeAllActiveMounts(timeoutSeconds);
            }
            
            // Close database connections
            plugin.getDatabaseManager().closeConnection();
            
            long duration = System.currentTimeMillis() - startTime;
            if (plugin.getConfigManager().logShutdownOperations()) {
                plugin.getLogger().info("Graceful shutdown completed in " + duration + "ms");
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during graceful shutdown", e);
        } finally {
            shutdownInProgress.set(false);
        }
    }
    
    public void handleEmergencyShutdown() {
        if (!emergencyShutdown.compareAndSet(false, true)) {
            return; // Already handling emergency shutdown
        }
        
        if (plugin.getConfigManager().logShutdownOperations()) {
            plugin.getLogger().warning("Emergency shutdown initiated - storing all mounts immediately");
        }
        
        try {
            // Force immediate storage without timeout
            storeAllActiveMountsImmediate();
            
            // Force close database
            plugin.getDatabaseManager().closeConnection();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during emergency shutdown", e);
        }
    }
    
    private void storeAllActiveMounts(int timeoutSeconds) {
        if (plugin.getConfigManager().forceImmediateStorage()) {
            storeAllActiveMountsImmediate();
            return;
        }
        
        CountDownLatch latch = new CountDownLatch(1);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    storeAllActiveMountsAsync().thenRun(() -> {
                        latch.countDown();
                    });
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error storing mounts during shutdown", e);
                    latch.countDown();
                }
            }
        }.runTask(plugin);
        
        try {
            boolean completed = latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                plugin.getLogger().warning("Mount storage timeout exceeded, forcing immediate storage");
                storeAllActiveMountsImmediate();
            }
        } catch (InterruptedException e) {
            plugin.getLogger().log(Level.WARNING, "Shutdown interrupted, forcing immediate storage", e);
            storeAllActiveMountsImmediate();
        }
    }
    
    private CompletableFuture<Void> storeAllActiveMountsAsync() {
        return CompletableFuture.runAsync(() -> {
            int totalStored = 0;
            int errors = 0;
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                try {
                    plugin.getMountManager().storeAllPlayerMountsSync(player);
                    totalStored++;
                    
                    if (plugin.getConfigManager().logShutdownOperations()) {
                        plugin.getLogger().info("Stored mounts for player: " + player.getName());
                    }
                    
                } catch (Exception e) {
                    errors++;
                    plugin.getLogger().log(Level.WARNING, "Error storing mounts for player: " + player.getName(), e);
                }
            }
            
            if (plugin.getConfigManager().logShutdownOperations()) {
                plugin.getLogger().info("Shutdown storage complete: " + totalStored + " players processed, " + errors + " errors");
            }
        });
    }
    
    private void storeAllActiveMountsImmediate() {
        int totalStored = 0;
        int errors = 0;
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            try {
                // Store mounts synchronously to ensure they're saved before shutdown
                plugin.getMountManager().storeAllPlayerMountsSync(player);
                totalStored++;
                
                if (plugin.getConfigManager().logShutdownOperations()) {
                    plugin.getLogger().info("Immediately stored mounts for player: " + player.getName());
                }
                
            } catch (Exception e) {
                errors++;
                plugin.getLogger().log(Level.WARNING, "Error immediately storing mounts for player: " + player.getName(), e);
            }
        }
        
        if (plugin.getConfigManager().logShutdownOperations()) {
            plugin.getLogger().info("Immediate storage complete: " + totalStored + " players processed, " + errors + " errors");
        }
    }
    
    public boolean isShutdownInProgress() {
        return shutdownInProgress.get();
    }
    
    public boolean isEmergencyShutdown() {
        return emergencyShutdown.get();
    }
    
    public void notifyPlayersOfShutdown() {
        if (!plugin.getConfigManager().autoStoreOnShutdown()) {
            return;
        }
        
        String message = plugin.getConfigManager().getMessage("shutdown_storage");
        String prefix = plugin.getConfigManager().getMessagePrefix();
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix + message));
        }
    }
    
    public void scheduleShutdownWarning(int secondsBeforeShutdown) {
        new BukkitRunnable() {
            @Override
            public void run() {
                notifyPlayersOfShutdown();
            }
        }.runTaskLater(plugin, (secondsBeforeShutdown - 5) * 20L); // 5 seconds before shutdown
    }
    
    public CompletableFuture<Void> storePlayerMountsAsync(Player player) {
        return CompletableFuture.runAsync(() -> {
            try {
                plugin.getMountManager().storeAllPlayerMounts(player);
                
                if (plugin.getConfigManager().logShutdownOperations()) {
                    plugin.getLogger().info("Stored mounts for player during shutdown: " + player.getName());
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error storing mounts for player during shutdown: " + player.getName(), e);
            }
        });
    }
    
    public void handleServerRestart() {
        if (plugin.getConfigManager().logShutdownOperations()) {
            plugin.getLogger().info("Server restart detected - initiating mount storage");
        }
        
        // Schedule storage 10 seconds before restart
        scheduleShutdownWarning(10);
        
        // Perform actual storage
        new BukkitRunnable() {
            @Override
            public void run() {
                handleShutdown();
            }
        }.runTaskLater(plugin, 200L); // 10 seconds later
    }
    
    public void handleServerStop() {
        if (plugin.getConfigManager().logShutdownOperations()) {
            plugin.getLogger().info("Server stop detected - initiating immediate mount storage");
        }
        
        handleShutdown();
    }
    
    public void handlePluginDisable() {
        if (plugin.getConfigManager().logShutdownOperations()) {
            plugin.getLogger().info("Plugin disable detected - storing all mounts");
        }
        
        handleShutdown();
    }
    
    public void handleWorldUnload(String worldName) {
        if (plugin.getConfigManager().logShutdownOperations()) {
            plugin.getLogger().info("World unload detected for: " + worldName + " - storing affected mounts");
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Store mounts for all players in the unloading world
                    for (Player player : plugin.getServer().getOnlinePlayers()) {
                        if (player.getWorld().getName().equals(worldName)) {
                            plugin.getMountManager().storeAllPlayerMounts(player);
                        }
                    }
                    
                    // Clean up active mounts in the database for this world
                    plugin.runAsync(() -> {
                        // This would require additional database method to clean by world
                        plugin.getDatabaseManager().cleanupOrphanedMounts();
                    });
                    
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error storing mounts for world unload: " + worldName, e);
                }
            }
        }.runTask(plugin);
    }
    
    public void reset() {
        shutdownInProgress.set(false);
        emergencyShutdown.set(false);
    }
    
    public ShutdownStats getShutdownStats() {
        return new ShutdownStats(
            shutdownInProgress.get(),
            emergencyShutdown.get(),
            plugin.getConfigManager().enableShutdownHandler(),
            plugin.getConfigManager().autoStoreOnShutdown(),
            plugin.getConfigManager().getShutdownStorageTimeout()
        );
    }
    
    public static class ShutdownStats {
        private final boolean shutdownInProgress;
        private final boolean emergencyShutdown;
        private final boolean handlerEnabled;
        private final boolean autoStore;
        private final int timeoutSeconds;
        
        public ShutdownStats(boolean shutdownInProgress, boolean emergencyShutdown, 
                           boolean handlerEnabled, boolean autoStore, int timeoutSeconds) {
            this.shutdownInProgress = shutdownInProgress;
            this.emergencyShutdown = emergencyShutdown;
            this.handlerEnabled = handlerEnabled;
            this.autoStore = autoStore;
            this.timeoutSeconds = timeoutSeconds;
        }
        
        public boolean isShutdownInProgress() { return shutdownInProgress; }
        public boolean isEmergencyShutdown() { return emergencyShutdown; }
        public boolean isHandlerEnabled() { return handlerEnabled; }
        public boolean isAutoStore() { return autoStore; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        
        @Override
        public String toString() {
            return "ShutdownStats{" +
                    "shutdownInProgress=" + shutdownInProgress +
                    ", emergencyShutdown=" + emergencyShutdown +
                    ", handlerEnabled=" + handlerEnabled +
                    ", autoStore=" + autoStore +
                    ", timeoutSeconds=" + timeoutSeconds +
                    '}';
        }
    }
}