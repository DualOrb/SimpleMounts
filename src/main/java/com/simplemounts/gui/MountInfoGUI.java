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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class MountInfoGUI {
    
    private final SimpleMounts plugin;
    private final Player player;
    private final String mountName;
    
    public MountInfoGUI(SimpleMounts plugin, Player player, String mountName) {
        this.plugin = plugin;
        this.player = player;
        this.mountName = mountName;
    }
    
    public void open() {
        String title = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Mount Info - " + mountName;
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        
        // Add border
        GUIManager.addBorder(inventory);
        
        // Load mount data
        loadMountInfo(inventory);
        
        player.openInventory(inventory);
    }
    
    private void loadMountInfo(Inventory inventory) {
        plugin.runAsync(() -> {
            plugin.getMountManager().getMountData(player, mountName).thenAccept(mountData -> {
                plugin.runSync(() -> {
                    if (mountData != null) {
                        displayMountInfo(inventory, mountData);
                    } else {
                        displayNotFound(inventory);
                    }
                });
            });
        });
    }
    
    private void displayMountInfo(Inventory inventory, MountData mountData) {
        // Parse mount attributes
        Map<String, Object> attributes = parseAttributes(mountData.getMountDataYaml());
        
        // Check if mount is active
        boolean isActive = plugin.getMountManager().getPlayerActiveMounts(player.getUniqueId())
            .stream()
            .anyMatch(uuid -> mountName.equals(plugin.getMountManager().getMountName(uuid)));
        
        // Main mount display item
        ItemStack mainItem = GUIManager.createMountItem(
            mountData.getMountName(),
            mountData.getMountType(),
            isActive,
            getDoubleAttribute(attributes, "health", 20.0),
            getDoubleAttribute(attributes, "maxHealth", 20.0),
            getDoubleAttribute(attributes, "speed", 0.2),
            getDoubleAttribute(attributes, "jumpStrength", 0.7)
        );
        inventory.setItem(22, mainItem);
        
        // Detailed stats
        addDetailedStats(inventory, attributes, mountData);
        
        // Action buttons
        addActionButtons(inventory, mountData, isActive);
        
        // Navigation
        addNavigationButtons(inventory);
    }
    
    private void addDetailedStats(Inventory inventory, Map<String, Object> attributes, MountData mountData) {
        // Health stat
        ItemStack healthItem = GUIManager.createNavigationItem(
            Material.RED_DYE,
            "Health",
            "Current: " + String.format("%.1f", getDoubleAttribute(attributes, "health", 20.0)),
            "Maximum: " + String.format("%.1f", getDoubleAttribute(attributes, "maxHealth", 20.0)),
            "Percentage: " + String.format("%.1f", (getDoubleAttribute(attributes, "health", 20.0) / getDoubleAttribute(attributes, "maxHealth", 20.0)) * 100) + "%"
        );
        inventory.setItem(12, healthItem);
        
        // Speed stat
        double speed = getDoubleAttribute(attributes, "speed", 0.2);
        ItemStack speedItem = GUIManager.createNavigationItem(
            Material.LIGHT_BLUE_DYE,
            "Speed",
            "Raw Speed: " + String.format("%.3f", speed),
            "Blocks/Second: " + String.format("%.1f", speed * 43.17),
            "Percentage: " + String.format("%.1f", (speed / 0.3375) * 100) + "%"
        );
        inventory.setItem(14, speedItem);
        
        // Jump stat
        double jump = getDoubleAttribute(attributes, "jumpStrength", 0.7);
        ItemStack jumpItem = GUIManager.createNavigationItem(
            Material.LIME_DYE,
            "Jump Strength",
            "Raw Jump: " + String.format("%.3f", jump),
            "Jump Height: " + String.format("%.1f", getJumpHeight(jump)) + " blocks",
            "Percentage: " + String.format("%.1f", (jump / 1.0) * 100) + "%"
        );
        inventory.setItem(30, jumpItem);
        
        // Mount info
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        ItemStack infoItem = GUIManager.createNavigationItem(
            Material.PAPER,
            "Mount Information",
            "Type: " + mountData.getMountType(),
            "Created: " + dateFormat.format(new Date(mountData.getCreatedAt())),
            "Last Used: " + dateFormat.format(new Date(mountData.getLastAccessed())),
            "Has Chest: " + (mountData.hasChestInventory() ? "Yes" : "No")
        );
        inventory.setItem(32, infoItem);
        
        // Equipment info
        if (hasEquipment(attributes)) {
            ItemStack equipmentItem = GUIManager.createNavigationItem(
                Material.DIAMOND_HORSE_ARMOR,
                "Equipment",
                "Saddle: " + (getBooleanAttribute(attributes, "saddled", false) ? "Yes" : "No"),
                "Armor: " + getStringAttribute(attributes, "armor", "None"),
                "Chest: " + (getBooleanAttribute(attributes, "carryingChest", false) ? "Yes" : "No")
            );
            inventory.setItem(20, equipmentItem);
        }
    }
    
    private void addActionButtons(Inventory inventory, MountData mountData, boolean isActive) {
        // Summon/Store button
        if (isActive) {
            ItemStack storeButton = GUIManager.createActionItem(
                Material.ENDER_CHEST,
                "Store Mount",
                "store_mount",
                "Click to store this mount",
                "The mount will be safely stored",
                "and removed from the world"
            );
            inventory.setItem(48, storeButton);
        } else {
            ItemStack summonButton = GUIManager.createActionItem(
                Material.GRASS_BLOCK,
                "Summon Mount",
                "summon_mount",
                "Click to summon this mount",
                "The mount will appear near you",
                "ready to be ridden"
            );
            inventory.setItem(48, summonButton);
        }
        
        // Rename button
        ItemStack renameButton = GUIManager.createActionItem(
            Material.NAME_TAG,
            "Rename Mount",
            "rename_mount",
            "Click to rename this mount",
            "You'll need to type the new name",
            "in chat after clicking"
        );
        inventory.setItem(50, renameButton);
        
        // Release button
        ItemStack releaseButton = GUIManager.createActionItem(
            Material.LAVA_BUCKET,
            "Release Mount",
            "release_mount",
            "Click to permanently release this mount",
            ChatColor.RED + "WARNING: This cannot be undone!",
            "The mount will be deleted forever"
        );
        inventory.setItem(52, releaseButton);
    }
    
    private void addNavigationButtons(Inventory inventory) {
        // Back button
        ItemStack backButton = GUIManager.createNavigationItem(
            Material.ARROW,
            "Back to Mount List",
            "Return to the main mount manager"
        );
        inventory.setItem(45, backButton);
        
        // Close button
        ItemStack closeButton = GUIManager.createNavigationItem(
            Material.BARRIER,
            "Close",
            "Close the mount manager"
        );
        inventory.setItem(49, closeButton);
        
        // Refresh button
        ItemStack refreshButton = GUIManager.createNavigationItem(
            Material.LIME_DYE,
            "Refresh",
            "Refresh the mount information"
        );
        inventory.setItem(53, refreshButton);
    }
    
    private void displayNotFound(Inventory inventory) {
        ItemStack notFoundItem = GUIManager.createNavigationItem(
            Material.BARRIER,
            "Mount Not Found",
            "The mount '" + mountName + "' could not be found.",
            "It may have been released or renamed.",
            "Click to return to the mount list."
        );
        inventory.setItem(22, notFoundItem);
        
        // Back button
        ItemStack backButton = GUIManager.createNavigationItem(
            Material.ARROW,
            "Back to Mount List",
            "Return to the main mount manager"
        );
        inventory.setItem(49, backButton);
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
    
    private boolean getBooleanAttribute(Map<String, Object> attributes, String key, boolean defaultValue) {
        Object value = attributes.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    private String getStringAttribute(Map<String, Object> attributes, String key, String defaultValue) {
        Object value = attributes.get(key);
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }
    
    private boolean hasEquipment(Map<String, Object> attributes) {
        return attributes.containsKey("saddled") || 
               attributes.containsKey("armor") || 
               attributes.containsKey("carryingChest");
    }
    
    private double getJumpHeight(double jumpStrength) {
        if (jumpStrength <= 0.4) return 1.11;
        if (jumpStrength <= 0.5) return 1.62;
        if (jumpStrength <= 0.6) return 2.22;
        if (jumpStrength <= 0.7) return 2.89;
        if (jumpStrength <= 0.8) return 3.63;
        if (jumpStrength <= 0.9) return 4.44;
        if (jumpStrength <= 1.0) return 5.30;
        return 6.0;
    }
    
    public void refresh() {
        MountInfoGUI refreshedGUI = new MountInfoGUI(plugin, player, mountName);
        refreshedGUI.open();
    }
}