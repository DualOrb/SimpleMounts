package simplemounts.simplemounts.mounts.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class PortalHandler implements Listener {

    public PortalHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPortalEnter(EntityPortalEnterEvent event) {
        try {
            if(!(event.getEntity() instanceof AbstractHorse)) return;

            AbstractHorse horse = (AbstractHorse)event.getEntity();
            EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);

            if(!entityManager.isMount(horse)) return;

            entityManager.storeSummonedMount((Player)horse.getOwner());
        } catch (Throwable e) {
            ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
            errorManager.error("Portal Handler - Internal Failure",e);
        }
    }
}
