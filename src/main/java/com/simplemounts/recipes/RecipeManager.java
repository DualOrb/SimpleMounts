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
    
    public void unregisterRecipes() {
        // Remove the whistle recipe
        NamespacedKey key = new NamespacedKey(plugin, "mount_whistle_recipe");
        Bukkit.removeRecipe(key);
    }
    
    public MountWhistle getMountWhistle() {
        return mountWhistle;
    }
}