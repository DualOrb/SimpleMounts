package simplemounts.simplemounts.Mounts.commands;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.json.simple.JSONObject;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.Util.Database.Mount;
import simplemounts.simplemounts.Util.Managers.ChatManager;
import simplemounts.simplemounts.Util.Managers.EntityManager;
import simplemounts.simplemounts.Util.Managers.ErrorManager;
import simplemounts.simplemounts.Util.Services.ServiceLocator;

import java.io.IOException;
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
        if(!(sender instanceof Player)) return true;
        if(!(sender.hasPermission("SimpleMounts.ClaimMounts"))) {return true;}

        Player player = (Player) sender;

        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
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
        if(mounts.size() >= SimpleMounts.getCustomConfig().getInt("basic.max-mounts")) {
            errorManager.error("You are currently at the max amount of mounts",player);
            return true;
        }

        if(entityManager.getOwningPlayer(horse) != null) {
            if(entityManager.getOwningPlayer(horse).equals(player)) {errorManager.error("You have already claimed this mount",player);return true;}
        }

        try {
            JSONObject json = entityManager.createEntitySave(horse,player);

            horse.remove(); //Remove original horse

            chatManager.sendPlayerMessage("You have tamed a " + horse.getType().toString().toLowerCase() + "!", player);
            player.spawnParticle(Particle.CRIT,player.getLocation(),5);
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST,1.0f,1.0f);

        } catch (Throwable e) {
            //General Exception. Undo all actions
            errorManager.error("Failed to write entity to file", player,e);

        }
        return true;
    }

}
