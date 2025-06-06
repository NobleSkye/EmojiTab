package com.github.remynfv.emojitab.utils

import org.bukkit.entity.Player
import java.util.*

/**
 * Manages player-specific settings like emoji toggle state.
 */
object Settings {
    
    private val disabledPlayers = mutableSetOf<UUID>()
    
    /**
     * Checks if emojis are disabled for the given player.
     */
    fun getEmojiDisabled(player: Player): Boolean {
        return disabledPlayers.contains(player.uniqueId)
    }
    
    /**
     * Sets the emoji disabled state for the given player.
     */
    fun setEmojiDisabled(player: Player, disabled: Boolean) {
        if (disabled) {
            disabledPlayers.add(player.uniqueId)
        } else {
            disabledPlayers.remove(player.uniqueId)
        }
    }
    
    /**
     * Clears all player settings (useful for plugin reload).
     */
    fun clearAllSettings() {
        disabledPlayers.clear()
    }
    
    /**
     * Gets the number of players who have emojis disabled.
     */
    fun getDisabledPlayerCount(): Int {
        return disabledPlayers.size
    }
}
