package decisionmatrix.db

import decisionmatrix.Option
import org.jdbi.v3.core.Jdbi

class OptionRepository(private val jdbi: Jdbi) {

    fun insert(option: Option): Long {
        return jdbi.withHandle<Long, Exception> { handle ->
            handle.createUpdate(
                """
                INSERT INTO options (decision_id, name) 
                VALUES (:decisionId, :name)
                """.trimIndent()
            )
                .bind("decisionId", option.decisionId)
                .bind("name", option.name)
                .executeAndReturnGeneratedKeys("id")
                .mapTo(Long::class.javaObjectType)
                .one()
        }
    }

    fun findById(id: Long): Option? {
        return jdbi.withHandle<Option?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, decision_id, name
                FROM options
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ ->
                    Option(
                        id = rs.getLong("id"),
                        decisionId = rs.getLong("decision_id"),
                        name = rs.getString("name"),
                    )
                }
                .findOne()
                .orElse(null)
        }
    }
}
