package decisionmatrix.db

import decisionmatrix.Option
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

class OptionRepository(private val jdbi: Jdbi) {

    fun insert(option: Option): Option {
        return jdbi.withHandle<Option, Exception> { handle ->
            handle.createQuery(
                """
                INSERT INTO options (decision_id, name) 
                VALUES (:decisionId, :name)
                RETURNING *
                """.trimIndent()
            )
                .bind("decisionId", option.decisionId)
                .bind("name", option.name)
                .map { rs, _ -> mapOption(rs) }
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
                .map { rs, _ -> mapOption(rs) }
                .findOne()
                .orElse(null)
        }
    }

    private fun mapOption(rs: ResultSet): Option {
        return Option(
            id = rs.getLong("id"),
            decisionId = rs.getLong("decision_id"),
            name = rs.getString("name"),
        )
    }
}
