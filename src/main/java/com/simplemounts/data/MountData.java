package com.simplemounts.data;

import java.util.UUID;

public class MountData {
    
    private final UUID playerUuid;
    private final String mountName;
    private final String mountType;
    private final String mountDataYaml;
    private final String chestInventoryData;
    private final long createdAt;
    private final long lastAccessed;
    
    public MountData(UUID playerUuid, String mountName, String mountType, String mountDataYaml, 
                     String chestInventoryData, long createdAt, long lastAccessed) {
        this.playerUuid = playerUuid;
        this.mountName = mountName;
        this.mountType = mountType;
        this.mountDataYaml = mountDataYaml;
        this.chestInventoryData = chestInventoryData;
        this.createdAt = createdAt;
        this.lastAccessed = lastAccessed;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public String getMountName() {
        return mountName;
    }
    
    public String getMountType() {
        return mountType;
    }
    
    public String getMountDataYaml() {
        return mountDataYaml;
    }
    
    public String getChestInventoryData() {
        return chestInventoryData;
    }
    
    public boolean hasChestInventory() {
        return chestInventoryData != null && !chestInventoryData.isEmpty();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getLastAccessed() {
        return lastAccessed;
    }
    
    public MountType getMountTypeEnum() {
        try {
            return MountType.valueOf(mountType);
        } catch (IllegalArgumentException e) {
            return MountType.UNKNOWN;
        }
    }
    
    @Override
    public String toString() {
        return "MountData{" +
                "playerUuid=" + playerUuid +
                ", mountName='" + mountName + '\'' +
                ", mountType='" + mountType + '\'' +
                ", hasChestInventory=" + hasChestInventory() +
                ", createdAt=" + createdAt +
                ", lastAccessed=" + lastAccessed +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        MountData mountData = (MountData) o;
        
        if (!playerUuid.equals(mountData.playerUuid)) return false;
        return mountName.equals(mountData.mountName);
    }
    
    @Override
    public int hashCode() {
        int result = playerUuid.hashCode();
        result = 31 * result + mountName.hashCode();
        return result;
    }
}