package simplemounts.simplemounts.Mounts.Recipes;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import simplemounts.simplemounts.SimpleMounts;

import java.util.List;

public class WhistleRecipe {

    private static ItemStack whistle;

    public WhistleRecipe() {
        whistle = new ItemStack(Material.GOAT_HORN,1);
        ItemMeta meta = whistle.getItemMeta();
        meta.setCustomModelData(1);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);

        whistle.setItemMeta(meta);

        ItemMeta itemMeta = whistle.getItemMeta();
        itemMeta.setDisplayName(ChatColor.RESET + "" + ChatColor.GRAY + "Horse Whistle");
        itemMeta.setLore(List.of(ChatColor.DARK_PURPLE + "With a whistle your",ChatColor.DARK_PURPLE + "trusty steed arrives"));

        whistle.setItemMeta(itemMeta);

        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(SimpleMounts.getPlugin(),"horse-whistle"), whistle);
        recipe.shape(" S "," I "," N ");
        recipe.setIngredient('S',Material.STRING);
        recipe.setIngredient('I',Material.IRON_INGOT);
        recipe.setIngredient('N',Material.IRON_NUGGET);

        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getWhistle() {
        return whistle;
    }
}
