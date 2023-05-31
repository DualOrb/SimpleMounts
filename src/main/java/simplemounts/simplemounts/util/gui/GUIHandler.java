package simplemounts.simplemounts.util.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.database.Database;
import simplemounts.simplemounts.util.database.Mount;
import simplemounts.simplemounts.util.managers.ChatManager;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

import java.util.ArrayList;

public class GUIHandler implements Listener {

    private ItemManager itemManager;

    public GUIHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this,plugin);
        itemManager = ServiceLocator.getLocator().getService(ItemManager.class);
    }

    @EventHandler
    public void onPlayerDropItemEvent(PlayerDropItemEvent event) {
        //Perm items can't be dropped in survival
        if(itemManager.getPermItems().contains(event.getItemDrop().getItemStack())){
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerInventoryClick(InventoryClickEvent event) {
        if(itemManager.getPermItems().contains(event.getCurrentItem())) event.setCancelled(true);
        if(event.getClickedInventory() == null) return; //Catch if player just presses esc to exit inventory

        int clicked = event.getSlot();
        Player player = (Player)event.getWhoClicked();

        //Check to see if it contains the identifying item -> player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullmeta = (SkullMeta)head.getItemMeta();
        skullmeta.setDisplayName(ChatColor.GOLD + "Display Name: " + ChatColor.GRAY + player.getName());
        skullmeta.setOwningPlayer(player);
        head.setItemMeta(skullmeta);

        if(!event.getClickedInventory().contains(head)) return;

        //If its a horse egg, summon the horse and check if all others are unsummoned

        Database database = ServiceLocator.getLocator().getService(Database.class);
        ArrayList<Mount> mounts = database.getMounts(player);

        if(clicked > mounts.size()-1) {event.setCancelled(true);return;} //Invalid place clicked. Prevents console spam

        EntityManager em = ServiceLocator.getLocator().getService(EntityManager.class);

        //Logic for if a horse stored or summoned
        if(em.isSummoned(player)) {
            AbstractHorse h = (AbstractHorse)em.getSummonedMount(player);
            em.storeSummonedMount(player);
            if(mounts.get(clicked).getEntityId() != null) {
                if(mounts.get(clicked).getEntityId().equals(h.getEntityId())); {event.setCancelled(true);player.closeInventory();return;}
            }
        }



        //If shift click, release current mount. Should spawn it outside
        if(event.isRightClick() && event.isShiftClick()) {
            AbstractHorse h = em.spawnHorse(mounts.get(clicked),(Player)event.getWhoClicked());
            em.removeMount(player);
            event.setCancelled(true);
            return;
        }
        ChatManager cm = ServiceLocator.getLocator().getService(ChatManager.class);


        em.spawnHorse(mounts.get(clicked),(Player)event.getWhoClicked());

        if(mounts.get(clicked).getHorseData().get("name") == null) {
            cm.sendPlayerMessage( "Summoned horse", (Player)event.getWhoClicked());
            player.playSound(player.getLocation(), Sound.ENTITY_HORSE_GALLOP,1.0f,1.0f);
        } else {
            cm.sendPlayerMessage( "Summoned " + mounts.get(clicked).getHorseData().get("name"), (Player)event.getWhoClicked());

        }

        event.setCancelled(true);

        player.closeInventory();
    }
}
