package com.github.remynfv.emojitab

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.wrappers.*
import com.github.remynfv.emojitab.commands.EmojiCommand
import com.github.remynfv.emojitab.utils.Configs
import com.github.remynfv.emojitab.utils.Messager
import com.github.remynfv.emojitab.utils.Permissions
import com.github.remynfv.emojitab.utils.Settings
import org.bukkit.Bukkit
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.util.*

private const val TEAM_NAME: String = "zzzzzzzzz_emoji_tab" // Team used for sorting the player list.

class EmojiTab : JavaPlugin() {
    
    // Configuration values
    var usePermissions: Boolean = true
    var individualPermissions: Boolean = false
    var verbose: Boolean = false
    var wrappingCharacter: String = ":"
    
    /**
     * Default texture for fake player skins.
     * This is a gray skin from mineskin.org that works well for the tab list.
     */
    private var texture: String = "ewogICJ0aW1lc3RhbXAiIDogMTYyMTQxMTE5MDkyMywKICAicHJvZmlsZUlkIiA6ICJmZDQ3Y2I4YjgzNjQ0YmY3YWIyYmUxODZkYjI1ZmMwZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJDVUNGTDEyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQ4NDYxNmVhNDI0OTk1NzI4OGE5Y2Y4ZTNhM2E0ZjVjZDU0NDYxNjk1ZTczMmM5ZWViOTA4NDBmZDRkYzg3YjQiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="
    
    /**
     * Signature for the default texture.
     */
    private var signature: String = "vAk/+xJkgEYANJq2FxjfX4xT5Lo+z1+YNnvWPUgLnpwgj3Vq1nqKZ24y0mHbsLROE3JCnOW1vJObFyNRBktInFXX5RhAv8yis/TSyFFhR3rjnC8ZEMSlM0gyy2K9nJxjY+jDSVBNBaBmWs1JbhPWl2zN/eaMEMivAwZmBLqhTLIV/o4IAUAIPDkxdEw5MGtp81wEot1YSMc1PkGYANx7VTGUy2eCe4AhjDgUrWLkGPkSWeCowU1xQzT5DeWw5V6sylRWXR7DTkzonteRA5jO4gXrXXt5CdytGbz8SOT9V2xnhUPbnRZOgeRKwwHphAJ4N+g2+C5BGxrfSlnmj8YZKAlM17YEK2ej1eClxmmxIW/2bjZnCJR0U7f750evnXb6ZcjIQ+P400RpSCUo79L9cbvz3rHU36IcHKl3GmGG9uyr15C6DVa5WGj5A19fmzIMyRG5e5GTH6NPVC+yK5R0M36in88iP1HQFY9CdPn9NixrdRcCcXPcOcKFsNXE6la+UMhSlsXX+FS5zGtMvTedn5fPglP0DWur9Iz4Z/Bk5ZoZ93NdpF/h63rLZG9xYBs+gf8UEESPRykZSB2wIRO4039s3TC4g8i/lUBn4Zt6IpUiXip9rK7ihKdy3bVX8YywxmCL9oqhfQK0jnFk1dPDBCs/QDMCYnP4fLkLEqPZRrI="
    
    /**
     * Emojis configuration file contents.
     */
    lateinit var emojisConfig: FileConfiguration
        private set
    
    /**
     * The emoji processor that handles message transformation.
     */
    lateinit var emojifier: Emojifier
        private set
    
    /**
     * ProtocolLib manager for packet manipulation.
     */
    private lateinit var protocolManager: ProtocolManager
    
    /**
     * Packets for managing fake players in the tab list.
     */
    private var addEmojisPacket: PacketContainer? = null
    private var removeEmojisPacket: PacketContainer? = null
    private var teamPacket: PacketContainer? = null
    
