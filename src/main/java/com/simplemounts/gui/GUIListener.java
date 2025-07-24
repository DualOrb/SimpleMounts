package com.simplemounts.gui;

import com.simplemounts.SimpleMounts;
import com.simplemounts.data.MountData;
import com.simplemounts.core.MountManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GUIListener implements Listener {
    
    private final SimpleMounts plugin;
    private final GUIManager guiManager;
    private final ConcurrentMap<Player, Integer> waitingForRename;
    
    public GUIListener(SimpleMounts plugin) {
        this.plugin = plugin;
        this.guiManager = plugin.getGUIManager();
        this.waitingForRename = new ConcurrentHashMap<>();
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        
        // Check if player has an active GUI session
        if (!guiManager.hasActiveSession(player)) {
            return;
        }
        
        // Prevent moving items in GUI
        event.setCancelled(true);
        
        // Get the GUI session
        GUISession session = guiManager.getSession(player);
        if (session == null) {
            return;
        }
        
        // Update last interaction
        session.updateLastInteraction();
        
        // Handle clicks based on GUI type
        switch (session.getType()) {
            case MOUNT_LIST:
                handleMountListClick(event, player, session);
                break;
            case MOUNT_INFO:
                handleMountInfoClick(event, player, session);
                break;
            case CONFIRMATION:
                handleConfirmationClick(event, player, session);
                break;
        }
    }
    
    private void handleMountListClick(InventoryClickEvent event, Player player, GUISession session) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }
        
        String displayName = meta.getDisplayName();
        
        // Handle navigation buttons
        if (displayName.contains("Next Page")) {
            MountListGUI mountGUI = new MountListGUI(plugin, player, session.getPage());
            mountGUI.nextPage();
            return;
        }
        
        if (displayName.contains("Previous Page")) {
            MountListGUI mountGUI = new MountListGUI(plugin, player, session.getPage());
            mountGUI.previousPage();
            return;
        }
        
        // Ignore clicks on player head (it's just decorative)
        if (displayName.contains("'s Mounts")) {
            return;
        }
        
        // Handle mount clicks
        if (isMountItem(clicked)) {
            Integer mountId = extractMountId(clicked);
            if (mountId != null) {
                // Any click - summon/store mount (removed right-click details)
                handleMountAction(player, mountId, clicked);
            }
        }
    }
    
    private void handleMountInfoClick(InventoryClickEvent event, Player player, GUISession session) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }
        
        String displayName = meta.getDisplayName();
        
        // Handle navigation buttons
        if (displayName.contains("Back to Mount List")) {
            guiManager.openMountGUI(player);
            return;
        }
        
        if (displayName.contains("Close")) {
            player.closeInventory();
            return;
        }
        
        if (displayName.contains("Refresh")) {
            MountInfoGUI infoGUI = new MountInfoGUI(plugin, player, session.getSelectedMountId());
            infoGUI.refresh();
            return;
        }
        
        // Handle action buttons
        Integer mountId = session.getSelectedMountId();
        if (mountId == null) {
            return;
        }
        
        if (displayName.contains("Summon Mount")) {
            guiManager.openConfirmationGUI(player, "summon_mount", mountId);
        } else if (displayName.contains("Store Mount")) {
            guiManager.openConfirmationGUI(player, "store_mount", mountId);
        } else if (displayName.contains("Rename Mount")) {
            guiManager.openConfirmationGUI(player, "rename_mount", mountId);
        } else if (displayName.contains("Release Mount")) {
            guiManager.openConfirmationGUI(player, "release_mount", mountId);
        }
    }
    
    private void handleConfirmationClick(InventoryClickEvent event, Player player, GUISession session) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            return;
        }
        
        String displayName = meta.getDisplayName();
        Integer mountId = session.getSelectedMountId();
        String action = session.getAction();
        
        if (displayName.contains("Confirm")) {
            executeAction(player, action, mountId);
        } else if (displayName.contains("Cancel")) {
            // Return to mount info
            guiManager.openMountInfoGUI(player, mountId);
        }
    }
    
    private void handleMountAction(Player player, Integer mountId, ItemStack mountItem) {
        // Check if mount is active by looking at the lore
        ItemMeta meta = mountItem.getItemMeta();
        if (meta == null || meta.getLore() == null) {
            return;
        }
        
        boolean isActive = false;
        for (String line : meta.getLore()) {
            if (line.contains("Status:") && line.contains("Active")) {
                isActive = true;
                break;
            }
        }
        
        if (isActive) {
            // Store the mount
            executeAction(player, "store_mount", mountId);
        } else {
            // Summon the mount
            executeAction(player, "summon_mount", mountId);
        }
    }
    
    private void executeAction(Player player, String action, Integer mountId) {
        MountManager mountManager = plugin.getMountManager();
        
        switch (action.toLowerCase()) {
            case "summon_mount":
                plugin.runAsync(() -> {
                    // The summonMount method already handles dismissing existing mounts
                    mountManager.summonMount(player, mountId).thenAccept(summoned -> {
                        plugin.runSync(() -> {
                            player.closeInventory();
                            guiManager.closeSession(player);
                        });
                    });
                });
                break;
                
            case "store_mount":
                plugin.runAsync(() -> {
                    mountManager.storeMount(player, mountId).thenAccept(stored -> {
                        plugin.runSync(() -> {
                            player.closeInventory();
                            guiManager.closeSession(player);
                        });
                    });
                });
                break;
                
            case "release_mount":
                plugin.runAsync(() -> {
                    mountManager.releaseMount(player, mountId);
                    plugin.runSync(() -> {
                        player.closeInventory();
                        guiManager.closeSession(player);
                    });
                });
                break;
                
            case "rename_mount":
                // Start rename process
                player.closeInventory();
                guiManager.closeSession(player);
                waitingForRename.put(player, mountId);
                player.sendMessage(ChatColor.YELLOW + "Please type the new name for your mount in chat:");
                player.sendMessage(ChatColor.GRAY + "Type 'cancel' to cancel the rename.");
                break;
        }
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        if (!waitingForRename.containsKey(player)) {
            return;
        }
        
        event.setCancelled(true);
        
        Integer mountId = waitingForRename.remove(player);
        String newName = event.getMessage().trim();
        
        if (newName.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "Rename cancelled.");
            return;
        }
        
        // Validate new name
        if (newName.length() < 3 || newName.length() > 16) {
            player.sendMessage(ChatColor.RED + "Mount name must be between 3 and 16 characters long.");
            return;
        }
        
        if (!newName.matches("^[a-zA-Z0-9_]+$")) {
            player.sendMessage(ChatColor.RED + "Mount name can only contain letters, numbers, and underscores.");
            return;
        }
        
        // Rename the mount
        plugin.runAsync(() -> {
            plugin.getMountManager().renameMount(player, mountId, newName);
        });
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        
        // Always close GUI session when inventory closes
        // This prevents players from getting stuck in GUI interaction mode
        if (guiManager.hasActiveSession(player)) {
            guiManager.closeSession(player);
        }
    }
    
    private boolean isMountItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        // Check if it's a spawn egg (mount items use spawn eggs)
        String materialName = item.getType().name();
        return materialName.contains("_SPAWN_EGG");
    }
    
    private Integer extractMountId(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        
        return item.getItemMeta().getPersistentDataContainer().get(
            new org.bukkit.NamespacedKey(plugin, "mount_id"), 
            org.bukkit.persistence.PersistentDataType.INTEGER
        );
    }
}