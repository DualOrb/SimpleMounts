package simplemounts.simplemounts.Mounts.Handlers;

import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import simplemounts.simplemounts.Mounts.GUI.MountsPage;
import simplemounts.simplemounts.Mounts.Recipes.WhistleRecipe;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.Util.Managers.ErrorManager;
import simplemounts.simplemounts.Util.Services.ServiceLocator;

import java.util.List;

public class SummonHandler implements Listener {

    public SummonHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Handles summoning mounts
     * @param event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {

        ItemStack item = event.getItem();
        if(item == null) return;
        if(!item.equals(WhistleRecipe.getWhistle())) return;
        Player player = event.getPlayer();

        Action action = event.getAction();
        if(!action.equals(Action.RIGHT_CLICK_AIR)) return;

        if(player.hasCooldown(Material.GOAT_HORN)) return;

        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);

        if(!(player.hasPermission("SimpleMounts.can-whistle"))) {errorManager.error("Sorry, you haven't learned how to whistle yet", player);return;}

        player.setCooldown(Material.GOAT_HORN, 100);
        if(SimpleMounts.getCustomConfig().getBoolean("advanced.custom-sounds")) {
            player.getWorld().playSound(player.getLocation(),"custom.whistle",1.0f,1.0f);
        } else {
            player.getWorld().playSound(player.getLocation(),Sound.ENTITY_GHAST_SCREAM,1.0f,1.0f);
        }
        Bukkit.getScheduler().runTaskLater(SimpleMounts.getPlugin(), new Runnable() {
            @Override
            public void run() {
                new MountsPage(player,player);
            }
        },30L);


    }

}
