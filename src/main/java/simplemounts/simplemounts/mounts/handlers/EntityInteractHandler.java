package simplemounts.simplemounts.mounts.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class EntityInteractHandler implements Listener {

    public EntityInteractHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {

        if (!(event.getRightClicked() instanceof AbstractHorse)) return;

        Player player = (Player) event.getPlayer();

        AbstractHorse h1 = (AbstractHorse) event.getRightClicked();

        EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);

        Player owningPlayer = entityManager.getOwningPlayer(h1);
        if (owningPlayer == null) return; //If is not a currently summoned mount, then its wild and player can ride

        AbstractHorse h2 = (AbstractHorse) entityManager.getSummonedMount(player);

        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
        if (h2 == null) {
            errorManager.error("This is not your mount.", player);
            event.setCancelled(true);
            return;
        }

        if(h1.getEntityId() != (h2.getEntityId())) {errorManager.error("This is not your mount",player); event.setCancelled(true);return;}

        return;
    }
}
