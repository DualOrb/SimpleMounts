# SimpleMounts Player Guide

Welcome to SimpleMounts! This plugin allows you to tame, store, and manage your favorite rideable animals with ease.

## 🚀 Quick Start

1. **Craft a Mount Whistle** - Your key to easy mount management
2. **Get a Taming Item** - Craft or obtain special taming treats
3. **Find a Rideable Animal** - Horses, donkeys, camels, striders, and more!
4. **Right-click with Taming Item** - Claim the animal as your mount
5. **Use Mount Whistle** - Right-click to open GUI and manage all your mounts

## 🎯 Basic Usage

### Your First Mount
1. Craft a **Golden Carrot** (default taming item)
2. Find a horse, donkey, or other rideable animal
3. Right-click the animal with your Golden Carrot
4. The animal is now yours! It will be automatically named or you can rename it later

### Storing Your Mount
- **While riding**: Type `/mount store` to store your current mount
- **Automatic**: Mounts are automatically stored when you log out
- **By name**: Use `/mount store <name>` if you're not riding it

### Using the Mount Whistle (Recommended)
- **Right-click Mount Whistle** to open the Mount Manager GUI
- **Click any stored mount** to summon it instantly
- **Click your active mount** to store it
- **Only one mount active** - switching automatically stores your current mount

### Summoning Your Mount (Command Method)
- Type `/mount summon <name>` to bring your mount to you
- Example: `/mount summon Thunder`
- Your mount will appear at a safe location near you

## 📋 Command Reference

All commands use `/mount` (or `/sm` for short):

| Command | Description | Example |
|---------|-------------|---------|
| `/mount list` | Show all your stored mounts | `/mount list` |
| `/mount summon <name>` | Summon a specific mount | `/mount summon Thunder` |
| `/mount store [name]` | Store current or named mount | `/mount store` or `/mount store Thunder` |
| `/mount info <name>` | View detailed mount information | `/mount info Thunder` |
| `/mount rename <old> <new>` | Rename a mount | `/mount rename Thunder Lightning` |
| `/mount release <name>` | Permanently delete a mount | `/mount release Thunder` |

## 🎺 Mount Whistle - GUI Management

The Mount Whistle is your primary tool for managing mounts through an easy-to-use interface.

### Crafting the Mount Whistle

```
 G     (G = Gold Ingot)
GHG    (H = Goat Horn)  
 S     (S = Saddle)
```

**Recipe**: Place a **Goat Horn** in the center, surround with **3 Gold Ingots**, and place a **Saddle** at the bottom.

### Using the Mount Whistle

1. **Right-click the whistle** to open the Mount Manager GUI
2. **Browse your mounts** - See all stored and active mounts at a glance
3. **Click to summon** - Click any stored mount to bring it to you
4. **Click to store** - Click your active mount (green "Active" status) to store it
5. **Automatic switching** - Summoning a different mount automatically stores your current one

### GUI Features

- **Visual Status**: Green "Active" or Yellow "Stored" indicators
- **Mount Details**: Health, speed, jump strength, and type information  
- **One-Click Actions**: No need for commands - just click!
- **Pagination**: Handles large mount collections with multiple pages
- **Right-click Info**: Right-click any mount for detailed information

### GUI Controls

| Action | Result |
|--------|--------|
| **Left-click stored mount** | Summon the mount |
| **Left-click active mount** | Store the mount |
| **Right-click any mount** | View detailed information |
| **Navigation arrows** | Browse pages (if you have many mounts) |
| **Refresh button** | Update the display |
| **Close button** | Exit the GUI |

## 🐎 Taming System

SimpleMounts uses a custom taming system with special items:

### Default Taming Item
- **Golden Carrot** - Works on all mount types
- **How to Use**: Right-click any rideable animal
- **Success Rate**: 100% (configurable by server)

### Type-Specific Taming Items
Different animals may require different treats:

| Mount Type | Preferred Taming Item | Notes |
|------------|----------------------|-------|
| **Horse** | Golden Apple | Premium taming item |
| **Strider** | Warped Fungus | Perfect for Nether exploration |
| **Camel** | Cactus | Desert specialty |
| **All Others** | Golden Carrot | Universal fallback |

### Taming Tips
- ✅ **Success**: Animal becomes yours instantly
- ❌ **Failure**: Item consumed, try again
- 🛡️ **Protected Animals**: Some animals may be protected by other plugins
- 🏠 **Region Restrictions**: Check if taming is allowed in your current area

## 🦄 Mount Types & Features

