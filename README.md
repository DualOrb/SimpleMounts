# SimpleMounts Plugin

A comprehensive Minecraft Spigot plugin for version 1.21.5 that allows players to tame, store, and summon rideable entities with custom items and GUI management.

## 🌟 Features

### Core Functionality
- **Custom Taming System**: Tame rideable entities with special craftable cookies
- **Mount Storage**: Store and summon mounts via commands or GUI
- **Auto-Storage**: Automatic storage on logout, server shutdown, and distance-based
- **GUI Management**: Interactive inventory GUI for mount management
- **Name System**: Optional mount naming with blacklist and sanitization
- **Multi-Mount Support**: Store multiple mounts with individual management

### Supported Mount Types
- **Horses** - All variants with armor and inventory support
- **Zombie Horses** - Undead horses with armor support and special taming
- **Skeleton Horses** - Skeletal horses with armor support and bone taming
- **Donkeys & Mules** - With chest storage capability
- **Camels** - Two-player capacity and unique mechanics
- **Striders** - Nether lava riding with saddle support
- **Pigs** - With carrot on stick control
- **Llamas** - Including chest storage and caravan mechanics
- **All Rideable Entities** - Extensible system for any rideable entity

### Production-Ready Features
- **Thread-Safe Operations**: ConcurrentHashMap usage for multiplayer safety
- **Rate Limiting**: Prevents command spam and abuse
- **Plugin Compatibility**: Non-interfering with other plugins
- **Database Optimization**: Automatic maintenance and cleanup
- **Memory Management**: Cleanup on player disconnect
- **Administrative Tools**: System monitoring and maintenance commands

## 📋 Requirements

- **Minecraft Version**: 1.21.5
- **Server Software**: Spigot, Paper, or compatible
- **Java Version**: 17+
- **Dependencies**: None (standalone plugin)

### Optional Plugin Integration
- WorldGuard, GriefPrevention, Towny (protection)
- Citizens, MythicMobs (entity filtering)
- Vault (economy integration - future)

## 🚀 Installation

1. Download the SimpleMounts.jar file
2. Place it in your server's `plugins/` folder
3. Restart your server
4. Configure the plugin in `plugins/SimpleMounts/config.yml`
5. Set permissions for your players

## ⚙️ Configuration

### Basic Configuration
```yaml
# Mount type settings
mount_types:
  enabled_types:
    - HORSE
    - ZOMBIE_HORSE
    - SKELETON_HORSE
    - DONKEY  
    - MULE
    - CAMEL
    - STRIDER
    - PIG
    - LLAMA

# Player limits
limits:
  default_max_mounts: 5
  max_name_length: 16
  min_name_length: 3

# Auto-storage settings
storage:
  auto_store_on_logout: true
  auto_store_on_shutdown: true
  distance_storage:
    max_distance: 32
    grace_period: 10
```

### Taming Items
The plugin uses craftable "Mount Taming Cookies" by default:

**Recipe** (3x3 crafting grid):
```
W G W
G C G  = 4x Mount Taming Cookie
W G W

W = Wheat
G = Gold Nugget  
C = Cocoa Beans
```

### Custom Taming Items
Configure type-specific taming items:
```yaml
taming_items:
  HORSE:
    material: "GOLDEN_APPLE"
    name: "&eHorse Taming Apple"
  ZOMBIE_HORSE:
    material: "ROTTEN_FLESH"
    name: "&2Zombie Horse Taming Flesh"
  SKELETON_HORSE:
    material: "BONE"
    name: "&fSkeleton Horse Taming Bone"
  STRIDER:
    material: "WARPED_FUNGUS"
    name: "&dStrider Taming Fungus"
```

## 🎮 Commands

### Player Commands
- `/mount gui` - Open the mount management GUI
- `/mount list` - List all stored mounts
- `/mount summon <name|id>` - Summon a specific mount
- `/mount store [name]` - Store current or specified mount
- `/mount rename <new_name>` - Rename currently active mount
- `/mount info <name|id>` - Show detailed mount information
- `/mount release <name|id>` - Permanently delete a mount

### Admin Commands
- `/mount reload` - Reload configuration
- `/mount give <item_type>` - Give taming items to players
- `/mount debug <command>` - Debug and troubleshooting commands
- `/mount system <status|maintenance|stats>` - System monitoring

### Command Aliases
- `/sm` - Short alias for `/mount`
- `/simplemounts` - Full plugin name alias

## 🔐 Permissions

### Basic Permissions
```yaml
simplemounts.use          # Basic plugin usage (default: true)
simplemounts.summon       # Summon stored mounts (default: true)
simplemounts.store        # Store mounts (default: true)
simplemounts.claim        # Tame wild mounts (default: true)
simplemounts.list         # List stored mounts (default: true)
simplemounts.release      # Delete mounts (default: true)
simplemounts.rename       # Rename mounts (default: true)
simplemounts.info         # View mount details (default: true)
```

### Limit-Based Permissions
```yaml
simplemounts.unlimited    # No mount storage limit
simplemounts.limit.10     # Store up to 10 mounts
simplemounts.limit.25     # Store up to 25 mounts
simplemounts.limit.50     # Store up to 50 mounts
simplemounts.limit.100    # Store up to 100 mounts
```

### Administrative Permissions
```yaml
simplemounts.admin                # All admin commands (default: op)
simplemounts.bypass.protection    # Bypass mount protection (default: op)
simplemounts.*                    # All permissions (default: op)
```

## 🎯 Usage Guide

