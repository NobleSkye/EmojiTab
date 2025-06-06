package com.github.remynfv.emojitab

import com.github.remynfv.emojitab.utils.Messager
import com.github.remynfv.emojitab.utils.Permissions
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.permissions.Permissible
import java.util.regex.Pattern

/**
 * Handles emoji processing and message transformation.
 */
class Emojifier(private val plugin: EmojiTab) {
    
    /**
     * List of all registered emojis.
     */
    var emojiList = mutableListOf<Emoji>()
        private set
    
    /**
     * Transforms a message by replacing emoji shortcodes with actual emoji characters.
     * 
     * @param message The original message component
     * @param permissionHolder The entity to check permissions for (can be null)
     * @return The message with emoji shortcodes replaced
     */
    fun emojifyMessage(message: Component, permissionHolder: Permissible?): Component {
        var transformedMessage = message
        
        for (emoji in emojiList) {
            // Skip emojis that don't need replacement (character same as shortcode)
            if (emoji.canBeSkipped) continue
            
            // Check individual permissions if enabled
            if (plugin.individualPermissions && 
                permissionHolder?.hasPermission(Permissions.USE_PREFIX + emoji.unwrappedShortCode) == false) {
                continue
            }
            
            // Create case-insensitive pattern for replacement
            val pattern = Pattern.compile(Pattern.quote(emoji.shortCode), Pattern.CASE_INSENSITIVE)
            val replacement = TextReplacementConfig.builder()
                .match(pattern)
                .replacement(emoji.character)
                .build()
            
            transformedMessage = transformedMessage.replaceText(replacement)
        }
        
        return transformedMessage
    }
    
    /**
     * Loads emojis from the configuration file into the emoji list.
     */
    fun loadEmojisFromConfig() {
        emojiList.clear()
        
        val config: FileConfiguration = plugin.emojisConfig
        val emojisSection = config.getConfigurationSection("emojis")
        
        if (emojisSection == null) {
            Messager.warn("No 'emojis' section found in emojis.yml!")
            return
        }
        
        val keys = emojisSection.getKeys(false)
        
        for (character in keys) {
            try {
                loadEmojiFromConfig(character, config)
            } catch (e: Exception) {
                Messager.warn("Failed to load emoji '$character': ${e.message}")
            }
        }
        
        Messager.send("Loaded ${emojiList.size} emoji entries")
    }
    
    /**
     * Loads a single emoji and its aliases from configuration.
     */
    private fun loadEmojiFromConfig(character: String, config: FileConfiguration) {
        val basePath = "emojis.$character"
        
        // Register the main emoji name
        val name = config.getString("$basePath.name")
        if (!name.isNullOrBlank()) {
            registerEmoji(character, name, wrap = true)
        } else {
            // If no name specified, register the character as itself
            registerEmoji(character, character, wrap = false)
        }
        
        // Register aliases
        val aliases = when {
            config.isList("$basePath.aliases") -> {
                config.getStringList("$basePath.aliases")
            }
            config.isString("$basePath.aliases") -> {
                listOf(config.getString("$basePath.aliases")!!)
            }
            else -> emptyList()
        }
        
        for (alias in aliases) {
            if (!alias.isNullOrBlank()) {
                registerEmoji(character, alias, wrap = true)
            }
        }
    }
    
    /**
     * Registers a new emoji with the given character and shortcode.
     * 
     * @param character The emoji character/string
     * @param shortcode The shortcode to register (without wrapping characters)
     * @param wrap Whether to wrap the shortcode with wrapping characters
     */
    private fun registerEmoji(character: String, shortcode: String, wrap: Boolean = true) {
        val wrappingChar = plugin.wrappingCharacter
        val maxLength = 16 - (2 * wrappingChar.length)
        
        // Truncate shortcode if too long
        if (shortcode.length > maxLength) {
            Messager.warn("Emoji shortcode '$shortcode' is too long and will be truncated!")
        }
        
        val truncatedShortcode = shortcode.take(maxLength)
        val finalShortcode = if (wrap) {
            "$wrappingChar$truncatedShortcode$wrappingChar"
        } else {
            truncatedShortcode
        }
        
        // Check for duplicates
        if (emojiList.any { it.shortCode.equals(finalShortcode, ignoreCase = true) }) {
            Messager.warn("Duplicate emoji shortcode '$finalShortcode' - skipping!")
            return
        }
        
        // Create and add the emoji
        val emoji = Emoji(character, finalShortcode, truncatedShortcode)
        emojiList.add(emoji)
        
        // Log if verbose mode is enabled
        if (plugin.verbose) {
            val isAlias = emojiList.any { it.character == character && it != emoji }
            val logMessage = if (isAlias) {
                "§8Registered emoji alias: $character -> $finalShortcode"
            } else {
                "Registered emoji: $character -> $finalShortcode"
            }
            Messager.send(logMessage)
        }
    }
    
    /**
     * Gets all emojis that start with the given prefix (for tab completion suggestions).
     */
    fun getEmojisStartingWith(prefix: String): List<Emoji> {
        return emojiList.filter { 
            it.shortCode.startsWith(prefix, ignoreCase = true) 
        }.sortedBy { it.shortCode }
    }
    
    /**
     * Gets an emoji by its exact shortcode.
     */
    fun getEmojiByShortcode(shortcode: String): Emoji? {
        return emojiList.find { 
            it.shortCode.equals(shortcode, ignoreCase = true) 
        }
    }
    
    /**
     * Gets all unique emoji characters (no duplicates).
     */
    fun getUniqueCharacters(): List<String> {
        return emojiList.map { it.character }.distinct().sorted()
    }
}
