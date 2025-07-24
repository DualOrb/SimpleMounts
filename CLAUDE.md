## Plugin Compatibility Strategy

### Non-Interference Design
1. **Custom Entity Tagging** - All SimpleMounts entities tagged with persistent metadata
2. **Event Priority Management** - Use LOWEST priority to allow other plugins to handle events first
3. **Protection Plugin Integration** - Check with WorldGuard, GriefPrevention, Towny, etc. before taming
4. **Namespace Isolation** - Use unique plugin namespaces for all data and commands
5. **Soft Dependencies** - Optional integration without hard requirements

### Conflict Prevention
1. **Entity Ownership Validation** - Check if entities belong to other mount plugins
2. **Command Conflict Resolution** - Use unique command aliases and subcommands
3. **Database Independence** - Separate database with no shared tables
4. **Event Cancellation Respect** - Honor event cancellations from other plugins
5. **Permission Integration** - Work with existing permission systems

### Integration Points
1. **Economy Plugins** - Optional taming item costs through Vault API
2. **Region Plugins** - Respect protected areas for taming and summoning
3. **Combat Plugins** - Honor PvP states and combat tagging
4. **Anti-Cheat Plugins** - Whitelist mount teleportation mechanics
5. **Citizens/MythicMobs** - Ignore NPC and custom mob entities# SimpleMounts Plugin - Design Document

## Overview
SimpleMounts is a Minecraft Spigot plugin for version 1.21.5 that allows players to tame, store, and summon horses, donkeys, and mules via commands. Mounts are automatically stored when players log out and can be retrieved with simple commands.

## Core Features

### Mount Types Supported
- **Horses** - All variants (colors, markings)
- **Donkeys** - Including chest storage
- **Mules** - Including chest storage
- **Camels** - Two-player capacity and unique mechanics
- **Striders** - Nether lava riding with saddle support
- **Pigs** - With carrot on stick control
- **Llamas** - Including chest storage and caravan mechanics
- **All Rideable Entities** - Any entity that can be ridden by players

### Key Functionality
1. **Auto-Storage on Logout/Shutdown** - All player mounts automatically stored when disconnecting or server shutdown
2. **Custom Taming System** - Custom item-based taming instead of vanilla mechanics
3. **Command-Based Management** - Summon, store, list, and release mounts
4. **Persistent Storage** - SQLite database with full inventory serialization
5. **Configurable Limits** - Permission-based mount limits and settings
6. **Plugin Compatibility** - Non-interfering design with other server plugins

## Technical Architecture

### Database Schema (SQLite)

#### `player_mounts` Table
```sql
CREATE TABLE player_mounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    player_uuid TEXT NOT NULL,
    mount_name TEXT NOT NULL,
    mount_type TEXT NOT NULL,
    mount_data TEXT NOT NULL,
    chest_inventory TEXT,
    created_at INTEGER NOT NULL,
    last_accessed INTEGER NOT NULL,
    UNIQUE(player_uuid, mount_name)
);
```

#### `active_mounts` Table
```sql
CREATE TABLE active_mounts (
    entity_uuid TEXT PRIMARY KEY,
    player_uuid TEXT NOT NULL,
    mount_name TEXT NOT NULL,
    world_name TEXT NOT NULL,
    x REAL NOT NULL,
    y REAL NOT NULL,
    z REAL NOT NULL,
    spawned_at INTEGER NOT NULL
);
```

#### `plugin_config` Table
```sql
CREATE TABLE plugin_config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
```

### Mount Data Serialization
Mount data stored as YAML format containing:
- **Base Attributes**: Health, max health, speed, jump strength (if applicable)
- **Appearance**: Color, style, markings (for horses), carpet color (for llamas)
- **Equipment**: Saddle, armor, carrot on stick (for pigs)
- **Chest Inventory**: Complete serialization of chest contents for donkeys/mules/llamas
  - Item stacks with NBT data, enchantments, custom names
  - Slot positions and organization
  - Shulker box contents and nested inventories
- **Special Properties**: 
  - Strider warmth and shivering state
  - Camel dashing cooldown and sitting state
  - Llama strength and caravan position
  - Pig boost time remaining
- **Metadata**: Custom name, age, breeding cooldown, ownership data
- **Location**: Last known position for summoning
- **Plugin Metadata**: Custom tags to identify SimpleMounts entities

