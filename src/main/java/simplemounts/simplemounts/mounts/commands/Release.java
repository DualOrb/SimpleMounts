package simplemounts.simplemounts.mounts.commands;

import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.ChatManager;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class Release implements CommandExecutor {

    /**
     * /mrelease
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        if (!(sender.hasPermission("SimpleMounts.release"))) {
            return false;
        }

        Player player = (Player) sender;
        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);

        try {
            ChatManager chatManager = ServiceLocator.getLocator().getService(ChatManager.class);
            EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);

            if(!entityManager.isSummoned(player)) {
                errorManager.error("Must first have a summoned mount", player);
                return true;
            }
            AbstractHorse h = (AbstractHorse)entityManager.getSummonedMount(player);

            //Correcting to vanilla spawns
            h.setPersistent(true);

            entityManager.removeMount(player);

            //reset stats to normal
            h.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(h.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() / SimpleMounts.getMountConfig().getDouble("attributes.health-modifier"));
            h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() / SimpleMounts.getMountConfig().getDouble("attributes.speed-modifier"));
            h.getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue(h.getAttribute(Attribute.HORSE_JUMP_STRENGTH).getValue() / SimpleMounts.getMountConfig().getDouble("attributes.jump-modifier"));


            chatManager.sendPlayerMessage("Goodbye my friend...",player);
            if(!h.getPassengers().isEmpty()) h.eject();
            player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ANGRY,1.0f,1.0f);

            //Code for walking away
            double pitch = ((player.getLocation().getPitch() + 90) * Math.PI) / 180;
            double yaw  = ((player.getLocation().getYaw() + 90)  * Math.PI) / 180;

            Vector vector = new Vector(Math.sin(pitch) * Math.cos(yaw), Math.cos(pitch), Math.sin(pitch) * Math.sin(yaw));

            h.setVelocity(vector);
        } catch (Throwable e) {
            errorManager.error("Failed to release mount", player,e);
        }

        return true;


    }
}
