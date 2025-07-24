package com.simplemounts.listeners;

import com.simplemounts.SimpleMounts;
import com.simplemounts.core.MountManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import java.util.UUID;

public class MountInteractionListener implements Listener {
    
    private final SimpleMounts plugin;
    private final MountManager mountManager;
    
    public MountInteractionListener(SimpleMounts plugin) {
        this.plugin = plugin;
        this.mountManager = plugin.getMountManager();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntered();
        Entity vehicle = event.getVehicle();
        
        if (!mountManager.isActiveMount(vehicle.getUniqueId())) {
            return;
        }
        
        if (!isPlayerOwnedMount(vehicle, player)) {
            if (plugin.getConfigManager().preventMountStealing()) {
                event.setCancelled(true);
                sendMessage(player, "mount_protected");
                return;
            }
        }
        
        // Update last accessed time
        String mountName = mountManager.getMountName(vehicle.getUniqueId());
        if (mountName != null) {
            plugin.runAsync(() -> {
                plugin.getDatabaseManager().updateLastAccessed(player.getUniqueId(), mountName);
            });
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player)) {
            return;
        }
        
        Entity vehicle = event.getVehicle();
        
        if (!mountManager.isActiveMount(vehicle.getUniqueId())) {
            return;
        }
        
        // Update active mount location
        plugin.runAsync(() -> {
            plugin.getDatabaseManager().addActiveMount(
                vehicle.getUniqueId(),
                UUID.fromString(vehicle.getMetadata("simplemounts.owner").get(0).asString()),
                mountManager.getMountName(vehicle.getUniqueId()),
                vehicle.getWorld().getName(),
                vehicle.getLocation().getX(),
                vehicle.getLocation().getY(),
                vehicle.getLocation().getZ()
            );
        });
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        
        if (!mountManager.isActiveMount(entity.getUniqueId())) {
            return;
        }
        
        if (!isPlayerOwnedMount(entity, player)) {
            if (plugin.getConfigManager().protectMountInteraction()) {
                event.setCancelled(true);
                sendMessage(player, "mount_protected");
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();
        
        if (!mountManager.isActiveMount(damaged.getUniqueId())) {
            return;
        }
        
        if (!plugin.getConfigManager().allowMountDamage()) {
            event.setCancelled(true);
            return;
        }
        
        if (damager instanceof Player) {
            Player player = (Player) damager;
            
            if (!isPlayerOwnedMount(damaged, player)) {
                if (plugin.getConfigManager().protectMountInteraction()) {
                    event.setCancelled(true);
                    sendMessage(player, "mount_protected");
                    return;
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        
        if (!mountManager.isActiveMount(entity.getUniqueId())) {
            return;
        }
        
        String mountName = mountManager.getMountName(entity.getUniqueId());
        if (mountName == null) {
            return;
        }
        
        String ownerUuidString = entity.getMetadata("simplemounts.owner").get(0).asString();
        UUID ownerUuid = UUID.fromString(ownerUuidString);
        
        plugin.runAsync(() -> {
            // Remove from active mounts
            plugin.getDatabaseManager().removeActiveMount(entity.getUniqueId());
            
            // Remove from stored mounts
            plugin.getDatabaseManager().deleteMountData(ownerUuid, mountName);
            
            // Notify owner if online
            Player owner = plugin.getServer().getPlayer(ownerUuid);
            if (owner != null) {
                plugin.runSync(() -> {
                    sendMessage(owner, "mount_died", mountName);
                });
            }
        });
    }
    
    private boolean isPlayerOwnedMount(Entity entity, Player player) {
        if (!entity.hasMetadata("simplemounts.owner")) {
            return false;
        }
        
        String ownerUuid = entity.getMetadata("simplemounts.owner").get(0).asString();
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