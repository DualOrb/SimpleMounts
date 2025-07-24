package com.simplemounts.gui;

import org.bukkit.entity.Player;

public class GUISession {
    
    private final Player player;
    private final GUIManager.GUIType type;
    private final int page;
    private Integer selectedMountId;
    private String action;
    private long lastInteraction;
    
    public GUISession(Player player, GUIManager.GUIType type, int page) {
        this.player = player;
        this.type = type;
        this.page = page;
        this.lastInteraction = System.currentTimeMillis();
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public GUIManager.GUIType getType() {
        return type;
    }
    
    public int getPage() {
        return page;
    }
    
    public Integer getSelectedMountId() {
        return selectedMountId;
    }
    
    public void setSelectedMountId(Integer selectedMountId) {
        this.selectedMountId = selectedMountId;
        updateLastInteraction();
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
        updateLastInteraction();
    }
    
    public long getLastInteraction() {
        return lastInteraction;
    }
    
    public void updateLastInteraction() {
        this.lastInteraction = System.currentTimeMillis();
    }
    
    public boolean isExpired(long timeoutMs) {
        return System.currentTimeMillis() - lastInteraction > timeoutMs;
    }
    
    @Override
    public String toString() {
        return "GUISession{" +
                "player=" + player.getName() +
                ", type=" + type +
                ", page=" + page +
                ", selectedMountId=" + selectedMountId +
                ", action='" + action + '\'' +
                ", lastInteraction=" + lastInteraction +
                '}';
    }
}