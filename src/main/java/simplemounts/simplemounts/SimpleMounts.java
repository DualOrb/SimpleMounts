package simplemounts.simplemounts;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import simplemounts.simplemounts.mounts.handlers.*;
import simplemounts.simplemounts.mounts.recipes.WhistleRecipe;
import simplemounts.simplemounts.mounts.commands.*;
import simplemounts.simplemounts.mounts.listeners.DistanceListener;
import simplemounts.simplemounts.util.database.Database;
import simplemounts.simplemounts.util.gui.GUIBuilder;
import simplemounts.simplemounts.util.gui.GUIHandler;
import simplemounts.simplemounts.util.gui.ItemManager;
import simplemounts.simplemounts.util.managers.ChatManager;
import simplemounts.simplemounts.util.managers.EntityManager;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

import java.io.File;
import java.io.IOException;

public final class SimpleMounts extends JavaPlugin {

    private static Plugin plugin;   //This plugin
    private static File pluginFolder; //Root file path
    private static File mountsFolder; //file path for mounts

    private static File customConfigFile;
    private static FileConfiguration customConfig;

    private static ServiceLocator serviceLocator;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        pluginFolder = getDataFolder();
        mountsFolder = new File(pluginFolder + File.separator + "playerData");
        createDataFolders();

        //Load conf file
        createCustomConfig();

        //Register services
        serviceLocator = ServiceLocator.getLocator();
        serviceLocator.registerService(ErrorManager.class, new ErrorManager());
        serviceLocator.registerService(ChatManager.class, new ChatManager());
        serviceLocator.registerService(EntityManager.class, new EntityManager());
        serviceLocator.registerService(Database.class, new Database());
        serviceLocator.registerService(GUIBuilder.class, new GUIBuilder());
        serviceLocator.registerService(ItemManager.class, new ItemManager());

        //Check Dependencies
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {Bukkit.getLogger().info("[Simple-Mounts] " + "Server is missing hard dependency: PlaceholderAPI");}

        log("Successfully loaded all dependencies");

        log("Initialising data...");

        setup();


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getLogger().info("[Simple-Mounts] " + "Shutting Down");
        EntityManager em = ServiceLocator.getLocator().getService(EntityManager.class);
        em.despawnAllMounts();
        Bukkit.getLogger().info("[Simple-Mounts] " + "All Mounts Stored");
    }

    private void setup() {

        new WhistleRecipe();

        log("Items and Recipes Loaded");

        //Load Handlers
        new GUIHandler(this);    //All player events

        log("Handlers Loaded");
        new DeathHandler(this);
        new LogoutHandler(this);
        new BreedHandler(this);
        new RidingHandler(this);
        new SummonHandler(this);
        new EntityInteractHandler(this);
        new TeleportHandler(this);

        new DistanceListener(this);
        //new ChunkDespawnHandler(this);

        //Register Commands
        this.getCommand("mounts").setExecutor(new OpenMounts());
        this.getCommand("mclaim").setExecutor(new ClaimMount());
        this.getCommand("mstore").setExecutor(new StoreMount());
        this.getCommand("mrename").setExecutor(new RenameMount());
        this.getCommand("mhelp").setExecutor(new Help());
        this.getCommand("mreload").setExecutor(new Reload());
        this.getCommand("mride").setExecutor(new Ride());
        this.getCommand("mrelease").setExecutor(new Release());
//        this.getCommand("RPGCard").setTabCompleter(new CharacterCardTabComplete());
        log("Commands Loaded");

        log("GUIs Loaded");
    }

    private void log(String log) {
        Bukkit.getLogger().info("[Simple-Mounts] " + log);
    }

    //If config file is not there, create a config file
    public void createCustomConfig() {
        customConfigFile = new File(pluginFolder, "config.yml");
        if(!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource("config.yml",false);
        }

        customConfig = new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch(IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static void reloadCustomConfig() {
        customConfigFile = new File(pluginFolder, "config.yml");
        customConfig = new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch(IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static FileConfiguration getCustomConfig() {
        return customConfig;
    }


    private void createDataFolders() {
        if(!pluginFolder.exists()) {
            pluginFolder.mkdir();
        }
        if(!mountsFolder.exists()) {
            mountsFolder.mkdir();
        }
    }

    public static Plugin getPlugin() {
        return plugin;
    }

    public static File getMountsFolder() {
        return mountsFolder;
    }

    public static File getPluginFolder() {return pluginFolder; }





}