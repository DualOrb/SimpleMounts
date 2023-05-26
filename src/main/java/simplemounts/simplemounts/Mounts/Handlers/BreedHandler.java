package simplemounts.simplemounts.Mounts.Handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import simplemounts.simplemounts.SimpleMounts;

public class BreedHandler implements Listener {

    public BreedHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBreedEvent(EntityBreedEvent event) {
        if(!(event.getEntity() instanceof AbstractHorse)) return;
        if(!(event.getBreeder() instanceof Player)) return;

        Player player = (Player)event.getBreeder();

        Horse father = (Horse)event.getFather();
        Horse mother = (Horse)event.getMother();

        Player fatherOwner = (Player)father.getOwner();
        Player motherOwner = (Player)mother.getOwner();

        //May need to get re worked
        if(fatherOwner != null && motherOwner != null) {
            if(!SimpleMounts.getCustomConfig().getBoolean("basic.is-breedable")) {
                SimpleMounts.sendUserError("Breeding of mounts is disabled",player);
                event.setCancelled(true);
                return;
            }
        }




    }
}
