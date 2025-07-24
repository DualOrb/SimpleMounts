package com.simplemounts.recipes;

import com.simplemounts.SimpleMounts;
import com.simplemounts.items.MountWhistle;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

public class RecipeManager {
    
    private final SimpleMounts plugin;
    private final MountWhistle mountWhistle;
    
    public RecipeManager(SimpleMounts plugin) {
        this.plugin = plugin;
        this.mountWhistle = new MountWhistle(plugin);
    }
    
    public void registerRecipes() {
        registerWhistleRecipe();
        registerTamingCookieRecipe();
    }
    
    private void registerWhistleRecipe() {
        // Create the whistle item
        ItemStack whistleItem = mountWhistle.createWhistle();
        
        // Create the shaped recipe
        NamespacedKey key = new NamespacedKey(plugin, "mount_whistle_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, whistleItem);
        
        // Set the recipe pattern - creates a magical mount whistle
        recipe.shape(
            " G ",
            "GHG",
            " S "
        );
        
        // Set the ingredients
        recipe.setIngredient('G', Material.GOLD_INGOT);     // Golden magic
        recipe.setIngredient('H', Material.GOAT_HORN);      // Base horn
        recipe.setIngredient('S', Material.SADDLE);         // Mount connection
        
        // Register the recipe
        try {
            Bukkit.addRecipe(recipe);
            plugin.getLogger().info("Registered mount whistle recipe successfully");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register mount whistle recipe: " + e.getMessage());
        }
    }
    
    private void registerTamingCookieRecipe() {
        // Create the taming cookie item from config
        ItemStack cookieItem = createTamingCookieItem();
        
        // Create the shaped recipe
        NamespacedKey key = new NamespacedKey(plugin, "mount_taming_cookie_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, cookieItem);
        
        // Set the recipe pattern - creates a magical mount taming cookie
        recipe.shape(
            "WGW",
            "GCG", 
            "WGW"
        );
        
        // Set the ingredients
        recipe.setIngredient('W', Material.WHEAT);           // Basic food ingredient
        recipe.setIngredient('G', Material.GOLD_NUGGET);     // Magic ingredient (cheaper than ingot)
        recipe.setIngredient('C', Material.COCOA_BEANS);     // Cookie base
        
        // Register the recipe
        try {
            Bukkit.addRecipe(recipe);
            plugin.getLogger().info("Registered mount taming cookie recipe successfully");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register mount taming cookie recipe: " + e.getMessage());
        }
    }
    
    private ItemStack createTamingCookieItem() {
        // Get the taming item from config
        com.simplemounts.data.TamingItem tamingItem = plugin.getConfigManager().getTamingItem("default");
        
        // Create the item stack
        ItemStack cookie = new ItemStack(Material.COOKIE, 4); // Recipe makes 4 cookies
        org.bukkit.inventory.meta.ItemMeta meta = cookie.getItemMeta();
        
        if (meta != null) {
            // Set name and lore from config
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', tamingItem.getName()));
            
            java.util.List<String> lore = new java.util.ArrayList<>();
            for (String loreLine : tamingItem.getLore()) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', loreLine));
            }
            meta.setLore(lore);
            
            // Add enchantments from config
            for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : tamingItem.getEnchantments().entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
            
            // Add item flags to hide enchantments for a cleaner look
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            
            cookie.setItemMeta(meta);
        }
        
        return cookie;
    }
    
    public void unregisterRecipes() {
        // Remove the whistle recipe
        NamespacedKey whistleKey = new NamespacedKey(plugin, "mount_whistle_recipe");
        Bukkit.removeRecipe(whistleKey);
        
        // Remove the taming cookie recipe
        NamespacedKey cookieKey = new NamespacedKey(plugin, "mount_taming_cookie_recipe");
        Bukkit.removeRecipe(cookieKey);
    }
    
    public MountWhistle getMountWhistle() {
        return mountWhistle;
    }
}