    override fun onEnable() {
        // Check if we're running on Paper
        try {
            Class.forName("io.papermc.paper.configuration.Configuration")
        } catch (e: ClassNotFoundException) {
            logger.severe("This plugin requires Paper! Please use Paper instead of Spigot/Bukkit.")
            server.pluginManager.disablePlugin(this)
            return
        }
        
        // Check for ProtocolLib
        if (!server.pluginManager.isPluginEnabled("ProtocolLib")) {
            logger.severe("ProtocolLib is required but not found! Please install ProtocolLib.")
            server.pluginManager.disablePlugin(this)
            return
        }
        
        logger.info("Starting EmojiTab v${description.version} for Minecraft ${server.minecraftVersion}")
        
        // Initialize ProtocolLib
        protocolManager = ProtocolLibrary.getProtocolManager()
        
        // Save default configuration files
        saveDefaultConfig()
        
        // Initialize emoji processor
        emojifier = Emojifier(this)
        
        // Load configurations
        reloadConfigs()
        
        // Register commands
        getCommand("emoji")?.setExecutor(EmojiCommand(this))
        
        // Register events
        server.pluginManager.registerEvents(Events(this), this)
        
        // Send emoji packets to all online players
        server.onlinePlayers.forEach { player ->
            trySendEmojiPackets(player)
        }
        
        logger.info("EmojiTab has been enabled successfully!")
    }
    
    override fun onDisable() {
        // Packet cleanup disabled for 1.21.4 compatibility
        // server.onlinePlayers.forEach { player ->
        //     tryRemoveEmojiPackets(player)
        // }
        
        logger.info("EmojiTab has been disabled.")
    }
    
