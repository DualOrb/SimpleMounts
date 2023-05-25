package simplemounts.simplemounts.Mounts.Handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.Util.Managers.EntityManager;

public class DeathHandler implements Listener {

    public DeathHandler(SimpleMounts plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent event) {
        //On death, mount should be removed from current mounts and persistent storage
        LivingEntity le = event.getEntity();
        if(!(le instanceof AbstractHorse)) return;
        AbstractHorse horse = (AbstractHorse)le;

        Player player = (Player)horse.getOwner();

        if(!EntityManager.isSummoned(player)) return; //Owner does not currently have a mount summoned

        if(!EntityManager.getSummonedMount(player).equals(event.getEntity())) return; //Entity killed was not the summoned mount

        //Now we know that we are dealing with a mount that died that was summoned

        EntityManager.removeMount(player);

        if(event.getEntity().getKiller() instanceof Player) {
            SimpleMounts.sendPlayerMessage("Your mount has been slain by " + event.getEntity().getKiller().getName() + "!",player);
        } else {
            SimpleMounts.sendPlayerMessage("Your mount has died!",player);
        }

    }
}
