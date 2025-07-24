package com.simplemounts.data;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ChestInventoryData {
    
    private final Map<Integer, ItemStack> items;
    private final int size;
    
    public ChestInventoryData(int size) {
        this.size = size;
        this.items = new HashMap<>();
    }
    
    public ChestInventoryData(Inventory inventory) {
        this.size = inventory.getSize();
        this.items = new HashMap<>();
        
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                items.put(i, item.clone());
            }
        }
    }
    
    public static ChestInventoryData fromInventory(Inventory inventory) {
        return new ChestInventoryData(inventory);
    }
    
    public void setItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < size) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                items.put(slot, item.clone());
            } else {
                items.remove(slot);
            }
        }
    }
    
    public ItemStack getItem(int slot) {
        if (slot >= 0 && slot < size) {
            ItemStack item = items.get(slot);
            return item != null ? item.clone() : null;
        }
        return null;
    }
    
    public void applyToInventory(Inventory inventory) {
        if (inventory.getSize() != size) {
            throw new IllegalArgumentException("Inventory size mismatch: expected " + size + ", got " + inventory.getSize());
        }
        
        inventory.clear();
        
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            int slot = entry.getKey();
            ItemStack item = entry.getValue();
            
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item.clone());
            }
        }
    }
    
    public Map<Integer, ItemStack> getItems() {
        Map<Integer, ItemStack> result = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            result.put(entry.getKey(), entry.getValue().clone());
        }
        return result;
    }
    
    public int getSize() {
        return size;
    }
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    public int getItemCount() {
        return items.size();
    }
    
    public boolean hasItem(int slot) {
        return items.containsKey(slot);
    }
    
    public void clear() {
        items.clear();
    }
    
    public ItemStack[] toArray() {
        ItemStack[] array = new ItemStack[size];
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < size) {
                array[slot] = entry.getValue().clone();
            }
        }
        return array;
    }
    
    public void fromArray(ItemStack[] array) {
        items.clear();
        for (int i = 0; i < Math.min(array.length, size); i++) {
            if (array[i] != null && array[i].getType() != org.bukkit.Material.AIR) {
                items.put(i, array[i].clone());
            }
        }
    }
    
    public int getTotalItemCount() {
        int count = 0;
        for (ItemStack item : items.values()) {
            count += item.getAmount();
        }
        return count;
    }
    
    public boolean hasSpace() {
        return items.size() < size;
    }
    
    public int getAvailableSlots() {
        return size - items.size();
    }
    
    @Override
    public String toString() {
        return "ChestInventoryData{" +
                "items=" + items.size() +
                ", size=" + size +
                ", totalItems=" + getTotalItemCount() +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ChestInventoryData that = (ChestInventoryData) o;
        
        if (size != that.size) return false;
        return items.equals(that.items);
    }
    
    @Override
    public int hashCode() {
        int result = items.hashCode();
        result = 31 * result + size;
        return result;
    }
}