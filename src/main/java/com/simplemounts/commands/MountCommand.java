package com.simplemounts.commands;

import com.simplemounts.SimpleMounts;
import com.simplemounts.core.MountManager;
import com.simplemounts.data.MountData;
import com.simplemounts.data.MountType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MountCommand implements CommandExecutor, TabCompleter {
    
    private final SimpleMounts plugin;
    private final MountManager mountManager;
    
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "summon", "store", "list", "release", "info", "rename", "gui", "reload", "debug", "system", "help", "give"
    );
    
    public MountCommand(SimpleMounts plugin) {
        this.plugin = plugin;
        this.mountManager = plugin.getMountManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (!(sender instanceof Player)) {
                String message = plugin.getConfigManager().getMessage("player_only_command");
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                return true;
            }
            
            Player player = (Player) sender;
            
            if (args.length == 0) {
                showHelp(player);
                return true;
            }
            
            String subcommand = args[0].toLowerCase();
            
            switch (subcommand) {
            case "summon":
                handleSummon(player, args);
                break;
            case "store":
                handleStore(player, args);
                break;
            case "list":
                handleList(player, args);
                break;
            case "release":
                handleRelease(player, args);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "rename":
                handleRename(player, args);
                break;
            case "gui":
                handleGUI(player, args);
                break;
            case "reload":
                handleReload(player, args);
                break;
            case "debug":
                handleDebug(player, args);
                break;
            case "system":
                handleSystem(player, args);
                break;
            case "give":
                handleGive(player, args);
                break;
            case "help":
            default:
                showHelp(player);
                break;
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error in MountCommand.onCommand: " + e.getMessage());
            e.printStackTrace();
            if (sender instanceof Player) {
                sendMessage((Player) sender, "mount_claim_failed");
            }
        }
        
        return true;
    }
    
    private void sendMessage(Player player, String key) {
        sendMessage(player, key, new HashMap<>());
    }
    
    private void sendMessage(Player player, String key, Map<String, String> placeholders) {
        try {
            String message = plugin.getConfigManager().getMessage(key);
            String prefix = plugin.getConfigManager().getMessagePrefix();
            
            // Replace placeholders
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send message with key '" + key + "' to player " + player.getName());
            player.sendMessage(ChatColor.RED + "An error occurred. Please try again.");
        }
    }
    
    private void handleSummon(Player player, String[] args) {
        try {
            if (!player.hasPermission("simplemounts.summon")) {
                sendMessage(player, "no_permission");
                return;
            }
            
            if (args.length < 2) {
                sendMessage(player, "usage_summon");
                return;
            }
            
            // Check if this is an ID-based command or name-based command
            if (args.length >= 3 && isNumeric(args[2])) {
                // Format: /mount summon <name> <id>
                String mountName = args[1];
                int mountId = Integer.parseInt(args[2]);
                
                plugin.runAsync(() -> {
                    try {
                        mountManager.summonMount(player, mountId);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error summoning mount ID " + mountId + " for player " + player.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else if (isNumeric(args[1])) {
                // Format: /mount summon <id>
                int mountId = Integer.parseInt(args[1]);
                
                plugin.runAsync(() -> {
                    try {
                        mountManager.summonMount(player, mountId);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error summoning mount ID " + mountId + " for player " + player.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                // Format: /mount summon <name>
                String mountName = args[1];
                
                plugin.runAsync(() -> {
                    try {
                        mountManager.summonMount(player, mountName);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error summoning mount '" + mountName + "' for player " + player.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                        plugin.runSync(() -> {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("name", mountName);
                            sendMessage(player, "mount_summon_failed", placeholders);
                        });
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in handleSummon: " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, "mount_summon_failed");
        }
    }
    
    private void handleStore(Player player, String[] args) {
        try {
            if (!player.hasPermission("simplemounts.store")) {
                sendMessage(player, "no_permission");
                return;
            }
            
            if (args.length > 1) {
                // Store specific mount by name
                String mountName = args[1];
                plugin.runAsync(() -> {
                    try {
                        mountManager.storeMount(player, mountName);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error storing mount '" + mountName + "' for player " + player.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                        plugin.runSync(() -> sendMessage(player, "mount_store_failed"));
                    }
                });
            } else {
                // Store currently ridden mount
                plugin.runAsync(() -> {
                    try {
                        mountManager.storeCurrentMount(player);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error storing current mount for player " + player.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                        plugin.runSync(() -> sendMessage(player, "mount_store_failed"));
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in handleStore: " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, "mount_store_failed");
        }
    }
    
    private void handleList(Player player, String[] args) {
        try {
            if (!player.hasPermission("simplemounts.list")) {
                sendMessage(player, "no_permission");
                return;
            }
            
            plugin.runAsync(() -> {
                try {
                    mountManager.getPlayerMounts(player).thenAccept(mounts -> {
                        plugin.runSync(() -> {
                            try {
                                displayMountList(player, mounts);
                            } catch (Exception e) {
                                plugin.getLogger().severe("Error displaying mount list for player " + player.getName() + ": " + e.getMessage());
                                e.printStackTrace();
                                sendMessage(player, "mount_claim_failed");
                            }
                        });
                    }).exceptionally(ex -> {
                        plugin.getLogger().severe("Error getting mounts for player " + player.getName() + ": " + ex.getMessage());
                        ex.printStackTrace();
                        plugin.runSync(() -> sendMessage(player, "mount_claim_failed"));
                        return null;
                    });
                } catch (Exception e) {
                    plugin.getLogger().severe("Error in async mount list operation: " + e.getMessage());
                    e.printStackTrace();
                    plugin.runSync(() -> sendMessage(player, "mount_claim_failed"));
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("Error in handleList: " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, "mount_claim_failed");
        }
    }
    
    private void handleRelease(Player player, String[] args) {
        try {
            if (!player.hasPermission("simplemounts.release")) {
                sendMessage(player, "no_permission");
                return;
            }
            
            if (args.length < 2) {
                sendMessage(player, "usage_release");
                return;
            }
            
            // Check if this is an ID-based command or name-based command
            if (args.length >= 3 && isNumeric(args[2])) {
                // Format: /mount release <name> <id>
                String mountName = args[1];
                int mountId = Integer.parseInt(args[2]);
                
                plugin.runAsync(() -> {
                    try {
                        mountManager.releaseMount(player, mountId);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error releasing mount ID " + mountId + " for player " + player.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else if (isNumeric(args[1])) {
                // Format: /mount release <id>
                int mountId = Integer.parseInt(args[1]);
                
                plugin.runAsync(() -> {
                    try {
                        mountManager.releaseMount(player, mountId);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error releasing mount ID " + mountId + " for player " + player.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } else {
                // Format: /mount release <name> - This will show selection if duplicates exist
                String mountName = args[1];
                
                plugin.runAsync(() -> {
                    try {
                        // Get mounts by name to check for duplicates
                        List<MountData> mounts = mountManager.getMountsByName(player, mountName).get();
                        if (mounts.isEmpty()) {
                            plugin.runSync(() -> {
                                Map<String, String> placeholders = new HashMap<>();
                                placeholders.put("name", mountName);
                                sendMessage(player, "mount_not_found", placeholders);
                            });
                            return;
                        }
                        
                        if (mounts.size() > 1) {
                            // Multiple mounts with the same name - ask user to specify ID
                            plugin.runSync(() -> {
                                Map<String, String> placeholders = new HashMap<>();
                                placeholders.put("name", mountName);
                                sendMessage(player, "multiple_mounts_found", placeholders);
                                
                                for (MountData mount : mounts) {
                                    Map<String, String> mountPlaceholders = new HashMap<>();
                                    mountPlaceholders.put("id", String.valueOf(mount.getId()));
                                    mountPlaceholders.put("type", mount.getMountTypeEnum().getDisplayName());
                                    mountPlaceholders.put("date", new java.text.SimpleDateFormat("MMM dd, yyyy").format(new java.util.Date(mount.getCreatedAt())));
                                    sendMessage(player, "mount_id_format", mountPlaceholders);
                                }
                                
                                Map<String, String> helpPlaceholders = new HashMap<>();
                                helpPlaceholders.put("command", "release");
                                helpPlaceholders.put("name", mountName);
                                sendMessage(player, "select_mount_by_id", helpPlaceholders);
                            });
                            return;
                        }
                        
                        // Single mount found, release it
                        mountManager.releaseMount(player, mounts.get(0).getId());
                        
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error releasing mount '" + mountName + "' for player " + player.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                        plugin.runSync(() -> {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("name", mountName);
                            sendMessage(player, "mount_release_failed", placeholders);
                        });
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in handleRelease: " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, "mount_release_failed");
        }
    }
    
    private void handleInfo(Player player, String[] args) {
        try {
            if (!player.hasPermission("simplemounts.info")) {
                sendMessage(player, "no_permission");
                return;
            }
            
            if (args.length < 2) {
                sendMessage(player, "usage_info");
                return;
            }
            
            plugin.runAsync(() -> {
                try {
                    // Check if this is an ID-based command or name-based command
                    if (args.length >= 3 && isNumeric(args[2])) {
                        // Format: /mount info <name> <id>
                        int mountId = Integer.parseInt(args[2]);
                        mountManager.getMountData(player, mountId).thenAccept(mountData -> {
                            plugin.runSync(() -> {
                                if (mountData != null) {
                                    displayMountInfo(player, mountData);
                                } else {
                                    Map<String, String> placeholders = new HashMap<>();
                                    placeholders.put("id", String.valueOf(mountId));
                                    sendMessage(player, "mount_not_found_with_id", placeholders);
                                }
                            });
                        });
                    } else if (isNumeric(args[1])) {
                        // Format: /mount info <id>
                        int mountId = Integer.parseInt(args[1]);
                        mountManager.getMountData(player, mountId).thenAccept(mountData -> {
                            plugin.runSync(() -> {
                                if (mountData != null) {
                                    displayMountInfo(player, mountData);
                                } else {
                                    Map<String, String> placeholders = new HashMap<>();
                                    placeholders.put("id", String.valueOf(mountId));
                                    sendMessage(player, "mount_not_found_with_id", placeholders);
                                }
                            });
                        });
                    } else {
                        // Format: /mount info <name> - This will show selection if duplicates exist
                        String mountName = args[1];
                        
                        // Get mounts by name to check for duplicates
                        List<MountData> mounts = mountManager.getMountsByName(player, mountName).get();
                        if (mounts.isEmpty()) {
                            plugin.runSync(() -> {
                                Map<String, String> placeholders = new HashMap<>();
                                placeholders.put("name", mountName);
                                sendMessage(player, "mount_not_found", placeholders);
                            });
                            return;
                        }
                        
                        if (mounts.size() > 1) {
                            // Multiple mounts with the same name - ask user to specify ID
                            plugin.runSync(() -> {
                                Map<String, String> placeholders = new HashMap<>();
                                placeholders.put("name", mountName);
                                sendMessage(player, "multiple_mounts_found", placeholders);
                                
                                for (MountData mount : mounts) {
                                    Map<String, String> mountPlaceholders = new HashMap<>();
                                    mountPlaceholders.put("id", String.valueOf(mount.getId()));
                                    mountPlaceholders.put("type", mount.getMountTypeEnum().getDisplayName());
                                    mountPlaceholders.put("date", new java.text.SimpleDateFormat("MMM dd, yyyy").format(new java.util.Date(mount.getCreatedAt())));
                                    sendMessage(player, "mount_id_format", mountPlaceholders);
                                }
                                
                                Map<String, String> helpPlaceholders = new HashMap<>();
                                helpPlaceholders.put("command", "info");
                                helpPlaceholders.put("name", mountName);
                                sendMessage(player, "select_mount_by_id", helpPlaceholders);
                            });
                            return;
                        }
                        
                        // Single mount found, show info
                        plugin.runSync(() -> {
                            displayMountInfo(player, mounts.get(0));
                        });
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("Error in handleInfo for player " + player.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    plugin.runSync(() -> {
                        sendMessage(player, "mount_not_found");
                    });
                }
            });
        } catch (Exception e) {
            plugin.getLogger().severe("Error in handleInfo: " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, "mount_not_found");
        }
    }
    
    private void handleRename(Player player, String[] args) {
        if (!player.hasPermission("simplemounts.rename")) {
            sendMessage(player, "no_permission");
            return;
        }
        
        if (args.length < 2) {
            sendMessage(player, "usage_rename");
            return;
        }
        
        String rawName = args[1]; // New name is the first argument
        
        // Validate and sanitize the new name
        String sanitizedName = plugin.getNameValidator().validateAndSanitizeName(rawName);
        String validationError = plugin.getNameValidator().getValidationError(rawName, sanitizedName);
        
        if (validationError != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("min", String.valueOf(plugin.getConfigManager().getMinNameLength()));
            placeholders.put("max", String.valueOf(plugin.getConfigManager().getMaxNameLength()));
            
            // Send appropriate error message based on validation result  
            if (validationError.contains("empty")) {
                sendMessage(player, "name_empty", placeholders);
            } else if (validationError.contains("invalid characters")) {
                sendMessage(player, "name_invalid_characters", placeholders);
            } else if (validationError.contains("not allowed")) {
                sendMessage(player, "name_blacklisted", placeholders);
            } else if (validationError.contains("between")) {
                sendMessage(player, "invalid_mount_name", placeholders);
            } else {
                sendMessage(player, "invalid_mount_name", placeholders);
            }
            return;
        }
        
        plugin.runAsync(() -> {
            try {
                // Get currently active mounts for the player
                Set<UUID> activeMounts = mountManager.getPlayerActiveMounts(player.getUniqueId());
                
                if (activeMounts.isEmpty()) {
                    plugin.runSync(() -> {
                        sendMessage(player, "no_active_mount_to_rename");
                    });
                    return;
                }
                
                if (activeMounts.size() > 1) {
                    plugin.runSync(() -> {
                        sendMessage(player, "multiple_active_mounts_rename");
                    });
                    return;
                }
                
                // Get the single active mount
                UUID activeMount = activeMounts.iterator().next();
                Integer mountId = mountManager.getMountId(activeMount);
                
                if (mountId == null) {
                    plugin.runSync(() -> {
                        sendMessage(player, "mount_rename_failed");
                    });
                    return;
                }
                
                // Rename the active mount using the sanitized name
                mountManager.renameMount(player, mountId, sanitizedName);
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error renaming mount for player " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
                plugin.runSync(() -> {
                    sendMessage(player, "mount_rename_failed");
                });
            }
        });
    }
    
    private void handleGUI(Player player, String[] args) {
        if (!player.hasPermission("simplemounts.use")) {
            sendMessage(player, "no_permission");
            return;
        }
        
        plugin.getGUIManager().openMountGUI(player);
    }
    
    private void handleReload(Player player, String[] args) {
        if (!player.hasPermission("simplemounts.admin")) {
            sendMessage(player, "no_permission");
            return;
        }
        
        plugin.runAsync(() -> {
            plugin.reloadConfiguration();
            plugin.runSync(() -> {
                sendMessage(player, "config_reloaded");
            });
        });
    }
    
    private void displayMountList(Player player, List<MountData> mounts) {
        try {
            if (mounts.isEmpty()) {
                sendMessage(player, "no_stored_mounts");
                return;
            }
            
            sendMessage(player, "stored_mounts_header");
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
            
            for (MountData mount : mounts) {
                try {
                    MountType type = mount.getMountTypeEnum();
                    String typeName = type.getDisplayName();
                    String lastAccessed = dateFormat.format(new Date(mount.getLastAccessed()));
                    
                    String chestInfo = mount.hasChestInventory() ? 
                        ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getMessage("mount_has_chest")) : "";
                    String activeInfo = isActiveMount(player, mount.getMountName()) ? 
                        ChatColor.translateAlternateColorCodes('&', plugin.getConfigManager().getMessage("mount_is_active")) : "";
                    
                    String prefix = plugin.getConfigManager().getMessagePrefix();
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        "&7- &e" + mount.getMountName() + " &7(" + typeName + ")" + chestInfo + activeInfo));
                    
                    String lastUsedMsg = ChatColor.translateAlternateColorCodes('&', 
                        plugin.getConfigManager().getMessage("mount_last_used")) + lastAccessed;
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  " + lastUsedMsg));
                } catch (Exception e) {
                    plugin.getLogger().warning("Error displaying mount: " + mount.getMountName() + " - " + e.getMessage());
                }
            }
            
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("count", String.valueOf(mounts.size()));
            sendMessage(player, "mount_list_total", placeholders);
        } catch (Exception e) {
            plugin.getLogger().severe("Error in displayMountList: " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, "mount_claim_failed");
        }
    }
    
    private void displayMountInfo(Player player, MountData mountData) {
        try {
            if (mountData == null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("name", "unknown");
                sendMessage(player, "mount_not_found", placeholders);
                return;
            }
            
            MountType type = mountData.getMountTypeEnum();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
            
            sendMessage(player, "mount_info_header");
            
            String nameMsg = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("mount_info_name")) + mountData.getMountName();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', nameMsg));
            
            String typeMsg = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("mount_info_type")) + type.getDisplayName();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', typeMsg));
            
            String createdMsg = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("mount_info_created")) + dateFormat.format(new Date(mountData.getCreatedAt()));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', createdMsg));
            
            String lastUsedMsg = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("mount_info_last_used")) + dateFormat.format(new Date(mountData.getLastAccessed()));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', lastUsedMsg));
            
            if (mountData.hasChestInventory()) {
                String chestMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("mount_has_chest_yes"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chestMsg));
            } else {
                String chestMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("mount_has_chest_no"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', chestMsg));
            }
            
            if (isActiveMount(player, mountData.getMountName())) {
                String statusMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("mount_status_active"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', statusMsg));
            } else {
                String statusMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("mount_status_stored"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', statusMsg));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in displayMountInfo: " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, "mount_claim_failed");
        }
    }
    
    private void showHelp(Player player) {
        try {
            sendMessage(player, "commands_header");
            
            if (player.hasPermission("simplemounts.summon")) {
                String helpMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("help_summon"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', helpMsg));
            }
            
            if (player.hasPermission("simplemounts.store")) {
                String helpMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("help_store"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', helpMsg));
            }
            
            if (player.hasPermission("simplemounts.list")) {
                String helpMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("help_list"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', helpMsg));
            }
            
            if (player.hasPermission("simplemounts.release")) {
                String helpMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("help_release"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', helpMsg));
            }
            
            if (player.hasPermission("simplemounts.info")) {
                String helpMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("help_info"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', helpMsg));
            }
            
            if (player.hasPermission("simplemounts.rename")) {
                String helpMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("help_rename"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', helpMsg));
            }
            
            if (player.hasPermission("simplemounts.admin")) {
                String helpMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("help_reload"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', helpMsg));
                
                String giveMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("help_give"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', giveMsg));
                
                String debugMsg = ChatColor.translateAlternateColorCodes('&', 
                    plugin.getConfigManager().getMessage("help_debug"));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', debugMsg));
            }
            
            String helpMsg = ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getMessage("help_help"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', helpMsg));
        } catch (Exception e) {
            plugin.getLogger().severe("Error in showHelp: " + e.getMessage());
            e.printStackTrace();
            sendMessage(player, "mount_claim_failed");
        }
    }
    
    private boolean isActiveMount(Player player, String mountName) {
        return mountManager.getPlayerActiveMounts(player.getUniqueId()).stream()
            .anyMatch(uuid -> mountName.equals(mountManager.getMountName(uuid)));
    }
    
    
    private boolean isValidMountName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        int length = name.length();
        int minLength = plugin.getConfigManager().getMinNameLength();
        int maxLength = plugin.getConfigManager().getMaxNameLength();
        
        return length >= minLength && length <= maxLength;
    }
    
    private void sendMessage(Player player, String messageKey, String... replacements) {
        String message = plugin.getConfigManager().getMessage(messageKey);
        String prefix = plugin.getConfigManager().getMessagePrefix();
        
        if (replacements.length > 0) {
            message = message.replace("{name}", replacements[0]);
            if (replacements.length > 1) {
                message = message.replace("{old_name}", replacements[0]);
                message = message.replace("{new_name}", replacements[1]);
            }
        }
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        Player player = (Player) sender;
        
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                .filter(sub -> hasPermissionForSubcommand(player, sub))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            
            switch (subcommand) {
                case "summon":
                case "info":
                case "release":
                case "rename":
                    return Collections.singletonList("<new_name>");
                case "store":
                    return Collections.singletonList("<name>");
                default:
                    return new ArrayList<>();
            }
        }
        
        
        return new ArrayList<>();
    }
    
    private void handleGive(Player player, String[] args) {
        if (!player.hasPermission("simplemounts.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /mount give <item>");
            player.sendMessage(ChatColor.YELLOW + "Available items: cookie, default, horse, zombie_horse, skeleton_horse, donkey, mule, camel, strider, pig, llama");
            return;
        }
        
        String itemType = args[1].toLowerCase();
        com.simplemounts.data.TamingItem tamingItem;
        
        switch (itemType) {
            case "default":
            case "cookie":
                tamingItem = plugin.getConfigManager().getTamingItem("default");
                break;
            case "horse":
                tamingItem = plugin.getConfigManager().getTamingItem("HORSE");
                break;
            case "zombie_horse":
                tamingItem = plugin.getConfigManager().getTamingItem("ZOMBIE_HORSE");
                break;
            case "skeleton_horse":
                tamingItem = plugin.getConfigManager().getTamingItem("SKELETON_HORSE");
                break;
            case "strider":
                tamingItem = plugin.getConfigManager().getTamingItem("STRIDER");
                break;
            case "camel":
                tamingItem = plugin.getConfigManager().getTamingItem("CAMEL");
                break;
            case "donkey":
                tamingItem = plugin.getConfigManager().getTamingItem("DONKEY");
                break;
            case "mule":
                tamingItem = plugin.getConfigManager().getTamingItem("MULE");
                break;
            case "pig":
                tamingItem = plugin.getConfigManager().getTamingItem("PIG");
                break;
            case "llama":
                tamingItem = plugin.getConfigManager().getTamingItem("LLAMA");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown taming item type: " + itemType);
                return;
        }
        
        org.bukkit.inventory.ItemStack item = tamingItem.createItemStack();
        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "Given you a " + itemType + " taming item!");
    }
    
    private void handleDebug(Player player, String[] args) {
        if (!player.hasPermission("simplemounts.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use debug commands.");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Debug commands:");
            player.sendMessage(ChatColor.YELLOW + "/mount debug gui - Check GUI sessions");
            player.sendMessage(ChatColor.YELLOW + "/mount debug cleargui [player] - Clear GUI session");
            return;
        }
        
        String debugCommand = args[1].toLowerCase();
        
        switch (debugCommand) {
            case "gui":
                // Show active GUI sessions
                player.sendMessage(ChatColor.GREEN + "Active GUI sessions: " + plugin.getGUIManager().getActiveSessionCount());
                break;
                
            case "cleargui":
                if (args.length >= 3) {
                    // Clear specific player's session
                    Player targetPlayer = plugin.getServer().getPlayer(args[2]);
                    if (targetPlayer != null) {
                        plugin.getGUIManager().closeSession(targetPlayer);
                        targetPlayer.closeInventory();
                        player.sendMessage(ChatColor.GREEN + "Cleared GUI session for " + targetPlayer.getName());
                    } else {
                        player.sendMessage(ChatColor.RED + "Player not found: " + args[2]);
                    }
                } else {
                    // Clear sender's session
                    plugin.getGUIManager().closeSession(player);
                    player.closeInventory();
                    player.sendMessage(ChatColor.GREEN + "Cleared your GUI session.");
                }
                break;
                
            default:
                player.sendMessage(ChatColor.RED + "Unknown debug command: " + debugCommand);
                break;
        }
    }
    
    private void handleSystem(Player player, String[] args) {
        if (!player.hasPermission("simplemounts.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use system commands.");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "System commands:");
            player.sendMessage(ChatColor.YELLOW + "/mount system status - Show system health");
            player.sendMessage(ChatColor.YELLOW + "/mount system maintenance - Run database maintenance");
            player.sendMessage(ChatColor.YELLOW + "/mount system stats - Show database statistics");
            return;
        }
        
        String systemCommand = args[1].toLowerCase();
        
        switch (systemCommand) {
            case "status":
                // Show system health
                player.sendMessage(ChatColor.GREEN + "=== SimpleMounts System Status ===");
                player.sendMessage(ChatColor.YELLOW + "Version: " + plugin.getDescription().getVersion());
                player.sendMessage(ChatColor.YELLOW + "Active sessions: " + plugin.getGUIManager().getActiveSessionCount());
                
                // Check memory usage
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
                long totalMemory = runtime.totalMemory() / 1024 / 1024; // MB
                long freeMemory = runtime.freeMemory() / 1024 / 1024; // MB
                long usedMemory = totalMemory - freeMemory;
                
                player.sendMessage(ChatColor.YELLOW + "Memory: " + usedMemory + "MB / " + maxMemory + "MB");
                player.sendMessage(ChatColor.YELLOW + "Online players: " + plugin.getServer().getOnlinePlayers().size());
                break;
                
            case "maintenance":
                player.sendMessage(ChatColor.YELLOW + "Starting database maintenance...");
                plugin.runAsync(() -> {
                    plugin.getDatabaseManager().performMaintenanceCleanup().thenAccept(success -> {
                        plugin.runSync(() -> {
                            if (success) {
                                player.sendMessage(ChatColor.GREEN + "Database maintenance completed successfully!");
                            } else {
                                player.sendMessage(ChatColor.RED + "Database maintenance encountered errors. Check console.");
                            }
                        });
                    });
                });
                break;
                
            case "stats":
                player.sendMessage(ChatColor.YELLOW + "Fetching database statistics...");
                plugin.runAsync(() -> {
                    plugin.getDatabaseManager().getDatabaseStats().thenAccept(totalMounts -> {
                        plugin.runSync(() -> {
                            if (totalMounts >= 0) {
                                player.sendMessage(ChatColor.GREEN + "Database statistics logged to console.");
                            } else {
                                player.sendMessage(ChatColor.RED + "Failed to fetch database statistics.");
                            }
                        });
                    });
                });
                break;
                
            default:
                player.sendMessage(ChatColor.RED + "Unknown system command: " + systemCommand);
                break;
        }
    }
    
    private boolean hasPermissionForSubcommand(Player player, String subcommand) {
        switch (subcommand) {
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
            case "reload":
                return player.hasPermission("simplemounts.admin");
            case "debug":
                return player.hasPermission("simplemounts.admin");
            case "system":
                return player.hasPermission("simplemounts.admin");
            case "help":
                return true;
            default:
                return false;
        }
    }
    
    private List<String> getPlayerMountNames(Player player, String partial) {
        try {
            List<MountData> mounts = mountManager.getPlayerMounts(player).get();
            return mounts.stream()
                .map(MountData::getMountName)
                .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}