package decisionmatrix.db

import decisionmatrix.Decision
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

interface DecisionRepository {
    fun insert(decision: Decision): Decision = throw NotImplementedError("not implemented")
    fun findById(id: Long): Decision? = throw NotImplementedError("not implemented")
}

class DecisionRepositoryImpl(private val jdbi: Jdbi) : DecisionRepository {
    override fun insert(decision: Decision): Decision {
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

    override fun findById(id: Long): Decision? {
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

    private fun mapDecision(rs: ResultSet): Decision {
        return Decision(
            id = rs.getLong("id"),
            name = rs.getString("name"),
            criteria = emptyList(),
            options = emptyList(),
        )
    }
}
