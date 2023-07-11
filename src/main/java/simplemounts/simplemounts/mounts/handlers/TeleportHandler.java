package simplemounts.simplemounts.mounts.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class TeleportHandler implements Listener {

    public TeleportHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);
        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);

        //Check to see if its a mount event / teleport < 10 blocks
        if(calcDistance(player.getLocation(), event.getTo()) < 10) {
            return;
        }

        try {
            if(entityManager.isSummoned(player)) {
                event.setCancelled(true);
                errorManager.error("Must Store mount before teleporting",player);
            }
        } catch(Throwable e) {
            errorManager.error("Teleport Handler - Internal Failure", player, e);
        }
    }

    /**
     * Calculates the vector distance in blocks between the 2 Locations
     * @param loc1
     * @param loc2
     * @return
     */
    private double calcDistance(Location loc1, Location loc2) {
        double exp1 = Math.pow((loc1.getX() - loc2.getX()),2);
        double exp2 = Math.pow((loc1.getZ() - loc2.getZ()),2);

        return Math.sqrt(exp1 + exp2);
    }

}
