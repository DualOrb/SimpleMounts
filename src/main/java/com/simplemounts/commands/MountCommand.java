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
        "summon", "store", "list", "release", "info", "rename", "gui", "reload", "help", "give"
    );
    
    public MountCommand(SimpleMounts plugin) {
        this.plugin = plugin;
        this.mountManager = plugin.getMountManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
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
            case "give":
                handleGive(player, args);
                break;
            case "help":
            default:
                showHelp(player);
                break;
        }
        
        return true;
    }
    
    private void handleSummon(Player player, String[] args) {
        if (!player.hasPermission("simplemounts.summon")) {
            sendMessage(player, "no_permission");
            return;
        }
        
        if (args.length < 2) {
            sendMessage(player, "Usage: /mount summon <name>");
            return;
        }
        
        String mountName = args[1];
        
        plugin.runAsync(() -> {
            mountManager.summonMount(player, mountName);
        });
    }
    
    private void handleStore(Player player, String[] args) {
        if (!player.hasPermission("simplemounts.store")) {
            sendMessage(player, "no_permission");
            return;
        }
        
        if (args.length > 1) {
            // Store specific mount by name
            String mountName = args[1];
            plugin.runAsync(() -> {
                mountManager.storeMount(player, mountName);
            });
        } else {
            // Store currently ridden mount
            plugin.runAsync(() -> {
                mountManager.storeCurrentMount(player);
            });
        }
    }
    
    private void handleList(Player player, String[] args) {
        if (!player.hasPermission("simplemounts.list")) {
            sendMessage(player, "no_permission");
            return;
        }
        
        plugin.runAsync(() -> {
            mountManager.getPlayerMounts(player).thenAccept(mounts -> {
                plugin.runSync(() -> {
                    displayMountList(player, mounts);
                });
            });
        });
    }
    
    private void handleRelease(Player player, String[] args) {
        if (!player.hasPermission("simplemounts.release")) {
            sendMessage(player, "no_permission");
            return;
        }
        
        if (args.length < 2) {
            sendMessage(player, "Usage: /mount release <name>");
            return;
        }
        
        String mountName = args[1];
        
        plugin.runAsync(() -> {
            mountManager.releaseMount(player, mountName);
        });
    }
    
    private void handleInfo(Player player, String[] args) {
        if (!player.hasPermission("simplemounts.info")) {
            sendMessage(player, "no_permission");
            return;
        }
        
        if (args.length < 2) {
            sendMessage(player, "Usage: /mount info <name>");
            return;
        }
        
        String mountName = args[1];
        
        plugin.runAsync(() -> {
            mountManager.getMountData(player, mountName).thenAccept(mountData -> {
                plugin.runSync(() -> {
                    displayMountInfo(player, mountData);
                });
            });
        });
    }
    
    private void handleRename(Player player, String[] args) {
        if (!player.hasPermission("simplemounts.rename")) {
            sendMessage(player, "no_permission");
            return;
        }
        
        if (args.length < 3) {
            sendMessage(player, "Usage: /mount rename <old_name> <new_name>");
            return;
        }
        
        String oldName = args[1];
        String newName = args[2];
        
        if (!isValidMountName(newName)) {
            sendMessage(player, "invalid_mount_name");
            return;
        }
        
        plugin.runAsync(() -> {
            mountManager.getMountData(player, oldName).thenAccept(oldMount -> {
                if (oldMount == null) {
                    sendMessage(player, "mount_not_found", oldName);
                    return;
                }
                
                mountManager.getMountData(player, newName).thenAccept(existingMount -> {
                    if (existingMount != null) {
                        sendMessage(player, "mount_name_exists", newName);
                        return;
                    }
                    
                    // Create new mount with new name
                    plugin.getDatabaseManager().saveMountData(
                        player.getUniqueId(),
                        newName,
                        oldMount.getMountType(),
                        oldMount.getMountDataYaml(),
                        oldMount.getChestInventoryData()
                    ).thenAccept(saved -> {
                        if (saved) {
                            // Delete old mount
                            plugin.getDatabaseManager().deleteMountData(player.getUniqueId(), oldName)
                                .thenAccept(deleted -> {
                                    if (deleted) {
                                        sendMessage(player, "mount_renamed", oldName, newName);
                                    } else {
                                        sendMessage(player, "mount_rename_failed");
                                    }
                                });
                        } else {
                            sendMessage(player, "mount_rename_failed");
                        }
                    });
                });
            });
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
        String prefix = plugin.getConfigManager().getMessagePrefix();
        
        if (mounts.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                prefix + "&7You don't have any stored mounts."));
            return;
        }
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            prefix + "&6Your Stored Mounts:"));
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        
        for (MountData mount : mounts) {
            MountType type = mount.getMountTypeEnum();
            String typeName = type.getDisplayName();
            String lastAccessed = dateFormat.format(new Date(mount.getLastAccessed()));
            
            String chestInfo = mount.hasChestInventory() ? " &7[Chest]" : "";
            String activeInfo = isActiveMount(player, mount.getMountName()) ? " &a[Active]" : "";
            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7- &e" + mount.getMountName() + " &7(" + typeName + ")" + chestInfo + activeInfo));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "  &7Last used: " + lastAccessed));
        }
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            prefix + "&7Total: &e" + mounts.size() + " &7mounts"));
    }
    
    private void displayMountInfo(Player player, MountData mountData) {
        String prefix = plugin.getConfigManager().getMessagePrefix();
        
        if (mountData == null) {
            sendMessage(player, "mount_not_found");
            return;
        }
        
        MountType type = mountData.getMountTypeEnum();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            prefix + "&6Mount Information:"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7Name: &e" + mountData.getMountName()));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7Type: &e" + type.getDisplayName()));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7Created: &e" + dateFormat.format(new Date(mountData.getCreatedAt()))));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7Last Used: &e" + dateFormat.format(new Date(mountData.getLastAccessed()))));
        
        if (mountData.hasChestInventory()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7Chest: &aYes"));
        }
        
        if (isActiveMount(player, mountData.getMountName())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7Status: &aCurrently Active"));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7Status: &7Stored"));
        }
    }
    
    private void showHelp(Player player) {
        String prefix = plugin.getConfigManager().getMessagePrefix();
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            prefix + "&6SimpleMounts Commands:"));
        
        if (player.hasPermission("simplemounts.summon")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7/mount summon <name> &f- Summon a stored mount"));
        }
        
        if (player.hasPermission("simplemounts.store")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7/mount store [name] &f- Store your current mount"));
        }
        
        if (player.hasPermission("simplemounts.list")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7/mount list &f- List all your stored mounts"));
        }
        
        if (player.hasPermission("simplemounts.release")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7/mount release <name> &f- Permanently delete a mount"));
        }
        
        if (player.hasPermission("simplemounts.info")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7/mount info <name> &f- Show detailed mount information"));
        }
        
        if (player.hasPermission("simplemounts.rename")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7/mount rename <old> <new> &f- Rename a mount"));
        }
        
        if (player.hasPermission("simplemounts.admin")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7/mount reload &f- Reload the configuration"));
        }
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7/mount help &f- Show this help message"));
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
                    return getPlayerMountNames(player, args[1]);
                case "store":
                    return Collections.singletonList("<name>");
                default:
                    return new ArrayList<>();
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("rename")) {
            return Collections.singletonList("<new_name>");
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
            player.sendMessage(ChatColor.YELLOW + "Available items: default, horse, strider, camel, donkey, mule, pig, llama");
            return;
        }
        
        String itemType = args[1].toLowerCase();
        com.simplemounts.data.TamingItem tamingItem;
        
        switch (itemType) {
            case "default":
                tamingItem = plugin.getConfigManager().getTamingItem("default");
                break;
            case "horse":
                tamingItem = plugin.getConfigManager().getTamingItem("HORSE");
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
}