package simplemounts.simplemounts.util.managers;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import simplemounts.simplemounts.SimpleMounts;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class EffectManager {

    public void mountClaimEffect(Player player) {
        final int radius = 1;

        double y = 0;

        for(int i = 0;i < 10; i++) {
            double x = radius * Math.cos(i);
            double z = radius * Math.sin(i);
            player.spawnParticle(Particle.TOTEM, player.getLocation().add(x,y,z),5,1,1,1);
        }
    }

    public void mountStoreEffect(Player p,Entity e) {
        p.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,e.getLocation(),10,1,1,1);
    }
}
