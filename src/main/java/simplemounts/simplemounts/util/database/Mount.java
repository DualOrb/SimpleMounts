package simplemounts.simplemounts.util.database;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.UUID;

public class Mount {

    private UUID ownerId;
    private boolean isSummoned;
    private JSONObject horseData;
    private UUID mountId;
    private UUID entityId;
    private ArrayList<UUID> trusted;

    public Mount(ArrayList<Object> row) {
        trusted = new ArrayList<>();
        ownerId = UUID.fromString((String)row.get(0));
        mountId = UUID.fromString((String)row.get(1));
        if(row.get(2).equals("1")) isSummoned = true; else isSummoned = false;
        if(row.get(3) != null) if(!row.get(3).equals("-")) entityId = UUID.fromString((String)row.get(3));
        JSONParser parser = new JSONParser();
        try {
            horseData = (JSONObject)parser.parse((String)row.get(4)) ;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID getOwner() {
        return ownerId;
    }

    public boolean isSummoned() {
        return isSummoned;
    }

    public JSONObject getHorseData() {
        return horseData;
    }

    public UUID getMountId() {
        return mountId;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String toString() {
        return "OwnerID:"+ownerId+",MountID:"+mountId+",HorseData:"+horseData+",isSummed"+isSummoned+",EntityId:"+entityId;
    }
}
