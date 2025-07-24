package com.simplemounts.listeners;

import com.simplemounts.SimpleMounts;
import com.simplemounts.core.MountManager;
import com.simplemounts.data.MountType;
import com.simplemounts.data.TamingItem;
import com.simplemounts.util.RideableDetector;
import org.bukkit.ChatColor;
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
        if (!plugin.getConfigManager().isCustomTamingEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!RideableDetector.isRideable(entity)) {
            return;
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        if (!plugin.getConfigManager().isMountTypeEnabled(mountType.name())) {
            return;
        }
        
        if (!player.hasPermission("simplemounts.claim")) {
            return;
        }
        
        if (isEntityAlreadyOwned(entity)) {
            sendMessage(player, "entity_protected");
            return;
        }
        
        if (isProtectedByOtherPlugins(entity, player)) {
            sendMessage(player, "entity_protected");
            return;
        }
        
        TamingItem tamingItem = plugin.getConfigManager().getTamingItem(mountType.name());
        if (!tamingItem.matches(item)) {
            tamingItem = plugin.getConfigManager().getTamingItem("default");
            if (!tamingItem.matches(item)) {
                return;
            }
        }
        
        if (plugin.getConfigManager().requireEmptyHand() && 
            player.getInventory().getItemInOffHand().getType() != org.bukkit.Material.AIR) {
            sendMessage(player, "require_empty_hand");
            return;
        }
        
        event.setCancelled(true);
        
        if (Math.random() * 100 > plugin.getConfigManager().getTamingSuccessChance()) {
            sendMessage(player, "taming_failed");
            return;
        }
        
        String mountName = generateMountName(mountType);
        
        plugin.runAsync(() -> {
            mountManager.claimMount(player, entity, mountName).thenAccept(success -> {
                if (success && tamingItem.isConsumeOnUse()) {
                    plugin.runSync(() -> {
                        consumeItem(player, item);
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
            return true;
        }
        
        // Check for MythicMobs
        if (plugin.getConfigManager().ignoreMythicMobs() && 
            entity.hasMetadata("MythicMob")) {
            return true;
        }
        
        // Check for other plugin metadata indicating protection
        if (entity.hasMetadata("protected") || 
            entity.hasMetadata("no-tame") ||
            entity.hasMetadata("mount-protected")) {
            return true;
        }
        
        // TODO: Add integration with specific protection plugins
        // This would require checking WorldGuard regions, GriefPrevention claims, etc.
        
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
        return baseName + "_" + System.currentTimeMillis();
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