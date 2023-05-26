package simplemounts.simplemounts.util.database;

import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import simplemounts.simplemounts.SimpleMounts;
import simplemounts.simplemounts.util.managers.ErrorManager;
import simplemounts.simplemounts.util.services.ServiceLocator;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

/**
 *
 */
public class Database {

    private static Connection conn = null;
    private static ErrorManager errorManager;

    /**
     * Initialises the database files needed on startup
     * player_id - The player's UUID
     * mount_id - A UUID the plugin creates to manage all mounts, instanced or not
     * horse_data - the json data of the specified mount
     * isSummoned - if the mount is currently in the overworld
     * entity_id - If summoned, the summoned entity's id in the world
     */
    public Database() {
        errorManager = ServiceLocator.getLocator().getService(ErrorManager.class);

        //Initial Connection
        try {
            final String url = "jdbc:sqlite:" + SimpleMounts.getMountsFolder() + File.separator + "mounts.db";

            conn = DriverManager.getConnection(url);

            errorManager.error("Database Connection established");

            //Setting up table
            String sql = "CREATE TABLE IF NOT EXISTS mounts (\n"
                    + "player_id varchar(255),\n"
                    + "mount_id varchar(255),\n"
                    + "isSummoned integer NOT NULL,\n"
                    + "entity_id varchar(255),\n"   //May be null
                    + "horse_data text NOT NULL,\n"
                    + "trusted_players text ,\n"    //JSON ARRAY
                    + "CONSTRAINT Pk_Mount PRIMARY KEY (player_id,mount_id)"
                    + ");";
            Statement statement = conn.createStatement();
            statement.execute(sql);
        } catch (SQLException e) {
            errorManager.error("Unable to establish Database Connection: " + e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Inserts a specified mount into the table
     * @param player
     * @param horse
     * @return
     */
    public UUID insertNewMount(Player player, JSONObject horse) {
        UUID uuid = UUID.randomUUID();
        String sql = "INSERT INTO mounts(player_id,mount_id,isSummoned,horse_data) VALUES(?,?,?,?)";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,player.getUniqueId().toString());
            pstmt.setString(2,uuid.toString());
            pstmt.setInt(3,0);
            pstmt.setString(4,horse.toJSONString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return uuid;
    }

    /**
     * Updates an existing mounts details
     * @param player
     * @param mountId
     * @param field
     * @param obj
     */
    public void updateMount(Player player, UUID mountId, String field, Object obj) {

        String sql = "UPDATE mounts SET " + field + " = ? "
                + "WHERE mount_id = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            if(obj instanceof JSONObject || obj instanceof String) {
                pstmt.setString(1,obj.toString());
            } else if(obj instanceof UUID) {
                pstmt.setString(1,((UUID)obj).toString());
            }else if(obj == null) {
                pstmt.setString(1,"-");
            } else {
                //Must be the integer for summoned
                pstmt.setInt(1,(int)obj);
            }
            pstmt.setString(2,mountId.toString());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isSummoned(Player player, UUID mountId) {
        String sql = "SELECT isSummoned FROM mounts WHERE player_id = ? && mount_id = ?";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,player.getUniqueId().toString());
            pstmt.setString(2,mountId.toString());

            ResultSet rs = pstmt.executeQuery();

            //Should only ever be 1 result
            rs.next();
            String s = rs.getString(1);

            if(s.equals("1")) return true;
            else return false;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Query for returning all mounts of a specific player
     * @param player
     * @return
     */
    public ArrayList<JSONObject> getEntities(Player player) {
        ArrayList<JSONObject> entities = new ArrayList<>();

        String sql = "SELECT horse_data FROM mounts WHERE player_id = ?";

        ResultSet rs;
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,player.getUniqueId().toString());
            rs = pstmt.executeQuery();

            while(rs.next()) {
                try {
                    entities.add((JSONObject)new JSONParser().parse(rs.getString(1)));
                } catch (ParseException e) {

                    errorManager.error("Failed to load x1 mount from DB: ",player,e);
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return entities;
    }

    public ArrayList<Mount> getMounts(Player player) {
        ArrayList<Mount> mounts = new ArrayList<>();

        String sql = "SELECT * FROM mounts WHERE player_id = ?";

        ResultSet rs;
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,player.getUniqueId().toString());
            rs = pstmt.executeQuery();

            while(rs.next()) {
                ArrayList<Object> o = new ArrayList<>();
                o.add(rs.getString(1));
                o.add(rs.getString(2));
                o.add(rs.getString(3));
                o.add(rs.getString(4));
                o.add(rs.getString(5));

                mounts.add(new Mount(o));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return mounts;
    }

    /**
     * Removes the mount from the database
     * @param player
     * @param uuid
     */
    public void removeMount(Player player, UUID uuid) {
        String sql = "DELETE FROM mounts WHERE player_id = ? AND mount_id = ?";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,player.getUniqueId().toString());
            pstmt.setString(2,uuid.toString());

            pstmt.executeUpdate();
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a player to the "trusted" field in the database. Stored as a JSON ARRAY
     * @param mountId
     * @param trusted
     */
    public void addTrustedPlayer(UUID mountId, Player trusted) {
        String sql = "SELECT trusted_players FROM mounts WHERE mount_id = ?";

        ResultSet rs;
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1,mountId.toString());

            rs = pstmt.executeQuery();

            rs.next();
            String s = rs.getString(1);

            JSONArray arr;
            if(s == null) {
                arr = new JSONArray();
                arr.add(trusted.getUniqueId().toString());
            } else {
                JSONParser parser = new JSONParser();
                parser.parse(s);        //Will need to add a key to be able to get to the json array if done this way

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }


    }
}
