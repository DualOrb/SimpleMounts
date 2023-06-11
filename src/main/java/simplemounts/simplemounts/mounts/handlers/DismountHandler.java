package simplemounts.simplemounts.mounts.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.spigotmc.event.entity.EntityDismountEvent;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class DismountHandler implements Listener {

    public DismountHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        if(!SimpleMounts.getMountConfig().getBoolean("basic.store-on-dismount")) return;

        if(!(event.getEntity() instanceof Player)) return;
        if(!(event.getDismounted() instanceof AbstractHorse)) return;

        Player player = (Player)event.getEntity();
        AbstractHorse horse = (AbstractHorse)event.getDismounted();

        EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);

        if(!entityManager.isSummoned(player)) return;
        if(!entityManager.isMount(horse)) return;

        entityManager.storeSummonedMount(player);
    }
}
