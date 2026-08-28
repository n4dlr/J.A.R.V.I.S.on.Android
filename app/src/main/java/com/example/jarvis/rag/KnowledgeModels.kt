package com.example.jarvis.rag

enum class KnowledgeSource {
    JARVIS_DOCS,
    ANDROID_HELP,
    USER_NOTES,
    IMPORTED_DOC
}

data class KnowledgeChunk(
    val id: String,
    val title: String,
    val content: String,
    val source: KnowledgeSource,
    val score: Float = 0f
)
