package com.simplemounts.core;

import com.simplemounts.SimpleMounts;
import com.simplemounts.data.TamingItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.util.*;
import java.util.logging.Level;

public class ConfigManager {
    
    private final SimpleMounts plugin;
    private FileConfiguration config;
    
    public ConfigManager(SimpleMounts plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        validateConfig();
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        validateConfig();
    }
    
    private void validateConfig() {
        try {
            // Validate basic settings
            if (config.getInt("limits.default_max_mounts", 5) < 1) {
                plugin.getLogger().warning("default_max_mounts must be at least 1, setting to 5");
                config.set("limits.default_max_mounts", 5);
            }
            
            if (config.getInt("limits.max_name_length", 16) < 1) {
                plugin.getLogger().warning("max_name_length must be at least 1, setting to 16");
                config.set("limits.max_name_length", 16);
            }
            
            if (config.getInt("limits.min_name_length", 3) < 1) {
                plugin.getLogger().warning("min_name_length must be at least 1, setting to 3");
                config.set("limits.min_name_length", 3);
            }
            
            // Validate message keys
            validateMessageKeys();
            
            // Validate distance storage settings
            int maxDistance = config.getInt("storage.distance_storage.max_distance", 32);
            if (maxDistance < 1 || maxDistance > 64) {
                plugin.getLogger().warning("distance_storage.max_distance must be between 1 and 64, setting to 32");
                config.set("storage.distance_storage.max_distance", 32);
            }
            
            int checkInterval = config.getInt("storage.distance_storage.check_interval", 100);
            if (checkInterval < 20) {
                plugin.getLogger().warning("distance_storage.check_interval must be at least 20 ticks (1 second), setting to 100");
                config.set("storage.distance_storage.check_interval", 100);
            }
            
            int gracePeriod = config.getInt("storage.distance_storage.grace_period", 10);
            if (gracePeriod < 1) {
                plugin.getLogger().warning("distance_storage.grace_period must be at least 1 second, setting to 10");
                config.set("storage.distance_storage.grace_period", 10);
            }
            
            plugin.saveConfig();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error validating configuration", e);
        }
    }
    
