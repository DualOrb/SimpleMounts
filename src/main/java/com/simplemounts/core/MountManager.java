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
    private final Map<UUID, Integer> entityMountIds;
    private final Map<UUID, Long> mountDistanceWarningTime;
    
    public MountManager(SimpleMounts plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager();
        this.config = plugin.getConfigManager();
        this.serializer = new MountSerializer(plugin);
        
        this.playerActiveMounts = new ConcurrentHashMap<>();
        this.entityMountNames = new ConcurrentHashMap<>();
        this.entityMountIds = new ConcurrentHashMap<>();
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
            
            // Validate and sanitize mount name using NameValidator
            String sanitizedMountName = null;
            if (mountName != null && !mountName.trim().isEmpty()) {
                sanitizedMountName = plugin.getNameValidator().validateAndSanitizeName(mountName);
                String validationError = plugin.getNameValidator().getValidationError(mountName, sanitizedMountName);
                
                if (validationError != null) {
                    plugin.getLogger().info("DEBUG: Mount name validation failed: " + validationError + " (original: '" + mountName + "', sanitized: '" + sanitizedMountName + "')");
                    
                    // Send appropriate error message based on validation result
                    if (validationError.contains("empty")) {
                        sendMessage(player, "name_empty");
                    } else if (validationError.contains("invalid characters")) {
                        sendMessage(player, "name_invalid_characters");
                    } else if (validationError.contains("not allowed")) {
                        sendMessage(player, "name_blacklisted");
                    } else if (validationError.contains("between")) {
                        sendMessage(player, "invalid_mount_name", String.valueOf(config.getMinNameLength()), String.valueOf(config.getMaxNameLength()));
                    } else {
                        sendMessage(player, "invalid_mount_name", String.valueOf(config.getMinNameLength()), String.valueOf(config.getMaxNameLength()));
                    }
                    return false;
                }
            }
            
            // Use the sanitized name for further processing
            final String finalMountName = sanitizedMountName;
            
            // Only check for duplicate names if mount has a name
            if (finalMountName != null && playerHasMountWithName(player, finalMountName)) {
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
                
                int mountId = database.saveMountData(
                    player.getUniqueId(),
                    finalMountName,
                    mountType.name(),
                    mountDataYaml,
                    chestInventoryData
                ).get();
                
                if (mountId > 0) {
                    tagEntityAsOwnedMount(entity, player, mountId, finalMountName);
                    trackActiveMount(player, entity, mountId, finalMountName);
                    
                    if (finalMountName != null && !finalMountName.trim().isEmpty()) {
                        sendMessage(player, "mount_claimed", finalMountName);
                    } else {
                        sendMessage(player, "mount_tamed_unnamed");
                    }
                    
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
    
    private void tagEntityAsOwnedMount(Entity entity, Player player, int mountId, String mountName) {
        // Tag the entity with metadata to identify it as a SimpleMounts entity
        entity.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "simplemounts_owner"), 
            org.bukkit.persistence.PersistentDataType.STRING, 
            player.getUniqueId().toString()
        );
        entity.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "simplemounts_id"), 
            org.bukkit.persistence.PersistentDataType.INTEGER, 
            mountId
        );
        if (mountName != null) {
            entity.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "simplemounts_name"), 
                org.bukkit.persistence.PersistentDataType.STRING, 
                mountName
            );
        }
    }
    
    private void trackActiveMount(Player player, Entity entity, int mountId, String mountName) {
        UUID playerUuid = player.getUniqueId();
        UUID entityUuid = entity.getUniqueId();
        
        // Add to tracking maps
        playerActiveMounts.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet()).add(entityUuid);
        entityMountIds.put(entityUuid, mountId);
        if (mountName != null) {
            entityMountNames.put(entityUuid, mountName);
        }
        
        // Add to database
        Location loc = entity.getLocation();
        database.addActiveMount(
            entityUuid, 
            playerUuid, 
            mountId,
            mountName, 
            loc.getWorld().getName(), 
            loc.getX(), 
            loc.getY(), 
            loc.getZ()
        );
    }
    
    public CompletableFuture<Boolean> summonMount(Player player, int mountId) {
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
                
                MountData mountData = database.getMountData(player.getUniqueId(), mountId).get();
                if (mountData == null) {
                    plugin.runSync(() -> {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("id", String.valueOf(mountId));
                        sendMessage(player, "mount_not_found_with_id", placeholders);
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
                        tagEntityAsOwnedMount(entity, player, mountId, mountData.getMountName());
                        trackActiveMount(player, entity, mountId, mountData.getMountName());
                        
                        // Set custom name on the entity to show mount name
                        if (mountData.hasName()) {
                            entity.setCustomName(ChatColor.GOLD + mountData.getMountName());
                            entity.setCustomNameVisible(true);
                        }
                        
                        // Play summoning effects
                        playSummoningEffects(spawnLocation, player);
                        
                        // Update database async
                        plugin.runAsync(() -> {
                            database.updateLastAccessed(player.getUniqueId(), mountId);
                        });
                        
                        sendMessage(player, "mount_summoned", mountData.getDisplayName());
                        future.complete(true);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error spawning mount entity", e);
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
                
                List<MountData> mounts = database.getMountsByName(player.getUniqueId(), mountName).get();
                if (mounts.isEmpty()) {
                    plugin.runSync(() -> {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("name", mountName);
                        sendMessage(player, "mount_not_found", placeholders);
                        future.complete(false);
                    });
                    return;
                }
                
                if (mounts.size() > 1) {
                    // Multiple mounts with the same name - ask user to specify ID
                    plugin.runSync(() -> {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("name", mountName);
                        sendMessage(player, "multiple_mounts_found", placeholders);
                        
                        for (MountData mount : mounts) {
                            Map<String, String> mountPlaceholders = new HashMap<>();
                            mountPlaceholders.put("id", String.valueOf(mount.getId()));
                            mountPlaceholders.put("type", mount.getMountTypeEnum().getDisplayName());
                            mountPlaceholders.put("date", new java.text.SimpleDateFormat("MMM dd, yyyy").format(new java.util.Date(mount.getCreatedAt())));
                            sendMessage(player, "mount_id_format", mountPlaceholders);
                        }
                        
                        Map<String, String> helpPlaceholders = new HashMap<>();
                        helpPlaceholders.put("command", "summon");
                        helpPlaceholders.put("name", mountName);
                        sendMessage(player, "select_mount_by_id", helpPlaceholders);
                        future.complete(false);
                    });
                    return;
                }
                
                // Single mount found, summon it
                MountData mountData = mounts.get(0);
                
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
                        tagEntityAsOwnedMount(entity, player, mountData.getId(), mountData.getMountName());
                        trackActiveMount(player, entity, mountData.getId(), mountData.getMountName());
                        
                        // Set custom name on the entity to show mount name
                        if (mountData.hasName()) {
                            entity.setCustomName(ChatColor.GOLD + mountData.getMountName());
                            entity.setCustomNameVisible(true);
                        }
                        
                        // Play summoning effects
                        playSummoningEffects(spawnLocation, player);
                        
                        // Update database async
                        plugin.runAsync(() -> {
                            database.updateLastAccessed(player.getUniqueId(), mountData.getId());
                        });
                        
                        sendMessage(player, "mount_summoned", mountData.getDisplayName());
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
    
    public CompletableFuture<Boolean> storeMount(Player player, int mountId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        plugin.getLogger().info("DEBUG: storeMount by ID called for " + player.getName() + " with mount ID: " + mountId);
        
        if (!hasPermission(player, "simplemounts.store")) {
            plugin.getLogger().info("DEBUG: No permission");
            sendMessage(player, "no_permission");
            future.complete(false);
            return future;
        }
        
        // Find the mount entity by ID
        plugin.runAsync(() -> {
            Set<UUID> activeMounts = playerActiveMounts.get(player.getUniqueId());
            plugin.getLogger().info("DEBUG: Active mounts for player: " + (activeMounts != null ? activeMounts.size() : "null"));
            
            UUID targetUuid = null;
            if (activeMounts != null) {
                for (UUID entityUuid : activeMounts) {
                    Integer activeMountId = entityMountIds.get(entityUuid);
                    plugin.getLogger().info("DEBUG: Checking mount UUID " + entityUuid + " with ID: " + activeMountId);
                    if (mountId == activeMountId) {
                        targetUuid = entityUuid;
                        plugin.getLogger().info("DEBUG: Found matching UUID for mount ID " + mountId);
                        break;
                    }
                }
            }
            
            if (targetUuid == null) {
                plugin.getLogger().info("DEBUG: No matching UUID found for mount ID " + mountId);
                plugin.runSync(() -> {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("id", String.valueOf(mountId));
                    sendMessage(player, "mount_not_found_with_id", placeholders);
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
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("id", String.valueOf(mountId));
                    sendMessage(player, "mount_not_found_with_id", placeholders);
                    future.complete(false);
                    return;
                }
                
                final Entity finalTargetMount = targetMount;
                
                // Store the mount
                plugin.runAsync(() -> {
                    boolean result = storeEntityAsMountById(player, finalTargetMount, mountId);
                    plugin.getLogger().info("DEBUG: Storage result: " + result);
                    future.complete(result);
                });
            });
        });
        
        return future;
    }
    
    private boolean storeEntityAsMountById(Player player, Entity vehicle, int mountId) {
        try {
            plugin.getLogger().info("DEBUG: storeEntityAsMountById called with mount ID: " + mountId);
            
            MountType mountType = MountType.fromEntityType(vehicle.getType());
            MountAttributes attributes = MountAttributes.fromEntity(vehicle);
            String mountDataYaml = serializer.serializeAttributes(attributes);
            String chestInventoryData = null;
            
            if (mountType.canHaveChest() && vehicle instanceof InventoryHolder) {
                chestInventoryData = serializer.serializeChestInventory(((InventoryHolder) vehicle).getInventory());
            }
            
            plugin.getLogger().info("DEBUG: About to update database for mount ID: " + mountId);
            boolean updated = database.updateMountData(
                mountId,
                mountType.name(),
                mountDataYaml,
                chestInventoryData
            ).get();
            
            plugin.getLogger().info("DEBUG: Database update result: " + updated);
            
            if (updated) {
                untrackActiveMount(player, vehicle);
                
                // Play storing effects before removing
                plugin.runSync(() -> {
                    playStoringEffects(vehicle.getLocation(), player);
                    vehicle.remove();
                });
                
                sendMessage(player, "mount_stored");
                return true;
            } else {
                sendMessage(player, "mount_store_failed");
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error in storeEntityAsMountById", e);
            sendMessage(player, "mount_store_failed");
            return false;
        }
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
            
            // Get the mount ID from the entity
            Integer mountId = getMountId(vehicle.getUniqueId());
            if (mountId == null) {
                plugin.getLogger().warning("Mount ID not found for entity " + vehicle.getUniqueId());
                sendMessage(player, "mount_store_failed");
                return false;
            }
            
            plugin.getLogger().info("DEBUG: About to update database for mount ID: " + mountId);
            boolean updated = database.updateMountData(
                mountId,
                mountType.name(),
                mountDataYaml,
                chestInventoryData
            ).get();
            
            plugin.getLogger().info("DEBUG: Database update result: " + updated);
            
            if (updated) {
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
    
    public CompletableFuture<Boolean> releaseMount(Player player, int mountId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!hasPermission(player, "simplemounts.release")) {
                sendMessage(player, "no_permission");
                return false;
            }
            
            try {
                MountData mountData = database.getMountData(player.getUniqueId(), mountId).get();
                if (mountData == null) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("id", String.valueOf(mountId));
                    sendMessage(player, "mount_not_found_with_id", placeholders);
                    return false;
                }
                
                // Remove active mount if it exists
                Set<UUID> activeMounts = playerActiveMounts.get(player.getUniqueId());
                if (activeMounts != null) {
                    for (UUID entityUuid : activeMounts) {
                        Integer activeMountId = entityMountIds.get(entityUuid);
                        if (activeMountId != null && activeMountId == mountId) {
                            Entity entity = plugin.getServer().getEntity(entityUuid);
                            if (entity != null) {
                                entity.remove();
                            }
                            untrackActiveMount(player, entityUuid);
                            break;
                        }
                    }
                }
                
                boolean deleted = database.deleteMountData(player.getUniqueId(), mountId).get();
                if (deleted) {
                    sendMessage(player, "mount_released", mountData.getDisplayName());
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
    
    public CompletableFuture<List<MountData>> getMountsByName(Player player, String mountName) {
        return database.getMountsByName(player.getUniqueId(), mountName);
    }
    
    public CompletableFuture<MountData> getMountData(Player player, int mountId) {
        return database.getMountData(player.getUniqueId(), mountId);
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
            
            // Get the mount ID from the entity
            Integer mountId = getMountId(entity.getUniqueId());
            if (mountId == null) {
                plugin.getLogger().warning("Mount ID not found for entity " + entity.getUniqueId());
                return false;
            }
            
            // Update existing mount data in database
            boolean saved = database.updateMountData(
                mountId,
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
            List<MountData> existing = database.getMountsByName(player.getUniqueId(), mountName).get();
            return !existing.isEmpty();
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
        // This is the legacy method - redirect to ID-based version
        int tempId = 0; // Temporary ID for legacy calls
        tagEntityAsOwnedMount(entity, owner, tempId, mountName);
    }
    
    private void trackActiveMount(Player player, Entity entity, String mountName) {
        // This is the legacy method - redirect to ID-based version
        // For backwards compatibility, we'll generate a temporary ID
        int tempId = 0; // Temporary ID for legacy calls
        trackActiveMount(player, entity, tempId, mountName);
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
        entityMountIds.remove(entityUuid);
        database.removeActiveMount(entityUuid);
    }
    
    private boolean isPlayerOwnedMount(Entity entity, Player player) {
        org.bukkit.NamespacedKey ownerKey = new org.bukkit.NamespacedKey(plugin, "simplemounts_owner");
        String ownerUuid = entity.getPersistentDataContainer().get(ownerKey, org.bukkit.persistence.PersistentDataType.STRING);
        
        return ownerUuid != null && ownerUuid.equals(player.getUniqueId().toString());
    }
    
    private boolean isEntityOwnedByOtherPlayer(Entity entity, Player player) {
        org.bukkit.NamespacedKey ownerKey = new org.bukkit.NamespacedKey(plugin, "simplemounts_owner");
        String ownerUuid = entity.getPersistentDataContainer().get(ownerKey, org.bukkit.persistence.PersistentDataType.STRING);
        
        if (ownerUuid != null) {
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
        Map<String, String> placeholders = new HashMap<>();
        if (replacements.length > 0) {
            placeholders.put("name", replacements[0]);
            placeholders.put("limit", replacements[0]);
            placeholders.put("min", replacements[0]);
            if (replacements.length > 1) {
                placeholders.put("limit", replacements[1]);
                placeholders.put("max", replacements[1]);
                placeholders.put("grace", replacements[1]);
                if (replacements.length > 2) {
                    placeholders.put("type", replacements[2]);
                }
            }
        }
        sendMessage(player, messageKey, placeholders);
    }
    
    private void sendMessage(Player player, String messageKey, Map<String, String> placeholders) {
        String message = config.getMessage(messageKey);
        String prefix = config.getMessagePrefix();
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
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
    
    public Integer getMountId(UUID entityUuid) {
        return entityMountIds.get(entityUuid);
    }
    
    public boolean isActiveMount(UUID entityUuid) {
        return entityUuid != null && entityMountNames.containsKey(entityUuid);
    }
    
    public boolean isMountActive(Player player, String mountName) {
        if (mountName == null) {
            return false; // Unnamed mounts can't be checked by name
        }
        
        Set<UUID> activeMounts = playerActiveMounts.get(player.getUniqueId());
        if (activeMounts == null) {
            return false;
        }
        
        return activeMounts.stream()
            .anyMatch(uuid -> mountName.equals(entityMountNames.get(uuid)));
    }
    
    public boolean isMountActive(Player player, int mountId) {
        Set<UUID> activeMounts = playerActiveMounts.get(player.getUniqueId());
        if (activeMounts == null) {
            return false;
        }
        
        return activeMounts.stream()
            .anyMatch(uuid -> {
                Integer entityMountId = entityMountIds.get(uuid);
                return entityMountId != null && entityMountId == mountId;
            });
    }
    
    public CompletableFuture<Boolean> renameMount(Player player, int mountId, String newName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!hasPermission(player, "simplemounts.rename")) {
                sendMessage(player, "no_permission");
                return false;
            }
            
            try {
                MountData mountData = database.getMountData(player.getUniqueId(), mountId).get();
                if (mountData == null) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("id", String.valueOf(mountId));
                    sendMessage(player, "mount_not_found_with_id", placeholders);
                    return false;
                }
                
                boolean updated = database.updateMountName(player.getUniqueId(), mountId, newName).get();
                if (updated) {
                    // Update active mount name if it's currently active
                    Set<UUID> activeMounts = playerActiveMounts.get(player.getUniqueId());
                    if (activeMounts != null) {
                        for (UUID entityUuid : activeMounts) {
                            Integer activeMountId = entityMountIds.get(entityUuid);
                            if (activeMountId != null && activeMountId == mountId) {
                                if (newName != null && !newName.trim().isEmpty()) {
                                    entityMountNames.put(entityUuid, newName);
                                } else {
                                    entityMountNames.remove(entityUuid);
                                }
                                
                                // Update entity custom name on main thread
                                final UUID finalEntityUuid = entityUuid;
                                final String finalNewName = newName;
                                plugin.runSync(() -> {
                                    Entity entity = plugin.getServer().getEntity(finalEntityUuid);
                                    if (entity != null) {
                                        if (finalNewName != null && !finalNewName.trim().isEmpty()) {
                                            entity.setCustomName(ChatColor.GOLD + finalNewName);
                                            entity.setCustomNameVisible(true);
                                        } else {
                                            entity.setCustomName(null);
                                            entity.setCustomNameVisible(false);
                                        }
                                    }
                                });
                                break;
                            }
                        }
                    }
                    
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("old_name", mountData.getDisplayName());
                    placeholders.put("new_name", newName != null && !newName.trim().isEmpty() ? 
                        newName : config.getMessage("unnamed_mount_display").replace("{type}", mountData.getMountTypeEnum().getDisplayName()).replace("{id}", String.valueOf(mountId)));
                    sendMessage(player, "mount_renamed", placeholders);
                    return true;
                } else {
                    sendMessage(player, "mount_rename_failed");
                    return false;
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error renaming mount", e);
                sendMessage(player, "mount_rename_failed");
                return false;
            }
        });
    }
}