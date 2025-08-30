package decisionmatrix.db

import decisionmatrix.Decision
import org.jdbi.v3.core.Jdbi

class DecisionRepository(private val jdbi: Jdbi) {

    fun insert(decision: Decision): Long {
        return jdbi.withHandle<Long, Exception> { handle ->
            handle.createUpdate(
                """
                INSERT INTO decisions (name) 
                VALUES (:name)
                """.trimIndent()
            )
                .bind("name", decision.name)
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Long::class.javaObjectType)
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
                .map { rs, _ ->
                    Decision(
                        id = rs.getLong("id"),
                        name = rs.getString("name"),
                        criteria = emptyList(),
                        options = emptyList(),
                    )
                }
                .findOne()
                .orElse(null)
        }
    }
}