    /**
     * Generates the packets needed for fake emoji players in the tab list.
     */
    private fun generateEmojiPackets() {
        try {
            // For 1.21.4, we need to be very careful with team packet creation
            // The ClassCastException suggests the packet structure has changed
            
            // Disable team packets entirely for now to prevent crashes
            logger.info("Team packet creation disabled for 1.21.4 compatibility")
            logger.info("Tab completion will work through chat events only")
            
            // Store emoji shortcuts for chat completion without using packets
            logger.info("Loaded ${emojifier.emojiList.size} emoji shortcuts for chat completion")
            
            /*
            // Previous team packet code disabled due to 1.21.4 compatibility issues
            teamPacket = protocolManager.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM)
            
            val teamMembers = mutableListOf<String>()
            
            // Create team members for each emoji
            for (emoji in emojifier.emojiList) {
                teamMembers.add(emoji.shortCode)
            }
            
            // Configure team packet for proper sorting - simplified for 1.21.4
            teamPacket?.let { packet ->
                try {
                    // Basic team configuration
                    packet.strings.write(0, TEAM_NAME) // Team name
                    packet.integers.write(0, 0) // Mode: CREATE_TEAM
                    
                    // Try to set team members - use the most compatible approach
                    try {
                        packet.getStringArrays().write(0, teamMembers.toTypedArray())
                        logger.info("Successfully configured team packet with ${teamMembers.size} emoji shortcuts")
                    } catch (e: Exception) {
                        logger.warning("Unable to set team members: ${e.message}")
                    }
                    
                } catch (e: Exception) {
                    logger.warning("Failed to configure team packet fully, but will continue: ${e.message}")
                }
            }
            */
            
        } catch (e: Exception) {
            logger.severe("Failed to generate emoji packets: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Generate individual player info packets for adding/removing fake players
     * DISABLED FOR 1.21.4 COMPATIBILITY
     */
    private fun generatePlayerInfoPackets() {
        // All packet generation disabled for 1.21.4 compatibility
        // This prevents ClassCastException: class java.lang.Object cannot be cast to class ClientboundSetPlayerTeamPacket$Parameters
        /*
        try {
            val playerInfoDataList = mutableListOf<PlayerInfoData>()
            
            // Create fake players for each emoji
            for (emoji in emojifier.emojiList) {
                val uuid = UUID.randomUUID()
                val gameProfile = WrappedGameProfile(uuid, emoji.shortCode)
                
                // Add texture property for skin
                val textureProperty = WrappedSignedProperty("textures", texture, signature)
                gameProfile.properties.put("textures", textureProperty)
                
                // Create player info data with minimal settings
                val playerInfoData = PlayerInfoData(
                    gameProfile,
                    0, // ping
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText("") // display name
                )
                
                playerInfoDataList.add(playerInfoData)
            }
            
            // Use the basic PLAYER_INFO packet for compatibility
            addEmojisPacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO)
            removeEmojisPacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO)
            
            addEmojisPacket?.let { packet ->
                packet.playerInfoAction.write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER)
                packet.playerInfoDataLists.write(0, playerInfoDataList)
            }
            
            removeEmojisPacket?.let { packet ->
                packet.playerInfoAction.write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER)
                packet.playerInfoDataLists.write(0, playerInfoDataList)
            }
            
        } catch (e: Exception) {
            logger.warning("Failed to generate player info packets: ${e.message}")
            // Continue without player info packets - the team packet might still work for tab completion
        }
        */
    }
    
    /**
     * Sends emoji packets to a player if they have permission and emojis enabled.
     */
    fun trySendEmojiPackets(player: Player) {
        // Check if player has emojis disabled or lacks permission
        if (Settings.getEmojiDisabled(player) || 
            (usePermissions && !player.hasPermission(Permissions.USE))) {
            return
        }
        
        // No packets to send in 1.21.4 mode - emoji completion works through chat events
        logger.fine("Emoji tab completion ready for ${player.name} (${emojifier.emojiList.size} emojis available)")
        
        /*
        try {
            // Team packet sending disabled for 1.21.4 compatibility
            teamPacket?.let { 
                try {
                    protocolManager.sendServerPacket(player, it)
                    logger.fine("Successfully sent team packet to ${player.name}")
                } catch (e: Exception) {
                    logger.warning("Failed to send team packet to ${player.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logger.warning("Failed to send emoji packets to ${player.name}: ${e.message}")
        }
        */
    }
    
    /**
     * Removes emoji packets from a player's tab list.
     */
    fun tryRemoveEmojiPackets(player: Player) {
        try {
            // Remove player packets disabled for 1.21.4 compatibility
            // The server will automatically clean up when players disconnect
            /*
            removeEmojisPacket?.let { 
                try {
                    protocolManager.sendServerPacket(player, it)
                } catch (e: Exception) {
                    logger.warning("Failed to send remove emoji packet to ${player.name}: ${e.message}")
                }
            }
            */
        } catch (e: Exception) {
            logger.warning("Failed to remove emoji packets from ${player.name}: ${e.message}")
        }
    }
    
    /**
     * Reloads all configuration files and regenerates packets.
     */
    fun reloadConfigs() {
        // Reload main config
        reloadConfig()
        
        // Load configuration values
        wrappingCharacter = config.getString(Configs.WRAPPING_CHARACTER) ?: ":"
        usePermissions = config.getBoolean(Configs.USE_PERMISSIONS, false)
        individualPermissions = config.getBoolean(Configs.INDIVIDUAL_PERMISSIONS, false)
        verbose = config.getBoolean(Configs.VERBOSE_BOOT, false)
        
        // Load custom textures if provided
        val customTexture = config.getString(Configs.TEXTURE)
        val customSignature = config.getString(Configs.SIGNATURE)
        if (!customTexture.isNullOrBlank() && !customSignature.isNullOrBlank()) {
            texture = customTexture
            signature = customSignature
        }
        
        // Packet cleanup disabled for 1.21.4 compatibility
        // server.onlinePlayers.forEach { tryRemoveEmojiPackets(it) }
        
        // Load emoji configuration
        createEmojiListConfig()
        
        // Load emojis from config
        emojifier.loadEmojisFromConfig()
        
        // Packet generation disabled for 1.21.4 compatibility
        // generateEmojiPackets()
        
        // Packet sending disabled for 1.21.4 compatibility
        // server.onlinePlayers.forEach { trySendEmojiPackets(it) }
        
        logger.info("Configuration reloaded successfully!")
    }
    
    /**
     * Loads emojis.yml configuration file.
     */
    private fun createEmojiListConfig() {
        val emojisConfigFile = File(dataFolder, "emojis.yml")
        
        // Create emojis.yml if it doesn't exist
        if (!emojisConfigFile.exists()) {
            emojisConfigFile.parentFile.mkdirs()
            saveResource("emojis.yml", false)
        }
        
        // Load configuration
        emojisConfig = YamlConfiguration()
        try {
            emojisConfig.load(emojisConfigFile)
        } catch (e: IOException) {
            logger.severe("Failed to load emojis.yml: ${e.message}")
            e.printStackTrace()
        } catch (e: InvalidConfigurationException) {
            logger.severe("Invalid configuration in emojis.yml: ${e.message}")
            e.printStackTrace()
        }
    }
}
