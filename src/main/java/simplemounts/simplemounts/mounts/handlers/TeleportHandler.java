package simplemounts.simplemounts.mounts.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class TeleportHandler implements Listener {

    public TeleportHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Handles teleporting. Stores mount on teleport
     * @param event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);

        if(!entityManager.isSummoned(player)) return;

        //Checks if it is a mount event (as technically it counts as a teleport for some reason)
        if(calcDistance(player,entityManager.getSummonedMount(player)) < 15) return;

//        event.setCancelled(true);
//
//        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
//
//        errorManager.error("Must store mounts before teleporting",player);
    }

    /**
     * Calculates the vector distance in blocks between the 2 entities
     * @param e1
     * @param e2
     * @return
     */
    public double calcDistance(Entity e1, Entity e2) {
        double exp1 = Math.pow((e1.getLocation().getX() - e2.getLocation().getX()),2);
        double exp2 = Math.pow((e1.getLocation().getZ() - e2.getLocation().getZ()),2);

        return Math.sqrt(exp1 + exp2);
    }
}
