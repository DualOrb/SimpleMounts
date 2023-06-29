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
import simplemounts.simplemounts.util.managers.*;
import simplemounts.simplemounts.util.services.ServiceLocator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SimpleMounts extends JavaPlugin {

    private static Plugin plugin;   //This plugin
    private static Path pluginFolder; //Root file path
    private static Path mountsFolder; //file path for mounts

    private static File mountConfigFile;
    private static FileConfiguration mountConfig;

    private static ServiceLocator serviceLocator;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;
        pluginFolder = Path.of(getDataFolder().getPath());
        try {
            Files.createDirectories(Path.of(pluginFolder + "/" + "playerData"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mountsFolder = Path.of(String.valueOf(pluginFolder),"playerData");

        //Load conf file
        createMountConfig();

        //Register services
        serviceLocator = ServiceLocator.getLocator();
        serviceLocator.registerService(ErrorManager.class, new ErrorManager());
        serviceLocator.registerService(ChatManager.class, new ChatManager());

        serviceLocator.registerService(Database.class, new Database());
        serviceLocator.registerService(EntityManager.class, new EntityManager());

        serviceLocator.registerService(ItemManager.class, new ItemManager());
        serviceLocator.registerService(GUIBuilder.class, new GUIBuilder());
        serviceLocator.registerService(ItemStackSerializer.class, new ItemStackSerializer());
        serviceLocator.registerService(EffectManager.class, new EffectManager());


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
        new MountDamageHandler(this);
        new PortalHandler(this);
        new EntitiesUnloadHandler(this);
        new DismountHandler(this);

        new DistanceListener(this);
        //new ChunkUnloadHandler(this);

        //Register Commands
        this.getCommand("mounts").setExecutor(new OpenMounts());
        this.getCommand("mclaim").setExecutor(new ClaimMount());
        this.getCommand("mstore").setExecutor(new StoreMount());
        this.getCommand("mrename").setExecutor(new RenameMount());
        this.getCommand("mhelp").setExecutor(new Help());
        this.getCommand("mreload").setExecutor(new Reload());
        this.getCommand("mride").setExecutor(new Ride());
        this.getCommand("mrelease").setExecutor(new Release());
        this.getCommand("mtrust").setExecutor(new Trust());
        this.getCommand("muntrust").setExecutor(new Untrust());
//        this.getCommand("RPGCard").setTabCompleter(new CharacterCardTabComplete());
        log("Commands Loaded");

        log("GUIs Loaded");
    }

    private void log(String log) {
        Bukkit.getLogger().info("[Simple-Mounts] " + log);
    }

    //If config file is not there, create a config file
    public void createMountConfig() {

        mountConfigFile = new File(String.valueOf(pluginFolder), "config.yml");
        if(!mountConfigFile.exists()) {
            mountConfigFile.getParentFile().mkdirs();
            saveResource("config.yml",false);
        }

        mountConfig = new YamlConfiguration();
        try {
            mountConfig.load(mountConfigFile);
        } catch(IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static void reloadCustomConfig() {

        mountConfigFile = new File(String.valueOf(pluginFolder), "config.yml");
        mountConfig = new YamlConfiguration();
        try {
            mountConfig.load(mountConfigFile);
        } catch(IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static FileConfiguration getMountConfig() {
        return mountConfig;
    }

    public static Plugin getPlugin() {
        return plugin;
    }

    public static Path getMountsFolder() {
        return mountsFolder;
    }

    public static Path getPluginFolder() {return pluginFolder; }

}