package decisionmatrix.db

import decisionmatrix.OptionCriteriaScore
import decisionmatrix.OptionCriteriaScoreInput
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

interface OptionCriteriaScoreRepository {
    fun insert(decisionId: Long, optionId: Long, criteriaId: Long, scoredBy: String, score: OptionCriteriaScoreInput): OptionCriteriaScore = throw NotImplementedError()
    fun findById(id: Long): OptionCriteriaScore? = throw NotImplementedError()
    fun update(id: Long, score: Int): OptionCriteriaScore? = throw NotImplementedError()
    fun delete(id: Long): Boolean = throw NotImplementedError()
}

class OptionCriteriaScoreRepositoryImpl(private val jdbi: Jdbi) : OptionCriteriaScoreRepository {
    override fun insert(decisionId: Long, optionId: Long, criteriaId: Long, scoredBy: String, score: OptionCriteriaScoreInput): OptionCriteriaScore {
        return jdbi.withHandle<OptionCriteriaScore, Exception> { handle ->
            handle.createQuery(
                """
                INSERT INTO option_criteria_scores (decision_id, option_id, criteria_id, scored_by, score) 
                VALUES (:decisionId, :optionId, :criteriaId, :scoredBy, :score)
                RETURNING *
                """.trimIndent()
            )
                .bind("decisionId", decisionId)
                .bind("optionId", optionId)
                .bind("criteriaId", criteriaId)
                .bind("scoredBy", scoredBy)
                .bind("score", score.score)
                .map { rs, _ -> mapOptionCriteriaScore(rs) }
                .one()
        }
    }



    override fun findById(id: Long): OptionCriteriaScore? {
        return jdbi.withHandle<OptionCriteriaScore?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, decision_id, option_id, criteria_id, scored_by, score
                FROM option_criteria_scores
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ -> mapOptionCriteriaScore(rs) }
                .findOne()
                .orElse(null)
        }
    }

    override fun update(id: Long, score: Int): OptionCriteriaScore? {
        return jdbi.withHandle<OptionCriteriaScore?, Exception> { handle ->
            handle.createQuery(
                """
                UPDATE option_criteria_scores
                SET score = :score
                WHERE id = :id
                RETURNING *
                """.trimIndent()
            )
                .bind("id", id)
                .bind("score", score)
                .map { rs, _ -> mapOptionCriteriaScore(rs) }
                .findOne()
                .orElse(null)
        }
    }

    override fun delete(id: Long): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val updated = handle.createUpdate(
                """
                DELETE FROM option_criteria_scores
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .execute()
            updated > 0
        }
    }
}

private fun mapOptionCriteriaScore(rs: ResultSet): OptionCriteriaScore {
    return OptionCriteriaScore(
        id = rs.getLong("id"),
        decisionId = rs.getLong("decision_id"),
        optionId = rs.getLong("option_id"),
        criteriaId = rs.getLong("criteria_id"),
        scoredBy = rs.getString("scored_by"),
        score = rs.getInt("score"),
    )
}
