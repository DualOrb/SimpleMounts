package simplemounts.simplemounts.Mounts.Handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.Util.Managers.EntityManager;

public class TeleportHandler implements Listener {

    public TeleportHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Handles teleporting. Stores mount on teleport
     * @param event
     */
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if(!EntityManager.isSummoned(player)) return;

        double distance = calcDistance(player,EntityManager.getSummonedMount(player));

        if(distance < 15) return;

        EntityManager.storeSummonedMount(player);
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
