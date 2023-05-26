package simplemounts.simplemounts.util.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import static org.bukkit.enchantments.Enchantment.*;

/**
 * ItemManager
 * Is
 */
public class ItemManager {

    private static Map customItems;
    private static ArrayList<ItemStack> permItems;
    private static Map functionalItems;


    //Called to initialise all the static custom items we define here
    public ItemManager() {
        customItems = new Hashtable();              //Contains all custom items we create
        permItems = new ArrayList<ItemStack>();     //Items that can't be interacted with
        functionalItems = new Hashtable();          //For when we want to do a specific function based on an item usage
    }

    /**
     * getItemByName
     * Retrieves the custom item stored in the map woth the name as key
     *
     * @param itemName
     * @return ItemStack
     */
    public ItemStack getItemByName(String itemName) {
        if(!(customItems.containsKey(itemName))) { return null;}
        return (ItemStack)customItems.get(itemName);
    }

    public Map getFunctionalItems() {return functionalItems;}

    public ArrayList getPermItems() {
        return permItems;
    }

    /**
     * addPermItem
     * Adds an item to be stored in the list of permanent, non moveable items on the server
     * @param item
     */
    public void addPermItem(ItemStack item) {
        if(!(permItems.contains(item))) {permItems.add(item);}   //Only adds if item is not already in list
        return;

    }

    /**
     * addFunctionalItem
     * adds an item to be stored in the hashtable, has a code we can check for which function
     */
    public void addFunctionalItem(ItemStack item, String code) {
        functionalItems.put(item, code);
    }

    /**
     * isFunctionalItem
     * Checks if the functional item is in the list. If it is, returns a code. If not, returns null
     */
    public String isFunctionalItem(ItemStack item) {
        if(!(functionalItems.containsKey(item))) {return null; }
        return functionalItems.get(item).toString();
    }

    /**
     * exists
     * Checks whether an item exists or not within the item list
     *
     * @param ItemName
     * @return bool true if exists | false if not
     */
    public boolean exists(String ItemName) {
        return customItems.containsKey(ItemName);
    }

    /**
     *
     */

}

