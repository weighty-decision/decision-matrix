package decisionmatrix.db

import decisionmatrix.OptionInput
import decisionmatrix.Option
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

interface OptionRepository {
    fun insert(option: OptionInput): Option = throw NotImplementedError()
    fun findById(id: Long): Option? = throw NotImplementedError()
    fun update(id: Long, decisionId: Long, name: String): Option? = throw NotImplementedError()
    fun delete(id: Long, decisionId: Long): Boolean = throw NotImplementedError()
}

class OptionRepositoryImpl(private val jdbi: Jdbi) : OptionRepository {
    override fun insert(option: OptionInput): Option {
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

    // todo swap out individual args with an object input
    override fun update(id: Long, decisionId: Long, name: String): Option? {
        return jdbi.withHandle<Option?, Exception> { handle ->
            handle.createQuery(
                """
                UPDATE options
                SET name = :name
                WHERE id = :id AND decision_id = :decisionId
                RETURNING *
                """.trimIndent()
            )
                .bind("id", id)
                .bind("decisionId", decisionId)
                .bind("name", name)
                .map { rs, _ -> mapOption(rs) }
                .findOne()
                .orElse(null)
        }
    }

    // todo only need the id input, not the decisionId
    override fun delete(id: Long, decisionId: Long): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val updated = handle.createUpdate(
                """
                DELETE FROM options
                WHERE id = :id AND decision_id = :decisionId
                """.trimIndent()
            )
                .bind("id", id)
                .bind("decisionId", decisionId)
                .execute()
            updated > 0
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
