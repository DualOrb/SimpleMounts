package simplemounts.simplemounts.Mounts.commands;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.Util.Managers.EntityManager;
import simplemounts.simplemounts.Util.Managers.ErrorManager;
import simplemounts.simplemounts.Util.Services.ServiceLocator;

import java.io.IOException;

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
        EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);
        entityManager.storeSummonedMount(player);

        return true;
    }
}
