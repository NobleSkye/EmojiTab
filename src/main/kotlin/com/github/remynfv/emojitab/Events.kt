package com.github.remynfv.emojitab

import com.github.remynfv.emojitab.utils.Permissions
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Event listener for handling player actions and chat events.
 */
class Events(private val plugin: EmojiTab) : Listener {
    
    /**
     * Handles chat messages and applies emoji transformations.
     * Uses the newer AsyncChatEvent for better performance.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onAsyncChat(event: AsyncChatEvent) {
        val player = event.player
        
        // Check if player has permission to use emojis
        if (plugin.usePermissions && !player.hasPermission(Permissions.USE)) {
            return
        }
        
        // Transform the message to include emojis
        val originalMessage = event.message()
        val emojifiedMessage = plugin.emojifier.emojifyMessage(originalMessage, player)
        
        // Update the event with the transformed message
        event.message(emojifiedMessage)
    }
    
    /**
     * Sends emoji packets to players when they join the server.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // Packet sending disabled for 1.21.4 compatibility
        // plugin.server.scheduler.runTaskLater(plugin, Runnable {
        //     plugin.trySendEmojiPackets(player)
        // }, 20L) // 1 second delay
    }
    
    /**
     * Clean up when players leave (optional, packets are automatically cleaned up).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        // Packet cleanup disabled for 1.21.4 compatibility
        // The server automatically cleans up player info packets when players disconnect
        // plugin.tryRemoveEmojiPackets(event.player)
    }
}
