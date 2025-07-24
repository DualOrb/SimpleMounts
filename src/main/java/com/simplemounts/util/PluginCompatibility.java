package com.simplemounts.util;

import com.simplemounts.SimpleMounts;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class PluginCompatibility {
    
    private final SimpleMounts plugin;
    private final Map<String, Boolean> pluginCache;
    
    public PluginCompatibility(SimpleMounts plugin) {
        this.plugin = plugin;
        this.pluginCache = new HashMap<>();
    }
    
    public boolean isPluginEnabled(String pluginName) {
        return pluginCache.computeIfAbsent(pluginName, name -> {
            PluginManager pm = plugin.getServer().getPluginManager();
            Plugin targetPlugin = pm.getPlugin(name);
            return targetPlugin != null && targetPlugin.isEnabled();
        });
    }
    
    public void checkConflictingPlugins() {
        if (!plugin.getConfigManager().checkConflictsOnStartup()) {
            return;
        }
        
        String[] potentialConflicts = {
            "Horses", "HorseTPWithMe", "UltimateHorse", "HorseTweaks",
            "EquestrianAddons", "HorseStats", "HorseInfo", "HorseFeed"
        };
        
        for (String conflictPlugin : potentialConflicts) {
            if (isPluginEnabled(conflictPlugin)) {
                plugin.getLogger().warning("Potential conflict detected with plugin: " + conflictPlugin);
                plugin.getLogger().warning("Please check compatibility settings in config.yml");
            }
        }
    }
    
    public boolean isEntityProtected(Entity entity, Player player) {
        if (!plugin.getConfigManager().respectProtectionPlugins()) {
            return false;
        }
        
        // Check Citizens NPCs
        if (plugin.getConfigManager().ignoreNpcs() && isPluginEnabled("Citizens")) {
            if (entity.hasMetadata("NPC")) {
                return true;
            }
        }
        
        // Check MythicMobs
        if (plugin.getConfigManager().ignoreMythicMobs() && isPluginEnabled("MythicMobs")) {
            if (entity.hasMetadata("MythicMob")) {
                return true;
            }
        }
        
        // Check WorldGuard
        if (isPluginEnabled("WorldGuard")) {
            if (checkWorldGuardProtection(entity, player)) {
                return true;
            }
        }
        
        // Check GriefPrevention
        if (isPluginEnabled("GriefPrevention")) {
            if (checkGriefPreventionProtection(entity, player)) {
                return true;
            }
        }
        
        // Check Towny
        if (isPluginEnabled("Towny")) {
            if (checkTownyProtection(entity, player)) {
                return true;
            }
        }
        
        // Check Factions
        if (isPluginEnabled("Factions")) {
            if (checkFactionsProtection(entity, player)) {
                return true;
            }
        }
        
        // Check Lands
        if (isPluginEnabled("Lands")) {
            if (checkLandsProtection(entity, player)) {
                return true;
            }
        }
        
        // Check ClaimChunk
        if (isPluginEnabled("ClaimChunk")) {
            if (checkClaimChunkProtection(entity, player)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean checkWorldGuardProtection(Entity entity, Player player) {
        try {
            // This would require WorldGuard API integration
            // For now, we'll use a simple metadata check
            if (entity.hasMetadata("worldguard.protected")) {
                return true;
            }
            
            // Check for region flags if WorldGuard API is available
            // This is a placeholder - actual implementation would require WorldGuard dependency
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking WorldGuard protection", e);
            return false;
        }
    }
    
    private boolean checkGriefPreventionProtection(Entity entity, Player player) {
        try {
            // This would require GriefPrevention API integration
            // For now, we'll use a simple metadata check
            if (entity.hasMetadata("griefprevention.protected")) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking GriefPrevention protection", e);
            return false;
        }
    }
    
    private boolean checkTownyProtection(Entity entity, Player player) {
        try {
            // This would require Towny API integration
            if (entity.hasMetadata("towny.protected")) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking Towny protection", e);
            return false;
        }
    }
    
    private boolean checkFactionsProtection(Entity entity, Player player) {
        try {
            // This would require Factions API integration
            if (entity.hasMetadata("factions.protected")) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking Factions protection", e);
            return false;
        }
    }
    
    private boolean checkLandsProtection(Entity entity, Player player) {
        try {
            // This would require Lands API integration
            if (entity.hasMetadata("lands.protected")) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking Lands protection", e);
            return false;
        }
    }
    
    private boolean checkClaimChunkProtection(Entity entity, Player player) {
        try {
            // This would require ClaimChunk API integration
            if (entity.hasMetadata("claimchunk.protected")) {
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking ClaimChunk protection", e);
            return false;
        }
    }
    
    public boolean hasEconomySupport() {
        return isPluginEnabled("Vault") && isPluginEnabled("Economy");
    }
    
    public boolean canPlayerAffordTaming(Player player, double cost) {
        if (!hasEconomySupport()) {
            return true; // No economy, so free
        }
        
        try {
            // This would require Vault API integration
            // For now, we'll assume they can afford it
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking player economy", e);
            return true;
        }
    }
    
    public boolean chargePlayerForTaming(Player player, double cost) {
        if (!hasEconomySupport() || cost <= 0) {
            return true;
        }
        
        try {
            // This would require Vault API integration
            // For now, we'll assume success
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error charging player for taming", e);
            return false;
        }
    }
    
    public void logCompatibilityInfo() {
        plugin.getLogger().info("Checking plugin compatibility...");
        
        String[] supportedPlugins = {
            "WorldGuard", "GriefPrevention", "Towny", "Factions", "Lands", 
            "ClaimChunk", "Citizens", "MythicMobs", "Vault", "Essentials"
        };
        
        for (String pluginName : supportedPlugins) {
            if (isPluginEnabled(pluginName)) {
                plugin.getLogger().info("Found compatible plugin: " + pluginName);
            }
        }
    }
    
    public boolean isAntiCheatFriendly() {
        // Check for common anti-cheat plugins and ensure our teleportation is whitelisted
        String[] antiCheatPlugins = {
            "NoCheatPlus", "AAC", "AntiCheatReloaded", "Matrix", "Vulcan", "Spartan"
        };
        
        for (String antiCheat : antiCheatPlugins) {
            if (isPluginEnabled(antiCheat)) {
                plugin.getLogger().info("Anti-cheat plugin detected: " + antiCheat);
                plugin.getLogger().info("Mount teleportation may need to be whitelisted");
                return false;
            }
        }
        
        return true;
    }
    
    public void clearCache() {
        pluginCache.clear();
    }
    
    public void reloadCompatibility() {
        clearCache();
        checkConflictingPlugins();
        logCompatibilityInfo();
        
        if (!isAntiCheatFriendly()) {
            plugin.getLogger().warning("Anti-cheat compatibility issues detected");
            plugin.getLogger().warning("Please whitelist mount teleportation in your anti-cheat configuration");
        }
    }
}