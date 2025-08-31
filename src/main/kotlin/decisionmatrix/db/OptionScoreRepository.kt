package decisionmatrix.db

import decisionmatrix.OptionScoreInput
import decisionmatrix.OptionScore
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

interface OptionScoreRepository {
    fun insert(optionId: Long, score: OptionScoreInput): OptionScore = throw NotImplementedError()
    fun findById(id: Long): OptionScore? = throw NotImplementedError()
    fun update(id: Long, score: Int): OptionScore? = throw NotImplementedError()
    fun delete(id: Long): Boolean = throw NotImplementedError()
}

class OptionScoreRepositoryImpl(private val jdbi: Jdbi) : OptionScoreRepository {
    override fun insert(optionId: Long, score: OptionScoreInput): OptionScore {
        return jdbi.withHandle<OptionScore, Exception> { handle ->
            handle.createQuery(
                """
                INSERT INTO option_scores (option_id, score) 
                VALUES (:optionId, :score)
                RETURNING *
                """.trimIndent()
            )
                .bind("optionId", optionId)
                .bind("score", score.score)
                .map { rs, _ -> mapOptionScore(rs) }
                .one()
        }
    }

    override fun findById(id: Long): OptionScore? {
        return jdbi.withHandle<OptionScore?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, option_id, score
                FROM option_scores
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ -> mapOptionScore(rs) }
                .findOne()
                .orElse(null)
        }
    }

    override fun update(id: Long, score: Int): OptionScore? {
        return jdbi.withHandle<OptionScore?, Exception> { handle ->
            handle.createQuery(
                """
                UPDATE option_scores
                SET score = :score
                WHERE id = :id
                RETURNING *
                """.trimIndent()
            )
                .bind("id", id)
                .bind("score", score)
                .map { rs, _ -> mapOptionScore(rs) }
                .findOne()
                .orElse(null)
        }
    }

    override fun delete(id: Long): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val updated = handle.createUpdate(
                """
                DELETE FROM option_scores
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .execute()
            updated > 0
        }
    }
}

private fun mapOptionScore(rs: ResultSet): OptionScore {
    return OptionScore(
        id = rs.getLong("id"),
        optionId = rs.getLong("option_id"),
        score = rs.getInt("score"),
    )
}
