package com.simplemounts.util;

import com.simplemounts.core.ConfigManager;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PermissionUtils {
    
    private static final Pattern LIMIT_PERMISSION_PATTERN = Pattern.compile("simplemounts\\.limit\\.(\\d+)");
    
    public static int getMaxMounts(Player player, ConfigManager config) {
        if (player.hasPermission("simplemounts.unlimited")) {
            return -1; // Unlimited
        }
        
        int maxLimit = config.getDefaultMaxMounts();
        
        // Check for specific limit permissions
        for (String permission : player.getEffectivePermissions().stream()
                .map(perm -> perm.getPermission())
                .toArray(String[]::new)) {
            
            Matcher matcher = LIMIT_PERMISSION_PATTERN.matcher(permission);
            if (matcher.matches()) {
                try {
                    int limit = Integer.parseInt(matcher.group(1));
                    if (limit > maxLimit) {
                        maxLimit = limit;
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid permission format
                }
            }
        }
        
        return maxLimit;
    }
    
    public static boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }
    
    public static boolean hasAnyPermission(Player player, String... permissions) {
        for (String permission : permissions) {
            if (player.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean hasAllPermissions(Player player, String... permissions) {
        for (String permission : permissions) {
            if (!player.hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean canUseCommand(Player player, String command) {
        switch (command.toLowerCase()) {
            case "summon":
                return player.hasPermission("simplemounts.summon");
            case "store":
                return player.hasPermission("simplemounts.store");
            case "list":
                return player.hasPermission("simplemounts.list");
            case "release":
                return player.hasPermission("simplemounts.release");
            case "info":
                return player.hasPermission("simplemounts.info");
            case "rename":
                return player.hasPermission("simplemounts.rename");
            case "claim":
                return player.hasPermission("simplemounts.claim");
            case "reload":
                return player.hasPermission("simplemounts.admin");
            case "help":
                return player.hasPermission("simplemounts.use");
            default:
                return false;
        }
    }
    
    public static boolean canClaimMountType(Player player, String mountType) {
        String basePermission = "simplemounts.claim";
        String typePermission = "simplemounts.claim." + mountType.toLowerCase();
        
        return player.hasPermission(basePermission) || player.hasPermission(typePermission);
    }
    
    public static boolean canSummonMountType(Player player, String mountType) {
        String basePermission = "simplemounts.summon";
        String typePermission = "simplemounts.summon." + mountType.toLowerCase();
        
        return player.hasPermission(basePermission) || player.hasPermission(typePermission);
    }
    
    public static boolean canBypassProtection(Player player) {
        return player.hasPermission("simplemounts.bypass.protection");
    }
    
    public static boolean canAccessOtherPlayerMounts(Player player) {
        return player.hasPermission("simplemounts.admin") || 
               player.hasPermission("simplemounts.access.others");
    }
    
    public static boolean canIgnoreLimits(Player player) {
        return player.hasPermission("simplemounts.unlimited") || 
               player.hasPermission("simplemounts.admin");
    }
    
    public static boolean canUseDebugCommands(Player player) {
        return player.hasPermission("simplemounts.debug") || 
               player.hasPermission("simplemounts.admin");
    }
    
    public static String getHighestLimitPermission(Player player) {
        int highestLimit = 0;
        String highestPermission = "simplemounts.limit.0";
        
        for (String permission : player.getEffectivePermissions().stream()
                .map(perm -> perm.getPermission())
                .toArray(String[]::new)) {
            
            Matcher matcher = LIMIT_PERMISSION_PATTERN.matcher(permission);
            if (matcher.matches()) {
                try {
                    int limit = Integer.parseInt(matcher.group(1));
                    if (limit > highestLimit) {
                        highestLimit = limit;
                        highestPermission = permission;
                    }
                } catch (NumberFormatException e) {
                    // Ignore invalid permission format
                }
            }
        }
        
        return highestPermission;
    }
    
    public static boolean hasBasicPermissions(Player player) {
        return player.hasPermission("simplemounts.use");
    }
    
    public static boolean isAdmin(Player player) {
        return player.hasPermission("simplemounts.admin") || player.isOp();
    }
    
    public static String[] getRequiredPermissions(String command) {
        switch (command.toLowerCase()) {
            case "summon":
                return new String[]{"simplemounts.use", "simplemounts.summon"};
            case "store":
                return new String[]{"simplemounts.use", "simplemounts.store"};
            case "list":
                return new String[]{"simplemounts.use", "simplemounts.list"};
            case "release":
                return new String[]{"simplemounts.use", "simplemounts.release"};
            case "info":
                return new String[]{"simplemounts.use", "simplemounts.info"};
            case "rename":
                return new String[]{"simplemounts.use", "simplemounts.rename"};
            case "claim":
                return new String[]{"simplemounts.use", "simplemounts.claim"};
            case "reload":
                return new String[]{"simplemounts.admin"};
            case "help":
                return new String[]{"simplemounts.use"};
            default:
                return new String[0];
        }
    }
}