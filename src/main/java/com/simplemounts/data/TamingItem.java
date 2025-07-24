package com.simplemounts.data;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TamingItem {
    
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final Integer customModelData;
    private final Map<Enchantment, Integer> enchantments;
    private final boolean consumeOnUse;
    
    public TamingItem(Material material, String name, List<String> lore, Integer customModelData, 
                      Map<Enchantment, Integer> enchantments, boolean consumeOnUse) {
        this.material = material;
        this.name = name;
        this.lore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
        this.customModelData = customModelData;
        this.enchantments = enchantments;
        this.consumeOnUse = consumeOnUse;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public String getName() {
        return name;
    }
    
    public List<String> getLore() {
        return new ArrayList<>(lore);
    }
    
    public Integer getCustomModelData() {
        return customModelData;
    }
    
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }
    
    public boolean isConsumeOnUse() {
        return consumeOnUse;
    }
    
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }
            
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            
            if (customModelData != null) {
                meta.setCustomModelData(customModelData);
            }
            
            item.setItemMeta(meta);
        }
        
        if (enchantments != null && !enchantments.isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
        }
        
        return item;
    }
    
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return customModelData == null && (name == null || name.isEmpty()) && lore.isEmpty();
        }
        
        // Debug logging
        System.out.println("DEBUG TamingItem.matches() - Expected: " + this.toString());
        System.out.println("DEBUG TamingItem.matches() - Item: " + item.getType() + 
            " name: " + (meta.hasDisplayName() ? meta.getDisplayName() : "none") +
            " customModelData: " + (meta.hasCustomModelData() ? meta.getCustomModelData() : "none") +
            " lore: " + (meta.hasLore() ? meta.getLore() : "none"));
        
        if (customModelData != null) {
            if (!meta.hasCustomModelData() || meta.getCustomModelData() != customModelData) {
                return false;
            }
        }
        
        if (name != null && !name.isEmpty()) {
            String expectedName = ChatColor.translateAlternateColorCodes('&', name);
            if (!meta.hasDisplayName() || !meta.getDisplayName().equals(expectedName)) {
                return false;
            }
        } else if (meta.hasDisplayName()) {
            // If taming item has no name configured but item has a display name, it doesn't match
            return false;
        }
        
        if (!lore.isEmpty()) {
            if (!meta.hasLore()) {
                return false;
            }
            
            List<String> itemLore = meta.getLore();
            List<String> expectedLore = new ArrayList<>();
            for (String line : lore) {
                expectedLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            
            if (!itemLore.equals(expectedLore)) {
                return false;
            }
        }
        
        if (enchantments != null && !enchantments.isEmpty()) {
            Map<Enchantment, Integer> itemEnchantments = item.getEnchantments();
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                if (!itemEnchantments.containsKey(entry.getKey()) || 
                    !itemEnchantments.get(entry.getKey()).equals(entry.getValue())) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public boolean isValid() {
        return material != null && material != Material.AIR;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        TamingItem that = (TamingItem) o;
        
        if (consumeOnUse != that.consumeOnUse) return false;
        if (material != that.material) return false;
        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(lore, that.lore)) return false;
        if (!Objects.equals(customModelData, that.customModelData)) return false;
        return Objects.equals(enchantments, that.enchantments);
    }
    
    @Override
    public int hashCode() {
        int result = material.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + lore.hashCode();
        result = 31 * result + (customModelData != null ? customModelData.hashCode() : 0);
        result = 31 * result + (enchantments != null ? enchantments.hashCode() : 0);
        result = 31 * result + (consumeOnUse ? 1 : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "TamingItem{" +
                "material=" + material +
                ", name='" + name + '\'' +
                ", lore=" + lore +
                ", customModelData=" + customModelData +
                ", enchantments=" + enchantments +
                ", consumeOnUse=" + consumeOnUse +
                '}';
    }
}