### Getting Started
1. **Craft Taming Cookies**: Use the 3x3 recipe with wheat, gold nuggets, and cocoa beans
2. **Find a Rideable Entity**: Locate horses, zombie horses, skeleton horses, donkeys, camels, etc.
3. **Right-Click to Tame**: Use the taming cookie on the entity (or specific items like rotten flesh for zombie horses, bones for skeleton horses)
4. **Mount Management**: Use `/mount gui` or commands to manage your mounts

### Mount Storage
- **Automatic**: Mounts auto-store when you log out or go too far away
- **Manual**: Use `/mount store` while riding a mount
- **GUI**: Click the store button in the mount GUI

### Mount Summoning
- **By Name**: `/mount summon MyHorse`
- **By ID**: `/mount summon 15` (for duplicate names)
- **GUI**: Click on a mount in the GUI to summon it

### Name Management
- **Unnamed Mounts**: Mounts are unnamed by default
- **Renaming**: Use `/mount rename <new_name>` while riding a mount
- **Name Validation**: Names are automatically sanitized and checked against blacklist

## 🔧 Database & Storage

### Database Information
- **Type**: SQLite (lightweight, no external dependencies)
- **Location**: `plugins/SimpleMounts/mounts.db`
- **Tables**: `player_mounts`, `active_mounts`, `plugin_config`
- **Maintenance**: Automatic cleanup every 6 hours

### Data Stored
- **Mount Attributes**: Health, speed, jump strength, appearance
- **Inventory Data**: Complete chest contents with NBT data
- **Mount Metadata**: Names, creation dates, last accessed times
- **Player Data**: Mount ownership and active tracking

## 🛡️ Plugin Compatibility

### Protection Plugins
The plugin respects and integrates with:
- **WorldGuard**: Region-based protection
- **GriefPrevention**: Claim-based protection  
- **Towny**: Town-based permissions
- **Factions**: Faction territory respect
- **Lands**: Land claim integration

### Entity Plugins
Safe integration with:
- **Citizens**: Ignores NPC entities
- **MythicMobs**: Ignores custom MythicMobs
- **Other Mount Plugins**: Won't interfere with existing mount systems

## 📊 Administrative Features

### System Monitoring
```bash
/mount system status      # Show system health and memory usage
/mount system stats       # Database statistics
/mount system maintenance # Manual database cleanup
```

### Debug Commands
```bash
/mount debug gui          # Check active GUI sessions
/mount debug cleargui     # Clear stuck GUI sessions
```

### Database Maintenance
The plugin automatically:
- Cleans up unused mounts older than 90 days
- Removes stale active mount entries
- Performs database vacuum operations
- Logs statistics for monitoring

## 🔒 Security Features

### Rate Limiting
- **Command Cooldown**: 2-second minimum between actions
- **Action Limits**: Maximum 30 actions per minute per player
- **Abuse Prevention**: Automatic blocking of spam attempts

### Data Protection
- **Player Isolation**: Each player's data is completely separate
- **Input Validation**: All user input is sanitized and validated
- **Name Blacklist**: Configurable blocked names with automatic filtering
- **Permission Validation**: All actions require proper permissions

## 🐛 Troubleshooting

### Common Issues

**Q: Mounts disappear when I log out**
A: This is normal behavior. Mounts auto-store for safety. Use `/mount gui` to summon them back.

**Q: Can't tame animals with regular items**
A: You need to craft the special Mount Taming Cookies using the 3x3 recipe.

**Q: Mount limit reached error**
A: Check your permissions. You may need `simplemounts.limit.X` or `simplemounts.unlimited`.

**Q: GUI showing no mounts**
A: Ensure mounts are properly stored. Use `/mount list` to see stored mounts via command.

### Performance Issues
If experiencing lag:
1. Check `/mount system status` for memory usage
2. Run `/mount system maintenance` to clean database
3. Reduce `max_distance` in distance storage settings
4. Lower `max_entities_per_chunk` in production settings

### Debug Mode
Enable debug logging in config.yml:
```yaml
debug: true
```
This will provide detailed console output for troubleshooting.

## 📁 File Structure

```
plugins/SimpleMounts/
├── config.yml              # Main configuration
├── name-blacklist.txt       # Blocked mount names
├── mounts.db               # SQLite database
└── SimpleMounts.jar        # Plugin file
```

## 🔄 Updates & Maintenance

### Automatic Maintenance
- **Database Cleanup**: Every 6 hours
- **Memory Cleanup**: On player disconnect
- **Orphaned Mount Cleanup**: On server startup

### Manual Maintenance
```bash
/mount system maintenance    # Run database cleanup
/mount reload               # Reload configuration
```

### Backup Recommendations
- Backup `mounts.db` regularly
- Include `config.yml` and `name-blacklist.txt` in backups
- Test restores on development servers

## 🤝 Support & Contributing

### Getting Help
1. Check this README for common solutions
2. Enable debug mode for detailed logging
3. Use `/mount system status` to check system health
4. Check server console for error messages

### Configuration Support
- All settings are documented in `config.yml`
- Use `/mount reload` after making changes
- Test changes on development servers first

### Performance Optimization
- Adjust `distance_storage` settings for your server size
- Configure appropriate mount limits per permission group
- Monitor database size and run maintenance as needed

## 📄 License

This plugin is provided as-is for server use. Modification and redistribution should respect original authorship.

## 🏷️ Version Information

- **Current Version**: 1.0.0
- **Minecraft Version**: 1.21.5
- **API Version**: 1.21
- **Build Target**: Spigot/Paper

---

**SimpleMounts** - Making mount management simple, safe, and scalable for multiplayer Minecraft servers.