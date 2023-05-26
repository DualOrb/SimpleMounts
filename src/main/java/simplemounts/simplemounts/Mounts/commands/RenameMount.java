package simplemounts.simplemounts.Mounts.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.Util.Database.Mount;
import simplemounts.simplemounts.Util.Managers.ChatManager;
import simplemounts.simplemounts.Util.Managers.EntityManager;
import simplemounts.simplemounts.Util.Managers.ErrorManager;
import simplemounts.simplemounts.Util.Services.ServiceLocator;

import java.util.ArrayList;

public class RenameMount implements CommandExecutor {

    /**
     * /mrename
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) return false;
        if(!(sender.hasPermission("SimpleMounts.rename"))) return false;
        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
        if(args.length != 1) {errorManager.error("Must provide a name",(Player)sender); return true;}

        Player player = (Player)sender;

        EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);

        Entity e = entityManager.getSummonedMount(player);

        if(e == null) {errorManager.error("Must first summon a mount",player);return true;}

        //Check for nametag in hand
        if(!player.getInventory().getItemInMainHand().getType().equals(Material.NAME_TAG)) {errorManager.error("Must be holding a name tag!",player); return true;}

        ArrayList<Mount> mounts = entityManager.getMounts(player);

        String name = "";
        if(args != null) {
            name = implode(" ", args[0]);
            name = name.replace("&", "§");
            name = name.replace("_", " ");
        }

        //Validate Name chosen
        if(name.length() > 25) {errorManager.error("Name is too long",player);return true;}
        ChatManager chatManager = ServiceLocator.getLocator().getService(ChatManager.class);
        if(!chatManager.validateName(name)) {errorManager.error("Profanity Detected. Try again",player);return true;}

        for(Mount m: mounts) {
            if(m.isSummoned()) {
                e.setCustomName(name);
                e.setCustomNameVisible(true);
                player.getInventory().removeItem(new ItemStack(Material.NAME_TAG,1));   //remove x1 nametage
                player.playSound(player, Sound.ITEM_BOOK_PAGE_TURN,2.5f,2.5f);
                chatManager.sendPlayerMessage("Successfully Renamed Mount",player);
                return true;
            }
        }

        return false;
    }

    public static String implode(String separator, String... data) { //... spread operator.
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < data.length - 1; ++i) {
            if (!data[i].matches(" *")) {
                sb.append(data[i]);
                sb.append(separator);
            }
        }

        sb.append(data[data.length - 1]);
        return sb.toString();
    }
}
