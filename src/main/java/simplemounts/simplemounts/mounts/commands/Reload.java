package simplemounts.simplemounts.mounts.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.ChatManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class Reload implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.hasPermission("SimpleMounts.reload"))) {
            return false;
        }

        SimpleMounts.reloadCustomConfig();
        String s = "Reloaded Simple Mounts Config";

        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
        ChatManager chatManager = ServiceLocator.getLocator().getService(ChatManager.class);

        if(!(sender instanceof Player)) {errorManager.error(s);return true;}
        chatManager.sendPlayerMessage(s,(Player)sender);
        return true;

    }
}
