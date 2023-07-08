package simplemounts.simplemounts.mounts.commands;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.database.Mount;
import simplemounts.simplemounts.util.managers.ChatManager;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

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
        Player player = (Player)sender;
        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
        if(args.length != 1) {errorManager.error("Must provide a name",(Player)sender); return true;}

        try {
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

            //check profanity
            if(SimpleMounts.getMountConfig().getBoolean("basic.profanity-filtered")) {
                String profanity = chatManager.validateName(name);
                if(profanity != null) {
                    errorManager.error("Profanity Detected",player);
                    errorManager.log(player.getName() + " tried to rename a horse with profanity : " + profanity + " | Name: " + name + "If this was an error, please contact Nicksarmor");
                    return true;}
            }

            for(Mount m: mounts) {
                if(m.isSummoned()) {
                    e.setCustomName(name);
                    e.setCustomNameVisible(true);
                    player.getInventory().removeItem(new ItemStack(Material.NAME_TAG,1));   //remove x1 nametage
                    player.playSound(player, Sound.ITEM_BOOK_PAGE_TURN,2.5f,2.5f);
                    chatManager.sendPlayerMessage("Successfully Renamed Mount",player);
                    errorManager.log(player.getName() + " renamed their mount: " + name);
                    return true;
                }
            }

        } catch (Throwable e) {
            errorManager.error("Unable to rename mount",player,e);

            return false;
        }
        errorManager.error("Mount not found",player);


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
