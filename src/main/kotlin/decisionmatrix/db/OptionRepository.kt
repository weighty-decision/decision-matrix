package decisionmatrix.db

import decisionmatrix.Option
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

interface OptionRepository {
    fun insert(option: Option): Option = throw NotImplementedError("not implemented")
    fun findById(id: Long): Option? = throw NotImplementedError("not implemented")
}

class OptionRepositoryImpl(private val jdbi: Jdbi) : OptionRepository {
    override fun insert(option: Option): Option {
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

    override fun findById(id: Long): Option? {
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
}

private fun mapOption(rs: ResultSet): Option {
    return Option(
        id = rs.getLong("id"),
        decisionId = rs.getLong("decision_id"),
        name = rs.getString("name"),
    )
}
