package simplemounts.simplemounts.mounts.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.ChatManager;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class Ride implements CommandExecutor {

    /**
     * /mride
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        if (!(sender.hasPermission("SimpleMounts.Ride"))) {
            return true;
        }

        Player player = (Player) sender;

        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
        EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);

        try {
            Entity entity = entityManager.getSummonedMount(player);


            if(entity == null) {errorManager.error("Must have a mount summoned", player); return true;}

            ChatManager cm = ServiceLocator.getLocator().getService(ChatManager.class);

            entity.teleport(player.getLocation());

            cm.sendPlayerMessage("Yeehawwwwww",player);

            Bukkit.getScheduler().runTaskLater(SimpleMounts.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    entity.addPassenger(player);
                }
            },3L);
        } catch (Throwable e) {
            errorManager.error("Unable to ride mount",player,e);
            entityManager.storeSummonedMount(player);
        }

        return true;
    }
}
