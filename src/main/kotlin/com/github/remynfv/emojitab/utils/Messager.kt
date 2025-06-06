package com.github.remynfv.emojitab.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit

/**
 * Utility class for sending formatted messages to console and players.
 */
object Messager {
    
    private const val PREFIX = "[EmojiTab]"
    
    /**
     * Sends an info message to the console.
     */
    fun send(message: String) {
        Bukkit.getLogger().info("$PREFIX $message")
    }
    
    /**
     * Sends a warning message to the console.
     */
    fun warn(message: String) {
        Bukkit.getLogger().warning("$PREFIX $message")
    }
    
    /**
     * Sends an error message to the console.
     */
    fun error(message: String) {
        Bukkit.getLogger().severe("$PREFIX $message")
    }
    
    /**
     * Creates a "no permission" message component.
     */
    fun noPermission(): Component {
        return Component.text("You don't have permission to use this command!", NamedTextColor.RED)
    }
    
    /**
     * Creates a formatted info message component.
     */
    fun info(message: String): Component {
        return Component.text(message, NamedTextColor.GRAY)
    }
    
    /**
     * Creates a formatted success message component.
     */
    fun success(message: String): Component {
        return Component.text(message, NamedTextColor.GREEN)
    }
    
    /**
     * Creates a formatted warning message component.
     */
    fun warning(message: String): Component {
        return Component.text(message, NamedTextColor.YELLOW)
    }
    
    /**
     * Creates a formatted error message component.
     */
    fun errorComponent(message: String): Component {
        return Component.text(message, NamedTextColor.RED)
    }
}
