package decisionmatrix.db

import decisionmatrix.OptionScore
import org.jdbi.v3.core.Jdbi

class OptionScoreRepository(private val jdbi: Jdbi) {

    fun insert(score: OptionScore): Long {
        return jdbi.withHandle<Long, Exception> { handle ->
            handle.createUpdate(
                """
                INSERT INTO option_scores (option_id, score) 
                VALUES (:optionId, :score)
                """.trimIndent()
            )
                .bind("optionId", score.optionId)
                .bind("score", score.score)
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Long::class.javaObjectType)
                .one()
        }
    }

    fun findById(id: Long): OptionScore? {
        return jdbi.withHandle<OptionScore?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, option_id, score
                FROM option_scores
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ ->
                    OptionScore(
                        id = rs.getLong("id"),
                        optionId = rs.getLong("option_id"),
                        score = rs.getInt("score"),
                    )
                }
                .findOne()
                .orElse(null)
        }
    }
}
