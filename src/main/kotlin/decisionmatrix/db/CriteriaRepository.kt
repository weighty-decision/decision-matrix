package decisionmatrix.db

import decisionmatrix.Criteria
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

interface CriteriaRepository {
    fun insert(criteria: Criteria): Criteria = throw NotImplementedError("not implemented")
    fun findById(id: Long): Criteria? = throw NotImplementedError("not implemented")
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

}

fun mapCriteria(rs: ResultSet): Criteria {
    return Criteria(
        id = rs.getLong("id"),
        decisionId = rs.getLong("decision_id"),
        name = rs.getString("name"),
        weight = rs.getInt("weight"),
    )
}