### Class Structure

#### Core Classes
- `SimpleMounts.java` - Main plugin class with shutdown handling
- `MountManager.java` - Core mount management logic
- `DatabaseManager.java` - SQLite operations
- `ConfigManager.java` - Configuration handling
- `ShutdownHandler.java` - Server shutdown mount cleanup

#### Data Classes
- `MountData.java` - Mount information container
- `MountType.java` - Enum for supported rideable entity types
- `MountAttributes.java` - Type-specific attribute handlers
- `TamingItem.java` - Custom taming item configuration
- `ChestInventoryData.java` - Chest contents serialization

#### Commands
- `MountCommand.java` - Main command handler with subcommands

#### Listeners
- `CustomTamingListener.java` - Custom item-based taming system
- `MountInteractionListener.java` - Mount interaction and protection
- `PlayerListener.java` - Login/logout/shutdown events
- `InventoryListener.java` - Chest interaction prevention

#### Utilities
- `MountSerializer.java` - Convert mounts to/from YAML storage format
- `InventorySerializer.java` - Full chest inventory serialization with NBT
- `PermissionUtils.java` - Permission checking utilities
- `RideableDetector.java` - Detect and validate rideable entities
- `PluginCompatibility.java` - Ensure compatibility with other plugins
- `ItemValidator.java` - Custom item validation and matching

## Commands System

### Primary Command: `/mount` (aliases: `/sm`, `/simplemounts`)

#### Subcommands
- `/mount summon <name>` - Summon a stored mount
- `/mount store [name]` - Store currently riding mount
- `/mount list` - List all stored mounts
- `/mount release <name>` - Permanently delete a mount
- `/mount info <name>` - Show detailed mount information
- `/mount rename <old_name> <new_name>` - Rename a mount
- `/mount reload` - Reload configuration (admin only)

### Permission Nodes
- `simplemounts.use` - Basic mount usage
- `simplemounts.summon` - Summon mounts
- `simplemounts.store` - Store mounts
- `simplemounts.claim` - Claim wild mounts with taming items
- `simplemounts.list` - List mounts
- `simplemounts.release` - Release mounts
- `simplemounts.rename` - Rename mounts
- `simplemounts.admin` - Administrative commands
- `simplemounts.limit.<number>` - Override mount limit
- `simplemounts.unlimited` - No mount limit
- `simplemounts.bypass.protection` - Bypass mount protection (admin only)

## Configuration System

