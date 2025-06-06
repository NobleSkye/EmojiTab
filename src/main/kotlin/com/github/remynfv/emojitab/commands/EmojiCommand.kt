package com.github.remynfv.emojitab.commands

import com.github.remynfv.emojitab.EmojiTab
import com.github.remynfv.emojitab.utils.Messager
import com.github.remynfv.emojitab.utils.Permissions
import com.github.remynfv.emojitab.utils.Settings
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Command handler for the /emoji command.
 */
class EmojiCommand(private val plugin: EmojiTab) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (args.getOrNull(0)?.lowercase()) {
            "list" -> handleListCommand(sender, args)
            "reload" -> handleReloadCommand(sender)
            "toggle" -> handleToggleCommand(sender)
            "help" -> handleHelpCommand(sender)
            else -> handleHelpCommand(sender)
        }
        return true
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return when (args.size) {
            1 -> listOf("list", "reload", "toggle", "help").filter { 
                it.startsWith(args[0], ignoreCase = true) 
            }
            2 -> when (args[0].lowercase()) {
                "list" -> listOf("all", "page").filter { 
                    it.startsWith(args[1], ignoreCase = true) 
                }
                else -> emptyList()
            }
            else -> emptyList()
        }
    }
    
    /**
     * Handles the list subcommand to show available emojis.
     */
    private fun handleListCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission(Permissions.USE)) {
            sender.sendMessage(Messager.noPermission())
            return
        }
        
        val emojis = plugin.emojifier.emojiList
        if (emojis.isEmpty()) {
            sender.sendMessage(Component.text("No emojis are currently loaded!", NamedTextColor.YELLOW))
            return
        }
        
        // Parse page number
        val pageSize = 20
        val page = if (args.size > 1) {
            args[1].toIntOrNull() ?: 1
        } else 1
        
        val totalPages = (emojis.size + pageSize - 1) / pageSize
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, emojis.size)
        
        if (page < 1 || page > totalPages) {
            sender.sendMessage(Component.text("Invalid page number! Valid pages: 1-$totalPages", NamedTextColor.RED))
            return
        }
        
        // Header
        sender.sendMessage(
            Component.text("=== Emojis (Page $page/$totalPages) ===", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
        )
        
        // List emojis for this page
        for (i in startIndex until endIndex) {
            val emoji = emojis[i]
            val component = Component.text("${emoji.character} ", NamedTextColor.WHITE)
                .append(Component.text(emoji.shortCode, NamedTextColor.GRAY))
                .hoverEvent(HoverEvent.showText(
                    Component.text("Click to insert: ${emoji.shortCode}", NamedTextColor.YELLOW)
                ))
                .clickEvent(ClickEvent.suggestCommand(emoji.shortCode))
            
            sender.sendMessage(component)
        }
        
        // Footer with navigation
        if (totalPages > 1) {
            val footer = Component.text("Page $page of $totalPages", NamedTextColor.GRAY)
            
            if (page > 1) {
                footer.append(Component.text(" [Previous]", NamedTextColor.BLUE)
                    .clickEvent(ClickEvent.runCommand("/emoji list ${page - 1}"))
                    .hoverEvent(HoverEvent.showText(Component.text("Go to page ${page - 1}"))))
            }
            
            if (page < totalPages) {
                footer.append(Component.text(" [Next]", NamedTextColor.BLUE)
                    .clickEvent(ClickEvent.runCommand("/emoji list ${page + 1}"))
                    .hoverEvent(HoverEvent.showText(Component.text("Go to page ${page + 1}"))))
            }
            
            sender.sendMessage(footer)
        }
        
        sender.sendMessage(Component.text("Total: ${emojis.size} emojis", NamedTextColor.GRAY))
    }
    
    /**
     * Handles the reload subcommand to reload plugin configuration.
     */
    private fun handleReloadCommand(sender: CommandSender) {
        if (!sender.hasPermission(Permissions.RELOAD)) {
            sender.sendMessage(Messager.noPermission())
            return
        }
        
        try {
            plugin.reloadConfigs()
            sender.sendMessage(Component.text("EmojiTab configuration reloaded successfully!", NamedTextColor.GREEN))
        } catch (e: Exception) {
            sender.sendMessage(Component.text("Failed to reload configuration: ${e.message}", NamedTextColor.RED))
            plugin.logger.severe("Failed to reload configuration: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Handles the toggle subcommand to toggle emoji display for the player.
     */
    private fun handleToggleCommand(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED))
            return
        }
        
        if (!sender.hasPermission(Permissions.TOGGLE)) {
            sender.sendMessage(Messager.noPermission())
            return
        }
        
        val currentlyDisabled = Settings.getEmojiDisabled(sender)
        Settings.setEmojiDisabled(sender, !currentlyDisabled)
        
        if (currentlyDisabled) {
            // Emojis were disabled, now enabling them
            // Packet sending disabled for 1.21.4 compatibility
            // plugin.trySendEmojiPackets(sender)
            sender.sendMessage(Component.text("Emoji chat completion enabled!", NamedTextColor.GREEN))
        } else {
            // Emojis were enabled, now disabling them
            // Packet removal disabled for 1.21.4 compatibility
            // plugin.tryRemoveEmojiPackets(sender)
            sender.sendMessage(Component.text("Emoji chat completion disabled!", NamedTextColor.YELLOW))
        }
    }
    
    /**
     * Shows help information for the emoji command.
     */
    private fun handleHelpCommand(sender: CommandSender) {
        sender.sendMessage(
            Component.text("=== EmojiTab Commands ===", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
        )
        
        val commands = listOf(
            "/emoji list [page]" to "Shows available emojis",
            "/emoji toggle" to "Toggle emoji tab completion",
            "/emoji reload" to "Reload plugin configuration (admin)",
            "/emoji help" to "Show this help message"
        )
        
        for ((cmd, desc) in commands) {
            sender.sendMessage(
                Component.text(cmd, NamedTextColor.AQUA)
                    .append(Component.text(" - $desc", NamedTextColor.GRAY))
            )
        }
        
        sender.sendMessage(
            Component.text("\nTip: Start typing an emoji shortcode (like :cloud:) and press TAB!", NamedTextColor.YELLOW)
        )
    }
}
