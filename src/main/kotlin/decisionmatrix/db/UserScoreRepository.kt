package decisionmatrix.db

import decisionmatrix.UserScore
import decisionmatrix.UserScoreInput
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

interface UserScoreRepository {
    fun insert(decisionId: Long, optionId: Long, criteriaId: Long, scoredBy: String, score: UserScoreInput): UserScore =
        throw NotImplementedError()

    fun findById(id: Long): UserScore? = throw NotImplementedError()
    fun findAllByDecisionId(decisionId: Long): List<UserScore> = throw NotImplementedError()
    fun update(id: Long, score: Int): UserScore? = throw NotImplementedError()
    fun delete(id: Long): Boolean = throw NotImplementedError()
}

class UserScoreRepositoryImpl(private val jdbi: Jdbi) : UserScoreRepository {
    override fun insert(decisionId: Long, optionId: Long, criteriaId: Long, scoredBy: String, score: UserScoreInput): UserScore {
        return jdbi.withHandle<UserScore, Exception> { handle ->
            handle.createQuery(
                """
                INSERT INTO user_scores (decision_id, option_id, criteria_id, scored_by, score) 
                VALUES (:decisionId, :optionId, :criteriaId, :scoredBy, :score)
                RETURNING *
                """.trimIndent()
            )
                .bind("decisionId", decisionId)
                .bind("optionId", optionId)
                .bind("criteriaId", criteriaId)
                .bind("scoredBy", scoredBy)
                .bind("score", score.score)
                .map { rs, _ -> mapUserScore(rs) }
                .one()
        }
    }

    override fun findAllByDecisionId(decisionId: Long): List<UserScore> {
        return jdbi.withHandle<List<UserScore>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, decision_id, option_id, criteria_id, scored_by, score, created_at
                FROM user_scores
                WHERE decision_id = :decisionId
                ORDER BY id
                """.trimIndent()
            )
                .bind("decisionId", decisionId)
                .map { rs, _ -> mapUserScore(rs) }
                .list()
        }
    }

    override fun findById(id: Long): UserScore? {
        return jdbi.withHandle<UserScore?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, decision_id, option_id, criteria_id, scored_by, score, created_at
                FROM user_scores
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ -> mapUserScore(rs) }
                .findOne()
                .orElse(null)
        }
    }

    override fun update(id: Long, score: Int): UserScore? {
        return jdbi.withHandle<UserScore?, Exception> { handle ->
            handle.createQuery(
                """
                UPDATE user_scores
                SET score = :score
                WHERE id = :id
                RETURNING *
                """.trimIndent()
            )
                .bind("id", id)
                .bind("score", score)
                .map { rs, _ -> mapUserScore(rs) }
                .findOne()
                .orElse(null)
        }
    }

    override fun delete(id: Long): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val updated = handle.createUpdate(
                """
                DELETE FROM user_scores
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .execute()
            updated > 0
        }
    }
}

private fun mapUserScore(rs: ResultSet): UserScore {
    return UserScore(
        id = rs.getLong("id"),
        decisionId = rs.getLong("decision_id"),
        optionId = rs.getLong("option_id"),
        criteriaId = rs.getLong("criteria_id"),
        scoredBy = rs.getString("scored_by"),
        score = rs.getInt("score"),
        createdAt = rs.getTimestamp("created_at")?.toInstant()
    )
}
