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
            
            plugin.saveConfig();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error validating configuration", e);
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
        return config.getString("messages." + key, "Message not found: " + key);
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