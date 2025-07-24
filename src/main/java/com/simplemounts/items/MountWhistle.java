package com.simplemounts.items;

import com.simplemounts.SimpleMounts;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class MountWhistle {
    
    private final SimpleMounts plugin;
    private final NamespacedKey whistleKey;
    
    public MountWhistle(SimpleMounts plugin) {
        this.plugin = plugin;
        this.whistleKey = new NamespacedKey(plugin, "mount_whistle");
    }
    
    public ItemStack createWhistle() {
        ItemStack whistle = new ItemStack(Material.GOAT_HORN);
        ItemMeta meta = whistle.getItemMeta();
        
        if (meta != null) {
            // Set display name
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Mount Whistle");
            
            // Set lore
            List<String> lore = Arrays.asList(
                ChatColor.GRAY + "A magical horn that summons",
                ChatColor.GRAY + "your trusted mounts to your side.",
                "",
                ChatColor.YELLOW + "Right-click to open Mount Manager",
                ChatColor.GRAY + "Select any mount to summon or store it",
                "",
                ChatColor.AQUA + "✦ One mount active at a time",
                ChatColor.AQUA + "✦ Auto-stores current when switching",
                "",
                ChatColor.DARK_GRAY + "Enchanted for mount enthusiasts"
            );
            meta.setLore(lore);
            
            // Set custom model data for texture pack support
            meta.setCustomModelData(100001);
            
            // Add persistent data to identify this as a mount whistle
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(whistleKey, PersistentDataType.STRING, "mount_whistle");
            
            whistle.setItemMeta(meta);
        }
        
        return whistle;
    }
    
    public boolean isMountWhistle(ItemStack item) {
        if (item == null || item.getType() != Material.GOAT_HORN) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(whistleKey, PersistentDataType.STRING);
    }
    
    public void handleWhistleUse(Player player, ItemStack whistle) {
        if (!isMountWhistle(whistle)) {
            return;
        }
        
        // Always open mount manager GUI when whistle is used
        plugin.getGUIManager().openMountGUI(player);
    }
    
    public NamespacedKey getWhistleKey() {
        return whistleKey;
    }
}