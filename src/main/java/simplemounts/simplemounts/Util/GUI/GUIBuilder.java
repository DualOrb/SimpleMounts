package simplemounts.simplemounts.Util.GUI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import simplemounts.simplemounts.Util.Services.ServiceLocator;

import java.util.ArrayList;

public class GUIBuilder {

    private ItemManager itemManager;

    public GUIBuilder() {
        itemManager = ServiceLocator.getLocator().getService(ItemManager.class);
    }

    /**
     * Makes an Inventory GUI with spaces to add items to
     *
     * @param name   String
     * @param color  ChatColor
     * @param spaces int
     * @param owner  Player nullable
     * @return Inventory
     * @throws Exception
     */
    public Inventory buildGUI(String name, ChatColor color, int spaces, Player owner, boolean spaceBetween) throws Exception {
        if (name == null || spaces == 0) {
            throw new Exception("Invalid Parameters");
        }

        String classname = name + "Inventory";
        if (spaceBetween) {
            spaces *= 2;
            spaces -= 1;
        }
        Inventory GUI = Bukkit.createInventory(owner, 9, color + name); //Have to initialise here, or else will throw error

        //Inventory must be multiple of 9 to create
        if (spaces <= 9) {
            GUI = Bukkit.createInventory(owner, 9, color + name);
        } else if (spaces <= 18) {
            GUI = Bukkit.createInventory(owner, 18, color + name);
        } else if (spaces <= 27) {
            GUI = Bukkit.createInventory(owner, 27, color + name);
        } else if (spaces <= 36) {
            GUI = Bukkit.createInventory(owner, 36, color + name);
        } else if (spaces <= 45) {
            GUI = Bukkit.createInventory(owner, 45, color + name);
        } else if (spaces <= 54) {
            GUI = Bukkit.createInventory(owner, 54, color + name);
        } else {
            //Inventory GUI = Bukkit.createInventory(owner, 9, color + name);
            throw new Exception("Invalid GUI size. Size too large");
        }


        GUI = fillBetweenSpaces(GUI, spaceBetween);

        return GUI;
    }

    /**
     * Recieves the inventory and fills the gaps between the needed spaces with a background material
     * Needs a 27
     *
     * @param GUI
     * @return
     */
    private Inventory fillBetweenSpaces(Inventory GUI, boolean spaceBetween) {
        ItemStack backgroundItem = itemManager.getItemByName("Background");

        int num;
        if (spaceBetween) {
            num = 2;
        } else {
            num = 1;
        }
        //Goes from back to front filling in the spaces. Makes sense why when we don't do space between
        for (int i = GUI.getSize() - num; i > 0; i--) {
            GUI.setItem(i, backgroundItem);
            if (spaceBetween) {
                i--;
            }
        }

        return GUI;
    }

    /**
     * Fills the spaces around a GUI that already has items placed inside
     */
    public Inventory fillWithBackgroundItem(Inventory GUI, Material material) {
        //Getting the perm items list
        ArrayList<ItemStack> permItems = itemManager.getPermItems();
        int permItemsLength = permItems.size();
        //Setting up the background item with its own meta data
        ItemStack backgroundItem = new ItemStack(material);
        ItemMeta item_meta = backgroundItem.getItemMeta();
        item_meta.setDisplayName(" ");
        item_meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item_meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        backgroundItem.setItemMeta(item_meta);
        itemManager.addPermItem(backgroundItem);

        //Air constant for the loop
        final ItemStack AIR = new ItemStack(Material.AIR);

        //Looping through the inventory and filling with a background item
        int GUISize = GUI.getSize();
        for (int i = 0; i < GUISize; i++) {
            if (GUI.getItem(i) == null || GUI.getItem(i) == AIR) {
                GUI.setItem(i, backgroundItem);
            }
        }


        return GUI;
    }

    /**
     * newBackgroundItem
     * creates a background item that cannot be touched for the GUIs
     */
    public ItemStack newBackgroundItem(Material material) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta item_meta = item.getItemMeta();
        item_meta.setDisplayName("");
        item_meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        item_meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item_meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(item_meta);
        itemManager.addPermItem(item);

        return item;
    }

    /**
     * newFunctionalITem
     * creates a new item that can be checked for its function in a GUI
     *
     * @param material
     * @param name
     * @param function
     * @param quantity
     */
    public ItemStack newFunctionalItem(Material material, String name, String function, int quantity) {
        if (quantity == 0) {
            quantity = 1;
        }

        ItemStack item = new ItemStack(material, quantity);
        ItemMeta item_meta = item.getItemMeta();
        item_meta.setDisplayName(name);
        item_meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        item_meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item_meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(item_meta);
        itemManager.addPermItem(item);
        itemManager.addFunctionalItem(item, function);

        return item;
    }

    public ItemStack newFunctionalItem(ItemStack item, String name, String function) {

        ItemMeta item_meta = item.getItemMeta();
        item_meta.setDisplayName(name);
        item_meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item_meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(item_meta);
        itemManager.addPermItem(item);
        itemManager.addFunctionalItem(item, function);

        return item;
    }
}
