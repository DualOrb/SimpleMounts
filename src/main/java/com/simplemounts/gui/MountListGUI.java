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
        String title = ChatColor.GOLD + "" + ChatColor.BOLD + "Mount Manager";
        if (page > 0) {
            title += " - Page " + (page + 1);
        }
        
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        
        // Add border
        GUIManager.addBorder(inventory);
        
        // Add navigation items
        addNavigationItems(inventory);
        
        // Load and display mounts
        loadMounts(inventory);
        
        player.openInventory(inventory);
    }
    
    private void addNavigationItems(Inventory inventory) {
        // Close button
        ItemStack closeButton = GUIManager.createNavigationItem(
            Material.BARRIER, 
            "Close", 
            "Click to close the mount manager"
        );
        inventory.setItem(49, closeButton);
        
        // Help button
        ItemStack helpButton = GUIManager.createNavigationItem(
            Material.BOOK,
            "Help",
            "Left-click a mount to summon/store",
            "Right-click a mount for more info",
            "Use the whistle to open this GUI"
        );
        inventory.setItem(45, helpButton);
        
        // Refresh button
        ItemStack refreshButton = GUIManager.createNavigationItem(
            Material.LIME_DYE,
            "Refresh",
            "Click to refresh the mount list"
        );
        inventory.setItem(53, refreshButton);
        
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
    
    private void loadMounts(Inventory inventory) {
        plugin.runAsync(() -> {
            plugin.getMountManager().getPlayerMounts(player).thenAccept(mounts -> {
                plugin.runSync(() -> {
                    displayMounts(inventory, mounts);
                });
            });
        });
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
            boolean isActive = plugin.getMountManager().isActiveMount(
                plugin.getMountManager().getPlayerActiveMounts(player.getUniqueId())
                    .stream()
                    .filter(uuid -> mountData.getMountName().equals(plugin.getMountManager().getMountName(uuid)))
                    .findFirst()
                    .orElse(null)
            );
            
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
        
        // Add mount count info
        ItemStack countInfo = GUIManager.createNavigationItem(
            Material.PAPER,
            "Mount Count",
            "Total Mounts: " + mounts.size(),
            "Page: " + (page + 1) + "/" + ((mounts.size() - 1) / itemsPerPage + 1)
        );
        inventory.setItem(47, countInfo);
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