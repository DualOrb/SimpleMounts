package com.simplemounts.gui;

import com.simplemounts.SimpleMounts;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ConfirmationGUI {
    
    private final SimpleMounts plugin;
    private final Player player;
    private final String action;
    private final String mountName;
    
    public ConfirmationGUI(SimpleMounts plugin, Player player, String action, String mountName) {
        this.plugin = plugin;
        this.player = player;
        this.action = action;
        this.mountName = mountName;
    }
    
    public void open() {
        String title = ChatColor.RED + "" + ChatColor.BOLD + "Confirm Action";
        Inventory inventory = Bukkit.createInventory(null, 27, title);
        
        // Fill background
        GUIManager.fillBackground(inventory);
        
        // Add confirmation items
        addConfirmationItems(inventory);
        
        player.openInventory(inventory);
    }
    
    private void addConfirmationItems(Inventory inventory) {
        // Main question item
        ItemStack questionItem = createQuestionItem();
        inventory.setItem(13, questionItem);
        
        // Confirm button
        ItemStack confirmButton = GUIManager.createActionItem(
            Material.LIME_CONCRETE,
            "Confirm",
            "confirm_" + action,
            "Click to confirm the action",
            getActionDescription(),
            ChatColor.GREEN + "This will proceed with the action"
        );
        inventory.setItem(11, confirmButton);
        
        // Cancel button
        ItemStack cancelButton = GUIManager.createActionItem(
            Material.RED_CONCRETE,
            "Cancel",
            "cancel_action",
            "Click to cancel the action",
            "No changes will be made",
            ChatColor.RED + "This will abort the action"
        );
        inventory.setItem(15, cancelButton);
    }
    
    private ItemStack createQuestionItem() {
        Material material = getActionMaterial();
        String title = getActionTitle();
        String[] lore = getActionLore();
        
        return GUIManager.createNavigationItem(material, title, lore);
    }
    
    private Material getActionMaterial() {
        switch (action.toLowerCase()) {
            case "release_mount":
                return Material.LAVA_BUCKET;
            case "store_mount":
                return Material.ENDER_CHEST;
            case "summon_mount":
                return Material.GRASS_BLOCK;
            case "rename_mount":
                return Material.NAME_TAG;
            default:
                return Material.QUESTION_MARK_MINECART;
        }
    }
    
    private String getActionTitle() {
        switch (action.toLowerCase()) {
            case "release_mount":
                return "Release Mount?";
            case "store_mount":
                return "Store Mount?";
            case "summon_mount":
                return "Summon Mount?";
            case "rename_mount":
                return "Rename Mount?";
            default:
                return "Confirm Action?";
        }
    }
    
    private String[] getActionLore() {
        switch (action.toLowerCase()) {
            case "release_mount":
                return new String[] {
                    "Are you sure you want to release:",
                    ChatColor.YELLOW + mountName,
                    "",
                    ChatColor.RED + "WARNING: This cannot be undone!",
                    ChatColor.RED + "The mount will be deleted forever."
                };
            case "store_mount":
                return new String[] {
                    "Store the mount:",
                    ChatColor.YELLOW + mountName,
                    "",
                    "The mount will be safely stored",
                    "and removed from the world."
                };
            case "summon_mount":
                return new String[] {
                    "Summon the mount:",
                    ChatColor.YELLOW + mountName,
                    "",
                    "The mount will appear near you",
                    "ready to be ridden."
                };
            case "rename_mount":
                return new String[] {
                    "Rename the mount:",
                    ChatColor.YELLOW + mountName,
                    "",
                    "You'll need to type the new name",
                    "in chat after confirming."
                };
            default:
                return new String[] {
                    "Confirm this action for:",
                    ChatColor.YELLOW + mountName
                };
        }
    }
    
    private String getActionDescription() {
        switch (action.toLowerCase()) {
            case "release_mount":
                return "Mount will be permanently deleted";
            case "store_mount":
                return "Mount will be stored safely";
            case "summon_mount":
                return "Mount will be summoned near you";
            case "rename_mount":
                return "Mount will be renamed";
            default:
                return "Action will be performed";
        }
    }
}