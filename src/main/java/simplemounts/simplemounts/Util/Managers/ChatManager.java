package simplemounts.simplemounts.Util.Managers;

import org.bukkit.entity.Player;
import simplemounts.simplemounts.SimpleMounts;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChatManager {

    private static List<String> badWords = null;

    public ChatManager() {
        if(badWords != null) return;

        badWords = new ArrayList<>();

        //Load bad words into a list, so we can later validate
        InputStreamReader isr = new InputStreamReader(SimpleMounts.getPlugin().getResource("bad-words.txt"));

        badWords = new BufferedReader(isr).lines().collect(Collectors.toList());

    }

    public boolean validateName(String name) {
        for(String s: badWords) {
            if(name.toLowerCase().contains(s)) return false;
        }
        return true;
    }

    public void sendPlayerMessage(String msg, Player player) {

    }
}
