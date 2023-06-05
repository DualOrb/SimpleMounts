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

        entityManager.storeSummonedMount(player);
    }
}
