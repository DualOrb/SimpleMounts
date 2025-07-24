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
    
    public MountManager(SimpleMounts plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager();
        this.config = plugin.getConfigManager();
        this.serializer = new MountSerializer(plugin);
        
        this.playerActiveMounts = new ConcurrentHashMap<>();
        this.entityMountNames = new ConcurrentHashMap<>();
        
        startCleanupTask();
    }
    
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            database.cleanupOrphanedMounts();
        }, 6000L, 6000L); // Run every 5 minutes
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
                sendMessage(player, "invalid_mount_name");
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
        return CompletableFuture.supplyAsync(() -> {
            if (!hasPermission(player, "simplemounts.summon")) {
                sendMessage(player, "no_permission");
                return false;
            }
            
            if (player.getVehicle() != null) {
                sendMessage(player, "already_riding");
                return false;
            }
            
            try {
                MountData mountData = database.getMountData(player.getUniqueId(), mountName).get();
                if (mountData == null) {
                    sendMessage(player, "mount_not_found", mountName);
                    return false;
                }
                
                if (config.autoDismissExistingMount()) {
                    dismissPlayerMounts(player);
                }
                
                Location spawnLocation = findSafeSpawnLocation(player, mountData.getMountTypeEnum());
                if (spawnLocation == null) {
                    sendMessage(player, "no_safe_location");
                    return false;
                }
                
                Entity entity = spawnMountEntity(spawnLocation, mountData);
                if (entity == null) {
                    sendMessage(player, "mount_spawn_failed");
                    return false;
                }
                
                applyMountAttributes(entity, mountData);
                tagEntityAsOwnedMount(entity, player, mountName);
                trackActiveMount(player, entity, mountName);
                
                database.updateLastAccessed(player.getUniqueId(), mountName);
                
                sendMessage(player, "mount_summoned", mountName);
                return true;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error summoning mount", e);
                sendMessage(player, "mount_summon_failed");
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> storeMount(Player player, String mountName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!hasPermission(player, "simplemounts.store")) {
                sendMessage(player, "no_permission");
                return false;
            }
            
            Entity vehicle = player.getVehicle();
            if (vehicle == null) {
                sendMessage(player, "not_riding_mount");
                return false;
            }
            
            if (!isPlayerOwnedMount(vehicle, player)) {
                sendMessage(player, "not_your_mount");
                return false;
            }
            
            try {
                MountType mountType = MountType.fromEntityType(vehicle.getType());
                MountAttributes attributes = MountAttributes.fromEntity(vehicle);
                String mountDataYaml = serializer.serializeAttributes(attributes);
                String chestInventoryData = null;
                
                if (mountType.canHaveChest() && vehicle instanceof InventoryHolder) {
                    chestInventoryData = serializer.serializeChestInventory(((InventoryHolder) vehicle).getInventory());
                }
                
                boolean saved = database.saveMountData(
                    player.getUniqueId(),
                    mountName,
                    mountType.name(),
                    mountDataYaml,
                    chestInventoryData
                ).get();
                
                if (saved) {
                    untrackActiveMount(player, vehicle);
                    vehicle.remove();
                    
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
        });
    }
    
    public CompletableFuture<Boolean> storeCurrentMount(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            Entity vehicle = player.getVehicle();
            if (vehicle == null) {
                return false;
            }
            
            String mountName = entityMountNames.get(vehicle.getUniqueId());
            if (mountName == null) {
                mountName = "mount_" + System.currentTimeMillis();
            }
            
            return storeMount(player, mountName).join();
        });
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
        int radius = config.getSafeSpotRadius();
        int maxHeightDiff = config.getMaxHeightDifference();
        
        for (int attempts = 0; attempts < 20; attempts++) {
            double x = playerLoc.getX() + (Math.random() - 0.5) * radius * 2;
            double z = playerLoc.getZ() + (Math.random() - 0.5) * radius * 2;
            double y = playerLoc.getY() + (Math.random() - 0.5) * maxHeightDiff * 2;
            
            Location testLoc = new Location(playerLoc.getWorld(), x, y, z);
            
            if (isSafeSpawnLocation(testLoc, mountType)) {
                return testLoc;
            }
        }
        
        if (config.teleportIfNoSafeSpot()) {
            return playerLoc.clone().add(0, 1, 0);
        }
        
        return null;
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
            MountAttributes attributes = serializer.deserializeAttributes(mountData.getMountDataYaml());
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
            if (replacements.length > 1) {
                message = message.replace("{limit}", replacements[1]);
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
        return entityMountNames.containsKey(entityUuid);
    }
}