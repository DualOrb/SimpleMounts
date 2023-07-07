package simplemounts.simplemounts.mounts.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.ChatManager;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

import java.util.List;

public class DeathHandler implements Listener {

    public DeathHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent event) {
        //On death, mount should be removed from current mounts and persistent storage
        LivingEntity le = event.getEntity();
        if(!(le instanceof AbstractHorse)) return;
        AbstractHorse horse = (AbstractHorse)le;

        Player player = (Player)horse.getOwner();
        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);

        try {


            EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);

            if(!entityManager.isSummoned(player)) return; //Owner does not currently have a mount summoned

            if(!entityManager.getSummonedMount(player).equals(event.getEntity())) return; //Entity killed was not the summoned mount

            //Now we know that we are dealing with a mount that died that was summoned
            if(SimpleMounts.getMountConfig().getBoolean("damage.can-respawn")) {
                horse.setHealth(horse.getMaxHealth()); //Put back to full health
                entityManager.storeSummonedMount(player); //Store back away

                //Remove dropped items at location
                List<Entity> nearbyEntities = horse.getNearbyEntities(5, 5, 5);
                List<ItemStack> drops = event.getDrops();
                for(Entity e: nearbyEntities) {
                    if(!(e instanceof Item)) continue;
                    Item item = (Item)e;
                    if(drops.contains(item.getItemStack())) e.remove();
                }
                return;
            }

            entityManager.removeMount(player);
            ChatManager chatManager = ServiceLocator.getLocator().getService(ChatManager.class);

            if(event.getEntity().getKiller() instanceof Player) {
                chatManager.sendPlayerMessage("Your mount has been slain by " + event.getEntity().getKiller().getName() + "!",player);
            } else {
                chatManager.sendPlayerMessage("Your mount has died!",player);
            }

        } catch (Throwable e) {
            errorManager.error("Death Handler - Internal Failure", player,e);
        }

    }
}
