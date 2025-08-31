package decisionmatrix.db

import decisionmatrix.OptionScore
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

interface OptionScoreRepository {
    fun insert(score: OptionScore): OptionScore = throw NotImplementedError("not implemented")
    fun findById(id: Long): OptionScore? = throw NotImplementedError("not implemented")
}

class OptionScoreRepositoryImpl(private val jdbi: Jdbi) : OptionScoreRepository {
    override fun insert(score: OptionScore): OptionScore {
        return jdbi.withHandle<OptionScore, Exception> { handle ->
            handle.createQuery(
                """
                INSERT INTO option_scores (option_id, score) 
                VALUES (:optionId, :score)
                RETURNING *
                """.trimIndent()
            )
                .bind("optionId", score.optionId)
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

    private fun mapOptionScore(rs: ResultSet): OptionScore {
        return OptionScore(
            id = rs.getLong("id"),
            optionId = rs.getLong("option_id"),
            score = rs.getInt("score"),
        )
    }
}
