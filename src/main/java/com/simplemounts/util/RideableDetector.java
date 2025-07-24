package com.simplemounts.util;

import com.simplemounts.data.MountType;
import org.bukkit.entity.*;
import org.bukkit.inventory.InventoryHolder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RideableDetector {
    
    private static final Set<EntityType> RIDEABLE_ENTITY_TYPES = new HashSet<>(Arrays.asList(
        EntityType.HORSE,
        EntityType.DONKEY,
        EntityType.MULE,
        EntityType.CAMEL,
        EntityType.STRIDER,
        EntityType.PIG,
        EntityType.LLAMA,
        EntityType.MINECART,
        EntityType.CHEST_MINECART
    ));
    
    public static boolean isRideable(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        return RIDEABLE_ENTITY_TYPES.contains(entity.getType()) || 
               entity instanceof Vehicle ||
               isCustomRideable(entity);
    }
    
    public static boolean isLivingMount(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        return mountType.isLiving();
    }
    
    public static boolean canHaveChest(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        return mountType.canHaveChest();
    }
    
    public static boolean canHaveArmor(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        return mountType.canHaveArmor();
    }
    
    public static boolean canBeTamed(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        return mountType.canBeTamed();
    }
    
    public static boolean needsCustomTaming(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        return mountType.needsCustomTaming();
    }
    
    public static boolean isVehicle(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        return mountType.isVehicle();
    }
    
    public static boolean isWaterSpecific(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        return mountType.isWaterSpecific();
    }
    
    public static boolean isNetherSpecific(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        return mountType.isNetherSpecific();
    }
    
    public static boolean requiresSpecialHandling(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        return mountType.requiresSpecialHandling();
    }
    
    public static boolean isAlreadyTamed(Entity entity) {
        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            return tameable.isTamed();
        }
        
        return false;
    }
    
    public static boolean hasOwner(Entity entity) {
        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            return tameable.getOwner() != null;
        }
        
        return entity.hasMetadata("simplemounts.owner") || 
               entity.hasMetadata("owner");
    }
    
    public static boolean isCustomRideable(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        // Check for custom rideable metadata
        if (entity.hasMetadata("custom_rideable") || 
            entity.hasMetadata("rideable") ||
            entity.hasMetadata("mount")) {
            return true;
        }
        
        // Check if entity implements Vehicle (for modded entities)
        if (entity instanceof Vehicle) {
            return true;
        }
        
        return false;
    }
    
    public static boolean isValidForEnvironment(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        
        switch (entity.getWorld().getEnvironment()) {
            case NETHER:
                return !mountType.isWaterSpecific();
            case THE_END:
                return !mountType.isWaterSpecific() && !mountType.isNetherSpecific();
            case NORMAL:
            default:
                return true;
        }
    }
    
    public static boolean canSurviveInWorld(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        // Striders need lava or special handling in overworld
        if (entity.getType() == EntityType.STRIDER) {
            return entity.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER ||
                   entity.getLocation().getBlock().getType().toString().contains("LAVA");
        }
        
        // Boats need water or special handling
        // Boats are not currently supported in this version
        if (false) {
            return entity.getLocation().getBlock().getType().toString().contains("WATER") ||
                   entity.getLocation().getBlock().getType().toString().equals("AIR");
        }
        
        return true;
    }
    
    public static int getMaxPassengers(Entity entity) {
        if (entity == null) {
            return 0;
        }
        
        switch (entity.getType()) {
            case HORSE:
            case DONKEY:
            case MULE:
            case STRIDER:
            case PIG:
            case LLAMA:
                return 1;
            case CAMEL:
                return 2;
            case MINECART:
            case CHEST_MINECART:
                return 1;
            default:
                return 1;
        }
    }
    
    public static boolean canCarryItems(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        return canHaveChest(entity) || entity instanceof InventoryHolder;
    }
    
    public static String getMountCategory(Entity entity) {
        if (entity == null) {
            return "unknown";
        }
        
        MountType mountType = MountType.fromEntityType(entity.getType());
        
        if (mountType.isLiving()) {
            return "living";
        } else if (mountType.isVehicle()) {
            return "vehicle";
        } else {
            return "unknown";
        }
    }
    
    public static boolean isCompatibleWithPlayer(Entity entity, Player player) {
        if (entity == null || player == null) {
            return false;
        }
        
        // Check if entity is in the same world as player
        if (!entity.getWorld().equals(player.getWorld())) {
            return false;
        }
        
        // Check if entity can survive in current environment
        if (!canSurviveInWorld(entity)) {
            return false;
        }
        
        // Check if entity is already owned by someone else
        if (hasOwner(entity)) {
            if (entity instanceof Tameable) {
                Tameable tameable = (Tameable) entity;
                return player.equals(tameable.getOwner());
            }
            return false;
        }
        
        return true;
    }
}