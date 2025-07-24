package com.simplemounts.data;

import com.simplemounts.SimpleMounts;
import com.simplemounts.serialization.InventorySerializer;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class MountAttributes {
    
    private final Map<String, Object> attributes;
    private static InventorySerializer inventorySerializer;
    
    public MountAttributes() {
        this.attributes = new HashMap<>();
    }
    
    public MountAttributes(Map<String, Object> attributes) {
        this.attributes = new HashMap<>(attributes);
    }
    
    // Set the inventory serializer (should be called once during plugin initialization)
    public static void setInventorySerializer(InventorySerializer serializer) {
        inventorySerializer = serializer;
    }
    
    public static MountAttributes fromEntity(Entity entity) {
        MountAttributes attributes = new MountAttributes();
        
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            attributes.set("health", living.getHealth());
            attributes.set("maxHealth", living.getMaxHealth());
            attributes.set("customName", living.getCustomName());
            attributes.set("customNameVisible", living.isCustomNameVisible());
            attributes.set("age", living.getTicksLived());
        }
        
        if (entity instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse) entity;
            attributes.set("jumpStrength", horse.getJumpStrength());
            attributes.set("maxDomestication", horse.getMaxDomestication());
            attributes.set("domestication", horse.getDomestication());
            attributes.set("owner", horse.getOwner() != null ? horse.getOwner().getUniqueId().toString() : null);
            attributes.set("tamed", horse.isTamed());
            
            // Serialize ItemStacks properly instead of storing them directly
            if (horse.getInventory().getSaddle() != null) {
                attributes.setItemStack("saddle", horse.getInventory().getSaddle());
            }
            
            if (horse instanceof Horse) {
                Horse h = (Horse) horse;
                attributes.set("color", h.getColor().name());
                attributes.set("style", h.getStyle().name());
                if (h.getInventory().getArmor() != null) {
                    attributes.setItemStack("armor", h.getInventory().getArmor());
                }
            }
            
            if (horse instanceof ZombieHorse) {
                ZombieHorse zh = (ZombieHorse) horse;
                attributes.set("isZombie", true);
            }
            
            if (horse instanceof SkeletonHorse) {
                SkeletonHorse sh = (SkeletonHorse) horse;
                attributes.set("isSkeleton", true);
            }
            
            if (horse instanceof ChestedHorse) {
                ChestedHorse chested = (ChestedHorse) horse;
                attributes.set("carryingChest", chested.isCarryingChest());
                
                if (chested instanceof Llama) {
                    Llama llama = (Llama) chested;
                    attributes.set("strength", llama.getStrength());
                    
                    // Store carpet as ItemStack from inventory
                    if (llama.getInventory().getDecor() != null) {
                        attributes.setItemStack("carpet", llama.getInventory().getDecor());
                    }
                }
            }
        }
        
        if (entity instanceof Camel) {
            Camel camel = (Camel) entity;
            attributes.set("sitting", camel.isSitting());
            // attributes.set("dashAvailable", camel.isDashAvailable()); // May not be available
        }
        
        if (entity instanceof Strider) {
            Strider strider = (Strider) entity;
            attributes.set("shivering", strider.isShivering());
            // Check if strider has a saddle - improved reflection for Spigot 1.21.5
            try {
                java.lang.reflect.Method isSaddledMethod = strider.getClass().getMethod("isSaddled");
                boolean saddled = (Boolean) isSaddledMethod.invoke(strider);
                attributes.set("saddled", saddled);
            } catch (NoSuchMethodException e) {
                // Try alternative method names
                try {
                    java.lang.reflect.Method hasSaddleMethod = strider.getClass().getMethod("hasSaddle");
                    boolean saddled = (Boolean) hasSaddleMethod.invoke(strider);
                    attributes.set("saddled", saddled);
                } catch (Exception e2) {
                    // Method not available, default to false
                    attributes.set("saddled", false);
                }
            } catch (Exception e) {
                // Method failed, default to false
                attributes.set("saddled", false);
            }
        }
        
        if (entity instanceof Pig) {
            Pig pig = (Pig) entity;
            attributes.set("saddled", pig.hasSaddle());
            // attributes.set("boostTime", pig.getBoostTime()); // May not be available
        }
        
        if (entity instanceof Boat) {
            Boat boat = (Boat) entity;
            attributes.set("boatType", boat.getBoatType().name());
            attributes.set("maxSpeed", boat.getMaxSpeed());
            attributes.set("occupiedDeceleration", boat.getOccupiedDeceleration());
            attributes.set("unoccupiedDeceleration", boat.getUnoccupiedDeceleration());
        }
        
        if (entity instanceof Minecart) {
            Minecart minecart = (Minecart) entity;
            attributes.set("maxSpeed", minecart.getMaxSpeed());
            attributes.set("slowWhenEmpty", minecart.isSlowWhenEmpty());
            attributes.set("derailedVelocityMod", minecart.getDerailedVelocityMod());
            attributes.set("flyingVelocityMod", minecart.getFlyingVelocityMod());
        }
        
        return attributes;
    }
    
    public void applyToEntity(Entity entity) {
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            
            // Set max health first to avoid health validation errors
            if (has("maxHealth")) {
                living.setMaxHealth(getDouble("maxHealth"));
            }
            
            if (has("health")) {
                double health = getDouble("health");
                double maxHealth = living.getMaxHealth();
                // Ensure health doesn't exceed max health
                living.setHealth(Math.min(health, maxHealth));
            }
            
            if (has("customName")) {
                living.setCustomName(getString("customName"));
            }
            
            if (has("customNameVisible")) {
                living.setCustomNameVisible(getBoolean("customNameVisible"));
            }
        }
        
        if (entity instanceof AbstractHorse) {
            AbstractHorse horse = (AbstractHorse) entity;
            
            if (has("jumpStrength")) {
                horse.setJumpStrength(getDouble("jumpStrength"));
            }
            
            if (has("maxDomestication")) {
                horse.setMaxDomestication(getInt("maxDomestication"));
            }
            
            if (has("domestication")) {
                horse.setDomestication(getInt("domestication"));
            }
            
            if (has("tamed")) {
                horse.setTamed(getBoolean("tamed"));
            }
            
            if (has("saddle")) {
                ItemStack saddle = getItemStack("saddle");
                if (saddle != null) {
                    horse.getInventory().setSaddle(saddle);
                }
            }
            
            if (horse instanceof Horse) {
                Horse h = (Horse) horse;
                
                if (has("color")) {
                    h.setColor(Horse.Color.valueOf(getString("color")));
                }
                
                if (has("style")) {
                    h.setStyle(Horse.Style.valueOf(getString("style")));
                }
                
                if (has("armor")) {
                    ItemStack armor = getItemStack("armor");
                    if (armor != null) {
                        h.getInventory().setArmor(armor);
                    }
                }
            }
            
            if (horse instanceof ZombieHorse) {
                ZombieHorse zh = (ZombieHorse) horse;
                // Zombie horses don't support armor in 1.21.5
            }
            
            if (horse instanceof SkeletonHorse) {
                SkeletonHorse sh = (SkeletonHorse) horse;
                // Skeleton horses don't support armor in 1.21.5
            }
            
            if (horse instanceof ChestedHorse) {
                ChestedHorse chested = (ChestedHorse) horse;
                
                if (has("carryingChest")) {
                    chested.setCarryingChest(getBoolean("carryingChest"));
                }
                
                if (chested instanceof Llama) {
                    Llama llama = (Llama) chested;
                    
                    if (has("strength")) {
                        llama.setStrength(getInt("strength"));
                    }
                    
                    // Restore carpet from ItemStack
                    if (has("carpet")) {
                        ItemStack carpet = getItemStack("carpet");
                        if (carpet != null) {
                            llama.getInventory().setDecor(carpet);
                        }
                    }
                }
            }
        }
        
        if (entity instanceof Camel) {
            Camel camel = (Camel) entity;
            
            if (has("sitting")) {
                camel.setSitting(getBoolean("sitting"));
            }
        }
        
        if (entity instanceof Strider) {
            Strider strider = (Strider) entity;
            
            if (has("shivering")) {
                strider.setShivering(getBoolean("shivering"));
            }
            
            // Restore saddled state - improved reflection for Spigot 1.21.5
            if (has("saddled")) {
                try {
                    java.lang.reflect.Method setSaddledMethod = strider.getClass().getMethod("setSaddled", boolean.class);
                    setSaddledMethod.invoke(strider, getBoolean("saddled"));
                } catch (NoSuchMethodException e) {
                    // Try alternative method names
                    try {
                        java.lang.reflect.Method setSaddleMethod = strider.getClass().getMethod("setSaddle", boolean.class);
                        setSaddleMethod.invoke(strider, getBoolean("saddled"));
                    } catch (Exception e2) {
                        // Method not available, ignore
                    }
                } catch (Exception e) {
                    // Method failed, ignore
                }
            }
        }
        
        if (entity instanceof Pig) {
            Pig pig = (Pig) entity;
            
            if (has("saddled")) {
                pig.setSaddle(getBoolean("saddled"));
            }
            
            // if (has("boostTime")) {
            //     pig.setBoostTime(getInt("boostTime"));
            // }
        }
        
        if (entity instanceof Boat) {
            Boat boat = (Boat) entity;
            
            if (has("boatType")) {
                boat.setBoatType(Boat.Type.valueOf(getString("boatType")));
            }
            
            if (has("maxSpeed")) {
                boat.setMaxSpeed(getDouble("maxSpeed"));
            }
            
            if (has("occupiedDeceleration")) {
                boat.setOccupiedDeceleration(getDouble("occupiedDeceleration"));
            }
            
            if (has("unoccupiedDeceleration")) {
                boat.setUnoccupiedDeceleration(getDouble("unoccupiedDeceleration"));
            }
        }
        
        if (entity instanceof Minecart) {
            Minecart minecart = (Minecart) entity;
            
            if (has("maxSpeed")) {
                minecart.setMaxSpeed(getDouble("maxSpeed"));
            }
            
            if (has("slowWhenEmpty")) {
                minecart.setSlowWhenEmpty(getBoolean("slowWhenEmpty"));
            }
            
            // Vector conversion issues - comment out for now
            // if (has("derailedVelocityMod")) {
            //     minecart.setDerailedVelocityMod(getDouble("derailedVelocityMod"));
            // }
            
            // if (has("flyingVelocityMod")) {
            //     minecart.setFlyingVelocityMod(getDouble("flyingVelocityMod"));
            // }
        }
    }
    
    public void set(String key, Object value) {
        attributes.put(key, value);
    }
    
    public void setItemStack(String key, ItemStack item) {
        if (item == null) {
            attributes.remove(key);
            attributes.remove(key + "_serialized");
            return;
        }
        
        if (inventorySerializer != null) {
            try {
                String serialized = inventorySerializer.serializeItemStack(item);
                if (serialized != null) {
                    attributes.put(key + "_serialized", serialized);
                } else {
                    // Fallback to storing item directly if serialization fails
                    attributes.put(key, item);
                }
            } catch (Exception e) {
                // Fallback to storing item directly if serialization fails
                attributes.put(key, item);
            }
        } else {
            // If no serializer available, store directly (should not happen in normal operation)
            attributes.put(key, item);
        }
    }
    
    public ItemStack getItemStack(String key) {
        // First try to get serialized version
        String serializedKey = key + "_serialized";
        if (attributes.containsKey(serializedKey) && inventorySerializer != null) {
            try {
                String serialized = (String) attributes.get(serializedKey);
                if (serialized != null) {
                    return inventorySerializer.deserializeItemStack(serialized);
                }
            } catch (Exception e) {
                // Fall through to direct retrieval
            }
        }
        
        // Fallback to direct ItemStack retrieval (for backwards compatibility)
        Object value = attributes.get(key);
        if (value instanceof ItemStack) {
            return (ItemStack) value;
        }
        
        return null;
    }
    
    public Object get(String key) {
        return attributes.get(key);
    }
    
    public boolean has(String key) {
        return attributes.containsKey(key) || attributes.containsKey(key + "_serialized");
    }
    
    public String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }
    
    public int getInt(String key) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    public double getDouble(String key) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
    
    public boolean getBoolean(String key) {
        Object value = get(key);
        return value instanceof Boolean ? (Boolean) value : false;
    }
    
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }
    
    public void clear() {
        attributes.clear();
    }
    
    public boolean isEmpty() {
        return attributes.isEmpty();
    }
    
    @Override
    public String toString() {
        return "MountAttributes{" +
                "attributes=" + attributes +
                '}';
    }
}