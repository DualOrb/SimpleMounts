package simplemounts.simplemounts.mounts.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.ChatManager;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

import java.util.UUID;

/**
 * Handles the event in which a mount is within an unloaded chunk
 * will store the mount back to database
 */
public class EntitiesUnloadHandler implements Listener {

    private EntityManager entityManager;

    public EntitiesUnloadHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        entityManager = ServiceLocator.getLocator().getService(EntityManager.class);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitiesUnloadHandler(EntitiesUnloadEvent event) {

        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);

        try {
            for(Entity e: event.getEntities()) {

                UUID id = e.getUniqueId();

                if(!(e instanceof AbstractHorse)) continue;
                if(!entityManager.isMount(id)) continue;

                AbstractHorse horse = (AbstractHorse)e;

                if(!(horse.getOwner() instanceof Player)) continue;

                Player player = (Player)horse.getOwner();

                entityManager.storeSummonedMount(player,horse);
                errorManager.log( "Unloaded " + player.getName() + "'s mount");
            }

        } catch (Throwable e) {
            errorManager.error("Entities Unload Handler - Internal Failure", e);
        }
    }
}
