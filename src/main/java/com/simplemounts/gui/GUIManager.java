package com.simplemounts.gui;

import com.simplemounts.SimpleMounts;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GUIManager {
    
    private final SimpleMounts plugin;
    private final ConcurrentMap<Player, GUISession> activeSessions;
    
    public GUIManager(SimpleMounts plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        
        // Start periodic cleanup task to prevent stuck sessions
        startCleanupTask();
    }
    
    private void startCleanupTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Clean up sessions for offline players
            activeSessions.entrySet().removeIf(entry -> !entry.getKey().isOnline());
            
            // Clean up sessions that are older than 5 minutes (safety net)
            long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
            activeSessions.entrySet().removeIf(entry -> 
                entry.getValue().getLastInteraction() < fiveMinutesAgo);
        }, 1200L, 1200L); // Run every minute (20 ticks * 60 = 1200 ticks)
    }
    
    public void openMountGUI(Player player) {
        GUISession session = new GUISession(player, GUIType.MOUNT_LIST, 0);
        activeSessions.put(player, session);
        
        MountListGUI mountGUI = new MountListGUI(plugin, player);
        mountGUI.open();
    }
    
    public void openMountInfoGUI(Player player, Integer mountId) {
        GUISession session = new GUISession(player, GUIType.MOUNT_INFO, 0);
        session.setSelectedMountId(mountId);
        activeSessions.put(player, session);
        
        MountInfoGUI infoGUI = new MountInfoGUI(plugin, player, mountId);
        infoGUI.open();
    }
    
    public void openConfirmationGUI(Player player, String action, Integer mountId) {
        GUISession session = new GUISession(player, GUIType.CONFIRMATION, 0);
        session.setSelectedMountId(mountId);
        session.setAction(action);
        activeSessions.put(player, session);
        
        ConfirmationGUI confirmGUI = new ConfirmationGUI(plugin, player, action, mountId);
        confirmGUI.open();
    }
    
    public GUISession getSession(Player player) {
        return activeSessions.get(player);
    }
    
    public void closeSession(Player player) {
        activeSessions.remove(player);
    }
    
    public boolean hasActiveSession(Player player) {
        return activeSessions.containsKey(player);
    }
    
    public void closeAllSessions() {
        for (Player player : activeSessions.keySet()) {
            player.closeInventory();
        }
        activeSessions.clear();
    }
    
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    public Set<Player> getActiveSessionPlayers() {
        return new HashSet<>(activeSessions.keySet());
    }
    
    // Utility methods for creating GUI items
    
    public static ItemStack createBackgroundItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public static ItemStack createNavigationItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + name);
            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(ChatColor.GRAY + line);
                }
                meta.setLore(loreList);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public static ItemStack createActionItem(Material material, String name, String action, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + name);
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(ChatColor.GRAY + line);
            }
            loreList.add("");
            loreList.add(ChatColor.YELLOW + "Action: " + action);
            meta.setLore(loreList);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public static ItemStack createPlayerHead(Player player) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ChatColor.GOLD + player.getName() + "'s Mounts");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Click mounts to summon/store");
            lore.add(ChatColor.GRAY + "Hover over mounts for details");
            lore.add("");
            lore.add(ChatColor.AQUA + "✦ Only one mount active at a time");
            
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            skull.setItemMeta(meta);
        }
        return skull;
    }
    
    public static void fillBackground(Inventory inventory) {
        ItemStack background = createBackgroundItem();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, background);
            }
        }
    }
    
    public static void addBorder(Inventory inventory) {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            border.setItemMeta(meta);
        }
        
        int size = inventory.getSize();
        int rows = size / 9;
        
        // Top and bottom rows
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            if (rows > 1) {
                inventory.setItem(size - 9 + i, border);
            }
        }
        
        // Side columns
        for (int i = 1; i < rows - 1; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }
    
    public static ItemStack createMountItem(int mountId, String mountName, String mountType, boolean isActive, 
                                          double health, double maxHealth, double speed, double jump, SimpleMounts plugin) {
        Material material = getMountMaterial(mountType);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Store mount ID in persistent data for reliable retrieval
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "mount_id"), 
                org.bukkit.persistence.PersistentDataType.INTEGER, 
                mountId
            );
            
            // Set display name - show name if available, otherwise show "Unnamed Type #ID"
            String displayName;
            if (mountName != null && !mountName.trim().isEmpty()) {
                displayName = mountName;
            } else {
                displayName = "Unnamed " + mountType + " #" + mountId;
            }
            meta.setDisplayName(ChatColor.GOLD + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "ID: " + ChatColor.WHITE + "#" + mountId);
            lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + mountType);
            lore.add(ChatColor.GRAY + "Status: " + (isActive ? ChatColor.GREEN + "Active" : ChatColor.YELLOW + "Stored"));
            lore.add("");
            lore.add(ChatColor.RED + "Health: " + ChatColor.WHITE + String.format("%.1f", health) + "/" + String.format("%.1f", maxHealth));
            lore.add(ChatColor.AQUA + "Speed: " + ChatColor.WHITE + String.format("%.1f", speed * 43.17) + " bps");
            lore.add(ChatColor.GREEN + "Jump: " + ChatColor.WHITE + String.format("%.1f", getJumpHeight(jump)) + " blocks");
            lore.add("");
            if (isActive) {
                lore.add(ChatColor.RED + "» Click to Store");
            } else {
                lore.add(ChatColor.GREEN + "» Click to Summon");
            }
            
            meta.setLore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private static Material getMountMaterial(String mountType) {
        switch (mountType.toUpperCase()) {
            case "HORSE":
                return Material.HORSE_SPAWN_EGG;
            case "DONKEY":
                return Material.DONKEY_SPAWN_EGG;
            case "MULE":
                return Material.MULE_SPAWN_EGG;
            case "CAMEL":
                return Material.CAMEL_SPAWN_EGG;
            case "STRIDER":
                return Material.STRIDER_SPAWN_EGG;
            case "PIG":
                return Material.PIG_SPAWN_EGG;
            case "LLAMA":
                return Material.LLAMA_SPAWN_EGG;
            default:
                return Material.HORSE_SPAWN_EGG;
        }
    }
    
    private static double getJumpHeight(double jumpStrength) {
        if (jumpStrength <= 0.4) return 1.11;
        if (jumpStrength <= 0.5) return 1.62;
        if (jumpStrength <= 0.6) return 2.22;
        if (jumpStrength <= 0.7) return 2.89;
        if (jumpStrength <= 0.8) return 3.63;
        if (jumpStrength <= 0.9) return 4.44;
        if (jumpStrength <= 1.0) return 5.30;
        return 6.0;
    }
    
    public enum GUIType {
        MOUNT_LIST,
        MOUNT_INFO,
        CONFIRMATION
    }
}