package com.simplemounts.data;

import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class MountAttributes {
    
    private final Map<String, Object> attributes;
    
    public MountAttributes() {
        this.attributes = new HashMap<>();
    }
    
    public MountAttributes(Map<String, Object> attributes) {
        this.attributes = new HashMap<>(attributes);
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
            
            if (horse.getInventory().getSaddle() != null) {
                attributes.set("saddle", horse.getInventory().getSaddle());
            }
            
            if (horse instanceof Horse) {
                Horse h = (Horse) horse;
                attributes.set("color", h.getColor().name());
                attributes.set("style", h.getStyle().name());
                if (h.getInventory().getArmor() != null) {
                    attributes.set("armor", h.getInventory().getArmor());
                }
            }
            
            if (horse instanceof ChestedHorse) {
                ChestedHorse chested = (ChestedHorse) horse;
                attributes.set("carryingChest", chested.isCarryingChest());
                if (chested instanceof Llama) {
                    Llama llama = (Llama) chested;
                    attributes.set("strength", llama.getStrength());
                    attributes.set("carpetColor", llama.getCarpetColor() != null ? llama.getCarpetColor().name() : null);
                }
            }
        }
        
        if (entity instanceof Camel) {
            Camel camel = (Camel) entity;
            attributes.set("sitting", camel.isSitting());
            attributes.set("dashAvailable", camel.isDashAvailable());
        }
        
        if (entity instanceof Strider) {
            Strider strider = (Strider) entity;
            attributes.set("shivering", strider.isShivering());
            attributes.set("saddled", strider.isSaddled());
        }
        
        if (entity instanceof Pig) {
            Pig pig = (Pig) entity;
            attributes.set("saddled", pig.hasSaddle());
            attributes.set("boostTime", pig.getBoostTime());
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
            
            if (has("health")) {
                living.setHealth(getDouble("health"));
            }
            
            if (has("maxHealth")) {
                living.setMaxHealth(getDouble("maxHealth"));
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
                horse.getInventory().setSaddle((ItemStack) get("saddle"));
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
                    h.getInventory().setArmor((ItemStack) get("armor"));
                }
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
                    
                    if (has("carpetColor")) {
                        llama.setCarpetColor(org.bukkit.DyeColor.valueOf(getString("carpetColor")));
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
            
            if (has("saddled")) {
                strider.setSaddled(getBoolean("saddled"));
            }
        }
        
        if (entity instanceof Pig) {
            Pig pig = (Pig) entity;
            
            if (has("saddled")) {
                pig.setSaddle(getBoolean("saddled"));
            }
            
            if (has("boostTime")) {
                pig.setBoostTime(getInt("boostTime"));
            }
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
            
            if (has("derailedVelocityMod")) {
                minecart.setDerailedVelocityMod(getDouble("derailedVelocityMod"));
            }
            
            if (has("flyingVelocityMod")) {
                minecart.setFlyingVelocityMod(getDouble("flyingVelocityMod"));
            }
        }
    }
    
    public void set(String key, Object value) {
        attributes.put(key, value);
    }
    
    public Object get(String key) {
        return attributes.get(key);
    }
    
    public boolean has(String key) {
        return attributes.containsKey(key);
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