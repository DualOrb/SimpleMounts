package com.simplemounts.serialization;

import com.simplemounts.SimpleMounts;
import com.simplemounts.util.CustomItemDetector;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public class InventorySerializer {
    
    private final SimpleMounts plugin;
    private final CustomItemDetector customItemDetector;
    
    public InventorySerializer(SimpleMounts plugin) {
        this.plugin = plugin;
        this.customItemDetector = new CustomItemDetector(plugin);
    }
    
    public String serializeItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        
        try {
            // Always use NBT serialization for custom items if configured
            if (plugin.getConfigManager().includeNbtData() || 
                (plugin.getConfigManager().forceNbtForCustomItems() && isCustomItem(item))) {
                return serializeItemStackWithNBT(item);
            } else {
                return serializeItemStackBasic(item);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error serializing ItemStack", e);
            
            // Fallback to basic serialization if NBT fails
            try {
                return serializeItemStackBasic(item);
            } catch (Exception fallbackError) {
                plugin.getLogger().log(Level.SEVERE, "Fallback serialization also failed", fallbackError);
                return null;
            }
        }
    }
    
    public ItemStack deserializeItemStack(String serializedItem) {
        if (serializedItem == null || serializedItem.isEmpty()) {
            return null;
        }
        
        try {
            // Try NBT deserialization first (more likely to preserve custom items)
            if (plugin.getConfigManager().includeNbtData() || isNBTSerialized(serializedItem)) {
                return deserializeItemStackWithNBT(serializedItem);
            } else {
                return deserializeItemStackBasic(serializedItem);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error deserializing ItemStack", e);
            
            // Try fallback deserialization
            try {
                if (isNBTSerialized(serializedItem)) {
                    return deserializeItemStackBasic(serializedItem);
                } else {
                    return deserializeItemStackWithNBT(serializedItem);
                }
            } catch (Exception fallbackError) {
                plugin.getLogger().log(Level.SEVERE, "Fallback deserialization also failed", fallbackError);
                return null;
            }
        }
    }
    
    private String serializeItemStackWithNBT(ItemStack item) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos)) {
                boos.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error serializing ItemStack with NBT, falling back to basic", e);
            return serializeItemStackBasic(item);
        }
    }
    
    private ItemStack deserializeItemStackWithNBT(String serializedItem) {
        try {
            byte[] data = Base64.getDecoder().decode(serializedItem);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            try (BukkitObjectInputStream bois = new BukkitObjectInputStream(bais)) {
                return (ItemStack) bois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.WARNING, "Error deserializing ItemStack with NBT, falling back to basic", e);
            return deserializeItemStackBasic(serializedItem);
        }
    }
    
    private String serializeItemStackBasic(ItemStack item) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("item", item);
        return config.saveToString();
    }
    
    private ItemStack deserializeItemStackBasic(String serializedItem) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(serializedItem);
            return config.getItemStack("item");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error deserializing basic ItemStack", e);
            return null;
        }
    }
    
    public String serializeInventoryArray(ItemStack[] items) {
        try {
            Map<String, Object> inventoryMap = new HashMap<>();
            inventoryMap.put("size", items.length);
            
            Map<String, String> itemsMap = new HashMap<>();
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null && items[i].getType() != Material.AIR) {
                    String serializedItem = serializeItemStack(items[i]);
                    if (serializedItem != null) {
                        itemsMap.put(String.valueOf(i), serializedItem);
                    }
                }
            }
            
            inventoryMap.put("items", itemsMap);
            
            YamlConfiguration config = new YamlConfiguration();
            config.set("inventory", inventoryMap);
            return config.saveToString();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error serializing inventory array", e);
            return null;
        }
    }
    
    public ItemStack[] deserializeInventoryArray(String serializedInventory) {
        try {
            if (serializedInventory == null || serializedInventory.isEmpty()) {
                return new ItemStack[0];
            }
            
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(serializedInventory);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> inventoryMap = (Map<String, Object>) config.get("inventory");
            
            if (inventoryMap == null) {
                return new ItemStack[0];
            }
            
            int size = (Integer) inventoryMap.getOrDefault("size", 0);
            ItemStack[] items = new ItemStack[size];
            
            @SuppressWarnings("unchecked")
            Map<String, String> itemsMap = (Map<String, String>) inventoryMap.get("items");
            
            if (itemsMap != null) {
                for (Map.Entry<String, String> entry : itemsMap.entrySet()) {
                    try {
                        int slot = Integer.parseInt(entry.getKey());
                        if (slot >= 0 && slot < size) {
                            ItemStack item = deserializeItemStack(entry.getValue());
                            if (item != null) {
                                items[slot] = item;
                            }
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid slot number in inventory: " + entry.getKey());
                    }
                }
            }
            
            return items;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error deserializing inventory array", e);
            return new ItemStack[0];
        }
    }
    
    public Map<String, Object> analyzeItemStack(ItemStack item) {
        Map<String, Object> analysis = new HashMap<>();
        
        if (item == null) {
            analysis.put("type", "null");
            return analysis;
        }
        
        analysis.put("type", item.getType().name());
        analysis.put("amount", item.getAmount());
        analysis.put("durability", item.getDurability());
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            analysis.put("hasDisplayName", meta.hasDisplayName());
            if (meta.hasDisplayName()) {
                analysis.put("displayName", meta.getDisplayName());
            }
            
            analysis.put("hasLore", meta.hasLore());
            if (meta.hasLore()) {
                analysis.put("loreLines", meta.getLore().size());
            }
            
            analysis.put("hasEnchants", meta.hasEnchants());
            if (meta.hasEnchants()) {
                analysis.put("enchantCount", meta.getEnchants().size());
            }
            
            analysis.put("hasCustomModelData", meta.hasCustomModelData());
            if (meta.hasCustomModelData()) {
                analysis.put("customModelData", meta.getCustomModelData());
            }
            
            analysis.put("isUnbreakable", meta.isUnbreakable());
        }
        
        return analysis;
    }
    
    public boolean validateItemStack(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        if (item.getType() == Material.AIR) {
            return true;
        }
        
        if (item.getAmount() <= 0) {
            return false;
        }
        
        if (item.getAmount() > item.getType().getMaxStackSize()) {
            plugin.getLogger().warning("ItemStack amount exceeds max stack size: " + item.getAmount() + " > " + item.getType().getMaxStackSize());
            return false;
        }
        
        return true;
    }
    
    public ItemStack sanitizeItemStack(ItemStack item) {
        if (item == null) {
            return null;
        }
        
        if (item.getType() == Material.AIR) {
            return new ItemStack(Material.AIR);
        }
        
        ItemStack sanitized = item.clone();
        
        if (sanitized.getAmount() <= 0) {
            return new ItemStack(Material.AIR);
        }
        
        if (sanitized.getAmount() > sanitized.getType().getMaxStackSize()) {
            sanitized.setAmount(sanitized.getType().getMaxStackSize());
        }
        
        return sanitized;
    }
    
    public long getItemStackSize(ItemStack item) {
        if (item == null) {
            return 0;
        }
        
        String serialized = serializeItemStack(item);
        if (serialized == null) {
            return 0;
        }
        
        try {
            return serialized.getBytes("UTF-8").length;
        } catch (Exception e) {
            return serialized.length();
        }
    }
    
    public String getItemStackInfo(ItemStack item) {
        if (item == null) {
            return "null";
        }
        
        StringBuilder info = new StringBuilder();
        info.append(item.getType().name());
        
        if (item.getAmount() > 1) {
            info.append(" x").append(item.getAmount());
        }
        
        if (item.getDurability() > 0) {
            info.append(" (").append(item.getDurability()).append(" damage)");
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                info.append(" [").append(meta.getDisplayName()).append("]");
            }
            
            if (meta.hasEnchants()) {
                info.append(" (").append(meta.getEnchants().size()).append(" enchants)");
            }
        }
        
        return info.toString();
    }
    
    public boolean isItemStackEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }
    
    public ItemStack[] filterEmptyItems(ItemStack[] items) {
        if (items == null) {
            return new ItemStack[0];
        }
        
        ItemStack[] filtered = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            if (!isItemStackEmpty(items[i])) {
                filtered[i] = items[i];
            }
        }
        
        return filtered;
    }
    
    public int countNonEmptyItems(ItemStack[] items) {
        if (items == null) {
            return 0;
        }
        
        int count = 0;
        for (ItemStack item : items) {
            if (!isItemStackEmpty(item)) {
                count++;
            }
        }
        
        return count;
    }
    
    public boolean hasNestedInventories(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        return item.getType().name().contains("SHULKER_BOX") || 
               item.getType().name().contains("BUNDLE");
    }
    
    public String serializeNestedInventory(ItemStack item) {
        if (!hasNestedInventories(item) || !plugin.getConfigManager().handleNestedInventories()) {
            return null;
        }
        
        try {
            return serializeItemStackWithNBT(item);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error serializing nested inventory", e);
            return null;
        }
    }
    
    // Custom item detection methods
    
    public boolean isCustomItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        return customItemDetector.isAnyCustomItem(item) ||
               hasCustomPersistentData(item) ||
               hasCustomNBT(item);
    }
    
    public boolean isMMOItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check for MMOItems namespace keys
        if (meta.getPersistentDataContainer().has(new NamespacedKey("mmoitems", "type"), PersistentDataType.STRING)) {
            return true;
        }
        
        if (meta.getPersistentDataContainer().has(new NamespacedKey("mmoitems", "id"), PersistentDataType.STRING)) {
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
    
    public boolean isItemsAdderItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check for ItemsAdder namespace keys
        if (meta.getPersistentDataContainer().has(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING)) {
            return true;
        }
        
        // Check for custom model data (ItemsAdder commonly uses this)
        if (meta.hasCustomModelData()) {
            return true;
        }
        
        return false;
    }
    
    public boolean isOraxenItem(ItemStack item) {
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
    
    public boolean isEcoItem(ItemStack item) {
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
    
    public boolean hasCustomPersistentData(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        
        // Check for any non-vanilla persistent data
        for (NamespacedKey key : container.getKeys()) {
            if (!key.getNamespace().equals("minecraft")) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean hasCustomNBT(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        // Check for signs of custom NBT by looking at the item's string representation
        String itemString = item.toString();
        
        // Common custom NBT indicators
        return itemString.contains("CUSTOM_") || 
               itemString.contains("CustomModelData") ||
               itemString.contains("display:{") ||
               itemString.contains("PublicBukkitValues");
    }
    
    public boolean isNBTSerialized(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return false;
        }
        
        // Check if it's Base64 encoded (NBT serialization)
        try {
            Base64.getDecoder().decode(serialized);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public String getCustomItemInfo(ItemStack item) {
        if (item == null || !isCustomItem(item)) {
            return "Not a custom item";
        }
        
        // Try to detect using the custom item detector first
        String detectedPlugin = customItemDetector.detectCustomItemPlugin(item);
        if (detectedPlugin != null) {
            return customItemDetector.getCustomItemInfo(item, detectedPlugin);
        }
        
        // Fallback to manual detection
        if (hasCustomPersistentData(item)) {
            return "Custom persistent data detected";
        } else if (hasCustomNBT(item)) {
            return "Custom NBT detected";
        }
        
        return "Unknown custom item";
    }
    
    public ItemStack createCustomItemPlaceholder(String customItemInfo) {
        // Create a placeholder item for custom items that fail to deserialize
        ItemStack placeholder = new ItemStack(Material.BARRIER);
        ItemMeta meta = placeholder.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§c§lCustom Item (Failed to Load)");
            meta.setLore(Arrays.asList(
                "§7This custom item could not be loaded.",
                "§7Plugin may be missing or disabled.",
                "§7Original info: §e" + customItemInfo
            ));
            placeholder.setItemMeta(meta);
        }
        
        return placeholder;
    }
    
    public boolean validateCustomItemSerialization(ItemStack item) {
        if (item == null || !isCustomItem(item)) {
            return true;
        }
        
        if (!plugin.getConfigManager().validateCustomItemsBeforeStoring()) {
            return true; // Skip validation if disabled
        }
        
        try {
            // Test serialization and deserialization
            String serialized = serializeItemStack(item);
            if (serialized == null) {
                if (plugin.getConfigManager().logCustomItemIssues()) {
                    plugin.getLogger().warning("Failed to serialize custom item: " + getCustomItemInfo(item));
                }
                return false;
            }
            
            ItemStack deserialized = deserializeItemStack(serialized);
            if (deserialized == null) {
                if (plugin.getConfigManager().logCustomItemIssues()) {
                    plugin.getLogger().warning("Failed to deserialize custom item: " + getCustomItemInfo(item));
                }
                return false;
            }
            
            // Basic validation - check if item type matches
            if (deserialized.getType() != item.getType()) {
                if (plugin.getConfigManager().logCustomItemIssues()) {
                    plugin.getLogger().warning("Custom item type mismatch after serialization: " + getCustomItemInfo(item));
                }
                return false;
            }
            
            // Check if custom data is preserved
            if (isCustomItem(item) && !isCustomItem(deserialized)) {
                if (plugin.getConfigManager().logCustomItemIssues()) {
                    plugin.getLogger().warning("Custom item data lost during serialization: " + getCustomItemInfo(item));
                }
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            if (plugin.getConfigManager().logCustomItemIssues()) {
                plugin.getLogger().log(Level.WARNING, "Custom item validation failed: " + getCustomItemInfo(item), e);
            }
            return false;
        }
    }
    
    // Convenience methods for accessing custom item detector
    
    public CustomItemDetector getCustomItemDetector() {
        return customItemDetector;
    }
    
    public boolean isSpecificCustomItem(ItemStack item, String pluginName) {
        return customItemDetector.isCustomItemFromPlugin(item, pluginName);
    }
    
    public void registerCustomItemDetector(String pluginName, 
                                         java.util.function.Function<ItemStack, Boolean> detector,
                                         java.util.function.Function<ItemStack, String> infoExtractor) {
        customItemDetector.registerCustomDetector(pluginName, detector, infoExtractor);
    }
    
    public Set<String> getSupportedCustomItemPlugins() {
        return customItemDetector.getAllDetectors().keySet();
    }
}