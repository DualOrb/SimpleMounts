package com.simplemounts.util;

import com.simplemounts.SimpleMounts;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class NameValidator {
    
    private final SimpleMounts plugin;
    private final Set<String> blacklistedNames;
    private final File blacklistFile;
    
    // Pattern to match color codes and formatting codes
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(?i)&[0-9A-FK-OR]");
    // Pattern to match special characters (keep only letters, numbers, spaces, hyphens, underscores)
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s\\-_]");
    
    public NameValidator(SimpleMounts plugin) {
        this.plugin = plugin;
        this.blacklistedNames = new HashSet<>();
        this.blacklistFile = new File(plugin.getDataFolder(), "name-blacklist.txt");
        
        loadBlacklist();
    }
    
    /**
     * Validates and sanitizes a mount name
     * @param rawName The raw name input from player
     * @return The sanitized name, or null if invalid/blacklisted
     */
    public String validateAndSanitizeName(String rawName) {
        if (rawName == null) {
            return null; // Allow null for unnamed mounts
        }
        
        // Strip color codes and special characters
        String sanitizedName = sanitizeName(rawName);
        
        // Check if empty after sanitization
        if (sanitizedName.trim().isEmpty()) {
            return null;
        }
        
        // Check blacklist (case insensitive)
        if (isBlacklisted(sanitizedName)) {
            return null;
        }
        
        // Check length limits
        if (!isValidLength(sanitizedName)) {
            return null;
        }
        
        return sanitizedName.trim();
    }
    
    /**
     * Strips color codes and special characters from name
     * @param rawName The raw name input
     * @return Sanitized name with only allowed characters
     */
    public String sanitizeName(String rawName) {
        if (rawName == null) {
            return null;
        }
        
        // First strip color codes (e.g., &7, &l, &n, etc.)
        String withoutColors = COLOR_CODE_PATTERN.matcher(rawName).replaceAll("");
        
        // Also strip ChatColor codes that might have been translated
        String withoutChatColors = ChatColor.stripColor(withoutColors);
        
        // Remove special characters (keep only letters, numbers, spaces, hyphens, underscores)
        String sanitized = SPECIAL_CHARS_PATTERN.matcher(withoutChatColors != null ? withoutChatColors : withoutColors).replaceAll("");
        
        // Normalize whitespace (replace multiple spaces with single space)
        sanitized = sanitized.replaceAll("\\s+", " ");
        
        return sanitized;
    }
    
    /**
     * Check if a name is blacklisted (case insensitive)
     */
    public boolean isBlacklisted(String name) {
        if (name == null) {
            return false;
        }
        
        String lowerName = name.toLowerCase().trim();
        
        // Check exact matches
        if (blacklistedNames.contains(lowerName)) {
            return true;
        }
        
        // Check if name contains any blacklisted words
        for (String blacklisted : blacklistedNames) {
            if (lowerName.contains(blacklisted)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if name meets length requirements
     */
    private boolean isValidLength(String name) {
        if (name == null) {
            return true; // Null is allowed for unnamed mounts
        }
        
        int length = name.trim().length();
        return length >= plugin.getConfigManager().getMinNameLength() && 
               length <= plugin.getConfigManager().getMaxNameLength();
    }
    
    /**
     * Load blacklist from file
     */
    private void loadBlacklist() {
        try {
            // Create default blacklist file if it doesn't exist
            if (!blacklistFile.exists()) {
                createDefaultBlacklist();
            }
            
            // Read blacklist from file
            List<String> lines = Files.readAllLines(blacklistFile.toPath());
            blacklistedNames.clear();
            
            for (String line : lines) {
                line = line.trim();
                // Skip empty lines and comments
                if (!line.isEmpty() && !line.startsWith("#")) {
                    blacklistedNames.add(line.toLowerCase());
                }
            }
            
            plugin.getLogger().info("Loaded " + blacklistedNames.size() + " blacklisted names from " + blacklistFile.getName());
            
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load name blacklist: " + e.getMessage());
        }
    }
    
    /**
     * Create default blacklist file with common inappropriate names
     */
    private void createDefaultBlacklist() {
        try {
            blacklistFile.getParentFile().mkdirs();
            
            try (FileWriter writer = new FileWriter(blacklistFile)) {
                writer.write("# SimpleMounts Name Blacklist\n");
                writer.write("# Add one name per line (case insensitive)\n");
                writer.write("# Lines starting with # are comments\n");
                writer.write("# \n");
                writer.write("# Common inappropriate names:\n");
                writer.write("admin\n");
                writer.write("moderator\n");
                writer.write("staff\n");
                writer.write("server\n");
                writer.write("console\n");
                writer.write("system\n");
                writer.write("null\n");
                writer.write("undefined\n");
                writer.write("test\n");
                writer.write("debug\n");
                writer.write("fuck\n");
                writer.write("shit\n");
                writer.write("damn\n");
                writer.write("hell\n");
                writer.write("ass\n");
                writer.write("bitch\n");
                writer.write("bastard\n");
                writer.write("crap\n");
                writer.write("piss\n");
                writer.write("nazi\n");
                writer.write("hitler\n");
                writer.write("racist\n");
                writer.write("rape\n");
                writer.write("murder\n");
                writer.write("kill\n");
                writer.write("die\n");
                writer.write("suicide\n");
                writer.write("gay\n");
                writer.write("retard\n");
                writer.write("stupid\n");
                writer.write("idiot\n");
                writer.write("moron\n");
                writer.write("loser\n");
                writer.write("noob\n");
                writer.write("hacker\n");
                writer.write("cheater\n");
                writer.write("spam\n");
                writer.write("scam\n");
                writer.write("bot\n");
                writer.write("fake\n");
            }
            
            plugin.getLogger().info("Created default name blacklist at " + blacklistFile.getName());
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default blacklist file: " + e.getMessage());
        }
    }
    
    /**
     * Reload blacklist from file
     */
    public void reloadBlacklist() {
        loadBlacklist();
    }
    
    /**
     * Get validation error message for a name
     */
    public String getValidationError(String rawName, String sanitizedName) {
        if (rawName == null) {
            return null; // Null is allowed
        }
        
        if (sanitizedName == null || sanitizedName.trim().isEmpty()) {
            if (rawName.trim().isEmpty()) {
                return "Name cannot be empty";
            } else {
                return "Name contains only invalid characters";
            }
        }
        
        if (isBlacklisted(sanitizedName)) {
            return "Name is not allowed";
        }
        
        if (!isValidLength(sanitizedName)) {
            return "Name must be between " + plugin.getConfigManager().getMinNameLength() + 
                   " and " + plugin.getConfigManager().getMaxNameLength() + " characters";
        }
        
        return null; // No error
    }
}