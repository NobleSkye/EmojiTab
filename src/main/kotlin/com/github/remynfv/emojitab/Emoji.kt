package com.github.remynfv.emojitab

/**
 * Represents a single emoji with its character and shortcode.
 * 
 * @param character The actual emoji character or unicode string (e.g., "☁", "😀")
 * @param shortCode The complete shortcode including wrapping characters (e.g., ":cloud:", ":smile:")
 * @param unwrappedShortCode The shortcode without wrapping characters (e.g., "cloud", "smile")
 */
data class Emoji(
    val character: String,
    val shortCode: String,
    val unwrappedShortCode: String
) {
    
    /**
     * True if this emoji's character is the same as its shortcode.
     * This means it doesn't need to be processed for replacement in chat.
     */
    val canBeSkipped: Boolean = character == shortCode
    
    /**
     * The display name for this emoji (shortcode without wrapping characters).
     */
    val displayName: String = unwrappedShortCode
    
    init {
        require(shortCode.length <= 16) {
            "Emoji shortcode cannot be longer than 16 characters: '$shortCode'"
        }
        require(character.isNotBlank()) {
            "Emoji character cannot be blank"
        }
        require(shortCode.isNotBlank()) {
            "Emoji shortcode cannot be blank"
        }
        require(unwrappedShortCode.isNotBlank()) {
            "Emoji unwrapped shortcode cannot be blank"
        }
    }
    
    override fun toString(): String {
        return "Emoji(character='$character', shortCode='$shortCode')"
    }
}
