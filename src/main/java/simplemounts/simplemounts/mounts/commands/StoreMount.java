package simplemounts.simplemounts.mounts.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class StoreMount implements CommandExecutor {

    /**
     * /mstore
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) return false;
        if(!(sender.hasPermission("SimpleMounts.ClaimMounts"))) {return false;}

        Player player = (Player) sender;
        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);

        try {
            EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);
            entityManager.storeSummonedMount(player);
        } catch (Throwable e) {
            errorManager.error("Unable to store mount", player, e);
        }


        return true;
    }
}