    private void validateMessageKeys() {
        try {
            // List of required message keys
            String[] requiredKeys = {
                "prefix",
                // Core messages
                "mount_stored", "mount_summoned", "mount_claimed", "mount_released", "mount_renamed",
                "config_reloaded", "mount_claimed_and_stored",
                // Error messages
                "mount_not_found", "mount_summon_failed", "mount_store_failed", "mount_release_failed",
                "mount_spawn_failed", "mount_claim_failed", "mount_rename_failed",
                // Validation messages
                "invalid_mount_type", "mount_type_disabled", "invalid_mount_name", "mount_name_exists",
                "mount_limit_reached", "type_limit_reached", "already_riding", "not_riding_mount",
                "not_your_mount", "no_safe_location", "no_permission",
                // Taming messages
                "entity_protected", "taming_failed", "wrong_taming_item", "require_empty_hand",
                // Protection messages
                "mount_protected", "mount_died",
                // Distance storage messages
                "mount_too_far_warning", "mount_auto_stored_distance",
                // System messages
                "shutdown_storage",
                // Usage messages
                "player_only_command", "usage_summon", "usage_store", "usage_release", "usage_info",
                "usage_rename", "usage_give", "usage_debug",
                // List messages
                "no_stored_mounts", "stored_mounts_header", "mount_list_total", "mount_info_header",
                "commands_header",
                // Status indicators
                "mount_has_chest", "mount_is_active", "mount_last_used", "mount_info_name",
                "mount_info_type", "mount_info_created", "mount_info_last_used", "mount_has_chest_yes",
                "mount_has_chest_no", "mount_status_active", "mount_status_stored",
                // Help messages
                "help_summon", "help_store", "help_list", "help_release", "help_info", "help_rename",
                "help_reload", "help_help", "help_give", "help_debug"
            };
            
            List<String> missingKeys = new ArrayList<>();
            for (String key : requiredKeys) {
                if (!config.contains("messages." + key)) {
                    missingKeys.add(key);
                }
            }
            
            if (!missingKeys.isEmpty()) {
                plugin.getLogger().warning("Missing message keys in config: " + String.join(", ", missingKeys));
                plugin.getLogger().warning("Plugin may not function properly. Please update your config.yml");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error validating message keys", e);
        }
    }
    
    public boolean isCustomTamingEnabled() {
        return config.getBoolean("taming.enable_custom_taming", true);
    }
    
    public int getTamingSuccessChance() {
        return Math.max(0, Math.min(100, config.getInt("taming.success_chance", 100)));
    }
    
    public boolean requireEmptyHand() {
        return config.getBoolean("taming.require_empty_hand", false);
    }
    
    public boolean playTamingEffects() {
        return config.getBoolean("taming.play_effects", true);
    }
    
    public boolean respectProtectionPlugins() {
        return config.getBoolean("taming.integration.respect_protection_plugins", true);
    }
    
    public List<String> getProtectionPlugins() {
        return config.getStringList("taming.integration.protection_plugins");
    }
    
    public TamingItem getTamingItem(String mountType) {
        ConfigurationSection tamingSection = config.getConfigurationSection("taming.taming_items");
        if (tamingSection == null) {
            return getDefaultTamingItem();
        }
        
        ConfigurationSection typeSection = tamingSection.getConfigurationSection(mountType);
        if (typeSection == null) {
            typeSection = tamingSection.getConfigurationSection("default");
            if (typeSection == null) {
                return getDefaultTamingItem();
            }
        }
        
        try {
            String materialName = typeSection.getString("material", "GOLDEN_CARROT");
            Material material = Material.getMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("Invalid material '" + materialName + "' for taming item, using GOLDEN_CARROT");
                material = Material.GOLDEN_CARROT;
            }
            
            String name = typeSection.getString("name", "&6Mount Taming Treat");
            List<String> lore = typeSection.getStringList("lore");
            Integer customModelData = typeSection.getInt("custom_model_data", 0);
            if (customModelData == 0) customModelData = null;
            
            Map<Enchantment, Integer> enchantments = new HashMap<>();
            ConfigurationSection enchantSection = typeSection.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                for (String enchantKey : enchantSection.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantKey.toLowerCase()));
                    if (enchantment != null) {
                        enchantments.put(enchantment, enchantSection.getInt(enchantKey));
                    }
                }
            }
            
            boolean consumeOnUse = typeSection.getBoolean("consume_on_use", true);
            
            return new TamingItem(material, name, lore, customModelData, enchantments, consumeOnUse);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error loading taming item for " + mountType, e);
            return getDefaultTamingItem();
        }
    }
    
    private TamingItem getDefaultTamingItem() {
        return new TamingItem(
            Material.GOLDEN_CARROT,
            "&6Mount Taming Treat",
            Arrays.asList("&7Right-click a rideable animal", "&7to claim it as your mount!"),
            null,
            new HashMap<>(),
            true
        );
    }
    
    public Set<String> getEnabledMountTypes() {
        List<String> enabledTypes = config.getStringList("mount_types.enabled_types");
        return new HashSet<>(enabledTypes);
    }
    
    public boolean isMountTypeEnabled(String mountType) {
        return getEnabledMountTypes().contains(mountType);
    }
    
    public boolean restoreArmorOnSummon(String mountType) {
        return config.getBoolean("mount_types." + mountType.toLowerCase() + ".restore_armor", true);
    }
    
    public boolean restoreChestOnSummon(String mountType) {
        return config.getBoolean("mount_types." + mountType.toLowerCase() + ".restore_chest", true);
    }
    
    public boolean restoreDecorationsOnSummon(String mountType) {
        return config.getBoolean("mount_types." + mountType.toLowerCase() + ".restore_decorations", true);
    }
    
    public boolean healOnSummon(String mountType) {
        return config.getBoolean("mount_types." + mountType.toLowerCase() + ".heal_on_summon", true);
    }
    
    public boolean allowVanillaTaming(String mountType) {
        return config.getBoolean("mount_types." + mountType.toLowerCase() + ".allow_vanilla_taming", false);
    }
    
    public boolean summonCamelSitting() {
        return config.getBoolean("mount_types.camel.summon_sitting", false);
    }
    
    public boolean keepStriderWarmOutsideNether() {
        return config.getBoolean("mount_types.strider.keep_warm_outside_nether", true);
    }
    
    public boolean autoEquipCarrotOnStick() {
        return config.getBoolean("mount_types.pig.auto_equip_carrot", true);
    }
    
    public boolean preserveLlamaCaravan() {
        return config.getBoolean("mount_types.llama.preserve_caravan", false);
    }
    
    public int getDefaultMaxMounts() {
        return config.getInt("limits.default_max_mounts", 5);
    }
    
    public int getMaxNameLength() {
        return config.getInt("limits.max_name_length", 16);
    }
    
    public int getMinNameLength() {
        return config.getInt("limits.min_name_length", 3);
    }
    
    public int getTypeLimitForMount(String mountType) {
        return config.getInt("limits.type_limits." + mountType, -1);
    }
    
    public int getSafeSpotRadius() {
        return config.getInt("summoning.safe_spot_radius", 10);
    }
    
    public int getMaxHeightDifference() {
        return config.getInt("summoning.max_height_difference", 5);
    }
    
    public boolean teleportIfNoSafeSpot() {
        return config.getBoolean("summoning.teleport_if_no_safe_spot", true);
    }
    
    public boolean autoDismissExistingMount() {
        return config.getBoolean("summoning.auto_dismiss_existing", true);
    }
    
    public boolean striderLavaSafety() {
        return config.getBoolean("summoning.strider_lava_safety", true);
    }
    
    public boolean striderPreferLava() {
        return config.getBoolean("summoning.strider_prefer_lava", true);
    }
    
    public boolean autoStoreOnLogout() {
        return config.getBoolean("storage.auto_store_on_logout", true);
    }
    
    public boolean autoStoreOnDeath() {
        return config.getBoolean("storage.auto_store_on_death", false);
    }
    
    public boolean autoStoreOnWorldChange() {
        return config.getBoolean("storage.auto_store_on_world_change", false);
    }
    
    public boolean autoStoreOnDimensionChange() {
        return config.getBoolean("storage.auto_store_on_dimension_change", true);
    }
    
    public boolean autoStoreOnShutdown() {
        return config.getBoolean("storage.auto_store_on_shutdown", true);
    }
    
    public int getShutdownStorageTimeout() {
        return config.getInt("storage.shutdown_storage_timeout", 30);
    }
    
    public boolean autoResummonOnLogin() {
        return config.getBoolean("storage.auto_resummon_on_login", false);
    }
    
    public int getDistanceStorageMaxDistance() {
        int distance = config.getInt("storage.distance_storage.max_distance", 32);
        return Math.min(Math.max(distance, 1), 64); // Ensure between 1 and 64
    }
    
    public int getDistanceStorageCheckInterval() {
        return Math.max(config.getInt("storage.distance_storage.check_interval", 100), 20); // At least 1 second
    }
    
    public int getDistanceStorageGracePeriod() {
        return Math.max(config.getInt("storage.distance_storage.grace_period", 10), 1); // At least 1 second
    }
    
    public boolean preventMountStealing() {
        return config.getBoolean("mount_behavior.prevent_mount_stealing", true);
    }
    
    public boolean allowMountDamage() {
        return config.getBoolean("mount_behavior.allow_mount_damage", true);
    }
    
    public boolean restoreInventoryOnSummon() {
        return config.getBoolean("mount_behavior.restore_inventory", true);
    }
    
    public boolean keepMountsLoaded() {
        return config.getBoolean("mount_behavior.keep_loaded", false);
    }
    
    public boolean protectMountInteraction() {
        return config.getBoolean("mount_behavior.protect_mount_interaction", true);
    }
    
    public boolean protectChestAccess() {
        return config.getBoolean("mount_behavior.protect_chest_access", true);
    }
    
    public boolean enableShutdownHandler() {
        return config.getBoolean("shutdown.enable_shutdown_handler", true);
    }
    
    public boolean forceImmediateStorage() {
        return config.getBoolean("shutdown.force_immediate_storage", true);
    }
    
    public boolean logShutdownOperations() {
        return config.getBoolean("shutdown.log_shutdown_operations", true);
    }
    
    public boolean checkConflictsOnStartup() {
        return config.getBoolean("compatibility.check_conflicts_on_startup", true);
    }
    
    public boolean isPluginCompatibilityEnabled(String pluginName) {
        return config.getBoolean("compatibility.plugins." + pluginName.toLowerCase() + ".enabled", true);
    }
    
    public boolean ignoreMythicMobs() {
        return config.getBoolean("compatibility.plugins.mythicmobs.ignore_mythic_mobs", true);
    }
    
    public boolean ignoreNpcs() {
        return config.getBoolean("compatibility.plugins.citizens.ignore_npcs", true);
    }
    
    public boolean respectSpawnProtection() {
        return config.getBoolean("compatibility.plugins.essentials.respect_spawn_protection", true);
    }
    
    public boolean autoDetectNewTypes() {
        return config.getBoolean("rideable_detection.auto_detect_new_types", true);
    }
    
    public List<String> getCustomRideableTypes() {
        return config.getStringList("rideable_detection.custom_rideable_types");
    }
    
    public boolean includeNbtData() {
        return config.getBoolean("inventory.include_nbt_data", true);
    }
    
    public boolean compressInventoryData() {
        return config.getBoolean("inventory.compress_data", true);
    }
    
    public int getMaxInventorySlots() {
        return config.getInt("inventory.max_slots", 27);
    }
    
    public boolean handleNestedInventories() {
        return config.getBoolean("inventory.handle_nested_inventories", true);
    }
    
    public boolean forceNbtForCustomItems() {
        return config.getBoolean("inventory.custom_items.force_nbt_for_custom", true);
    }
    
    public boolean createPlaceholdersForFailedItems() {
        return config.getBoolean("inventory.custom_items.create_placeholders", true);
    }
    
    public boolean validateCustomItemsBeforeStoring() {
        return config.getBoolean("inventory.custom_items.validate_before_storing", true);
    }
    
    public List<String> getSupportedCustomItemPlugins() {
        return config.getStringList("inventory.custom_items.supported_plugins");
    }
    
    public boolean logCustomItemIssues() {
        return config.getBoolean("inventory.custom_items.log_custom_item_issues", true);
    }
    
    public String getMessagePrefix() {
        return config.getString("messages.prefix", "&8[&6SimpleMounts&8] ");
    }
    
    public String getMessage(String key) {
        try {
            String message = config.getString("messages." + key);
            if (message != null && !message.trim().isEmpty()) {
                return message;
            }
            
            // Fallback messages for critical keys
            String fallback = getFallbackMessage(key);
            if (fallback != null) {
                plugin.getLogger().warning("Using fallback message for missing key: " + key);
                return fallback;
            }
            
            plugin.getLogger().warning("Message key not found and no fallback available: " + key);
            return "&cMessage not found: " + key;
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting message for key '" + key + "': " + e.getMessage());
            return "&cError loading message: " + key;
        }
    }
    
    private String getFallbackMessage(String key) {
        // Essential fallback messages to prevent plugin breaking
        switch (key) {
            case "prefix": return "&8[&6SimpleMounts&8] ";
            case "mount_stored": return "&aMount '{name}' has been stored!";
            case "mount_summoned": return "&aMount '{name}' has been summoned!";
            case "mount_not_found": return "&cMount '{name}' not found!";
            case "no_permission": return "&cYou don't have permission to do that!";
            case "mount_summon_failed": return "&cFailed to summon mount. Please try again.";
            case "mount_store_failed": return "&cFailed to store mount. Please try again.";
            case "mount_claim_failed": return "&cFailed to perform mount operation. Please try again.";
            case "player_only_command": return "&cThis command can only be used by players.";
            case "usage_summon": return "&cUsage: /mount summon <name>";
            case "usage_store": return "&cUsage: /mount store [name]";
            case "usage_release": return "&cUsage: /mount release <name>";
            case "usage_info": return "&cUsage: /mount info <name>";
            case "no_stored_mounts": return "&7You don't have any stored mounts.";
            case "stored_mounts_header": return "&6Your Stored Mounts:";
            case "commands_header": return "&6SimpleMounts Commands:";
            default: return null;
        }
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }
    
    public int getDefaultMountLimit() {
        return config.getInt("limits.default_max_mounts", 5);
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
}