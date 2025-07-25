package com.simplemounts;

import com.simplemounts.commands.MountCommand;
import com.simplemounts.core.ConfigManager;
import com.simplemounts.core.DatabaseManager;
import com.simplemounts.core.MountManager;
import com.simplemounts.core.ShutdownHandler;
import com.simplemounts.gui.GUIListener;
import com.simplemounts.gui.GUIManager;
import com.simplemounts.listeners.CustomTamingListener;
import com.simplemounts.listeners.InventoryListener;
import com.simplemounts.listeners.ItemInteractionListener;
import com.simplemounts.listeners.MountInteractionListener;
import com.simplemounts.listeners.PlayerListener;
import com.simplemounts.listeners.ServerListener;
import com.simplemounts.recipes.RecipeManager;
import com.simplemounts.util.NameValidator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public final class SimpleMounts extends JavaPlugin {
    
    private static SimpleMounts instance;
    
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private MountManager mountManager;
    private ShutdownHandler shutdownHandler;
    private GUIManager guiManager;
    private RecipeManager recipeManager;
    private NameValidator nameValidator;
    
    @Override
    public void onEnable() {
        instance = this;
        
        long startTime = System.currentTimeMillis();
        
        try {
            initializeManagers();
            registerCommands();
            registerListeners();
            registerRecipes();
            registerShutdownHandler();
            scheduleDatabaseMaintenance();
            startDistanceBasedStorage();
            
            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("SimpleMounts v" + getDescription().getVersion() + " enabled in " + loadTime + "ms");
            
        } catch (Exception e) {
            getLogger().severe("Failed to enable SimpleMounts: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            if (shutdownHandler != null) {
                shutdownHandler.handleShutdown();
            }
            
            if (guiManager != null) {
                guiManager.closeAllSessions();
            }
            
            if (recipeManager != null) {
                recipeManager.unregisterRecipes();
            }
            
            if (databaseManager != null) {
                databaseManager.closeConnection();
            }
            
            getLogger().info("SimpleMounts v" + getDescription().getVersion() + " disabled");
            
        } catch (Exception e) {
            getLogger().severe("Error during plugin shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeManagers() {
        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this);
        
        // Initialize InventorySerializer for MountAttributes to use
        com.simplemounts.serialization.InventorySerializer inventorySerializer = 
            new com.simplemounts.serialization.InventorySerializer(this);
        com.simplemounts.data.MountAttributes.setInventorySerializer(inventorySerializer);
        
        mountManager = new MountManager(this);
        shutdownHandler = new ShutdownHandler(this);
        guiManager = new GUIManager(this);
        recipeManager = new RecipeManager(this);
        nameValidator = new NameValidator(this);
        
        if (!databaseManager.initialize()) {
            throw new RuntimeException("Failed to initialize database");
        }
        
        getLogger().info("All managers initialized successfully");
    }
    
    private void registerCommands() {
        MountCommand mountCommand = new MountCommand(this);
        
        getCommand("mount").setExecutor(mountCommand);
        getCommand("mount").setTabCompleter(mountCommand);
        
        getCommand("sm").setExecutor(mountCommand);
        getCommand("sm").setTabCompleter(mountCommand);
        
        getCommand("simplemounts").setExecutor(mountCommand);
        getCommand("simplemounts").setTabCompleter(mountCommand);
        
        getLogger().info("Commands registered successfully");
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomTamingListener(this), this);
        getServer().getPluginManager().registerEvents(new MountInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemInteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerListener(this), this);
        
        getLogger().info("Event listeners registered successfully");
    }
    
    private void registerRecipes() {
        recipeManager.registerRecipes();
        getLogger().info("Recipes registered successfully");
    }
    
    private void registerShutdownHandler() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (shutdownHandler != null) {
                shutdownHandler.handleEmergencyShutdown();
            }
        }));
        
        getLogger().info("Shutdown handler registered successfully");
    }
    
    private void scheduleDatabaseMaintenance() {
        // Schedule database maintenance every 6 hours
        new BukkitRunnable() {
            @Override
            public void run() {
                runAsync(() -> {
                    getLogger().info("Starting scheduled database maintenance...");
                    databaseManager.performMaintenanceCleanup().thenAccept(success -> {
                        if (success) {
                            getLogger().info("Database maintenance completed successfully");
                        } else {
                            getLogger().warning("Database maintenance encountered errors");
                        }
                    });
                    
                    // Log database stats
                    databaseManager.getDatabaseStats();
                });
            }
        }.runTaskTimerAsynchronously(this, 72000L, 432000L); // Start after 1 hour, repeat every 6 hours
        
        getLogger().info("Database maintenance scheduled successfully");
    }
    
    private void startDistanceBasedStorage() {
        // Start distance-based auto storage task
        int checkInterval = configManager.getDistanceStorageCheckInterval();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                // Run distance checks for all online players
                for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                    runAsync(() -> {
                        try {
                            mountManager.checkDistanceStorage(player);
                        } catch (Exception e) {
                            getLogger().log(Level.WARNING, "Error during distance check for " + player.getName(), e);
                        }
                    });
                }
            }
        }.runTaskTimerAsynchronously(this, checkInterval, checkInterval);
        
        getLogger().info("Distance-based storage task started (check interval: " + checkInterval + " ticks)");
    }
    
    public void reloadConfiguration() {
        try {
            configManager.reloadConfig();
            if (nameValidator != null) {
                nameValidator.reloadBlacklist();
            }
            getLogger().info("Configuration reloaded successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to reload configuration", e);
        }
    }
    
    public static SimpleMounts getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public MountManager getMountManager() {
        return mountManager;
    }
    
    public ShutdownHandler getShutdownHandler() {
        return shutdownHandler;
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }
    
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
    
    public NameValidator getNameValidator() {
        return nameValidator;
    }
    
    public void runAsync(Runnable task) {
        new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTaskAsynchronously(this);
    }
    
    public void runSync(Runnable task) {
        new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTask(this);
    }
    
    public void runSyncDelayed(Runnable task, long delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTaskLater(this, delay);
    }
    
    public void runTaskLater(Runnable task, long delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTaskLater(this, delay);
    }
}