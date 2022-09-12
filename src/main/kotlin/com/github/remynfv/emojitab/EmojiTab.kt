package com.github.remynfv.emojitab

import com.comphenix.packetwrapper.WrapperPlayServerNamedEntitySpawn
import com.comphenix.packetwrapper.WrapperPlayServerPlayerInfo
import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.*
import com.github.remynfv.emojitab.commands.EmojiCommand
import com.github.remynfv.emojitab.utils.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.io.IOException
import java.util.*


private const val displayName: String = "" //Any character that is invisible

class EmojiTab : JavaPlugin()
{
    //From config.yml
    var usePermissions: Boolean = true
    var individualPermissions: Boolean = false
    var verbose: Boolean = false
    var wrappingCharacter: String = ""

    //Dim gray, courtesy of someone off mineskin.org (https://mineskin.org/14b3cfc390dc440282195d8a74b742f4)
    private var texture: String = "ewogICJ0aW1lc3RhbXAiIDogMTYyMTQxMTE5MDkyMywKICAicHJvZmlsZUlkIiA6ICJmZDQ3Y2I4YjgzNjQ0YmY3YWIyYmUxODZkYjI1ZmMwZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJDVUNGTDEyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQ4NDYxNmVhNDI0OTk1NzI4OGE5Y2Y4ZTNhM2E0ZjVjZDU0NDYxNjk1ZTczMmM5ZWViOTA4NDBmZDRkYzg3YjQiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ=="
    private var signature: String = "vAk/+xJkgEYANJq2FxjfX4xT5Lo+z1+YNnvWPUgLnpwgj3Vq1nqKZ24y0mHbsLROE3JCnOW1vJObFyNRBktInFXX5RhAv8yis/TSyFFhR3rjnC8ZEMSlM0gyy2K9nJxjY+jDSVBNBaBmWs1JbhPWl2zN/eaMEMivAwZmBLqhTLIV/o4IAUAIPDkxdEw5MGtp81wEot1YSMc1PkGYANx7VTGUy2eCe4AhjDgUrWLkGPkSWeCowU1xQzT5DeWw5V6sylRWXR7DTkzonteRA5jO4gXrXXt5CdytGbz8SOT9V2xnhUPbnRZOgeRKwwHphAJ4N+g2+C5BGxrfSlnmj8YZKAlM17YEK2ej1eClxmmxIW/2bjZnCJR0U7f750evnXb6ZcjIQ+P400RpSCUo79L9cbvz3rHU36IcHKl3GmGG9uyr15C6DVa5WGj5A19fmzIMyRG5e5GTH6NPVC+yK5R0M36in88iP1HQFY9CdPn9NixrdRcCcXPcOcKFsNXE6la+UMhSlsXX+FS5zGtMvTedn5fPglP0DWur9Iz4Z/Bk5ZoZ93NdpF/h63rLZG9xYBs+gf8UEESPRykZSB2wIRO4039s3TC4g8i/lUBn4Zt6IpUiXip9rK7ihKdy3bVX8YywxmCL9oqhfQK0jnFk1dPDBCs/QDMCYnP4fLkLEqPZRrI="

    //This is emojis.yml
    private lateinit var emojisConfig: FileConfiguration

    //The Great Emojifier class, where most of the work gets done
    lateinit var emojifier: Emojifier

    /**
     * The "textures" property that contains the skin information for the autogenerated emoji players.
     */
    private lateinit var defaultTexturesProperty: WrappedSignedProperty //Init in generateEmojiPackets

    /**
     * Packet to add all emojis.
     */
    private lateinit var addEmojisPacket: WrapperPlayServerPlayerInfo

    /**
     * Packet to remove all emojis.
     */
    private var removeEmojisPacket: WrapperPlayServerPlayerInfo? = null

    //Declare ProtocolManager
    private lateinit var protocolManager: ProtocolManager

    /*
    TODO Move player update functions into own class, declutter this class
     */
    override fun onEnable()
    {
        // Plugin startup logic
        Messager.send("Loaded!")

        //Init protocol manager
        protocolManager = ProtocolLibrary.getProtocolManager()

        Bukkit.shouldSendChatPreviews()

        //Save Configs
        saveDefaultConfig()

        //Initialize emoji list
        emojifier = Emojifier(this)

        //(Re)load configs for the first time
        reloadConfigs()

        //Register commands
        getCommand("emoji")!!.setExecutor(EmojiCommand(this))

        //Register events
        server.pluginManager.registerEvents(Events(this), this)
        registerEntitySpawnListener()
        registerRemovePlayerInfoListener()

        //Load emojis for any players who are online already
        for (player in Bukkit.getOnlinePlayers())
            sendEmojiPackets(player)
    }

