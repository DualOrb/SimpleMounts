package com.simplemounts.listeners;

import com.simplemounts.SimpleMounts;
import com.simplemounts.core.MountManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.Inventory;

public class InventoryListener implements Listener {
    
    private final SimpleMounts plugin;
    private final MountManager mountManager;
    
    public InventoryListener(SimpleMounts plugin) {
        this.plugin = plugin;
        this.mountManager = plugin.getMountManager();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        if (!isHorseInventory(inventory)) {
            return;
        }
        
        Entity entity = getEntityFromInventory(inventory);
        if (entity == null || !mountManager.isActiveMount(entity.getUniqueId())) {
            return;
        }
        
        if (!isPlayerOwnedMount(entity, player)) {
            if (plugin.getConfigManager().protectChestAccess()) {
                event.setCancelled(true);
                sendMessage(player, "mount_protected");
                return;
            }
        }
        
        // Update last accessed time
        Integer mountId = mountManager.getMountId(entity.getUniqueId());
        if (mountId != null) {
            plugin.runAsync(() -> {
                plugin.getDatabaseManager().updateLastAccessed(player.getUniqueId(), mountId);
            });
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        
        if (!isHorseInventory(inventory)) {
            return;
        }
        
        Entity entity = getEntityFromInventory(inventory);
        if (entity == null || !mountManager.isActiveMount(entity.getUniqueId())) {
            return;
        }
        
        if (!isPlayerOwnedMount(entity, player)) {
            if (plugin.getConfigManager().protectChestAccess()) {
                event.setCancelled(true);
                sendMessage(player, "mount_protected");
                return;
            }
        }
        
        // Auto-save inventory changes after a short delay
        plugin.runSyncDelayed(() -> {
            String mountName = mountManager.getMountName(entity.getUniqueId());
            if (mountName != null) {
                plugin.runAsync(() -> {
                    mountManager.storeMount(player, mountName);
                });
            }
        }, 20L); // 1 second delay
    }
    
    private boolean isHorseInventory(Inventory inventory) {
        return inventory.getType() == InventoryType.CHEST && 
               inventory instanceof HorseInventory;
    }
    
    private Entity getEntityFromInventory(Inventory inventory) {
        if (inventory instanceof HorseInventory) {
            HorseInventory horseInventory = (HorseInventory) inventory;
            return (Entity) horseInventory.getHolder();
        }
        
        // Try to find the entity by checking all entities in the world
        // This is a fallback method and might be less reliable
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof AbstractHorse) {
                    AbstractHorse horse = (AbstractHorse) entity;
                    if (horse.getInventory().equals(inventory)) {
                        return entity;
                    }
                }
            }
        }
        
        return null;
    }
    
    private boolean isPlayerOwnedMount(Entity entity, Player player) {
        org.bukkit.NamespacedKey ownerKey = new org.bukkit.NamespacedKey(plugin, "simplemounts_owner");
        String ownerUuid = entity.getPersistentDataContainer().get(ownerKey, org.bukkit.persistence.PersistentDataType.STRING);
        
        if (ownerUuid == null) {
            return false;
        }
        
        return ownerUuid.equals(player.getUniqueId().toString());
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