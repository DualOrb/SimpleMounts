package com.simplemounts.listeners;

import com.simplemounts.SimpleMounts;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class ServerListener implements Listener {
    
    private final SimpleMounts plugin;
    
    public ServerListener(SimpleMounts plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        // If our plugin is being disabled, handle shutdown
        if (event.getPlugin().equals(plugin)) {
            plugin.getLogger().info("Plugin disable event detected - storing all mounts");
            if (plugin.getShutdownHandler() != null) {
                plugin.getShutdownHandler().handlePluginDisable();
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldUnload(WorldUnloadEvent event) {
        // Store mounts in the unloading world before it unloads
        String worldName = event.getWorld().getName();
        plugin.getLogger().info("World unload detected: " + worldName + " - storing affected mounts");
        
        if (plugin.getShutdownHandler() != null) {
            plugin.getShutdownHandler().handleWorldUnload(worldName);
        }
    }
}