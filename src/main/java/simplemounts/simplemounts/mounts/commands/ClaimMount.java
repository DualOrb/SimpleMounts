package simplemounts.simplemounts.mounts.commands;

import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.json.simple.JSONObject;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.database.Mount;
import simplemounts.simplemounts.util.managers.ChatManager;
import simplemounts.simplemounts.util.managers.EffectManager;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

import java.util.ArrayList;

public class ClaimMount implements CommandExecutor {

    /**
     * /mclaim
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);

        if(!(sender instanceof Player)) return true;
        if(!(sender.hasPermission("SimpleMounts.ClaimMounts"))) {return true;}

        Player player = (Player) sender;
        try {


            ChatManager chatManager = ServiceLocator.getLocator().getService(ChatManager.class);

            //environment checks
            if(!(player.getVehicle() instanceof LivingEntity)) {//Checks if its a living entity the player is riding
                errorManager.error("Must be riding a ridable living entity to claim", player);
                return true;
            }

            LivingEntity le = (LivingEntity)player.getVehicle();

            if(le instanceof Pig || le instanceof Strider) {
                errorManager.error("You really think I'd let you claim this as a mount?", player);
                return true;
            }

            //Code for claiming of mount
            if(!(le instanceof Horse || le instanceof SkeletonHorse || le instanceof ZombieHorse)) {
                errorManager.error("Only entities of type horse, skele horse, and zombie horse, are supported at the moment", player);
                return true;    //temporary till more types
            }

            AbstractHorse horse = (AbstractHorse)le;

            //Current supported mounts
            if(horse.getOwner() == null && !(le instanceof SkeletonHorse) && !(le instanceof ZombieHorse)) {
                errorManager.error("The Horse must be tamed before you can claim it.",player);
                return true;
            }

            EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);

            ArrayList<Mount> mounts = entityManager.getMounts(player);

            //Check if player is already at the max amount of mounts
            if(mounts.size() >= SimpleMounts.getMountConfig().getInt("basic.max-mounts")) {
                errorManager.error("You are currently at the max amount of mounts",player);
                return true;
            }

            if(entityManager.getOwningPlayer(horse) != null) {
                if(entityManager.getOwningPlayer(horse).equals(player)) {errorManager.error("You have already claimed this mount",player);return true;}
            }

            //Now that all tests are done, apply the attribute modifiers
            horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * SimpleMounts.getMountConfig().getDouble("attributes.health-modifier"));
            horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue() * SimpleMounts.getMountConfig().getDouble("attributes.speed-modifier"));
            horse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).setBaseValue(horse.getAttribute(Attribute.HORSE_JUMP_STRENGTH).getValue() * SimpleMounts.getMountConfig().getDouble("attributes.jump-modifier"));

            JSONObject json = entityManager.createEntitySave(horse,player);

            horse.remove(); //Remove original horse

            chatManager.sendPlayerMessage("You have tamed a " + horse.getType().toString().toLowerCase() + "!", player);
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST,1.0f,1.0f);

            EffectManager effectManager = ServiceLocator.getLocator().getService(EffectManager.class);

            effectManager.mountClaimEffect(player);

            errorManager.log(player.getName() + " claimed mount " + json);
        } catch (Throwable e) {
            errorManager.error("Failed to claim mount", player,e);
        }

        return true;
    }

}
