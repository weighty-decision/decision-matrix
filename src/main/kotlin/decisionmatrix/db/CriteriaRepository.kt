package decisionmatrix.db

import decisionmatrix.Criteria
import org.jdbi.v3.core.Jdbi

class CriteriaRepository(private val jdbi: Jdbi) {

    fun insert(criteria: Criteria): Long {
        return jdbi.withHandle<Long, Exception> { handle ->
            handle.createUpdate(
                """
                INSERT INTO criteria (decision_id, name, weight) 
                VALUES (:decisionId, :name, :weight)
                """.trimIndent()
            )
                .bind("decisionId", criteria.decisionId)
                .bind("name", criteria.name)
                .bind("weight", criteria.weight)
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Long::class.javaObjectType)
                .one()
        }
    }

    fun findById(id: Long): Criteria? {
        return jdbi.withHandle<Criteria?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, decision_id, name, weight
                FROM criteria
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ ->
                    Criteria(
                        id = rs.getLong("id"),
                        decisionId = rs.getLong("decision_id"),
                        name = rs.getString("name"),
                        weight = rs.getInt("weight"),
                    )
                }
                .findOne()
                .orElse(null)
        }
    }
}
