package decisionmatrix.db

import decisionmatrix.Tag
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

interface TagRepository {
    fun findOrCreate(name: String): Tag = throw NotImplementedError()
    fun findByPrefix(prefix: String, limit: Int = 10): List<Tag> = throw NotImplementedError()
    fun findByDecisionId(decisionId: Long): List<Tag> = throw NotImplementedError()
    fun addTagToDecision(decisionId: Long, tagId: Long): Unit = throw NotImplementedError()
    fun removeTagFromDecision(decisionId: Long, tagId: Long): Unit = throw NotImplementedError()
}

class TagRepositoryImpl(private val jdbi: Jdbi) : TagRepository {
    override fun findOrCreate(name: String): Tag {
        val normalizedName = name.lowercase()

        return jdbi.withHandle<Tag, Exception> { handle ->
            // Try to find existing tag
            val existing = handle.createQuery(
                """
                SELECT id, name
                FROM tags
                WHERE name = :name
                """.trimIndent()
            )
                .bind("name", normalizedName)
                .map { rs, _ -> mapTag(rs) }
                .findOne()
                .orElse(null)

            if (existing != null) {
                existing
            } else {
                // Create new tag
                handle.createQuery(
                    """
                    INSERT INTO tags (name)
                    VALUES (:name)
                    RETURNING *
                    """.trimIndent()
                )
                    .bind("name", normalizedName)
                    .map { rs, _ -> mapTag(rs) }
                    .one()
            }
        }
    }

    override fun findByPrefix(prefix: String, limit: Int): List<Tag> {
        val normalizedPrefix = prefix.lowercase()

        return jdbi.withHandle<List<Tag>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, name
                FROM tags
                WHERE name LIKE :prefix
                ORDER BY name
                LIMIT :limit
                """.trimIndent()
            )
                .bind("prefix", "$normalizedPrefix%")
                .bind("limit", limit)
                .map { rs, _ -> mapTag(rs) }
                .list()
        }
    }

    override fun findByDecisionId(decisionId: Long): List<Tag> {
        return jdbi.withHandle<List<Tag>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT t.id, t.name
                FROM tags t
                JOIN decision_tags dt ON t.id = dt.tag_id
                WHERE dt.decision_id = :decisionId
                ORDER BY t.name
                """.trimIndent()
            )
                .bind("decisionId", decisionId)
                .map { rs, _ -> mapTag(rs) }
                .list()
        }
    }

    override fun addTagToDecision(decisionId: Long, tagId: Long) {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate(
                """
                INSERT INTO decision_tags (decision_id, tag_id)
                VALUES (:decisionId, :tagId)
                ON CONFLICT (decision_id, tag_id) DO NOTHING
                """.trimIndent()
            )
                .bind("decisionId", decisionId)
                .bind("tagId", tagId)
                .execute()
        }
    }

    override fun removeTagFromDecision(decisionId: Long, tagId: Long) {
        jdbi.withHandle<Unit, Exception> { handle ->
            handle.createUpdate(
                """
                DELETE FROM decision_tags
                WHERE decision_id = :decisionId AND tag_id = :tagId
                """.trimIndent()
            )
                .bind("decisionId", decisionId)
                .bind("tagId", tagId)
                .execute()
        }
    }
}

private fun mapTag(rs: ResultSet): Tag {
    return Tag(
        id = rs.getLong("id"),
        name = rs.getString("name"),
    )
}
