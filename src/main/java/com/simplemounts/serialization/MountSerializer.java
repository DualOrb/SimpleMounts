package com.simplemounts.serialization;

import com.simplemounts.SimpleMounts;
import com.simplemounts.data.ChestInventoryData;
import com.simplemounts.data.MountAttributes;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class MountSerializer {
    
    private final SimpleMounts plugin;
    private final Yaml yaml;
    private final InventorySerializer inventorySerializer;
    
    public MountSerializer(SimpleMounts plugin) {
        this.plugin = plugin;
        this.inventorySerializer = new InventorySerializer(plugin);
        
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        this.yaml = new Yaml(options);
    }
    
    public String serializeAttributes(MountAttributes attributes) {
        try {
            Map<String, Object> attributeMap = attributes.getAttributes();
            String yamlString = yaml.dump(attributeMap);
            
            if (plugin.getConfigManager().compressInventoryData()) {
                return compressString(yamlString);
            } else {
                return yamlString;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error serializing mount attributes", e);
            return yaml.dump(new HashMap<>());
        }
    }
    
    public MountAttributes deserializeAttributes(String serializedData) {
        try {
            if (serializedData == null || serializedData.isEmpty()) {
                return new MountAttributes();
            }
            
            String yamlString = serializedData;
            if (plugin.getConfigManager().compressInventoryData() && isCompressed(serializedData)) {
                yamlString = decompressString(serializedData);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> attributeMap = yaml.load(yamlString);
            
            if (attributeMap == null) {
                return new MountAttributes();
            }
            
            return new MountAttributes(attributeMap);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error deserializing mount attributes", e);
            return new MountAttributes();
        }
    }
    
    public String serializeChestInventory(Inventory inventory) {
        try {
            if (inventory == null) {
                return null;
            }
            
            ChestInventoryData chestData = ChestInventoryData.fromInventory(inventory);
            
            if (chestData.isEmpty()) {
                return null;
            }
            
            Map<String, Object> inventoryMap = new HashMap<>();
            inventoryMap.put("size", chestData.getSize());
            inventoryMap.put("items", new HashMap<>());
            inventoryMap.put("custom_items", new HashMap<>());
            
            @SuppressWarnings("unchecked")
            Map<String, Object> itemsMap = (Map<String, Object>) inventoryMap.get("items");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> customItemsMap = (Map<String, Object>) inventoryMap.get("custom_items");
            
            for (Map.Entry<Integer, ItemStack> entry : chestData.getItems().entrySet()) {
                int slot = entry.getKey();
                ItemStack item = entry.getValue();
                
                if (item != null) {
                    String serializedItem = inventorySerializer.serializeItemStack(item);
                    if (serializedItem != null) {
                        itemsMap.put(String.valueOf(slot), serializedItem);
                        
                        // Track custom items for additional validation
                        if (inventorySerializer.isCustomItem(item)) {
                            customItemsMap.put(String.valueOf(slot), inventorySerializer.getCustomItemInfo(item));
                        }
                    }
                }
            }
            
            String yamlString = yaml.dump(inventoryMap);
            
            if (plugin.getConfigManager().compressInventoryData()) {
                return compressString(yamlString);
            } else {
                return yamlString;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error serializing chest inventory", e);
            return null;
        }
    }
    
    public void deserializeChestInventory(String serializedData, Inventory inventory) {
        try {
            if (serializedData == null || serializedData.isEmpty()) {
                return;
            }
            
            String yamlString = serializedData;
            if (plugin.getConfigManager().compressInventoryData() && isCompressed(serializedData)) {
                yamlString = decompressString(serializedData);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> inventoryMap = yaml.load(yamlString);
            
            if (inventoryMap == null) {
                return;
            }
            
            int expectedSize = (Integer) inventoryMap.getOrDefault("size", inventory.getSize());
            if (expectedSize != inventory.getSize()) {
                plugin.getLogger().warning("Inventory size mismatch: expected " + expectedSize + ", got " + inventory.getSize());
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> itemsMap = (Map<String, Object>) inventoryMap.get("items");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> customItemsMap = (Map<String, Object>) inventoryMap.get("custom_items");
            
            if (itemsMap != null) {
                inventory.clear();
                
                for (Map.Entry<String, Object> entry : itemsMap.entrySet()) {
                    try {
                        int slot = Integer.parseInt(entry.getKey());
                        String serializedItem = (String) entry.getValue();
                        
                        if (slot >= 0 && slot < inventory.getSize()) {
                            ItemStack item = inventorySerializer.deserializeItemStack(serializedItem);
                            
                            if (item != null) {
                                inventory.setItem(slot, item);
                            } else if (plugin.getConfigManager().createPlaceholdersForFailedItems() && 
                                      customItemsMap != null && customItemsMap.containsKey(entry.getKey())) {
                                // Create placeholder for failed custom item deserialization
                                String customItemInfo = (String) customItemsMap.get(entry.getKey());
                                ItemStack placeholder = inventorySerializer.createCustomItemPlaceholder(customItemInfo);
                                inventory.setItem(slot, placeholder);
                                if (plugin.getConfigManager().logCustomItemIssues()) {
                                    plugin.getLogger().warning("Failed to deserialize custom item at slot " + slot + ": " + customItemInfo);
                                }
                            }
                        }
                        
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid slot number in chest inventory: " + entry.getKey());
                    }
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error deserializing chest inventory", e);
        }
    }
    
    public String serializeItemStack(ItemStack item) {
        return inventorySerializer.serializeItemStack(item);
    }
    
    public ItemStack deserializeItemStack(String serializedItem) {
        return inventorySerializer.deserializeItemStack(serializedItem);
    }
    
    private String compressString(String data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(data.getBytes("UTF-8"));
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error compressing data", e);
            return data;
        }
    }
    
    private String decompressString(String compressedData) {
        try {
            byte[] decodedData = Base64.getDecoder().decode(compressedData);
            ByteArrayInputStream bais = new ByteArrayInputStream(decodedData);
            try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                return baos.toString("UTF-8");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error decompressing data", e);
            return compressedData;
        }
    }
    
    private boolean isCompressed(String data) {
        try {
            Base64.getDecoder().decode(data);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public boolean validateSerializedData(String serializedData) {
        try {
            if (serializedData == null || serializedData.isEmpty()) {
                return false;
            }
            
            String yamlString = serializedData;
            if (plugin.getConfigManager().compressInventoryData() && isCompressed(serializedData)) {
                yamlString = decompressString(serializedData);
            }
            
            Object parsed = yaml.load(yamlString);
            return parsed != null;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    public Map<String, Object> parseSerializedData(String serializedData) {
        try {
            if (serializedData == null || serializedData.isEmpty()) {
                return new HashMap<>();
            }
            
            String yamlString = serializedData;
            if (plugin.getConfigManager().compressInventoryData() && isCompressed(serializedData)) {
                yamlString = decompressString(serializedData);
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> result = yaml.load(yamlString);
            
            return result != null ? result : new HashMap<>();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing serialized data", e);
            return new HashMap<>();
        }
    }
    
    public String serializeMap(Map<String, Object> map) {
        try {
            String yamlString = yaml.dump(map);
            
            if (plugin.getConfigManager().compressInventoryData()) {
                return compressString(yamlString);
            } else {
                return yamlString;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error serializing map", e);
            return yaml.dump(new HashMap<>());
        }
    }
    
    public long getSerializedDataSize(String serializedData) {
        if (serializedData == null) {
            return 0;
        }
        
        try {
            if (plugin.getConfigManager().compressInventoryData() && isCompressed(serializedData)) {
                String decompressed = decompressString(serializedData);
                return decompressed.getBytes("UTF-8").length;
            } else {
                return serializedData.getBytes("UTF-8").length;
            }
        } catch (Exception e) {
            return serializedData.length();
        }
    }
    
    public String getCompressionInfo(String serializedData) {
        if (serializedData == null) {
            return "No data";
        }
        
        try {
            if (plugin.getConfigManager().compressInventoryData() && isCompressed(serializedData)) {
                String decompressed = decompressString(serializedData);
                long originalSize = decompressed.getBytes("UTF-8").length;
                long compressedSize = serializedData.getBytes("UTF-8").length;
                double ratio = (double) compressedSize / originalSize * 100;
                return String.format("Compressed: %d -> %d bytes (%.1f%%)", originalSize, compressedSize, ratio);
            } else {
                long size = serializedData.getBytes("UTF-8").length;
                return String.format("Uncompressed: %d bytes", size);
            }
        } catch (Exception e) {
            return "Error calculating compression info";
        }
    }
}