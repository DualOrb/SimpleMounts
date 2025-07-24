package com.simplemounts.util;

import com.simplemounts.SimpleMounts;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CustomItemDetector {
    
    private final SimpleMounts plugin;
    private final Map<String, Function<ItemStack, Boolean>> detectors;
    private final Map<String, Function<ItemStack, String>> infoExtractors;
    
    public CustomItemDetector(SimpleMounts plugin) {
        this.plugin = plugin;
        this.detectors = new HashMap<>();
        this.infoExtractors = new HashMap<>();
        
        registerDefaultDetectors();
    }
    
    private void registerDefaultDetectors() {
        // MMOItems
        detectors.put("MMOItems", this::isMMOItem);
        infoExtractors.put("MMOItems", this::getMMOItemInfo);
        
        // ItemsAdder
        detectors.put("ItemsAdder", this::isItemsAdderItem);
        infoExtractors.put("ItemsAdder", this::getItemsAdderInfo);
        
        // Oraxen
        detectors.put("Oraxen", this::isOraxenItem);
        infoExtractors.put("Oraxen", this::getOraxenInfo);
        
        // EcoItems
        detectors.put("EcoItems", this::isEcoItem);
        infoExtractors.put("EcoItems", this::getEcoItemInfo);
        
        // CustomItems
        detectors.put("CustomItems", this::isCustomItem);
        infoExtractors.put("CustomItems", this::getCustomItemInfo);
        
        // MythicMobs
        detectors.put("MythicMobs", this::isMythicMobItem);
        infoExtractors.put("MythicMobs", this::getMythicMobItemInfo);
        
        // ExcellentCrates
        detectors.put("ExcellentCrates", this::isExcellentCratesItem);
        infoExtractors.put("ExcellentCrates", this::getExcellentCratesItemInfo);
        
        // SlimeFun
        detectors.put("SlimeFun", this::isSlimeFunItem);
        infoExtractors.put("SlimeFun", this::getSlimeFunItemInfo);
    }
    
    public boolean isCustomItemFromPlugin(ItemStack item, String pluginName) {
        Function<ItemStack, Boolean> detector = detectors.get(pluginName);
        return detector != null && detector.apply(item);
    }
    
    public String getCustomItemInfo(ItemStack item, String pluginName) {
        Function<ItemStack, String> extractor = infoExtractors.get(pluginName);
        return extractor != null ? extractor.apply(item) : "Unknown";
    }
    
    public String detectCustomItemPlugin(ItemStack item) {
        List<String> supportedPlugins = plugin.getConfigManager().getSupportedCustomItemPlugins();
        
        for (String pluginName : supportedPlugins) {
            if (isCustomItemFromPlugin(item, pluginName)) {
                return pluginName;
            }
        }
        
        return null;
    }
    
    public boolean isAnyCustomItem(ItemStack item) {
        return detectCustomItemPlugin(item) != null;
    }
    
    // MMOItems detection
    private boolean isMMOItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check for MMOItems namespace keys
        if (meta.getPersistentDataContainer().has(new NamespacedKey("mmoitems", "type"), PersistentDataType.STRING) ||
            meta.getPersistentDataContainer().has(new NamespacedKey("mmoitems", "id"), PersistentDataType.STRING)) {
            return true;
        }
        
        // Check for MMOItems lore patterns
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (String line : lore) {
                if (line.contains("§8[MMOItems]") || line.contains("§8Item ID:")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private String getMMOItemInfo(ItemStack item) {
        if (!item.hasItemMeta()) return "Unknown MMOItem";
        
        ItemMeta meta = item.getItemMeta();
        String type = meta.getPersistentDataContainer().get(new NamespacedKey("mmoitems", "type"), PersistentDataType.STRING);
        String id = meta.getPersistentDataContainer().get(new NamespacedKey("mmoitems", "id"), PersistentDataType.STRING);
        
        return "MMOItems: " + (type != null ? type : "Unknown") + ":" + (id != null ? id : "Unknown");
    }
    
    // ItemsAdder detection
    private boolean isItemsAdderItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check for ItemsAdder namespace keys
        if (meta.getPersistentDataContainer().has(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING)) {
            return true;
        }
        
        // Check for ItemsAdder custom model data pattern
        if (meta.hasCustomModelData()) {
            int customModelData = meta.getCustomModelData();
            // ItemsAdder typically uses specific ranges
            if (customModelData >= 1000000) {
                return true;
            }
        }
        
        return false;
    }
    
    private String getItemsAdderInfo(ItemStack item) {
        if (!item.hasItemMeta()) return "Unknown ItemsAdder Item";
        
        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING);
        
        return "ItemsAdder: " + (id != null ? id : "Unknown ID");
    }
    
    // Oraxen detection
    private boolean isOraxenItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check for Oraxen namespace keys
        if (meta.getPersistentDataContainer().has(new NamespacedKey("oraxen", "id"), PersistentDataType.STRING)) {
            return true;
        }
        
        return false;
    }
    
    private String getOraxenInfo(ItemStack item) {
        if (!item.hasItemMeta()) return "Unknown Oraxen Item";
        
        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(new NamespacedKey("oraxen", "id"), PersistentDataType.STRING);
        
        return "Oraxen: " + (id != null ? id : "Unknown ID");
    }
    
    // EcoItems detection
    private boolean isEcoItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check for EcoItems namespace keys
        if (meta.getPersistentDataContainer().has(new NamespacedKey("ecoitems", "id"), PersistentDataType.STRING)) {
            return true;
        }
        
        return false;
    }
    
    private String getEcoItemInfo(ItemStack item) {
        if (!item.hasItemMeta()) return "Unknown EcoItem";
        
        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(new NamespacedKey("ecoitems", "id"), PersistentDataType.STRING);
        
        return "EcoItems: " + (id != null ? id : "Unknown ID");
    }
    
    // CustomItems detection
    private boolean isCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check for CustomItems namespace keys
        if (meta.getPersistentDataContainer().has(new NamespacedKey("customitems", "id"), PersistentDataType.STRING)) {
            return true;
        }
        
        return false;
    }
    
    private String getCustomItemInfo(ItemStack item) {
        if (!item.hasItemMeta()) return "Unknown CustomItem";
        
        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(new NamespacedKey("customitems", "id"), PersistentDataType.STRING);
        
        return "CustomItems: " + (id != null ? id : "Unknown ID");
    }
    
    // MythicMobs detection
    private boolean isMythicMobItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check for MythicMobs namespace keys
        if (meta.getPersistentDataContainer().has(new NamespacedKey("mythicmobs", "type"), PersistentDataType.STRING)) {
            return true;
        }
        
        // Check for MythicMobs lore patterns
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (String line : lore) {
                if (line.contains("§8[MythicMobs]")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private String getMythicMobItemInfo(ItemStack item) {
        if (!item.hasItemMeta()) return "Unknown MythicMobs Item";
        
        ItemMeta meta = item.getItemMeta();
        String type = meta.getPersistentDataContainer().get(new NamespacedKey("mythicmobs", "type"), PersistentDataType.STRING);
        
        return "MythicMobs: " + (type != null ? type : "Unknown Type");
    }
    
    // ExcellentCrates detection
    private boolean isExcellentCratesItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check for ExcellentCrates namespace keys
        if (meta.getPersistentDataContainer().has(new NamespacedKey("excellentcrates", "id"), PersistentDataType.STRING)) {
            return true;
        }
        
        return false;
    }
    
    private String getExcellentCratesItemInfo(ItemStack item) {
        if (!item.hasItemMeta()) return "Unknown ExcellentCrates Item";
        
        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(new NamespacedKey("excellentcrates", "id"), PersistentDataType.STRING);
        
        return "ExcellentCrates: " + (id != null ? id : "Unknown ID");
    }
    
    // SlimeFun detection
    private boolean isSlimeFunItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check for SlimeFun namespace keys
        if (meta.getPersistentDataContainer().has(new NamespacedKey("slimefun", "id"), PersistentDataType.STRING)) {
            return true;
        }
        
        return false;
    }
    
    private String getSlimeFunItemInfo(ItemStack item) {
        if (!item.hasItemMeta()) return "Unknown SlimeFun Item";
        
        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(new NamespacedKey("slimefun", "id"), PersistentDataType.STRING);
        
        return "SlimeFun: " + (id != null ? id : "Unknown ID");
    }
    
    // Register custom detector
    public void registerCustomDetector(String pluginName, Function<ItemStack, Boolean> detector, Function<ItemStack, String> infoExtractor) {
        detectors.put(pluginName, detector);
        infoExtractors.put(pluginName, infoExtractor);
    }
    
    // Get all supported plugins
    public Map<String, Function<ItemStack, Boolean>> getAllDetectors() {
        return new HashMap<>(detectors);
    }
    
    // Check if a plugin detector is registered
    public boolean hasDetectorForPlugin(String pluginName) {
        return detectors.containsKey(pluginName);
    }
}