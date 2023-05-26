package simplemounts.simplemounts.mounts.commands;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import simplemounts.simplemounts.util.managers.ChatManager;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

public class Release implements CommandExecutor {

    /**
     * /mrelease
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        if (!(sender.hasPermission("SimpleMounts.release"))) {
            return false;
        }

        Player player = (Player) sender;

        ErrorManager errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);
        ChatManager chatManager = ServiceLocator.getLocator().getService(ChatManager.class);
        EntityManager entityManager = ServiceLocator.getLocator().getService(EntityManager.class);

        if(!entityManager.isSummoned(player)) {
            errorManager.error("Must first have a summoned mount", player);
            return true;
        }
        AbstractHorse h = (AbstractHorse)entityManager.getSummonedMount(player);

        //Correcting to vanilla spawns
        h.setPersistent(true);

        entityManager.removeMount(player);

        chatManager.sendPlayerMessage("Goodbye my friend...",player);
        player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ANGRY,1.0f,1.0f);

        return true;


    }
}
