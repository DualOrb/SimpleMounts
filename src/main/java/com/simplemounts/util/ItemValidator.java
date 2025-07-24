package com.simplemounts.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class ItemValidator {
    
    public static boolean isValidItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        if (item.getType() == Material.AIR) {
            return false;
        }
        
        if (item.getAmount() <= 0) {
            return false;
        }
        
        if (item.getAmount() > item.getType().getMaxStackSize()) {
            return false;
        }
        
        return true;
    }
    
    public static boolean matchesExactly(ItemStack item, ItemStack template) {
        if (item == null && template == null) {
            return true;
        }
        
        if (item == null || template == null) {
            return false;
        }
        
        if (item.getType() != template.getType()) {
            return false;
        }
        
        if (item.getAmount() != template.getAmount()) {
            return false;
        }
        
        if (item.getDurability() != template.getDurability()) {
            return false;
        }
        
        ItemMeta itemMeta = item.getItemMeta();
        ItemMeta templateMeta = template.getItemMeta();
        
        if (itemMeta == null && templateMeta == null) {
            return true;
        }
        
        if (itemMeta == null || templateMeta == null) {
            return false;
        }
        
        return itemMeta.equals(templateMeta);
    }
    
    public static boolean matchesTemplate(ItemStack item, Material material, String name, 
                                        List<String> lore, Integer customModelData, 
                                        Map<Enchantment, Integer> enchantments) {
        if (item == null || item.getType() != material) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Check custom model data
        if (customModelData != null) {
            if (meta == null || !meta.hasCustomModelData() || 
                meta.getCustomModelData() != customModelData) {
                return false;
            }
        }
        
        // Check display name
        if (name != null && !name.isEmpty()) {
            String expectedName = ChatColor.translateAlternateColorCodes('&', name);
            if (meta == null || !meta.hasDisplayName() || 
                !meta.getDisplayName().equals(expectedName)) {
                return false;
            }
        }
        
        // Check lore
        if (lore != null && !lore.isEmpty()) {
            if (meta == null || !meta.hasLore()) {
                return false;
            }
            
            List<String> itemLore = meta.getLore();
            if (itemLore.size() != lore.size()) {
                return false;
            }
            
            for (int i = 0; i < lore.size(); i++) {
                String expectedLine = ChatColor.translateAlternateColorCodes('&', lore.get(i));
                if (!itemLore.get(i).equals(expectedLine)) {
                    return false;
                }
            }
        }
        
        // Check enchantments
        if (enchantments != null && !enchantments.isEmpty()) {
            Map<Enchantment, Integer> itemEnchants = item.getEnchantments();
            
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                if (!itemEnchants.containsKey(entry.getKey()) || 
                    !itemEnchants.get(entry.getKey()).equals(entry.getValue())) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public static boolean hasValidMeta(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null;
    }
    
    public static boolean hasDisplayName(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName();
    }
    
    public static boolean hasLore(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasLore();
    }
    
    public static boolean hasEnchantments(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        return item.hasItemMeta() && item.getItemMeta().hasEnchants();
    }
    
    public static boolean hasCustomModelData(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasCustomModelData();
    }
    
    public static boolean isUnbreakable(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.isUnbreakable();
    }
    
    public static boolean isDamageable(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        return item.getType().getMaxDurability() > 0;
    }
    
    public static boolean isDamaged(ItemStack item) {
        if (item == null || !isDamageable(item)) {
            return false;
        }
        
        return item.getDurability() > 0;
    }
    
    public static boolean isRepairable(ItemStack item) {
        return isDamageable(item) && isDamaged(item);
    }
    
    public static boolean isStackable(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        return item.getType().getMaxStackSize() > 1;
    }
    
    public static boolean canStack(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }
        
        if (!isStackable(item1) || !isStackable(item2)) {
            return false;
        }
        
        if (item1.getType() != item2.getType()) {
            return false;
        }
        
        if (item1.getDurability() != item2.getDurability()) {
            return false;
        }
        
        ItemMeta meta1 = item1.getItemMeta();
        ItemMeta meta2 = item2.getItemMeta();
        
        if (meta1 == null && meta2 == null) {
            return true;
        }
        
        if (meta1 == null || meta2 == null) {
            return false;
        }
        
        return meta1.equals(meta2);
    }
    
    public static ItemStack sanitizeItem(ItemStack item) {
        if (item == null) {
            return null;
        }
        
        ItemStack sanitized = item.clone();
        
        // Fix invalid amounts
        if (sanitized.getAmount() <= 0) {
            return null;
        }
        
        if (sanitized.getAmount() > sanitized.getType().getMaxStackSize()) {
            sanitized.setAmount(sanitized.getType().getMaxStackSize());
        }
        
        // Fix invalid durability
        if (isDamageable(sanitized)) {
            short maxDurability = sanitized.getType().getMaxDurability();
            if (sanitized.getDurability() > maxDurability) {
                sanitized.setDurability(maxDurability);
            }
        }
        
        return sanitized;
    }
    
    public static String getItemDescription(ItemStack item) {
        if (item == null) {
            return "null";
        }
        
        StringBuilder description = new StringBuilder();
        description.append(item.getType().name());
        
        if (item.getAmount() > 1) {
            description.append(" x").append(item.getAmount());
        }
        
        if (isDamaged(item)) {
            description.append(" (damaged)");
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                description.append(" [").append(meta.getDisplayName()).append("]");
            }
            
            if (meta.hasEnchants()) {
                description.append(" (enchanted)");
            }
            
            if (meta.hasCustomModelData()) {
                description.append(" (custom model: ").append(meta.getCustomModelData()).append(")");
            }
        }
        
        return description.toString();
    }
    
    public static boolean isValidForSerialization(ItemStack item) {
        if (!isValidItem(item)) {
            return false;
        }
        
        // Check for potentially problematic items
        if (item.getType().name().contains("SPAWN_EGG")) {
            return false;
        }
        
        // Check for items with excessive data
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore.size() > 20) { // Arbitrary limit
                return false;
            }
            
            for (String line : lore) {
                if (line.length() > 100) { // Arbitrary limit
                    return false;
                }
            }
        }
        
        return true;
    }
}