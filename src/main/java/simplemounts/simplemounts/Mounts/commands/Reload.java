package simplemounts.simplemounts.Mounts.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.Util.Managers.ErrorManager;
import simplemounts.simplemounts.Util.Services.ServiceLocator;

public class Reload implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.hasPermission("SimpleMounts.reload"))) {
            return false;
        }

        SimpleMounts.reloadCustomConfig();
        String s = "Reloaded Simple Mounts Config";

        ErrorManager em = ServiceLocator.getLocator().getService(ErrorManager.class);

        if(!(sender instanceof Player)) {em.error(s);return true;}
        SimpleMounts.sendPlayerMessage(s,(Player)sender);
        return true;

    }
}
