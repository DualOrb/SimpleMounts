package simplemounts.simplemounts;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Sound;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import simplemounts.simplemounts.Mounts.Handlers.*;
import simplemounts.simplemounts.Mounts.Recipes.WhistleRecipe;
import simplemounts.simplemounts.Mounts.commands.*;
import simplemounts.simplemounts.Mounts.listeners.DistanceListener;
import simplemounts.simplemounts.Util.Database.Database;
import simplemounts.simplemounts.Util.GUI.InteractHandler;
import simplemounts.simplemounts.Util.GUI.ItemManager;
import simplemounts.simplemounts.Util.Managers.EntityManager;
import simplemounts.simplemounts.Util.Managers.ErrorManager;
import simplemounts.simplemounts.Util.Services.ServiceLocator;

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
        EntityManager.despawnAllMounts();
        Bukkit.getLogger().info("[Simple-Mounts] " + "All Mounts Stored");
    }

    private void setup() {

        EntityManager.init();
        Database.init();
        log("Database Initialised");

        ItemManager.setup();    //Initialises all our custom items for the plugin
        new WhistleRecipe();

        log("Items and Recipes Loaded");

        //Load Handlers
        new InteractHandler(this);    //All player events

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

    /**
     * Ensures there is a data folder on the server
     * if not, creates one
     */
    public static File checkPlayerFolder(Player player) {
        String folderName = player.getUniqueId().toString();

        File playerFolder = new File(SimpleMounts.getMountsFolder() + File.separator + folderName);

        if(!playerFolder.exists()) {
            playerFolder.mkdir();
        }
        return playerFolder;
    }

    /////////////////////////////
    //Server Utils
    ////////////////////////////
    private static String prefix = ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "Simple" + ChatColor.GRAY + "Mounts" + ChatColor.DARK_GRAY + "]" + ChatColor.GRAY;

    /**
     * Sends the targetted player a system msg
     * @param msg
     * @param player
     */
    public static void sendPlayerMessage(String msg, Player player) {
        player.sendMessage(prefix + " : " + msg);
    }

    /**
     * Sends an error msg to the player
     *  - Simple errors, user side, such as using the plugin wrong
     */
    public static void sendUserError(String msg, Player player) {
        String sysMessage = prefix + " : " + ChatColor.RED + msg;
        player.sendMessage(sysMessage);
        player.playSound(player, Sound.ENTITY_VILLAGER_NO,1.0f,1.0f);
    }
}