package com.example.jarvis.rag

import com.example.jarvis.domain.model.MemoryFact
import com.example.jarvis.domain.repository.JarvisRepository

class RAGEngine(
    private val repository: JarvisRepository,
    private val retriever: LightweightRetriever = LightweightRetriever()
) {

    /**
     * Retrieve relevant knowledge chunks matching [query], including user facts & custom docs.
     */
    suspend fun retrieveRelevantContext(query: String, topK: Int = 3): List<KnowledgeChunk> {
        val userFacts = try {
            repository.searchFacts(query).map { fact ->
                KnowledgeChunk(
                    id = "fact_${fact.key.hashCode()}",
                    title = "Yadda Saxlanılmış Fakt: ${fact.key}",
                    content = fact.value,
                    source = KnowledgeSource.USER_NOTES
                )
            }
        } catch (_: Exception) { emptyList() }

        val customDocs = try {
            repository.searchKnowledgeDocs(query).map { doc ->
                KnowledgeChunk(
                    id = doc.docId,
                    title = doc.title,
                    content = doc.content,
                    source = KnowledgeSource.IMPORTED_DOC
                )
            }
        } catch (_: Exception) { emptyList() }

        val dynamicChunks = userFacts + customDocs
        return retriever.search(query, dynamicChunks, topK)
    }

    /**
     * Produce an instant offline answer from retrieved knowledge if a strong match is found.
     */
    suspend fun answerIfKnowledgeAvailable(query: String): String? {
        val relevant = retrieveRelevantContext(query, topK = 2)
        if (relevant.isEmpty()) return null

        val topMatch = relevant.first()
        if (topMatch.score >= 1.0f) {
            return "${topMatch.title}:\n${topMatch.content}"
        }
        return null
    }
}
