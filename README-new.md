# EmojiTab - Minecraft 1.21.4+ Edition

A modern Minecraft Paper plugin that enables emoji tab completion by injecting fake players into the tab list. Players can type emoji shortcodes and press TAB to autocomplete them with actual emoji characters or unicode symbols.

## Features

- **Tab Completion**: Type `:cloud:` and press TAB to get `☁`
- **Unicode Support**: Full support for emoji characters and unicode symbols
- **Custom Emojis**: Add your own custom text shortcuts
- **Permissions**: Optional permission-based access control
- **Individual Permissions**: Grant specific emoji permissions per player
- **Modern Architecture**: Built for Minecraft 1.21.4+ with Paper API
- **ProtocolLib Integration**: Uses packet manipulation for seamless tab completion
- **Configurable**: Extensive configuration options
- **Performance Optimized**: Minimal impact on server performance

## Requirements

- **Minecraft 1.21.4+**
- **Paper Server** (Spigot/Bukkit not supported)
- **ProtocolLib 5.3.0+**
- **Java 21+**

## Installation

1. Download the latest release
2. Install ProtocolLib if not already installed
3. Place the plugin JAR in your `plugins/` folder
4. Restart your server
5. Configure `plugins/EmojiTab/config.yml` and `plugins/EmojiTab/emojis.yml` as needed

## How It Works

The plugin creates fake players in the tab list with names matching emoji shortcodes. When players type a shortcode and press TAB, Minecraft's built-in tab completion suggests these fake player names. The plugin then replaces the shortcodes with actual emoji characters in chat messages.

## Configuration

### config.yml
```yaml
# Character(s) to wrap around emoji shortcodes
emoji-wrapping: ":"

# Require emoji.use permission
use-permissions: false

# Require individual emoji permissions (emoji.use.<name>)
individual-permissions: false

# Show detailed loading info
verbose: false

# Custom skin data from mineskin.org
custom-texture-data: ""
custom-texture-signature: ""
```

### emojis.yml
```yaml
emojis:
  😀:
    name: grinning
    aliases: [smile, happy]
  ☁:
    name: cloud
    aliases: cloudy
  "[CUSTOM]":
    name: custom
    aliases: special
```

## Commands

- `/emoji list [page]` - Show available emojis
- `/emoji toggle` - Toggle emoji tab completion for yourself
- `/emoji reload` - Reload plugin configuration (admin)
- `/emoji help` - Show help information

## Permissions

- `emoji.use` - Use emojis in chat (default: true)
- `emoji.admin` - Access admin commands (default: op)
- `emoji.reload` - Reload configuration (default: op)
- `emoji.toggle` - Toggle own emoji display (default: true)
- `emoji.use.<name>` - Use specific emoji (when individual-permissions is enabled)

## Usage Examples

1. Type `:cloud:` and press TAB → Completes to `:cloud:`
2. Send the message → Plugin converts `:cloud:` to `☁` in chat
3. Other players see the emoji character in your message

## Custom Emojis

You can add custom text shortcuts:

```yaml
emojis:
  "[ADMIN]":
    name: admin
    aliases: [staff, mod]
  "★":
    name: star
    aliases: [special, vip]
```

## API for Developers

The plugin provides an API for other plugins to interact with:

```kotlin
// Get the plugin instance
val emojiTab = Bukkit.getPluginManager().getPlugin("EmojiTab") as EmojiTab

// Transform a message with emojis
val transformedMessage = emojiTab.emojifier.emojifyMessage(originalMessage, player)

// Check if player has emojis disabled
val disabled = Settings.getEmojiDisabled(player)
```

## Building from Source

```bash
git clone https://github.com/your-repo/EmojiTab
cd EmojiTab
mvn clean package
```

The compiled JAR will be in the `target/` directory.

## Differences from Original

This version has been completely rewritten for Minecraft 1.21.4+ with the following improvements:

- **Modern Paper API**: Uses latest Paper APIs and Adventure components
- **Updated Dependencies**: ProtocolLib 5.3.0+, Kotlin 2.0+, Java 21+
- **Better Packet Handling**: Updated for 1.21.4 packet changes
- **Improved Performance**: More efficient packet generation and handling
- **Enhanced Commands**: Better command interface with pagination and hover effects
- **Extended Configuration**: More configuration options and emoji definitions
- **Code Quality**: Complete rewrite with better architecture and error handling

## Support

For issues, feature requests, or questions:
- GitHub Issues: [Create an issue](https://github.com/your-repo/EmojiTab/issues)
- Discord: [Your Discord Server](https://discord.gg/your-server)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Credits

- Original EmojiTab by Legitimoose
- Updated for 1.21.4+ by [Your Name]
- ProtocolLib by dmulloy2
- Paper Team for the excellent server software
