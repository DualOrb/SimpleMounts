package simplemounts.simplemounts.mounts.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class Trust implements CommandExecutor {

    /**
     * /mtrust
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) return true;
        if(!(sender.hasPermission("SimpleMounts.Trust"))) {return true;}

        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
        Player player = (Player)sender;

        if(args.length != 1) {errorManager.error("Invalid number of arguments",player);return true;}

        EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);

        if(entityManager.isSummoned(player)) {errorManager.error("Must have a mount summoned"); return true;}

        if(Bukkit.getPlayer(args[0]) == null) {errorManager.error("Player to be trusted must be online");return true;}

        entityManager.addTrustedPlayer(player, Bukkit.getPlayer(args[0]));

        return true;
    }
}
