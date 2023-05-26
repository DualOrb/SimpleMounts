//package simplemounts.simplemounts.Mounts.Handlers;
//
//import org.bukkit.Bukkit;
//import org.bukkit.Chunk;
//import org.bukkit.entity.AbstractHorse;
//import org.bukkit.entity.Entity;
//import org.bukkit.entity.Player;
//import org.bukkit.event.EventHandler;
//import org.bukkit.event.EventPriority;
//import org.bukkit.event.Listener;
//import org.bukkit.event.world.ChunkUnloadEvent;
//import simplemounts.simplemounts.SimpleMounts;
//import simplemounts.simplemounts.Util.Managers.EntityManager;
//
//import java.util.ArrayList;
//
//public class ChunkDespawnHandler implements Listener {
//
//    public ChunkDespawnHandler(SimpleMounts plugin) {
//        Bukkit.getPluginManager().registerEvents(this, plugin);
//    }
//
//    /**
//     * On a chunk unload, if there is a mount there, then it should be stored back to the player and removed from world
//     * @param event
//     */
//    @EventHandler(priority = EventPriority.MONITOR) //Mounts should always be removed. Event is never cancelled
//    public void onChunkDespawn(ChunkUnloadEvent event) {
//        Chunk chunk = event.getChunk();
//
//        Entity[] entities = chunk.getEntities();
//
//        ArrayList<Entity> summonedMounts = EntityManager.getAllMounts();
//
//        Bukkit.getLogger().info("Unloaded Chunk at X" + chunk.getX() + " Z" + chunk.getZ() + " | Entities in Chunk: " + entities.length);
//
//        //stores the entity if it exists in the despawned chunk
//        for(Entity e: entities) {
//            Bukkit.getLogger().info("Check entity: " + e.getType());
//            for(Entity mount : summonedMounts) {
//                if(!(e instanceof AbstractHorse)) continue;
//                if(e.getUniqueId().equals(mount.getUniqueId())) {
//                    AbstractHorse horse = (AbstractHorse)mount;
//                    Player player = (Player)horse.getOwner();
//
//                    EntityManager.storeSummonedMount(player);
//                }
//
//            }
//        }
//    }
//}
