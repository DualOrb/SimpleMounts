package simplemounts.simplemounts.mounts.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class LogoutHandler implements Listener {

    public LogoutHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
        try {
            EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);
            entityManager.storeSummonedMount(player);
            errorManager.log( "Logout: unloaded " + player.getName() + "'s mount");

        } catch (Throwable e) {
            errorManager.error("Player Logout - Internal Failure",player,e);
        }
    }
}
