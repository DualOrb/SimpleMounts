package com.simplemounts.data;

import org.bukkit.entity.EntityType;

public enum MountType {
    HORSE(EntityType.HORSE, true, true, false),
    DONKEY(EntityType.DONKEY, true, false, true),
    MULE(EntityType.MULE, true, false, true),
    CAMEL(EntityType.CAMEL, true, false, false),
    STRIDER(EntityType.STRIDER, true, false, false),
    PIG(EntityType.PIG, true, false, false),
    LLAMA(EntityType.LLAMA, true, false, true),
    BOAT(EntityType.BOAT, false, false, false),
    CHEST_BOAT(EntityType.CHEST_BOAT, false, false, true),
    MINECART(EntityType.MINECART, false, false, false),
    CHEST_MINECART(EntityType.CHEST_MINECART, false, false, true),
    UNKNOWN(null, false, false, false);
    
    private final EntityType entityType;
    private final boolean isLiving;
    private final boolean canHaveArmor;
    private final boolean canHaveChest;
    
    MountType(EntityType entityType, boolean isLiving, boolean canHaveArmor, boolean canHaveChest) {
        this.entityType = entityType;
        this.isLiving = isLiving;
        this.canHaveArmor = canHaveArmor;
        this.canHaveChest = canHaveChest;
    }
    
    public EntityType getEntityType() {
        return entityType;
    }
    
    public boolean isLiving() {
        return isLiving;
    }
    
    public boolean canHaveArmor() {
        return canHaveArmor;
    }
    
    public boolean canHaveChest() {
        return canHaveChest;
    }
    
    public static MountType fromEntityType(EntityType entityType) {
        for (MountType mountType : values()) {
            if (mountType.entityType == entityType) {
                return mountType;
            }
        }
        return UNKNOWN;
    }
    
    public static MountType fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
    
    public boolean isValidMountType() {
        return this != UNKNOWN && entityType != null;
    }
    
    public boolean isRideable() {
        return isValidMountType();
    }
    
    public boolean requiresSpecialHandling() {
        return this == STRIDER || this == CAMEL || this == PIG;
    }
    
    public boolean isNetherSpecific() {
        return this == STRIDER;
    }
    
    public boolean isWaterSpecific() {
        return this == BOAT || this == CHEST_BOAT;
    }
    
    public boolean isVehicle() {
        return this == BOAT || this == CHEST_BOAT || this == MINECART || this == CHEST_MINECART;
    }
    
    public boolean canBeTamed() {
        return isLiving() && this != PIG;
    }
    
    public boolean needsCustomTaming() {
        return isLiving();
    }
    
    public String getDisplayName() {
        switch (this) {
            case HORSE: return "Horse";
            case DONKEY: return "Donkey";
            case MULE: return "Mule";
            case CAMEL: return "Camel";
            case STRIDER: return "Strider";
            case PIG: return "Pig";
            case LLAMA: return "Llama";
            case BOAT: return "Boat";
            case CHEST_BOAT: return "Chest Boat";
            case MINECART: return "Minecart";
            case CHEST_MINECART: return "Chest Minecart";
            default: return "Unknown";
        }
    }
    
    public String getConfigKey() {
        return name().toLowerCase();
    }
}