    private fun registerRemovePlayerInfoListener()
    {
        protocolManager.addPacketListener(
            object : PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO)
            {
                override fun onPacketSending(event: PacketEvent)
                {
                    if (event.packet.playerInfoAction.readSafely(0) == EnumWrappers.PlayerInfoAction.REMOVE_PLAYER)
                    {
                        //Create a wrapper and get the player
                        val wrapper = WrapperPlayServerPlayerInfo(event.packet)

                        //If p is null, we're re-reversing this thing and we can escape the loop
                        val p = Bukkit.getPlayer(wrapper.data.first().profile.uuid) ?: return

                        removeFlippedUUIDFromTab(p).sendPacket(event.player)
                        removeDerivedUUIDFromTab(p).sendPacket(event.player)
                    }
                }
            })
    }

    private fun registerEntitySpawnListener()
    {
        protocolManager.addPacketListener(
            object : PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.NAMED_ENTITY_SPAWN)
            {
                override fun onPacketSending(event: PacketEvent)
                {
                    //Get event player and a packet wrapper
                    val newPlayer = event.player
                    val wrapper = WrapperPlayServerNamedEntitySpawn(event.packet)

                    //Send ADD_PLAYER packet here to briefly change their username from "" to their actual name
                    val playerInfoPacket = WrapperPlayServerPlayerInfo()
                    playerInfoPacket.action = EnumWrappers.PlayerInfoAction.ADD_PLAYER

                    val p: Player = Bukkit.getPlayer(wrapper.playerUUID)!!
                    val uuid = p.uniqueId

                    val gameProfile = WrappedGameProfile(uuid, p.name)

                    val originalProperties = WrappedGameProfile.fromPlayer(p).properties
                    gameProfile.properties.putAll(originalProperties)

                    //Get player display name
                    val json = getPlayerNameForList(p)

                    val info = PlayerInfoData(gameProfile, p.ping, EnumWrappers.NativeGameMode.valueOf(p.gameMode.name), WrappedChatComponent.fromJson(json))
                    playerInfoPacket.data = List(1) { info }

                    //Send packet.
                    playerInfoPacket.sendPacket(newPlayer)

                    //Schedule a player update to make sure they're definitely reset after being loaded
                    object : BukkitRunnable()
                    {
                        override fun run()
                        {
                            updatePlayerForPlayer(newPlayer, p, false, true)
                            updatePlayerForPlayer(newPlayer, p, true, false)
                        }
                    }.runTaskLater(plugin, 1)
                }
            }
        )
    }

    private fun generateEmojiPackets()
    {
        //Create a list of players of size = emojiMap.keys.size
        addEmojisPacket = WrapperPlayServerPlayerInfo()
        addEmojisPacket.action = EnumWrappers.PlayerInfoAction.ADD_PLAYER

        val info = ArrayList<PlayerInfoData>()
        for (shortcode in emojifier.emojiMap.keys)
        {
            val shortcode2 = shortcode.take(16)
            val randomUUID = UUID.randomUUID()
            val gameProfile = WrappedGameProfile(randomUUID, shortcode2)
            defaultTexturesProperty = WrappedSignedProperty("textures", texture, signature)
            gameProfile.properties.put("textures", defaultTexturesProperty)
            info.add(PlayerInfoData(gameProfile, 0, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(displayName)))

        }
        addEmojisPacket.data = info

        //Configure an identical packet to do the reverse.
        val removeEmojisPacket = WrapperPlayServerPlayerInfo()
        removeEmojisPacket.data = addEmojisPacket.data
        removeEmojisPacket.action = EnumWrappers.PlayerInfoAction.REMOVE_PLAYER
        this.removeEmojisPacket = removeEmojisPacket
    }

    /**
     * Supposed to update visible players. doesn't seem to work?
     */
    private fun updateVisiblePlayers(player: Player, addPlayers: Boolean)
    {
        val updateDisplayNamesPacket = WrapperPlayServerPlayerInfo()
        updateDisplayNamesPacket.action = if (addPlayers) EnumWrappers.PlayerInfoAction.ADD_PLAYER else EnumWrappers.PlayerInfoAction.REMOVE_PLAYER

        val info = ArrayList<PlayerInfoData>()

        for (p in Bukkit.getOnlinePlayers())
        {
            val playerInfoData = getDataPlayerForPlayer(player, p, !addPlayers) ?: continue

            info.addAll(playerInfoData)
        }

        updateDisplayNamesPacket.data = info

//        removeSelf(player)
        updateDisplayNamesPacket.sendPacket(player)
    }

    /**
     * Removes the player from the tab menu for themself.
     */
    fun removeSelf(player: Player)
    {
        val updateDisplayNamesPacket = WrapperPlayServerPlayerInfo()
        updateDisplayNamesPacket.action = EnumWrappers.PlayerInfoAction.REMOVE_PLAYER

        val info = ArrayList<PlayerInfoData>()

        val playerInfoData = getDataPlayerForPlayer(player, player, true)?: return

        info.addAll(playerInfoData)

        updateDisplayNamesPacket.data = info

        updateDisplayNamesPacket.sendPacket(player)
    }

    /**
     * @param observer Player we're sending the packet to.
     * @param targetPlayer Player included in the packet.
     */
    fun updatePlayerForPlayer(observer: Player, targetPlayer: Player, addPlayers: Boolean, realUUIDs: Boolean = false)
    {
        //Create the packet
        val updateDisplayNamesPacket = WrapperPlayServerPlayerInfo()

        //Assign values
        updateDisplayNamesPacket.action = if (addPlayers) EnumWrappers.PlayerInfoAction.ADD_PLAYER else EnumWrappers.PlayerInfoAction.REMOVE_PLAYER

        //Send it
        updateDisplayNamesPacket.data = getDataPlayerForPlayer(observer, targetPlayer, realUUIDs)
        updateDisplayNamesPacket.sendPacket(observer)
    }

    /**
     * @param observer Player we're sending the packet to.
     * @param targetPlayer Player included in the packet.
     *
     * @return Null if vanished or hidden.
     */
    private fun getDataPlayerForPlayer(observer: Player, targetPlayer: Player, useRealUUIDs: Boolean): List<PlayerInfoData>?
    {
        if (VanishAPI.isVanished(targetPlayer)) //Generic vanish "API" support.
            return null

        if (observer.canSee(targetPlayer))
        {
            val playerListNameJson: String = getPlayerNameForList(targetPlayer)
            val targetPlayerUuid = if (useRealUUIDs) targetPlayer.uniqueId else UUID.nameUUIDFromBytes(targetPlayer.uniqueId.toString().toByteArray())

            //Making the name blank magically teleports them to the top of the tab menu
            val targetGameProfile = WrappedGameProfile(targetPlayerUuid, "") //This gets the real UUID so the latency will update

            val originalProperties = WrappedGameProfile.fromPlayer(targetPlayer).properties
            targetGameProfile.properties.putAll(originalProperties)

            /**
             * PlayerInfoData with correct skin, username of "", but correct display name.
             */
            val correctDisplayTopOfListInfoData = PlayerInfoData(targetGameProfile, targetPlayer.ping, EnumWrappers.NativeGameMode.valueOf(targetPlayer.gameMode.name), WrappedChatComponent.fromJson(playerListNameJson))

            //Somewhat strange hack to get a second UUID for a player.
            val uuidBackwards = UUID.fromString(targetPlayer.uniqueId.toString().reversed()) //Somewhat strange hack to get a second UUID for a player.

            //Create the gameprofile for tab completion
            /**
             * Gray user with the correct username but no skin/display name.
             * For tab completion.
             */
            val tabCompletionGrayProfile = WrappedGameProfile(uuidBackwards, targetPlayer.name)
            tabCompletionGrayProfile.properties.put("textures", defaultTexturesProperty)

            val tabCompletionGrayInfoData = PlayerInfoData(tabCompletionGrayProfile, 0, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(displayName))

            return listOf(tabCompletionGrayInfoData, correctDisplayTopOfListInfoData)
        }

        return null
    }

    /**
     * Used for rendering colors and teams in the tab list
     * @return JSON components as string.
     */
    fun getPlayerNameForList(p: Player): String
    {
        //Properly render vanilla scoreboard teams
        val team = p.scoreboard.getEntryTeam(p.name)
        val nameWithTeam: Component = if (team != null)
        {
            val prefix = team.prefix()
            val color = team.color()
            val suffix = team.suffix()
            prefix.append(p.playerListName().color(color)).append(suffix)
        }
        else
        {
            p.playerListName()
        }
        return GsonComponentSerializer.gson().serialize(nameWithTeam)
    }

    //Remove the fake player with the reversed uuid of player
    fun removeFlippedUUIDFromTab(player: Player): WrapperPlayServerPlayerInfo
    {
        val uuidBackwards = UUID.fromString(player.uniqueId.toString().reversed())

        val packet = WrapperPlayServerPlayerInfo()
        packet.action = EnumWrappers.PlayerInfoAction.REMOVE_PLAYER

        val gameProfile = WrappedGameProfile(uuidBackwards, player.name)

        val info = PlayerInfoData(gameProfile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null)
        packet.data = List(1) { info }

        return packet
    }

    /**
     * Remove UUID generated by main uuid
     */
    fun removeDerivedUUIDFromTab(player: Player): WrapperPlayServerPlayerInfo
    {
        val uuidDerived = UUID.nameUUIDFromBytes(player.uniqueId.toString().toByteArray())

        val packet = WrapperPlayServerPlayerInfo()
        packet.action = EnumWrappers.PlayerInfoAction.REMOVE_PLAYER

        val gameProfile = WrappedGameProfile(uuidDerived, "")

        val info = PlayerInfoData(gameProfile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null)
        packet.data = List(1) { info }

        return packet
    }

    /**
     * Sends all necessary packets to player, including emojis and tab-complete fake players
     */
    fun sendEmojiPackets(player: Player)
    {
        //If player has emojis disabled don't send them the players OR the fakeplayers
        if (Settings.getEmojiDisabled(player) || !(player.hasPermission(Permissions.USE) || !usePermissions))
            return

        addEmojisPacket.sendPacket(player)

        updateVisiblePlayers(player, false)
        updateVisiblePlayers(player, true)
    }

    fun sendRemoveEmojiPackets(player: Player)
    {
        removeEmojisPacket?.sendPacket(player)
        for (p in Bukkit.getOnlinePlayers())
        {
            removeFlippedUUIDFromTab(p).sendPacket(player)
            removeDerivedUUIDFromTab(p).sendPacket(player)
        }
    }

    override fun onDisable()
    {
        // Plugin shutdown logic
        removeAllFakePlayers() //Remove autocorrect bois for all players, to avoid clogging up the tab menu if unwanted
    }

    //Removes all fake players for server/config reloads
    private fun removeAllFakePlayers()
    {
        for (player in Bukkit.getOnlinePlayers())
            sendRemoveEmojiPackets(player)
    }

    //Big mama function that reloads everything in the correct order
    fun reloadConfigs()
    {
        reloadConfig()

        //Load config.yml settings in variables
        config.getBoolean(Configs.VERBOSE_BOOT).let { verbose = it }
        config.getBoolean(Configs.USE_PERMISSIONS).let { usePermissions = it }
        config.getBoolean(Configs.INDIVIDUAL_PERMISSIONS).let { individualPermissions = it }
        config.getString(Configs.WRAPPING_CHARACTER)?.let { wrappingCharacter = it }

        //Load custom skins
        val texture = config.getString(Configs.TEXTURE)
        val signature = config.getString(Configs.SIGNATURE)
        if (!texture.isNullOrBlank() && !signature.isNullOrBlank())
        {
            this.texture = texture
            this.signature = signature
        }

        //On first run it will be null
        if (removeEmojisPacket != null)
            removeAllFakePlayers()

        //Load the config into a variable
        createEmojiListConfig()

        //Load emojis into hashmap
        emojifier.loadEmojisFromConfig()

        //Create the packet to be sent out to players
        generateEmojiPackets()
    }

    //Get the FileConfiguration for the emoji list
    fun getEmojisConfig(): FileConfiguration
    {
        return this.emojisConfig
    }

    //Load emojis.yml into emojisConfig
    private fun createEmojiListConfig()
    {
        val emojisConfigFile = File(dataFolder, "emojis.yml")

        //Create emojis.yml if it doesn't exist yet.
        if (!emojisConfigFile.exists())
        {
            emojisConfigFile.parentFile.mkdirs()
            saveResource("emojis.yml", false)
        }

        //Load 'em in and hope it doesn't break
        emojisConfig = YamlConfiguration()
        try
        {
            emojisConfig.load(emojisConfigFile)
        } catch (e: IOException)
        {
            e.printStackTrace()
        } catch (e: InvalidConfigurationException)
        {
            e.printStackTrace()
        }
    }
}