### config.yml Structure
```yaml
# SimpleMounts Configuration

database:
  # Database file location (relative to plugin folder)
  file: "mounts.db"
  # Connection pool settings
  max_connections: 10
  connection_timeout: 30

# Custom taming system
taming:
  # Enable custom taming (disables vanilla taming detection)
  enable_custom_taming: true
  
  # Taming items configuration
  taming_items:
    # Default taming item for all mount types
    default:
      material: "GOLDEN_CARROT"
      name: "&6Mount Taming Treat"
      lore:
        - "&7Right-click a rideable animal"
        - "&7to claim it as your mount!"
      custom_model_data: null
      enchantments: []
      consume_on_use: true
      
    # Type-specific taming items
    HORSE:
      material: "GOLDEN_APPLE"
      name: "&eHorse Taming Apple"
      lore:
        - "&7Perfect for taming horses!"
      custom_model_data: 100001
      consume_on_use: true
      
    STRIDER:
      material: "WARPED_FUNGUS"
      name: "&dStrider Taming Fungus"
      lore:
        - "&7Irresistible to striders!"
      consume_on_use: true
      
    CAMEL:
      material: "CACTUS"
      name: "&aCamel Taming Cactus"
      lore:
        - "&7A desert delicacy!"
      consume_on_use: true
  
  # Taming settings
  success_chance: 100  # Percentage chance of successful taming
  require_empty_hand: false  # Require empty off-hand
  play_effects: true  # Play particles and sounds on taming
  
  # Integration with other plugins
  integration:
    # Check if entity is protected by other plugins before taming
    respect_protection_plugins: true
    # List of plugins to check compatibility with
    protection_plugins:
      - "WorldGuard"
      - "GriefPrevention"
      - "Towny"
      - "Factions"
      - "Lands"
      - "ClaimChunk"

# Mount type configurations
mount_types:
  # Enable/disable specific mount types
  enabled_types:
    - HORSE
    - DONKEY
    - MULE
    - CAMEL
    - STRIDER
    - PIG
    - LLAMA
    # Add any rideable entity type here
  
  # Type-specific settings
  horse:
    # Restore horse armor on summon
    restore_armor: true
    # Heal horse to full health on summon
    heal_on_summon: true
    # Allow vanilla taming alongside custom system
    allow_vanilla_taming: false
  
  donkey:
    restore_chest: true
    heal_on_summon: true
    allow_vanilla_taming: false
  
  mule:
    restore_chest: true
    heal_on_summon: true
    allow_vanilla_taming: false
  
  camel:
    # Restore camel decorations
    restore_decorations: true
    heal_on_summon: true
    # Summon camel in sitting position
    summon_sitting: false
    allow_vanilla_taming: false
  
  strider:
    # Keep strider warm when summoned in overworld
    keep_warm_outside_nether: true
    heal_on_summon: true
    allow_vanilla_taming: false
  
  pig:
    # Automatically equip carrot on stick when summoning
    auto_equip_carrot: true
    heal_on_summon: true
    allow_vanilla_taming: false
  
  llama:
    restore_chest: true
    restore_decorations: true
    heal_on_summon: true
    # Maintain caravan connections when possible
    preserve_caravan: false
    allow_vanilla_taming: false

limits:
  # Default maximum mounts per player
  default_max_mounts: 5
  # Maximum mount name length
  max_name_length: 16
  # Minimum mount name length
  min_name_length: 3
  # Limit mounts by type (optional)
  type_limits:
    # Example: max 2 striders per player
    # STRIDER: 2
    # CAMEL: 1

summoning:
  # Search radius for safe summoning spots
  safe_spot_radius: 10
  # Maximum height difference for summoning
  max_height_difference: 5
  # Teleport mount to player if no safe spot found
  teleport_if_no_safe_spot: true
  # Remove existing mount when summoning new one
  auto_dismiss_existing: true
  # Check for lava safety when summoning striders
  strider_lava_safety: true
  # Summon striders in lava if available
  strider_prefer_lava: true

storage:
  # Auto-store mounts on player logout
  auto_store_on_logout: true
  # Auto-store mounts on player death
  auto_store_on_death: false
  # Store mounts when changing worlds
  auto_store_on_world_change: false
  # Store mounts when changing dimensions
  auto_store_on_dimension_change: true
  # Auto-store all mounts on server shutdown
  auto_store_on_shutdown: true
  # Maximum time to wait for storage during shutdown (seconds)
  shutdown_storage_timeout: 30

mount_behavior:
  # Prevent other players from riding stored mounts
  prevent_mount_stealing: true
  # Allow mounts to take damage
  allow_mount_damage: true
  # Heal mounts when summoned
  heal_on_summon: true
  # Restore mount inventory on summon
  restore_inventory: true
  # Keep mounts loaded even when owner is far away
  keep_loaded: false
  # Prevent mount interaction by non-owners
  protect_mount_interaction: true
  # Prevent chest access by non-owners
  protect_chest_access: true

# Server shutdown handling
shutdown:
  # Enable graceful shutdown handling
  enable_shutdown_handler: true
  # Force immediate storage without confirmation
  force_immediate_storage: true
  # Log shutdown storage operations
  log_shutdown_operations: true

# Plugin compatibility settings
compatibility:
  # Check for conflicting plugins on startup
  check_conflicts_on_startup: true
  
  # Plugin-specific compatibility settings
  plugins:
    # Horses plugin compatibility
    horses:
      enabled: true
      priority: "LOWEST"  # Event priority to avoid conflicts
    
    # MythicMobs compatibility
    mythicmobs:
      enabled: true
      ignore_mythic_mobs: true  # Don't allow taming of MythicMobs
    
    # Citizens compatibility
    citizens:
      enabled: true
      ignore_npcs: true  # Don't allow taming of NPC entities
    
    # EssentialsX compatibility
    essentials:
      enabled: true
      respect_spawn_protection: true

# Rideable entity detection settings
rideable_detection:
  # Automatically detect new rideable entities from updates
  auto_detect_new_types: true
  # Custom rideable entities (for modded servers)
  custom_rideable_types: []
    # Example for modded entities:
    # - "MODDED_DRAGON"
    # - "CUSTOM_MOUNT"

# Inventory serialization settings
inventory:
  # Include NBT data in serialization
  include_nbt_data: true
  # Compress inventory data to save space
  compress_data: true
  # Maximum inventory slots to serialize per mount
  max_slots: 27
  # Handle nested inventories (shulker boxes, etc.)
  handle_nested_inventories: true

messages:
  prefix: "&8[&6SimpleMounts&8] "
  mount_stored: "&aMount '{name}' has been stored!"
  mount_summoned: "&aMount '{name}' has been summoned!"
  mount_claimed: "&aYou have successfully claimed '{name}'!"
  mount_limit_reached: "&cYou have reached your mount limit of {limit}!"
  mount_not_found: "&cMount '{name}' not found!"
  no_safe_location: "&cCould not find a safe location to summon your mount!"
  already_riding: "&cYou are already riding a mount!"
  mount_released: "&aMount '{name}' has been released!"
  invalid_mount_type: "&cThat entity type cannot be stored as a mount!"
  mount_type_disabled: "&cThat mount type is currently disabled!"
  type_limit_reached: "&cYou have reached your limit for {type} mounts!"
  taming_failed: "&cTaming failed! The animal rejected your offering."
  wrong_taming_item: "&cThis animal requires a different taming item!"
  mount_protected: "&cThis mount belongs to another player!"
  entity_protected: "&cThis entity is protected and cannot be tamed!"
  shutdown_storage: "&eStoring all mounts due to server shutdown..."
  
# Enable debug logging
debug: false
```

