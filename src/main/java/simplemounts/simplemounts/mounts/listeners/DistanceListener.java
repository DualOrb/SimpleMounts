package simplemounts.simplemounts.mounts.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.database.Mount;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

import java.util.ArrayList;

/**
 * Constantly listens for if the player(s) are outside of the specified range
 */
public class DistanceListener {

    private static Plugin plugin;

    public DistanceListener(Plugin plugin) {
        this.plugin = plugin;
        EntityManager em = ServiceLocator.getLocator().getService(EntityManager.class);

        final int range = SimpleMounts.getMountConfig().getInt("basic.leash-range");

        //Creates a timer task for every 5 seconds
        new BukkitRunnable() {
            public void run() {
                if(!SimpleMounts.getMountConfig().getBoolean("basic.leash-enabled")) return;

                final int range = SimpleMounts.getMountConfig().getInt("basic.leash-range");
                //Get the current map / players / mounts
                ArrayList<Mount> entities = em.getAllMounts();

                //All players in this list are known to be online
                for(Mount m: entities) {
                    Entity e = Bukkit.getEntity(m.getEntityId());

                    AbstractHorse horse = (AbstractHorse)e;
                    Player player = (Player)horse.getOwner();

                    double distance = calcDistance(horse,player);

                    //if outside the range, then store the mount
                    if(distance > range) {
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
