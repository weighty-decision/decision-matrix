package decisionmatrix.db

import decisionmatrix.Criteria
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

interface CriteriaRepository {
    fun insert(criteria: Criteria): Criteria = throw NotImplementedError("not implemented")
    fun findById(id: Long): Criteria? = throw NotImplementedError("not implemented")
    fun update(id: Long, decisionId: Long, name: String, weight: Int): Criteria? = throw NotImplementedError("not implemented")
    fun delete(id: Long, decisionId: Long): Boolean = throw NotImplementedError("not implemented")
}

class CriteriaRepositoryImpl(private val jdbi: Jdbi) : CriteriaRepository {
    override fun insert(criteria: Criteria): Criteria {
        return jdbi.withHandle<Criteria, Exception> { handle ->
            handle.createQuery(
                """
                INSERT INTO criteria (decision_id, name, weight) 
                VALUES (:decisionId, :name, :weight)
                RETURNING *
                """.trimIndent()
            )
                .bind("decisionId", criteria.decisionId)
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

    override fun update(id: Long, decisionId: Long, name: String, weight: Int): Criteria? {
        return jdbi.withHandle<Criteria?, Exception> { handle ->
            handle.createQuery(
                """
                UPDATE criteria
                SET name = :name, weight = :weight
                WHERE id = :id AND decision_id = :decisionId
                RETURNING *
                """.trimIndent()
            )
                .bind("id", id)
                .bind("decisionId", decisionId)
                .bind("name", name)
                .bind("weight", weight)
                .map { rs, _ -> mapCriteria(rs) }
                .findOne()
                .orElse(null)
        }
    }

    override fun delete(id: Long, decisionId: Long): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val updated = handle.createUpdate(
                """
                DELETE FROM criteria
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

fun mapCriteria(rs: ResultSet): Criteria {
    return Criteria(
        id = rs.getLong("id"),
        decisionId = rs.getLong("decision_id"),
        name = rs.getString("name"),
        weight = rs.getInt("weight"),
    )
}