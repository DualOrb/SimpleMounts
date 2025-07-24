package com.simplemounts.listeners;

import com.simplemounts.SimpleMounts;
import com.simplemounts.core.MountManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerListener implements Listener {
    
    private final SimpleMounts plugin;
    private final MountManager mountManager;
    
    public PlayerListener(SimpleMounts plugin) {
        this.plugin = plugin;
        this.mountManager = plugin.getMountManager();
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        plugin.runAsync(() -> {
            // Cleanup any orphaned mounts for this player
            plugin.getDatabaseManager().cleanupOrphanedMounts();
            
            // Check for any active mounts that need to be cleaned up
            plugin.getDatabaseManager().getPlayerActiveMounts(player.getUniqueId())
                .thenAccept(activeMounts -> {
                    for (java.util.UUID entityUuid : activeMounts) {
                        org.bukkit.entity.Entity entity = plugin.getServer().getEntity(entityUuid);
                        if (entity == null) {
                            // Entity no longer exists, clean up database
                            plugin.getDatabaseManager().removeActiveMount(entityUuid);
                        }
                    }
                });
        });
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Store mounts IMMEDIATELY before chunks unload - run synchronously
        if (plugin.getConfigManager().autoStoreOnLogout()) {
            try {
                plugin.getLogger().info("DEBUG: Player " + player.getName() + " logging out - attempting to store mounts");
                
                // Force all mounts to stay loaded during storage
                Set<UUID> activeMounts = mountManager.getPlayerActiveMounts(player.getUniqueId());
                Set<org.bukkit.Chunk> chunksToKeepLoaded = new HashSet<>();
                
                // Load and force chunks for all active mounts
                if (activeMounts != null) {
                    for (UUID entityUuid : activeMounts) {
                        org.bukkit.entity.Entity entity = plugin.getServer().getEntity(entityUuid);
                        if (entity != null) {
                            org.bukkit.Chunk chunk = entity.getLocation().getChunk();
                            if (!chunk.isLoaded()) {
                                chunk.load();
                            }
                            chunk.setForceLoaded(true);
                            chunksToKeepLoaded.add(chunk);
                        }
                    }
                }
                
                // Store all mounts synchronously
                mountManager.storeAllPlayerMountsSync(player);
                plugin.getLogger().info("Successfully stored mounts for " + player.getName() + " during logout");
                
                // Schedule chunk cleanup for later
                if (!chunksToKeepLoaded.isEmpty()) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        for (org.bukkit.Chunk chunk : chunksToKeepLoaded) {
                            try {
                                chunk.setForceLoaded(false);
                            } catch (Exception ignored) {
                                // Chunk may already be unloaded
                            }
                        }
                    }, 40L); // 2 second delay
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to store mounts for " + player.getName() + " during logout: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Clean up any active GUI sessions to prevent memory leaks
        plugin.getGUIManager().closeSession(player);
        
        // Clean up player data from memory to prevent memory leaks (delayed to allow storage to complete)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            mountManager.cleanupPlayerData(player.getUniqueId());
        }, 60L); // 3 second delay
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        if (plugin.getConfigManager().autoStoreOnDeath()) {
            plugin.runAsync(() -> {
                mountManager.storeAllPlayerMounts(player);
            });
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        
        boolean shouldStore = false;
        
        if (plugin.getConfigManager().autoStoreOnWorldChange()) {
            shouldStore = true;
        } else if (plugin.getConfigManager().autoStoreOnDimensionChange()) {
            // Check if this is a dimension change (different world environments)
            org.bukkit.World.Environment fromEnv = event.getFrom().getEnvironment();
            org.bukkit.World.Environment toEnv = player.getWorld().getEnvironment();
            
            if (fromEnv != toEnv) {
                shouldStore = true;
            }
        }
        
        if (shouldStore) {
            plugin.runAsync(() -> {
                mountManager.storeAllPlayerMounts(player);
            });
        }
    }
}