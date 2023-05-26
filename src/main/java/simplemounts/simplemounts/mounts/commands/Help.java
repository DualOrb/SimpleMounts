package simplemounts.simplemounts.mounts.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Help implements CommandExecutor {

    /**
     * /mhelp
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        if (!(sender.hasPermission("SimpleMounts.help")))return true;

        String helpString = "";
        helpString += ChatColor.DARK_GRAY + "--------" + ChatColor.GOLD + "Simple" + ChatColor.GRAY + "Mounts" + ChatColor.DARK_GRAY + "--------\n";
        helpString += createCommandString("/mounts","Opens the mounts interface");
        helpString += createCommandString("/mclaim","Claims a horse you are riding");
        helpString += createCommandString("/mstore","Stores a horse you currently have summoned");
        helpString += createCommandString("/mrename","Renames a summoned mount. Costs 1x nametag in main hand");

        sender.sendMessage(helpString);

        return true;
    }

    private String createCommandString(String command, String description) {
        return ChatColor.GOLD + command + "  -  " + ChatColor.GRAY + description + "\n";
    }
}