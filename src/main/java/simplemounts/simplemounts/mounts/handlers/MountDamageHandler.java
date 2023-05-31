package simplemounts.simplemounts.mounts.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class MountDamageHandler implements Listener {

    public MountDamageHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMountDamage(EntityDamageByEntityEvent event) {
        if(event.getDamager() == null) return;
        if(!(event.getDamager() instanceof Player)) return;
        if(!(event.getEntity() instanceof AbstractHorse)) return;

        Player player = (Player)event.getDamager();
        AbstractHorse horse = (AbstractHorse)event.getEntity();

        if(horse.getOwner() == null) return;
        if(!(horse.getOwner() instanceof Player)) return; //Checking if player is offline or not

        Player owner = (Player)horse.getOwner();

        EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);

        if(!entityManager.isMount(horse)) return;

        //Now we know that this is a mount that is being damaged

        //If immortal, cancel event
        if(SimpleMounts.getMountConfig().getBoolean("damage.is-immortal")) {
            event.setCancelled(true);
            return;
        }

        //Check if owner can damage
        if(player.getUniqueId().equals(owner.getUniqueId())) {
            if(!SimpleMounts.getMountConfig().getBoolean("damage.owner-can-damage")) {
                event.setCancelled(true);
                return;
            }
        }

        //Apply damage modifier to damage
        Double damageMod = SimpleMounts.getMountConfig().getDouble("damage.damage-modifier");

        event.setDamage(event.getDamage() * damageMod);


    }
}
