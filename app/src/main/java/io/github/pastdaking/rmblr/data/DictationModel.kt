package io.github.pastdaking.rmblr.data

enum class TranscriptionMode(val title: String, val subtitle: String, val iconName: String) {
    POST_PROCESS_CLEANUP(
        title = "AI Smart Polish",
        subtitle = "Removes filler words, fixes grammar, enhances tone with Gemini",
        iconName = "AutoFixHigh"
    ),
    DIRECT_VERBATIM(
        title = "Direct Verbatim",
        subtitle = "Transcribes speech exactly as spoken with raw accuracy",
        iconName = "RecordVoiceOver"
    )
}

enum class CleanupPreset(val displayName: String, val systemPrompt: String, val description: String) {
    SMART_CLEAN(
        displayName = "Clean & Smooth",
        systemPrompt = "Clean up this spoken speech transcript. Remove filler words (um, uh, like, you know, sort of, repeated words, false starts, stutters), fix punctuation and grammar, but preserve the exact meaning and natural tone. Output ONLY the cleaned text with no introductory or meta comments.",
        description = "Removes stutters, filler words, and awkward pauses."
    ),
    FORMAL_EMAIL(
        displayName = "Professional Email",
        systemPrompt = "Convert this voice dictation into a clear, professional, well-structured business email or message. Use polite, concise, corporate tone. Output ONLY the formatted text with no quotes or meta remarks.",
        description = "Turns thoughts into polite, crisp professional prose."
    ),
    CASUAL_CHAT(
        displayName = "Casual Chat",
        systemPrompt = "Transform this spoken transcript into a natural, friendly chat message for messaging apps. Keep it expressive and relaxed. Output ONLY the message text.",
        description = "Great for WhatsApp, iMessage, and casual chats."
    ),
    BULLET_POINTS(
        displayName = "Bullet Points",
        systemPrompt = "Format this spoken dictation into clear, concise, well-organized bullet points highlighting key ideas, action items, or discussion notes. Output ONLY the bulleted text.",
        description = "Converts stream-of-consciousness into structured notes."
    ),
    FIX_GRAMMAR(
        displayName = "Grammar & Punctuation",
        systemPrompt = "Fix all spelling, capitalization, punctuation, and grammatical mistakes in this transcript without altering vocabulary or tone. Output ONLY the corrected text.",
        description = "Minimal intervention, perfect syntax."
    ),
    CONCISE_SUMMARY(
        displayName = "Executive Summary",
        systemPrompt = "Summarize the key points and essential takeaway from this voice note in 1 to 2 crisp, high-impact sentences. Output ONLY the summary.",
        description = "Distills long dictation into punchy highlights."
    ),
    CUSTOM(
        displayName = "Custom Prompt",
        systemPrompt = "",
        description = "Use your custom instructions configured in AI Voice Studio."
    )
}

data class DictationHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val rawText: String,
    val cleanedText: String,
    val mode: TranscriptionMode,
    val preset: CleanupPreset,
    val durationSeconds: Int = 0
)

data class SnippetItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val shortcut: String
)
