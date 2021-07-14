package com.github.remynfv.emojitab

import com.github.remynfv.emojitab.utils.Messager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import org.bukkit.configuration.file.FileConfiguration
import org.jetbrains.annotations.NotNull

class Emojifier(private val plugin: EmojiTab)
{
    //Hashmap that stores all :shortcode: -> Emoji pairs
    var emojiMap = HashMap<String, String>()

    //Returns a string with shortcodes replaced by emojis
    fun emojifyString(message: String): String
    {
        var newMessage = message
        for (shortcode in emojiMap.keys)
        {
            if (message.contains(shortcode))
            {
                newMessage = message.replace(shortcode, emojiMap.getValue(shortcode))
            }
        }
        return newMessage
    }

    //Returns Component with shortcodes replaced by emojis
    fun emojifyMessage(message: @NotNull Component): Component
    {
        Messager.send(message.toString())

        var newMessage = message
        for (shortcode in emojiMap.keys)
        {
            //Regex string was broken here, replaced with non regex version to get it working
            val replacement: TextReplacementConfig = TextReplacementConfig.builder().matchLiteral("$shortcode").replacement(emojiMap.getValue(shortcode)).build()
            newMessage = newMessage.replaceText(replacement)
        }
        return newMessage
    }

    //Reads from emojis.yml and saves pairs to a hashmap
    fun loadEmojisFromConfig()
    {
        emojiMap = HashMap()
        val config: FileConfiguration = plugin.getEmojisConfig()
        val keys: MutableSet<String> = checkNotNull(config.getConfigurationSection("emojis")?.getKeys(false))

        for (character: String in keys)
        {
            //Register the main emoji
            val name = config.getString("emojis.$character.name")
            name?.let { registerEmoji(character, it) }

            //Register a list of aliases
            val aliases = config.getStringList("emojis.$character.aliases")
            if (!aliases.isNullOrEmpty())
            {
                for (alias in aliases)
                {
                    if (!alias.isNullOrBlank())
                        registerEmoji(character, alias)
                }
            }
            else //If the "aliases" is null, it must not be a list, so it is therefore a string
            {
                //Register a single string alias
                val alias = config.getString("emojis.$character.aliases")
                alias?.let { registerEmoji(character, it) }
            }
        }
    }

    private fun registerEmoji(character: String, shortcode: String)
    {
        val wrappingCharacter = plugin.wrappingCharacter
        val shortcodeWithWrapping = wrappingCharacter + shortcode + wrappingCharacter

        if (emojiMap.containsKey(shortcodeWithWrapping))
        {
            Messager.warn("Duplicate emoji name \"$shortcode\" Please double check your emojis.yml file!")
        }

        //Log emojis if verbose
        if(plugin.verbose)
        {
            if (!emojiMap.containsKey(character))
                Messager.send("Registered emoji $character to shortcode $shortcodeWithWrapping")
            else
                Messager.send("§8Registered emoji $character to shortcode $shortcodeWithWrapping (as alias)")
        }

        emojiMap[shortcodeWithWrapping] = character
    }
}