### Horses
- **Features**: Speed, jumping, armor support
- **Storage**: Automatically saves armor and decorations
- **Special**: Maintains color, markings, and stats

### Donkeys & Mules
- **Features**: Chest storage for items
- **Storage**: Complete chest inventory preserved
- **Special**: All items, enchantments, and NBT data saved

### Camels
- **Features**: Two-player riding, desert mobility
- **Storage**: Decorations and sitting state preserved
- **Special**: Dash ability and unique mechanics

### Striders
- **Features**: Lava walking in the Nether
- **Storage**: Warmth state and saddle preserved
- **Special**: Stays warm when summoned outside Nether

### Pigs
- **Features**: Carrot on stick control
- **Storage**: Boost time and equipment saved
- **Special**: Auto-equips carrot on stick when summoned

### Llamas
- **Features**: Chest storage, caravan formation
- **Storage**: Chest contents and decorations preserved
- **Special**: Maintains strength and carpet colors

## 💾 Storage & Limits

### Mount Limits
- **Default**: 5 mounts per player
- **Permission-based**: Some players may have higher limits
- **Type Limits**: May have specific limits per mount type

### What Gets Saved
- ✅ **Health & Stats**: Full health restoration on summon
- ✅ **Equipment**: Saddles, armor, decorations
- ✅ **Inventory**: Complete chest contents with enchantments
- ✅ **Appearance**: Colors, markings, decorations
- ✅ **Special States**: Sitting, warmth, boost times

### Automatic Storage
Your mounts are automatically stored when you:
- Log out of the server
- Server shuts down
- Change dimensions (Overworld ↔ Nether ↔ End)
- Die (if configured by server)

## 🛠️ Tips & Tricks

### Mount Whistle Pro Tips
- **Keep it handy**: The Mount Whistle is your main management tool
- **Visual indicators**: Green = Active, Yellow = Stored  
- **Quick switching**: Click different mounts to instantly switch between them
- **Right-click for details**: Get full mount stats and information
- **One mount rule**: Only one mount can be active - switching is automatic

### Naming Your Mounts
- Use descriptive names: "FastHorse", "PackMule", "NetherRider"
- Names must be 3-16 characters long
- Avoid special characters for best compatibility

### Managing Multiple Mounts
- **Primary**: Use Mount Whistle GUI for visual management
- **Backup**: Use `/mount list` to see all your mounts at a glance
- Check mount info before summoning: `/mount info <name>`
- Release unwanted mounts to free up slots: `/mount release <name>`

### Safe Summoning
- Mounts summon at safe locations near you
- If no safe spot is found, they'll teleport directly to you
- Striders prefer to summon in lava when available

### Inventory Management
- Donkey/Mule/Llama chests are fully preserved
- Shulker boxes and nested inventories are saved
- Enchanted items maintain all properties

## ❓ Troubleshooting

### Common Issues

**Q: Mount Whistle won't open GUI**
- Ensure you're right-clicking with the whistle
- Check that it's a proper Mount Whistle (crafted item)
- Verify you have permission to use the plugin

**Q: GUI shows no mounts**
- Make sure you have claimed at least one mount
- Try the refresh button in the GUI
- Use `/mount list` command to verify your mounts exist

**Q: "Mount not found" error**
- Check spelling with `/mount list`
- Mount names are case-sensitive

**Q: Can't tame an animal**
- Verify you have the correct taming item
- Check if the animal is protected by another plugin
- Ensure you have permission to tame mounts

**Q: Mount won't summon**
- Check if you're already riding another mount
- Verify there's space around you
- Try moving to an open area

**Q: Lost items from mount chest**
- Items are preserved automatically - try summoning again
- Check if mount was properly stored before logout

**Q: Reached mount limit**
- Use `/mount list` to see current mounts
- Release unused mounts with `/mount release <name>`
- Ask admins about increased limits

### Getting Help
- Contact server administrators for permission issues
- Report bugs to server staff
- Check server-specific mount rules and restrictions

## 🔧 Permissions

Basic permissions you might have:
- `simplemounts.use` - Basic plugin usage
- `simplemounts.summon` - Summon stored mounts
- `simplemounts.store` - Store mounts
- `simplemounts.claim` - Tame new mounts
- `simplemounts.limit.X` - Store X number of mounts

## 📜 Server-Specific Notes

Your server may have customized:
- Different taming items or recipes
- Modified mount limits
- Special mount types or abilities
- Integration with economy plugins (taming costs)
- Unique mount behaviors or restrictions

Check with your server's documentation or staff for specific details!

---

*Happy mounting! 🐎*