## Event Handling

### Custom Taming Events
1. **PlayerInteractEntityEvent** - Detect right-click with taming items
2. **ItemValidationEvent** - Validate custom taming items (NBT, model data, etc.)
3. **ProtectionCheckEvent** - Check entity protection from other plugins
4. **TamingSuccessEvent** - Handle successful mount claiming

### Mount Events
1. **VehicleEnterEvent** - Track when players mount rideable entities
2. **VehicleExitEvent** - Track when players dismount
3. **EntityDeathEvent** - Handle mount death scenarios
4. **InventoryClickEvent** - Protect mount chest inventories
5. **PlayerInteractEntityEvent** - Prevent unauthorized mount interactions

### Player Events
1. **PlayerJoinEvent** - Welcome message, cleanup orphaned mounts
2. **PlayerQuitEvent** - Auto-store all active mounts
3. **PlayerChangedWorldEvent** - Optional auto-storage
4. **PlayerDeathEvent** - Optional auto-storage

### Server Events
1. **ServerShutdownEvent** - Emergency mount storage
2. **PluginDisableEvent** - Graceful shutdown with mount cleanup
3. **WorldUnloadEvent** - Store mounts in unloading worlds

## Data Flow

### Custom Taming Flow
1. Player right-clicks rideable entity with configured taming item
2. Plugin validates item matches type-specific requirements (material, NBT, model data)
3. Check entity protection status with other plugins (WorldGuard, GriefPrevention, etc.)
4. Validate entity is a supported rideable type and enabled in config
5. Check if player has space for new mount (overall limit + type-specific limits)
6. Process taming chance and consume item if configured
7. Generate default name or prompt for custom name
8. Tag entity with SimpleMounts metadata for tracking
9. Serialize mount data with type-specific attributes and store in database as YAML
10. Add entity to active_mounts table for tracking
11. Notify player of successful claiming with effects

### Summoning Flow
1. Player executes `/mount summon <name>`
2. Validate mount exists and belongs to player
3. Check if player is already riding a mount
4. Find safe summoning location near player
5. Deserialize mount data from database
6. Spawn mount entity with restored attributes
7. Update last accessed timestamp

### Storage Flow
1. Player executes `/mount store`, logs out, or server shuts down
2. Detect currently ridden rideable entity or scan for player's active mounts
3. Validate entity belongs to player and has SimpleMounts metadata
4. Serialize current mount state including type-specific properties:
   - Health, equipment, special states
   - **Complete chest inventory** with full NBT data preservation
   - Item enchantments, custom names, lore, model data
   - Nested inventory contents (shulker boxes, etc.)
