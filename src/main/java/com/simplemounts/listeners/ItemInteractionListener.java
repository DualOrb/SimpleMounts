package com.simplemounts.listeners;

import com.simplemounts.SimpleMounts;
import com.simplemounts.items.MountWhistle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ItemInteractionListener implements Listener {
    
    private final SimpleMounts plugin;
    private final MountWhistle mountWhistle;
    
    public ItemInteractionListener(SimpleMounts plugin) {
        this.plugin = plugin;
        this.mountWhistle = plugin.getRecipeManager().getMountWhistle();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        
        if (item == null || !mountWhistle.isMountWhistle(item)) {
            return;
        }
        
        // Cancel the default interaction
        event.setCancelled(true);
        
        // Determine if it's a right-click or left-click
        boolean isRightClick = event.getAction() == Action.RIGHT_CLICK_AIR || 
                              event.getAction() == Action.RIGHT_CLICK_BLOCK;
        
        // Handle the whistle use
        mountWhistle.handleWhistleUse(event.getPlayer(), item, isRightClick);
    }
}