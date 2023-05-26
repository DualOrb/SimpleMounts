package simplemounts.simplemounts.util.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import simplemounts.simplemounts.SimpleMounts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Handles and manages log files pertaining to the plugin
 */
public class ErrorManager {

    private final Path systemLogPath;
    private final String filename = "system.log";

    /**
     * Initializes the log file if not there
     */
    public ErrorManager() {
        Path path = SimpleMounts.getPluginFolder();

        //Construct log file if not there
        Path.of(String.valueOf(path),filename);

        systemLogPath = Paths.get(String.valueOf(path),filename);
    }

    public void log(String msg) {
        writeToLogFile("LOG]" + msg);
    }

    //System level logs
    public void error(String msg) {
        writeToLogFile("SYSTEM]" + msg);
    }

    //User Errors
    public void error(String msg, Player player) {
        String sysMessage = "PLAYER " + player.getName() + "] " + msg;
        player.sendMessage(ChatColor.RED + msg);
        player.playSound(player, Sound.ENTITY_HORSE_DEATH,1.0f,1.0f);
        player.playSound(player, Sound.ENTITY_GHAST_SCREAM,1.0f,1.0f);
        Bukkit.getLogger().info(sysMessage);

        writeToLogFile(sysMessage);
    }

    /**
     * Sends an error message to the player, plays a sound, logs to console, and writes to log file
     * @param msg
     * @param player
     * @param e
     */
    public void error(String msg, Player player, Throwable e) {
        String sysMessage = "PLAYER " + player.getName() + "] " + ChatColor.RED + "CRITICAL: " + msg + "| ERROR-INFO: " + e;
        player.sendMessage(ChatColor.RED + msg);
        player.playSound(player, Sound.ENTITY_HORSE_DEATH,1.0f,1.0f);
        player.playSound(player, Sound.ENTITY_GHAST_SCREAM,1.0f,1.0f);
        Bukkit.getLogger().info(sysMessage);

        writeToLogFile(msg);

    }

    private void writeToLogFile(String msg) {
        //Prepare log string
        String s = "[" + Instant.now().getEpochSecond() + "]" + "[Source:" + msg;

        try {
            Files.writeString(systemLogPath, s + System.lineSeparator(), CREATE, APPEND);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }




}
