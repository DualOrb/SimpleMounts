package simplemounts.simplemounts.util.services;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

/**
 * A class for
 *  a. establishing a connection with the managing web server
 *  b. send periodic updates on how the plugin is functioning
 *  c. send errors as they occur to track issues
 *
 *  One way communication only
 */
public class ServerCommunicator {

    private enum messageType {INITIAL, UPDATE, ERROR};

    private Plugin plugin;
    private final String SERVER_ADDRESS = "192.168.0.1";

    /**
     * Initialise connection to the web server and schedule the updates
     * @param plugin
     */
    public ServerCommunicator(Plugin plugin) throws IOException {
        this.plugin = plugin;
        String ipAddress = plugin.getServer().getIp();

//        //Establish Connection
//        URL url = new URL("www.simplemounts.com/admin-panel");
//        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
//        urlConnection.setRequestMethod("POST");
//        urlConnection.setRequestProperty("Cache-Control", "no-cache");
//        urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
//        urlConnection.setRequestProperty("Accept-Encoding", "gzip,deflate");
//        urlConnection.setRequestProperty("Accept", "*/*");
//
//        urlConnection.setDoOutput(true);
//
//        //Map<String, Object> params
//        JSONObject json = new JSONObject(params);
//        String body = json.toString();
//        urlConnection.setFixedLengthStreamingMode(body.length());
//
//        try {
//            OutputStream os = urlConnection.getOutputStream();
//            os.write(body.getBytes("UTF-8"));
//            os.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//
//        } finally {
//            int status = urlConnection.getResponseCode();
//            String connectionMsg = urlConnection.getResponseMessage();
//            urlConnection.disconnect();
//
//            if (status != HttpURLConnection.HTTP_OK) {
//                throw new IOException("Post failed with error code " + status);
//            }
//        }
//
//        //Schedule regular updates to web server
//        new BukkitRunnable() {
//            public void run() {
//
//                }
//            }
//        }.runTaskTimer(plugin,100L,100L);
//    }

//    /**
//     * A generic update with information to send to the main web server
//     * @return
//     */
//    private JSONObject updateServer() {
//        return null;
//    }
//
//    private void sendError(Throwable e) {
//
    }


}