5. Update database record in YAML format with separate chest_inventory field
6. Remove entity from active_mounts table
7. Remove mount entity from world with cleanup
8. Notify player if manual storage, log if automatic

## Permission Integration

### Mount Limits by Permission
- Default: 5 mounts (configurable)
- `simplemounts.limit.10` - 10 mounts
- `simplemounts.limit.25` - 25 mounts
- `simplemounts.unlimited` - No limit

### Feature Permissions
- Core functionality requires `simplemounts.use`
- Each command has specific permission requirement
- Admin commands require `simplemounts.admin`

## Error Handling

### Database Errors
- Connection failures: Retry with exponential backoff
- Corruption: Backup and attempt repair
- Migration: Automatic schema updates
- **Shutdown timeout**: Force storage with data integrity warnings

### Mount Spawning Errors
- No safe location: Teleport to player or notify failure
- World not loaded: Queue for later summoning
- Mount entity issues: Regenerate from stored data
- **Inventory restoration**: Fallback to basic mount if chest data corrupted

### Player Errors
- Invalid mount names: Clear validation messages
- Permission denied: Informative error messages
- Command syntax: Helpful usage information
- **Taming item validation**: Clear feedback on item requirements

### Plugin Compatibility Errors
- **Protection conflicts**: Graceful failure with informative messages
- **Entity ownership**: Prevent duplicate claiming across plugins
- **Event conflicts**: Respect other plugin priorities and cancellations

### Shutdown Handling
- **Graceful storage**: Attempt to save all active mounts
- **Timeout protection**: Force shutdown after configured time limit
- **Data integrity**: Ensure no partial writes during emergency shutdown
- **Recovery**: Cleanup orphaned entities on next startup

## Performance Considerations

### Database Optimization
- Connection pooling for concurrent access
- Prepared statements for all queries
- Batch operations for multiple mount storage
- **Emergency shutdown**: Optimized bulk storage operations
- Regular database maintenance and cleanup
- **Inventory compression**: Reduce storage size for large chest contents

### Memory Management
- Lazy loading of mount data
- Cleanup of unused mount entities
- Efficient YAML serialization format
- Cache frequently accessed data
- **Shutdown optimization**: Minimize memory usage during bulk operations

### World Performance
- Limit concurrent mount spawning
- Despawn abandoned mounts after timeout
- Optimize safe location finding algorithm
- **Entity tracking**: Efficient active mount monitoring
- **Chunk loading**: Minimal impact on server chunk management

### Plugin Compatibility Performance
- **Async protection checks**: Non-blocking integration with other plugins
- **Event optimization**: Minimal overhead when other plugins handle events
- **Cache compatibility data**: Reduce repeated plugin interaction checks

## Future Expansion Possibilities

### Additional Features
- Mount breeding and genetics system
- Mount trading between players
- Mount racing and competitions
- Custom mount abilities and traits
- Integration with economy plugins
- Mount cosmetics and customization

### API Integration
- Developer API for third-party plugins
- Hook system for custom mount types
- Event system for mount-related actions
- Database access for external tools

## Testing Strategy

### Unit Tests
- Database operations and transaction handling
- Mount serialization/deserialization with complex inventories
- Permission checking and validation
- Configuration validation and custom item parsing
- **Inventory NBT handling**: Complete item data preservation
- **Plugin compatibility**: Mock protection plugin interactions

### Integration Tests
- Full mount lifecycle (claim → store → summon with chest items)
- Player logout/login scenarios with inventory preservation
- Command execution and validation
- Multi-player concurrent usage
- **Server shutdown scenarios**: Emergency storage and recovery
- **Custom taming system**: Item validation and success rates

### Performance Tests
- Large numbers of stored mounts with complex inventories
- Concurrent player operations during high load
- Database stress testing with bulk operations
- Memory usage monitoring during shutdown
- **Inventory serialization**: Large chest contents performance
- **Plugin compatibility overhead**: Impact measurement

### Compatibility Tests
- **Protection plugin integration**: WorldGuard, GriefPrevention, Towny
- **Economy plugin integration**: Vault API taming costs
- **Anti-cheat compatibility**: NoCheatPlus, AAC teleportation
- **Mount plugin conflicts**: Horses, EquestrianAddons, other mount plugins