package com.simplemounts.gui;

import com.simplemounts.SimpleMounts;
import com.simplemounts.data.MountData;
import com.simplemounts.serialization.MountSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MountListGUI {
    
    private final SimpleMounts plugin;
    private final Player player;
    private final int page;
    private final int itemsPerPage = 28; // 6 rows, excluding borders and navigation
    
    public MountListGUI(SimpleMounts plugin, Player player) {
        this(plugin, player, 0);
    }
    
    public MountListGUI(SimpleMounts plugin, Player player, int page) {
        this.plugin = plugin;
        this.player = player;
        this.page = page;
    }
    
    public void open() {
        // Load mounts asynchronously to get count for title
        plugin.runAsync(() -> {
            plugin.getMountManager().getPlayerMounts(player).thenAccept(mounts -> {
                plugin.runSync(() -> {
                    // Get mount limit for this player
                    int maxMounts = getPlayerMountLimit();
                    int currentMounts = mounts.size();
                    
                    // Create title with mount count
                    String title = ChatColor.GOLD + "" + ChatColor.BOLD + "Mounts " + 
                                  ChatColor.WHITE + "(" + currentMounts + "/" + maxMounts + ")";
                    if (page > 0) {
                        title += ChatColor.GOLD + " - P" + (page + 1);
                    }
                    
                    Inventory inventory = Bukkit.createInventory(null, 54, title);
                    
                    // Add border
                    GUIManager.addBorder(inventory);
                    
                    // Add player head and navigation items
                    addNavigationItems(inventory);
                    
                    // Display the loaded mounts
                    displayMounts(inventory, mounts);
                    
                    player.openInventory(inventory);
                });
            });
        });
    }
    
    private int getPlayerMountLimit() {
        // Check for unlimited permission
        if (player.hasPermission("simplemounts.unlimited")) {
            return 999;
        }
        
        // Check for specific limit permissions (highest takes precedence)
        if (player.hasPermission("simplemounts.limit.100")) return 100;
        if (player.hasPermission("simplemounts.limit.50")) return 50;
        if (player.hasPermission("simplemounts.limit.25")) return 25;
        if (player.hasPermission("simplemounts.limit.10")) return 10;
        
        // Default limit from config
        return plugin.getConfigManager().getDefaultMountLimit();
    }
    
    private void addNavigationItems(Inventory inventory) {
        // Player head in center bottom
        ItemStack playerHead = GUIManager.createPlayerHead(player);
        inventory.setItem(49, playerHead);
        
        // Previous page (if applicable)
        if (page > 0) {
            ItemStack prevButton = GUIManager.createNavigationItem(
                Material.ARROW,
                "Previous Page",
                "Go to page " + page
            );
            inventory.setItem(46, prevButton);
        }
    }
    
    
    private void displayMounts(Inventory inventory, List<MountData> mounts) {
        if (mounts.isEmpty()) {
            // Show empty state
            ItemStack emptyState = GUIManager.createNavigationItem(
                Material.SADDLE,
                "No Mounts Found",
                "You don't have any stored mounts yet.",
                "Use custom taming items on rideable",
                "entities to claim them as mounts!"
            );
            inventory.setItem(22, emptyState);
            return;
        }
        
        // Calculate pagination
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, mounts.size());
        
        // Add next page button if needed
        if (endIndex < mounts.size()) {
            ItemStack nextButton = GUIManager.createNavigationItem(
                Material.ARROW,
                "Next Page",
                "Go to page " + (page + 2)
            );
            inventory.setItem(52, nextButton);
        }
        
        // Display mounts
        int slot = 10; // Starting slot (avoiding border)
        for (int i = startIndex; i < endIndex; i++) {
            MountData mountData = mounts.get(i);
            
            // Skip border slots
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                if (slot % 9 == 0 || slot % 9 == 8) {
                    slot++;
                }
            }
            
            // Skip if we've reached the bottom row
            if (slot >= 45) {
                break;
            }
            
            // Parse mount attributes
            Map<String, Object> attributes = parseAttributes(mountData.getMountDataYaml());
            
            // Check if mount is active
            boolean isActive = plugin.getMountManager().isMountActive(player, mountData.getMountName());
            
            // Create mount item
            ItemStack mountItem = GUIManager.createMountItem(
                mountData.getMountName(),
                mountData.getMountType(),
                isActive,
                getDoubleAttribute(attributes, "health", 20.0),
                getDoubleAttribute(attributes, "maxHealth", 20.0),
                getDoubleAttribute(attributes, "speed", 0.2),
                getDoubleAttribute(attributes, "jumpStrength", 0.7)
            );
            
            inventory.setItem(slot, mountItem);
            slot++;
        }
        
        // Add page info (only if multiple pages)
        if (mounts.size() > itemsPerPage) {
            ItemStack pageInfo = GUIManager.createNavigationItem(
                Material.PAPER,
                "Page Info",
                "Page: " + (page + 1) + "/" + ((mounts.size() - 1) / itemsPerPage + 1)
            );
            inventory.setItem(47, pageInfo);
        }
    }
    
    private Map<String, Object> parseAttributes(String yamlData) {
        MountSerializer serializer = new MountSerializer(plugin);
        return serializer.parseSerializedData(yamlData);
    }
    
    private double getDoubleAttribute(Map<String, Object> attributes, String key, double defaultValue) {
        Object value = attributes.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    public void nextPage() {
        MountListGUI nextPageGUI = new MountListGUI(plugin, player, page + 1);
        nextPageGUI.open();
    }
    
    public void previousPage() {
        if (page > 0) {
            MountListGUI prevPageGUI = new MountListGUI(plugin, player, page - 1);
            prevPageGUI.open();
        }
    }
    
    public void refresh() {
        MountListGUI refreshedGUI = new MountListGUI(plugin, player, page);
        refreshedGUI.open();
    }
}