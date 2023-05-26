package simplemounts.simplemounts.Mounts.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import simplemounts.simplemounts.Mounts.GUI.MountsPage;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.Util.Managers.EntityManager;
import simplemounts.simplemounts.Util.Services.ServiceLocator;

import java.util.ArrayList;

/**
 * Constantly listens for if the player(s) are outside of the specified range
 */
public class DistanceListener {

    private static Plugin plugin;

    public DistanceListener(Plugin plugin) {
        this.plugin = plugin;
        EntityManager em = ServiceLocator.getLocator().getService(EntityManager.class);

        //Creates a timer task for every 5 seconds
        new BukkitRunnable() {
            public void run() {
                //Get the current map / players / mounts
                ArrayList<Entity> entities = em.getAllMounts();

                //All players in this list are known to be online
                for(Entity e: entities) {
                    AbstractHorse horse = (AbstractHorse)e;
                    Player player = (Player)horse.getOwner();

                    double distance = calcDistance(horse,player);

                    //if outside the range, then store the mount
                    if(distance > 15) {
                        em.storeSummonedMount(player);
                    }
                }
            }
        }.runTaskTimer(plugin,25L,25L);
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
