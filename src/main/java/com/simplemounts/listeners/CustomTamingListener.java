package com.simplemounts.listeners;

import com.simplemounts.SimpleMounts;
import com.simplemounts.core.MountManager;
import com.simplemounts.data.MountType;
import com.simplemounts.data.TamingItem;
import com.simplemounts.util.RideableDetector;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

public class CustomTamingListener implements Listener {
    
    private final SimpleMounts plugin;
    private final MountManager mountManager;
    
    public CustomTamingListener(SimpleMounts plugin) {
        this.plugin = plugin;
        this.mountManager = plugin.getMountManager();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Debug logging
        plugin.getLogger().info("DEBUG: Player " + player.getName() + " right-clicked entity " + entity.getType() + " with item " + (item != null ? item.getType() : "AIR"));
        
        if (!plugin.getConfigManager().isCustomTamingEnabled()) {
            plugin.getLogger().info("DEBUG: Custom taming is disabled");
            return;
        }
        
        if (!RideableDetector.isRideable(entity)) {
            plugin.getLogger().info("DEBUG: Entity " + entity.getType() + " is not rideable");
            return;
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        if (!plugin.getConfigManager().isMountTypeEnabled(mountType.name())) {
            plugin.getLogger().info("DEBUG: Mount type " + mountType.name() + " is not enabled");
            return;
        }
        
        if (!player.hasPermission("simplemounts.claim")) {
            plugin.getLogger().info("DEBUG: Player " + player.getName() + " lacks permission simplemounts.claim");
            return;
        }
        
        // Allow claiming already tamed entities - we'll override the ownership
        // if (isEntityAlreadyOwned(entity)) {
        //     plugin.getLogger().info("DEBUG: Entity is already owned");
        //     sendMessage(player, "entity_protected");
        //     return;
        // }
        
        if (isProtectedByOtherPlugins(entity, player)) {
            plugin.getLogger().info("DEBUG: Entity is protected by other plugins");
            sendMessage(player, "entity_protected");
            return;
        }
        
        TamingItem tamingItem = plugin.getConfigManager().getTamingItem(mountType.name());
        plugin.getLogger().info("DEBUG: Got taming item for " + mountType.name() + ": " + tamingItem.getMaterial());
        
        if (!tamingItem.matches(item)) {
            plugin.getLogger().info("DEBUG: Type-specific taming item doesn't match, trying default");
            tamingItem = plugin.getConfigManager().getTamingItem("default");
            plugin.getLogger().info("DEBUG: Got default taming item: " + tamingItem.getMaterial());
            
            if (!tamingItem.matches(item)) {
                plugin.getLogger().info("DEBUG: Default taming item doesn't match either. Item: " + (item != null ? item.getType() + " with name: " + (item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : "no name") : "null"));
                return;
            }
        }
        
        if (plugin.getConfigManager().requireEmptyHand() && 
            player.getInventory().getItemInOffHand().getType() != org.bukkit.Material.AIR) {
            plugin.getLogger().info("DEBUG: Player needs empty off-hand");
            sendMessage(player, "require_empty_hand");
            return;
        }
        
        plugin.getLogger().info("DEBUG: All checks passed, claiming mount!");
        event.setCancelled(true);
        
        // Check if entity is already tracked as an active mount
        if (mountManager.isActiveMount(entity.getUniqueId())) {
            plugin.getLogger().info("DEBUG: Entity is already an active SimpleMounts mount - skipping duplicate claim");
            return;
        }
        
        if (Math.random() * 100 > plugin.getConfigManager().getTamingSuccessChance()) {
            plugin.getLogger().info("DEBUG: Taming failed due to chance");
            sendMessage(player, "taming_failed");
            return;
        }
        
        String mountName = null; // Mounts are unnamed by default
        plugin.getLogger().info("DEBUG: Mount will be unnamed by default");
        final TamingItem finalTamingItem = tamingItem; // Make it final for lambda
        
        plugin.runAsync(() -> {
            mountManager.claimMount(player, entity, mountName).thenAccept(success -> {
                plugin.getLogger().info("DEBUG: Mount claim result: " + success);
                if (success) {
                    plugin.runSync(() -> {
                        // Play taming effects
                        playTamingEffects(entity.getLocation(), player);
                        
                        // Consume taming item if configured
                        if (finalTamingItem.isConsumeOnUse()) {
                            consumeItem(player, item);
                        }
                        
                        // Mount is claimed and active in the world
                        // Auto-store it after a short delay to allow player to see it
                        plugin.runAsync(() -> {
                            try {
                                Thread.sleep(2000); // 2 second delay to show the mount
                                // Get the mount ID and store by ID
                                Integer mountId = mountManager.getMountId(entity.getUniqueId());
                                if (mountId != null) {
                                    mountManager.storeMount(player, mountId).thenAccept(stored -> {
                                        // Auto-storage happens silently
                                    });
                                } else {
                                    plugin.getLogger().warning("Could not find mount ID for auto-storage");
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                    });
                }
            });
        });
    }
    
    private boolean isEntityAlreadyOwned(Entity entity) {
        return entity.hasMetadata("simplemounts.owner") || 
               entity.hasMetadata("simplemounts.claimed") ||
               (entity instanceof org.bukkit.entity.Tameable && 
                ((org.bukkit.entity.Tameable) entity).isTamed());
    }
    
    private boolean isProtectedByOtherPlugins(Entity entity, Player player) {
        if (!plugin.getConfigManager().respectProtectionPlugins()) {
            return false;
        }
        
        // Check for Citizens NPCs
        if (plugin.getConfigManager().ignoreNpcs() && 
            entity.hasMetadata("NPC")) {
            plugin.getLogger().info("DEBUG: Entity protected by Citizens NPC");
            return true;
        }
        
        // Check for MythicMobs
        if (plugin.getConfigManager().ignoreMythicMobs() && 
            entity.hasMetadata("MythicMob")) {
            plugin.getLogger().info("DEBUG: Entity protected by MythicMobs");
            return true;
        }
        
        // Check for other plugin metadata indicating protection
        if (entity.hasMetadata("protected") || 
            entity.hasMetadata("no-tame") ||
            entity.hasMetadata("mount-protected") ||
            entity.hasMetadata("shopkeeper") ||
            entity.hasMetadata("quest-npc") ||
            entity.hasMetadata("custom-entity")) {
            plugin.getLogger().info("DEBUG: Entity protected by generic metadata");
            return true;
        }
        
        // Check if entity belongs to another mount plugin
        if (entity.hasMetadata("horse-plugin") ||
            entity.hasMetadata("mounts") ||
            entity.hasMetadata("custom-mount") ||
            entity.getPersistentDataContainer().has(new org.bukkit.NamespacedKey("horses", "owner"), org.bukkit.persistence.PersistentDataType.STRING) ||
            entity.getPersistentDataContainer().has(new org.bukkit.NamespacedKey("ultimatehorses", "owner"), org.bukkit.persistence.PersistentDataType.STRING)) {
            plugin.getLogger().info("DEBUG: Entity protected by another mount plugin");
            return true;
        }
        
        // Check WorldGuard protection (if available)
        if (plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            try {
                org.bukkit.plugin.Plugin wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
                if (wgPlugin != null) {
                    // Basic check - more sophisticated integration could be added
                    if (!player.hasPermission("worldguard.region.bypass.*")) {
                        // In protected regions, players might not be able to tame
                        plugin.getLogger().info("DEBUG: WorldGuard present - respecting region permissions");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().info("DEBUG: WorldGuard check failed: " + e.getMessage());
            }
        }
        
        // Check for Towny protection
        if (plugin.getServer().getPluginManager().isPluginEnabled("Towny")) {
            try {
                // If in a town where the player doesn't have permissions
                if (!player.hasPermission("towny.town.claim.tame")) {
                    plugin.getLogger().info("DEBUG: Towny protection may apply");
                }
            } catch (Exception e) {
                plugin.getLogger().info("DEBUG: Towny check failed: " + e.getMessage());
            }
        }
        
        return false;
    }
    
    private void consumeItem(Player player, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        
        player.updateInventory();
    }
    
    private String generateMountName(MountType mountType) {
        String baseName = mountType.getDisplayName().toLowerCase();
        // Use only last 4 digits of timestamp to keep name shorter
        long timestamp = System.currentTimeMillis();
        String shortTimestamp = String.valueOf(timestamp % 10000);
        String mountName = baseName + "_" + shortTimestamp;
        
        // Ensure it fits within config limits
        int maxLength = plugin.getConfigManager().getMaxNameLength();
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
    
    private void playTamingEffects(Location location, Player player) {
        if (!plugin.getConfigManager().playTamingEffects()) {
            return;
        }
        
        // Play particles
        location.getWorld().spawnParticle(org.bukkit.Particle.HEART, location.clone().add(0, 1, 0), 15, 1.0, 1.0, 1.0, 0.1);
        location.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, location.clone().add(0, 0.5, 0), 10, 0.5, 0.5, 0.5, 0.1);
        
        // Play sounds
        player.playSound(location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.playSound(location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
    
    private void sendMessage(Player player, String messageKey, String... replacements) {
        String message = plugin.getConfigManager().getMessage(messageKey);
        String prefix = plugin.getConfigManager().getMessagePrefix();
        
        if (replacements.length > 0) {
            message = message.replace("{name}", replacements[0]);
        }
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
}