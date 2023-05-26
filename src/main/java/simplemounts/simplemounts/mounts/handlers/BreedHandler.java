package simplemounts.simplemounts.mounts.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

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
        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
        if(fatherOwner != null && motherOwner != null) {
            if(!SimpleMounts.getCustomConfig().getBoolean("basic.is-breedable")) {
                errorManager.error("Breeding of mounts is disabled",player);
                event.setCancelled(true);
                return;
            }
        }




    }
}
