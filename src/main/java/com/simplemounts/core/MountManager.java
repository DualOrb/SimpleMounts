package com.simplemounts.core;

import com.simplemounts.SimpleMounts;
import com.simplemounts.data.*;
import com.simplemounts.serialization.MountSerializer;
import com.simplemounts.util.PermissionUtils;
import com.simplemounts.util.RideableDetector;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MountManager {
    
    private final SimpleMounts plugin;
    private final DatabaseManager database;
    private final ConfigManager config;
    private final MountSerializer serializer;
    
    private final Map<UUID, Set<UUID>> playerActiveMounts;
    private final Map<UUID, String> entityMountNames;
    private final Map<UUID, Long> mountDistanceWarningTime;
    
    public MountManager(SimpleMounts plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager();
        this.config = plugin.getConfigManager();
        this.serializer = new MountSerializer(plugin);
        
        this.playerActiveMounts = new ConcurrentHashMap<>();
        this.entityMountNames = new ConcurrentHashMap<>();
        this.mountDistanceWarningTime = new ConcurrentHashMap<>();
        
        startCleanupTask();
        startDistanceMonitoring();
    }
    
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            database.cleanupOrphanedMounts();
        }, 6000L, 6000L); // Run every 5 minutes
    }
    
    private void startDistanceMonitoring() {
        int checkInterval = config.getDistanceStorageCheckInterval();
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            checkMountDistances();
        }, checkInterval, checkInterval);
    }
    
    private void checkMountDistances() {
        int maxDistance = config.getDistanceStorageMaxDistance();
        long gracePeriodMs = config.getDistanceStorageGracePeriod() * 1000L;
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<UUID, Set<UUID>> entry : playerActiveMounts.entrySet()) {
            UUID playerUuid = entry.getKey();
            org.bukkit.entity.Player player = plugin.getServer().getPlayer(playerUuid);
            
            if (player == null || !player.isOnline()) {
                continue;
            }
            
            Set<UUID> mountUuids = new HashSet<>(entry.getValue()); // Copy to avoid concurrent modification
            for (UUID mountUuid : mountUuids) {
                Entity mountEntity = plugin.getServer().getEntity(mountUuid);
                if (mountEntity == null) {
                    continue;
                }
                
                // Skip if player is riding this mount
                if (player.getVehicle() != null && player.getVehicle().getUniqueId().equals(mountUuid)) {
                    mountDistanceWarningTime.remove(mountUuid);
                    continue;
                }
                
                double distance = player.getLocation().distance(mountEntity.getLocation());
                if (distance > maxDistance) {
                    Long warningStartTime = mountDistanceWarningTime.get(mountUuid);
                    if (warningStartTime == null) {
                        // First time mount is too far - start grace period
                        mountDistanceWarningTime.put(mountUuid, currentTime);
                        String mountName = entityMountNames.get(mountUuid);
                        sendMessage(player, "mount_too_far_warning", mountName, String.valueOf(config.getDistanceStorageGracePeriod()));
                    } else if (currentTime - warningStartTime >= gracePeriodMs) {
                        // Grace period expired - store the mount
                        String mountName = entityMountNames.get(mountUuid);
                        plugin.getLogger().info("Auto-storing mount '" + mountName + "' for player " + player.getName() + " due to distance (" + String.format("%.1f", distance) + " > " + maxDistance + " blocks)");
                        
                        plugin.runAsync(() -> {
                            storeMount(player, mountName).thenAccept(stored -> {
                                if (stored) {
                                    plugin.runSync(() -> {
                                        sendMessage(player, "mount_auto_stored_distance", mountName);
                                    });
                                }
                            });
                        });
                        
                        mountDistanceWarningTime.remove(mountUuid);
                    }
                } else {
                    // Mount is within range - clear any warning
                    mountDistanceWarningTime.remove(mountUuid);
                }
            }
        }
    }
    
    public CompletableFuture<Boolean> claimMount(Player player, Entity entity, String mountName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!RideableDetector.isRideable(entity)) {
                sendMessage(player, "invalid_mount_type");
                return false;
            }
            
            MountType mountType = MountType.fromEntityType(entity.getType());
            if (!config.isMountTypeEnabled(mountType.name())) {
                sendMessage(player, "mount_type_disabled");
                return false;
            }
            
            if (!hasPermission(player, "simplemounts.claim")) {
                sendMessage(player, "no_permission");
                return false;
            }
            
            if (!canPlayerClaimMoreMounts(player, mountType)) {
                return false;
            }
            
            if (isEntityOwnedByOtherPlayer(entity, player)) {
                sendMessage(player, "mount_protected");
                return false;
            }
            
            if (!isValidMountName(mountName)) {
                plugin.getLogger().info("DEBUG: Invalid mount name '" + mountName + "' - length: " + mountName.length() + ", min: " + config.getMinNameLength() + ", max: " + config.getMaxNameLength());
                sendMessage(player, "invalid_mount_name", String.valueOf(config.getMinNameLength()), String.valueOf(config.getMaxNameLength()));
                return false;
            }
            
            if (playerHasMountWithName(player, mountName)) {
                sendMessage(player, "mount_name_exists");
                return false;
            }
            
            try {
                MountAttributes attributes = MountAttributes.fromEntity(entity);
                String mountDataYaml = serializer.serializeAttributes(attributes);
                String chestInventoryData = null;
                
                if (mountType.canHaveChest() && entity instanceof InventoryHolder) {
                    chestInventoryData = serializer.serializeChestInventory(((InventoryHolder) entity).getInventory());
                }
                
                boolean saved = database.saveMountData(
                    player.getUniqueId(),
                    mountName,
                    mountType.name(),
                    mountDataYaml,
                    chestInventoryData
                ).get();
                
                if (saved) {
                    tagEntityAsOwnedMount(entity, player, mountName);
                    trackActiveMount(player, entity, mountName);
                    
                    sendMessage(player, "mount_claimed", mountName);
                    
                    if (config.playTamingEffects()) {
                        playTamingEffects(entity.getLocation());
                    }
                    
                    return true;
                } else {
                    sendMessage(player, "mount_claim_failed");
                    return false;
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error claiming mount", e);
                sendMessage(player, "mount_claim_failed");
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> summonMount(Player player, String mountName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Do database operations async
        plugin.runAsync(() -> {
            try {
                if (!hasPermission(player, "simplemounts.summon")) {
                    plugin.runSync(() -> {
                        sendMessage(player, "no_permission");
                        future.complete(false);
                    });
                    return;
                }
                
                if (player.getVehicle() != null) {
                    plugin.runSync(() -> {
                        sendMessage(player, "already_riding");
                        future.complete(false);
                    });
                    return;
                }
                
                MountData mountData = database.getMountData(player.getUniqueId(), mountName).get();
                if (mountData == null) {
                    plugin.runSync(() -> {
                        sendMessage(player, "mount_not_found", mountName);
                        future.complete(false);
                    });
                    return;
                }
                
                // Move to main thread for entity operations
                plugin.runSync(() -> {
                    try {
                        if (config.autoDismissExistingMount()) {
                            dismissPlayerMounts(player);
                        }
                        
                        Location spawnLocation = findSafeSpawnLocation(player, mountData.getMountTypeEnum());
                        if (spawnLocation == null) {
                            sendMessage(player, "no_safe_location");
                            future.complete(false);
                            return;
                        }
                        
                        Entity entity = spawnMountEntity(spawnLocation, mountData);
                        if (entity == null) {
                            sendMessage(player, "mount_spawn_failed");
                            future.complete(false);
                            return;
                        }
                        
                        applyMountAttributes(entity, mountData);
                        tagEntityAsOwnedMount(entity, player, mountName);
                        trackActiveMount(player, entity, mountName);
                        
                        // Set custom name on the entity to show mount name
                        entity.setCustomName(ChatColor.GOLD + mountName);
                        entity.setCustomNameVisible(true);
                        
                        // Play summoning effects
                        playSummoningEffects(spawnLocation, player);
                        
                        // Update database async
                        plugin.runAsync(() -> {
                            database.updateLastAccessed(player.getUniqueId(), mountName);
                        });
                        
                        sendMessage(player, "mount_summoned", mountName);
                        future.complete(true);
                        
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error summoning mount on main thread", e);
                        sendMessage(player, "mount_summon_failed");
                        future.complete(false);
                    }
                });
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error summoning mount", e);
                plugin.runSync(() -> {
                    sendMessage(player, "mount_summon_failed");
                    future.complete(false);
                });
            }
        });
        
        return future;
    }
    
    public CompletableFuture<Boolean> storeCurrentMount(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            plugin.getLogger().info("DEBUG: storeCurrentMount called for " + player.getName());
            
            if (!hasPermission(player, "simplemounts.store")) {
                plugin.getLogger().info("DEBUG: No permission");
                sendMessage(player, "no_permission");
                return false;
            }
            
            Entity vehicle = player.getVehicle();
            plugin.getLogger().info("DEBUG: Vehicle: " + (vehicle != null ? vehicle.getType() : "null"));
            if (vehicle == null) {
                sendMessage(player, "not_riding_mount");
                return false;
            }
            
            if (!isPlayerOwnedMount(vehicle, player)) {
                plugin.getLogger().info("DEBUG: Not player owned mount");
                sendMessage(player, "not_your_mount");
                return false;
            }
            
            // Generate a name for the mount
            String mountName = generateMountName(vehicle);
            plugin.getLogger().info("DEBUG: Generated mount name: " + mountName);
            
            boolean result = storeEntityAsMount(player, vehicle, mountName);
            plugin.getLogger().info("DEBUG: Storage result: " + result);
            return result;
        });
    }
    
    public CompletableFuture<Boolean> storeMount(Player player, String mountName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        plugin.getLogger().info("DEBUG: storeMount called for " + player.getName() + " with mount: " + mountName);
        
        if (!hasPermission(player, "simplemounts.store")) {
            plugin.getLogger().info("DEBUG: No permission");
            sendMessage(player, "no_permission");
            future.complete(false);
            return future;
        }
        
        // Find the mount UUID first (can be done async)
        plugin.runAsync(() -> {
            Set<UUID> activeMounts = playerActiveMounts.get(player.getUniqueId());
            plugin.getLogger().info("DEBUG: Active mounts for player: " + (activeMounts != null ? activeMounts.size() : "null"));
            
            UUID targetUuid = null;
            if (activeMounts != null) {
                for (UUID entityUuid : activeMounts) {
                    String activeMountName = entityMountNames.get(entityUuid);
                    plugin.getLogger().info("DEBUG: Checking mount UUID " + entityUuid + " with name: " + activeMountName);
                    if (mountName.equals(activeMountName)) {
                        targetUuid = entityUuid;
                        plugin.getLogger().info("DEBUG: Found matching UUID");
                        break;
                    }
                }
            }
            
            if (targetUuid == null) {
                plugin.getLogger().info("DEBUG: No matching UUID found");
                plugin.runSync(() -> {
                    sendMessage(player, "mount_not_found", mountName);
                    future.complete(false);
                });
                return;
            }
            
            final UUID finalTargetUuid = targetUuid;
            
            // Search for entity on main thread
            plugin.runSync(() -> {
                Entity targetMount = null;
                plugin.getLogger().info("DEBUG: Searching for entity on main thread...");
                
                // Direct search through entities
                for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                    plugin.getLogger().info("DEBUG: Searching world: " + world.getName());
                    for (Entity entity : world.getEntities()) {
                        if (entity.getUniqueId().equals(finalTargetUuid)) {
                            targetMount = entity;
                            plugin.getLogger().info("DEBUG: Found target mount entity in world: " + world.getName());
                            break;
                        }
                    }
                    if (targetMount != null) break;
                }
                
                if (targetMount == null) {
                    plugin.getLogger().info("DEBUG: Target mount entity not found in any world");
                    sendMessage(player, "mount_not_found", mountName);
                    future.complete(false);
                    return;
                }
                
                final Entity finalTargetMount = targetMount;
                
                // Store the mount
                plugin.runAsync(() -> {
                    boolean result = storeEntityAsMount(player, finalTargetMount, mountName);
                    plugin.getLogger().info("DEBUG: Storage result: " + result);
                    future.complete(result);
                });
            });
        });
        
        return future;
    }
    
    private boolean storeEntityAsMount(Player player, Entity vehicle, String mountName) {
        try {
            plugin.getLogger().info("DEBUG: storeEntityAsMount called with mount: " + mountName);
            
            MountType mountType = MountType.fromEntityType(vehicle.getType());
            MountAttributes attributes = MountAttributes.fromEntity(vehicle);
            String mountDataYaml = serializer.serializeAttributes(attributes);
            String chestInventoryData = null;
            
            if (mountType.canHaveChest() && vehicle instanceof InventoryHolder) {
                chestInventoryData = serializer.serializeChestInventory(((InventoryHolder) vehicle).getInventory());
            }
            
            plugin.getLogger().info("DEBUG: About to save to database");
            boolean saved = database.saveMountData(
                player.getUniqueId(),
                mountName,
                mountType.name(),
                mountDataYaml,
                chestInventoryData
            ).get();
            
            plugin.getLogger().info("DEBUG: Database save result: " + saved);
            
            if (saved) {
                untrackActiveMount(player, vehicle);
                
                // Play storing effects before removing
                plugin.runSync(() -> {
                    playStoringEffects(vehicle.getLocation(), player);
                    vehicle.remove();
                });
                
                sendMessage(player, "mount_stored", mountName);
                return true;
            } else {
                sendMessage(player, "mount_store_failed");
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error storing mount", e);
            sendMessage(player, "mount_store_failed");
            return false;
        }
    }
    
    private String generateMountName(Entity entity) {
        MountType mountType = MountType.fromEntityType(entity.getType());
        String baseName = mountType.getDisplayName().toLowerCase();
        // Use only last 4 digits of timestamp to keep name shorter
        long timestamp = System.currentTimeMillis();
        String shortTimestamp = String.valueOf(timestamp % 10000);
        String mountName = baseName + "_" + shortTimestamp;
        
        // Ensure it fits within config limits
        int maxLength = config.getMaxNameLength();
        if (mountName.length() > maxLength) {
            // Truncate base name if needed
            int availableLength = maxLength - shortTimestamp.length() - 1; // -1 for underscore
            if (availableLength > 0) {
                baseName = baseName.substring(0, Math.min(baseName.length(), availableLength));
                mountName = baseName + "_" + shortTimestamp;
            } else {
                // If still too long, just use short timestamp
                mountName = "mount_" + shortTimestamp;
            }
        }
        
        return mountName;
    }
    
    public CompletableFuture<Boolean> releaseMount(Player player, String mountName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!hasPermission(player, "simplemounts.release")) {
                sendMessage(player, "no_permission");
                return false;
            }
            
            try {
                MountData mountData = database.getMountData(player.getUniqueId(), mountName).get();
                if (mountData == null) {
                    sendMessage(player, "mount_not_found", mountName);
                    return false;
                }
                
                Set<UUID> activeMounts = playerActiveMounts.get(player.getUniqueId());
                if (activeMounts != null) {
                    for (UUID entityUuid : activeMounts) {
                        Entity entity = plugin.getServer().getEntity(entityUuid);
                        if (entity != null && entityMountNames.get(entityUuid).equals(mountName)) {
                            entity.remove();
                            break;
                        }
                    }
                }
                
                boolean deleted = database.deleteMountData(player.getUniqueId(), mountName).get();
                if (deleted) {
                    sendMessage(player, "mount_released", mountName);
                    return true;
                } else {
                    sendMessage(player, "mount_release_failed");
                    return false;
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error releasing mount", e);
                sendMessage(player, "mount_release_failed");
                return false;
            }
        });
    }
    
    public CompletableFuture<List<MountData>> getPlayerMounts(Player player) {
        return database.getPlayerMounts(player.getUniqueId());
    }
    
    public CompletableFuture<MountData> getMountData(Player player, String mountName) {
        return database.getMountData(player.getUniqueId(), mountName);
    }
    
    public void dismissPlayerMounts(Player player) {
        Set<UUID> activeMounts = playerActiveMounts.get(player.getUniqueId());
        if (activeMounts != null) {
            for (UUID entityUuid : new HashSet<>(activeMounts)) {
                Entity entity = plugin.getServer().getEntity(entityUuid);
                if (entity != null) {
                    entity.remove();
                }
                untrackActiveMount(player, entityUuid);
            }
        }
    }
    
    public void storeAllPlayerMounts(Player player) {
        Set<UUID> activeMounts = playerActiveMounts.get(player.getUniqueId());
        if (activeMounts != null) {
            for (UUID entityUuid : new HashSet<>(activeMounts)) {
                Entity entity = plugin.getServer().getEntity(entityUuid);
                if (entity != null) {
                    String mountName = entityMountNames.get(entityUuid);
                    if (mountName != null) {
                        storeMount(player, mountName);
                    }
                }
            }
        }
    }
    
    public void storeAllPlayerMountsSync(Player player) {
        Set<UUID> activeMounts = playerActiveMounts.get(player.getUniqueId());
        if (activeMounts == null || activeMounts.isEmpty()) {
            plugin.getLogger().info("DEBUG: No active mounts found for " + player.getName());
            return;
        }
        
        plugin.getLogger().info("DEBUG: Found " + activeMounts.size() + " active mounts for " + player.getName());
        
        // Keep track of chunks to ensure they stay loaded during storage
        Set<org.bukkit.Chunk> chunksToKeepLoaded = new HashSet<>();
        
        for (UUID entityUuid : new HashSet<>(activeMounts)) {
            Entity entity = plugin.getServer().getEntity(entityUuid);
            if (entity != null) {
                // Force chunk to stay loaded during storage
                org.bukkit.Chunk chunk = entity.getLocation().getChunk();
                if (!chunk.isLoaded()) {
                    chunk.load();
                }
                chunksToKeepLoaded.add(chunk);
                chunk.setForceLoaded(true);
                
                String mountName = entityMountNames.get(entityUuid);
                if (mountName != null) {
                    plugin.getLogger().info("DEBUG: Storing mount " + mountName + " for " + player.getName() + " at " + entity.getLocation());
                    
                    try {
                        // Store mount synchronously
                        boolean stored = storeMountSync(player, entity, mountName);
                        if (stored) {
                            plugin.getLogger().info("DEBUG: Successfully stored mount " + mountName);
                        } else {
                            plugin.getLogger().warning("DEBUG: Failed to store mount " + mountName);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("DEBUG: Error storing mount " + mountName + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    plugin.getLogger().warning("DEBUG: Mount name not found for entity " + entityUuid);
                }
            } else {
                plugin.getLogger().warning("DEBUG: Entity not found for UUID " + entityUuid + " - may have already been unloaded");
            }
        }
        
        // Allow chunks to unload after a delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (org.bukkit.Chunk chunk : chunksToKeepLoaded) {
                chunk.setForceLoaded(false);
            }
        }, 20L); // 1 second delay
    }
    
    private boolean storeMountSync(Player player, Entity entity, String mountName) {
        try {
            MountType mountType = MountType.fromEntityType(entity.getType());
            MountAttributes attributes = MountAttributes.fromEntity(entity);
            String mountDataYaml = serializer.serializeAttributes(attributes);
            String chestInventoryData = null;
            
            if (mountType.canHaveChest() && entity instanceof InventoryHolder) {
                chestInventoryData = serializer.serializeChestInventory(((InventoryHolder) entity).getInventory());
            }
            
            // Perform database operation synchronously (this might block but it's necessary during logout)
            boolean saved = database.saveMountData(
                player.getUniqueId(),
                mountName,
                mountType.name(),
                mountDataYaml,
                chestInventoryData
            ).get(); // .get() makes it synchronous
            
            if (saved) {
                untrackActiveMount(player, entity);
                
                // Play storing effects and remove entity
                playStoringEffects(entity.getLocation(), player);
                entity.remove();
                
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error in storeMountSync", e);
            return false;
        }
    }
    
    private boolean canPlayerClaimMoreMounts(Player player, MountType mountType) {
        try {
            int currentCount = database.getPlayerMountCount(player.getUniqueId()).get();
            int maxMounts = PermissionUtils.getMaxMounts(player, config);
            
            if (maxMounts != -1 && currentCount >= maxMounts) {
                sendMessage(player, "mount_limit_reached", String.valueOf(maxMounts));
                return false;
            }
            
            int typeLimit = config.getTypeLimitForMount(mountType.name());
            if (typeLimit > 0) {
                int currentTypeCount = database.getPlayerMountCountByType(player.getUniqueId(), mountType.name()).get();
                if (currentTypeCount >= typeLimit) {
                    sendMessage(player, "type_limit_reached", mountType.getDisplayName());
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking mount limits", e);
            return false;
        }
    }
    
    private boolean playerHasMountWithName(Player player, String mountName) {
        try {
            MountData existing = database.getMountData(player.getUniqueId(), mountName).get();
            return existing != null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking mount name", e);
            return false;
        }
    }
    
    private Location findSafeSpawnLocation(Player player, MountType mountType) {
        Location playerLoc = player.getLocation();
        
        // Get direction player is facing and spawn mount 2 blocks in front
        org.bukkit.util.Vector direction = playerLoc.getDirection().normalize();
        Location frontLocation = playerLoc.clone().add(direction.multiply(2.0));
        
        // Ensure mount spawns on ground level
        org.bukkit.World world = frontLocation.getWorld();
        int highestY = world.getHighestBlockYAt(frontLocation);
        frontLocation.setY(highestY + 1);
        
        // Check if this location is safe
        if (isSafeSpawnLocation(frontLocation, mountType)) {
            return frontLocation;
        }
        
        // Fallback: try directly in front at player's Y level
        frontLocation = playerLoc.clone().add(direction.multiply(1.5));
        if (isSafeSpawnLocation(frontLocation, mountType)) {
            return frontLocation;
        }
        
        // Last resort: spawn at player location slightly above
        return playerLoc.clone().add(0, 1, 0);
    }
    
    private boolean isSafeSpawnLocation(Location location, MountType mountType) {
        if (location.getBlock().getType().isSolid()) {
            return false;
        }
        
        if (location.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
            return false;
        }
        
        if (mountType == MountType.STRIDER) {
            return location.getBlock().getType().toString().contains("LAVA") || 
                   !config.striderLavaSafety();
        }
        
        return true;
    }
    
    private Entity spawnMountEntity(Location location, MountData mountData) {
        MountType mountType = mountData.getMountTypeEnum();
        
        if (mountType.getEntityType() == null) {
            return null;
        }
        
        Entity entity = location.getWorld().spawnEntity(location, mountType.getEntityType());
        
        if (config.healOnSummon(mountType.getConfigKey()) && entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            living.setHealth(living.getMaxHealth());
        }
        
        return entity;
    }
    
    private void applyMountAttributes(Entity entity, MountData mountData) {
        try {
            plugin.getLogger().info("DEBUG: applyMountAttributes called for " + entity.getType());
            MountAttributes attributes = serializer.deserializeAttributes(mountData.getMountDataYaml());
            plugin.getLogger().info("DEBUG: Deserialized attributes, applying to entity");
            attributes.applyToEntity(entity);
            
            if (mountData.hasChestInventory() && entity instanceof InventoryHolder) {
                serializer.deserializeChestInventory(mountData.getChestInventoryData(), 
                    ((InventoryHolder) entity).getInventory());
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying mount attributes", e);
        }
    }
    
    private void tagEntityAsOwnedMount(Entity entity, Player owner, String mountName) {
        entity.setMetadata("simplemounts.owner", new org.bukkit.metadata.FixedMetadataValue(plugin, owner.getUniqueId().toString()));
        entity.setMetadata("simplemounts.name", new org.bukkit.metadata.FixedMetadataValue(plugin, mountName));
        entity.setMetadata("simplemounts.claimed", new org.bukkit.metadata.FixedMetadataValue(plugin, System.currentTimeMillis()));
    }
    
    private void trackActiveMount(Player player, Entity entity, String mountName) {
        playerActiveMounts.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>())
                          .add(entity.getUniqueId());
        entityMountNames.put(entity.getUniqueId(), mountName);
        
        database.addActiveMount(
            entity.getUniqueId(),
            player.getUniqueId(),
            mountName,
            entity.getWorld().getName(),
            entity.getLocation().getX(),
            entity.getLocation().getY(),
            entity.getLocation().getZ()
        );
    }
    
    private void untrackActiveMount(Player player, Entity entity) {
        untrackActiveMount(player, entity.getUniqueId());
    }
    
    private void untrackActiveMount(Player player, UUID entityUuid) {
        Set<UUID> activeMounts = playerActiveMounts.get(player.getUniqueId());
        if (activeMounts != null) {
            activeMounts.remove(entityUuid);
            if (activeMounts.isEmpty()) {
                playerActiveMounts.remove(player.getUniqueId());
            }
        }
        
        entityMountNames.remove(entityUuid);
        database.removeActiveMount(entityUuid);
    }
    
    private boolean isPlayerOwnedMount(Entity entity, Player player) {
        return entity.hasMetadata("simplemounts.owner") &&
               entity.getMetadata("simplemounts.owner").get(0).asString().equals(player.getUniqueId().toString());
    }
    
    private boolean isEntityOwnedByOtherPlayer(Entity entity, Player player) {
        if (entity.hasMetadata("simplemounts.owner")) {
            String ownerUuid = entity.getMetadata("simplemounts.owner").get(0).asString();
            return !ownerUuid.equals(player.getUniqueId().toString());
        }
        return false;
    }
    
    private boolean isValidMountName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        int length = name.length();
        return length >= config.getMinNameLength() && length <= config.getMaxNameLength();
    }
    
    private void playSummoningEffects(Location location, Player player) {
        // Play particles - magical summoning effect
        location.getWorld().spawnParticle(org.bukkit.Particle.DRAGON_BREATH, location.clone().add(0, 1, 0), 20, 1.0, 1.0, 1.0, 0.1);
        location.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, location.clone().add(0, 0.5, 0), 15, 0.5, 0.5, 0.5, 0.1);
        location.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, location, 30, 1.5, 1.5, 1.5, 1.0);
        
        // Play sounds
        player.playSound(location, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        player.playSound(location, org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.5f);
        
        // Delay for second wave of effects
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            location.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, location.clone().add(0, 1, 0), 10, 0.8, 0.8, 0.8, 0.1);
            player.playSound(location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
        }, 10L);
    }
    
    private void playStoringEffects(Location location, Player player) {
        // Play particles - disappearing effect
        location.getWorld().spawnParticle(org.bukkit.Particle.POOF, location.clone().add(0, 1, 0), 25, 1.0, 1.0, 1.0, 0.1);
        location.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, location.clone().add(0, 0.5, 0), 20, 0.5, 0.5, 0.5, 1.0);
        location.getWorld().spawnParticle(org.bukkit.Particle.WITCH, location, 15, 1.0, 1.0, 1.0, 0.1);
        
        // Play sounds
        player.playSound(location, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        player.playSound(location, org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 0.6f, 1.2f);
    }
    
    private void playTamingEffects(Location location) {
        location.getWorld().spawnParticle(org.bukkit.Particle.HEART, location, 10, 0.5, 0.5, 0.5);
        location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }
    
    private boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }
    
    private void sendMessage(Player player, String messageKey, String... replacements) {
        String message = config.getMessage(messageKey);
        String prefix = config.getMessagePrefix();
        
        if (replacements.length > 0) {
            message = message.replace("{name}", replacements[0]);
            message = message.replace("{limit}", replacements[0]);
            message = message.replace("{min}", replacements[0]);
            if (replacements.length > 1) {
                message = message.replace("{limit}", replacements[1]);
                message = message.replace("{max}", replacements[1]);
                message = message.replace("{grace}", replacements[1]);
                if (replacements.length > 2) {
                    message = message.replace("{type}", replacements[2]);
                }
            }
        }
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
    
    public Set<UUID> getPlayerActiveMounts(UUID playerUuid) {
        return playerActiveMounts.getOrDefault(playerUuid, new HashSet<>());
    }
    
    public String getMountName(UUID entityUuid) {
        return entityMountNames.get(entityUuid);
    }
    
    public boolean isActiveMount(UUID entityUuid) {
        return entityUuid != null && entityMountNames.containsKey(entityUuid);
    }
    
    public boolean isMountActive(Player player, String mountName) {
        Set<UUID> activeMounts = playerActiveMounts.get(player.getUniqueId());
        if (activeMounts == null) {
            return false;
        }
        
        return activeMounts.stream()
            .anyMatch(uuid -> mountName.equals(entityMountNames.get(uuid)));
    }
    
    public CompletableFuture<Boolean> renameMount(Player player, String oldName, String newName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!hasPermission(player, "simplemounts.rename")) {
                sendMessage(player, "no_permission");
                return false;
            }
            
            try {
                MountData mountData = database.getMountData(player.getUniqueId(), oldName).get();
                if (mountData == null) {
                    sendMessage(player, "mount_not_found", oldName);
                    return false;
                }
                
                // Check if new name already exists
                MountData existingMount = database.getMountData(player.getUniqueId(), newName).get();
                if (existingMount != null) {
                    player.sendMessage(ChatColor.RED + "A mount with the name '" + newName + "' already exists!");
                    return false;
                }
                
                // Delete old record and insert new one with new name
                boolean deleted = database.deleteMountData(player.getUniqueId(), oldName).get();
                if (deleted) {
                    boolean saved = database.saveMountData(
                        mountData.getPlayerUuid(),
                        newName, // New name
                        mountData.getMountType(),
                        mountData.getMountDataYaml(),
                        mountData.getChestInventoryData()
                    ).get();
                    if (saved) {
                        player.sendMessage(ChatColor.GREEN + "Mount renamed from '" + oldName + "' to '" + newName + "'!");
                        return true;
                    }
                }
                
                player.sendMessage(ChatColor.RED + "Failed to rename mount. Please try again.");
                return false;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error renaming mount", e);
                player.sendMessage(ChatColor.RED + "An error occurred while renaming the mount.");
                return false;
            }
        });
    }
}