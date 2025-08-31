package decisionmatrix.db

import decisionmatrix.Decision
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

class DecisionRepository(private val jdbi: Jdbi) {

    private fun mapDecision(rs: ResultSet): Decision {
        return Decision(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            criteria = emptyList(),
            options = emptyList(),
        )
    }

    fun insert(decision: Decision): Decision {
        return jdbi.withHandle<Decision, Exception> { handle ->
            handle.createQuery(
                """
                INSERT INTO decisions (name) 
                VALUES (:name)
                RETURNING *
                """.trimIndent()
            )
                .bind("name", decision.name)
                .map { rs, _ -> mapDecision(rs) }
                .one()
        }
    }

    fun findById(id: Long): Decision? {
        return jdbi.withHandle<Decision?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, name
                FROM decisions
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ -> mapDecision(rs) }
                .findOne()
                .orElse(null)
        }
    }
}
