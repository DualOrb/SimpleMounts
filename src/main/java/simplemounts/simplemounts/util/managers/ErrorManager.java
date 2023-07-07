package simplemounts.simplemounts.util.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.database.Log;

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
    private final Path errorLogPath;


    /**
     * Initializes the log file if not there
     */
    public ErrorManager() {
        Path path = SimpleMounts.getPluginFolder();

        //Construct log file if not there
        Path.of(String.valueOf(path),"system.log");
        Path.of(String.valueOf(path),"errors.log");

        systemLogPath = Paths.get(String.valueOf(path),"system.log");
        errorLogPath = Paths.get(String.valueOf(path),"errors.log");
    }

    public void log(String msg) {
        writeToLogFile("LOG]" + msg);
    }

    //System level logs
    public void error(String msg) {
        writeToLogFile("SYSTEM]" + msg);
    }

    public void error(String msg, Throwable e) {
        Log log = new Log(msg, "SYSTEM",e);
        writeToLogFile(log);
    }

    //User Errors
    public void error(String msg, Player player) {
        String sysMessage = "PLAYER " + player.getName() + "] " + msg;
        player.sendMessage(ChatColor.RED + msg);
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
        Log log = new Log(msg,"PLAYER: " + player.getName(), e);

        String sysMessage = "PLAYER " + player.getName() + "] " + ChatColor.RED + "CRITICAL: " + msg + "| ERROR-INFO: " + e;
        player.sendMessage(ChatColor.RED + msg);
        player.playSound(player, Sound.ENTITY_GHAST_SCREAM,1.0f,1.0f);
        Bukkit.getLogger().info(sysMessage);

        writeToLogFile(log);

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

    private void writeToLogFile(Log log) {
        try {
            Files.writeString(systemLogPath, log.toString(), CREATE, APPEND);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }




}
