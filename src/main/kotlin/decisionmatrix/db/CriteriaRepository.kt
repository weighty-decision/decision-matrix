package decisionmatrix.db

import decisionmatrix.Criteria
import decisionmatrix.CriteriaInput
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

interface CriteriaRepository {
    fun insert(decisionId: Long, criteria: CriteriaInput): Criteria = throw NotImplementedError()
    fun findById(id: Long): Criteria? = throw NotImplementedError()
    fun update(id: Long, name: String, weight: Int): Criteria? = throw NotImplementedError()
    fun delete(id: Long): Boolean = throw NotImplementedError()
}

class CriteriaRepositoryImpl(private val jdbi: Jdbi) : CriteriaRepository {
    override fun insert(decisionId: Long, criteria: CriteriaInput): Criteria {
        return jdbi.withHandle<Criteria, Exception> { handle ->
            handle.createQuery(
                """
                INSERT INTO criteria (decision_id, name, weight) 
                VALUES (:decisionId, :name, :weight)
                RETURNING *
                """.trimIndent()
            )
                .bind("decisionId", decisionId)
                .bind("name", criteria.name)
                .bind("weight", criteria.weight)
                .map { rs, _ -> mapCriteria(rs) }
                .one()
        }
    }

    override fun findById(id: Long): Criteria? {
        return jdbi.withHandle<Criteria?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, decision_id, name, weight
                FROM criteria
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ -> mapCriteria(rs) }
                .findOne()
                .orElse(null)
        }
    }

    // todo update to accept an object not individual params
    override fun update(id: Long, name: String, weight: Int): Criteria? {
        return jdbi.withHandle<Criteria?, Exception> { handle ->
            handle.createQuery(
                """
                UPDATE criteria
                SET name = :name, weight = :weight
                WHERE id = :id
                RETURNING *
                """.trimIndent()
            )
                .bind("id", id)
                .bind("name", name)
                .bind("weight", weight)
                .map { rs, _ -> mapCriteria(rs) }
                .findOne()
                .orElse(null)
        }
    }

    override fun delete(id: Long): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val updated = handle.createUpdate(
                """
                DELETE FROM criteria
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .execute()
            updated > 0
        }
    }
}

fun mapCriteria(rs: ResultSet): Criteria {
    return Criteria(
        id = rs.getLong("id"),
        decisionId = rs.getLong("decision_id"),
        name = rs.getString("name"),
        weight = rs.getInt("weight"),
    )
}
