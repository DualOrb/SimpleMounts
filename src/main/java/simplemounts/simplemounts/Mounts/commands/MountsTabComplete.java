package simplemounts.simplemounts.Mounts.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.Util.Managers.ErrorManager;
import simplemounts.simplemounts.Util.Services.ServiceLocator;

import java.util.ArrayList;
import java.util.List;

public class MountsTabComplete implements TabCompleter {

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
        if(args.length == 1) {  //First tab will be all online players
            List<String> playerNames = new ArrayList<>();
            Player[] players = new Player[Bukkit.getServer().getOnlinePlayers().size()];
            for (int i = 0; i < players.length; i++) {
                playerNames.add(players[i].getName());
            }

            return playerNames;

        } else if (args.length == 2) {
            errorManager.error("Too Many Arguments",(Player)sender);
            return null;
        }

        return null;
    }
}
