package decisionmatrix.db

import decisionmatrix.DecisionInput
import decisionmatrix.Criteria
import decisionmatrix.Decision
import decisionmatrix.Option
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

interface DecisionRepository {
    fun insert(decision: DecisionInput): Decision = throw NotImplementedError()
    fun findById(id: Long): Decision? = throw NotImplementedError()
    fun update(id: Long, name: String): Decision? = throw NotImplementedError()
    fun delete(id: Long): Boolean = throw NotImplementedError()
}

class DecisionRepositoryImpl(private val jdbi: Jdbi) : DecisionRepository {
    override fun insert(decision: DecisionInput): Decision {
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
            val rows = handle.createQuery(
                """
                SELECT 
                    d.id as decision_id, 
                    d.name as decision_name,
                    c.id as criteria_id,
                    c.name as criteria_name,
                    c.weight as criteria_weight,
                    o.id as option_id,
                    o.name as option_name
                FROM decisions d
                LEFT JOIN criteria c ON d.id = c.decision_id
                LEFT JOIN options o ON d.id = o.decision_id
                WHERE d.id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .mapToMap()
                .list()

            if (rows.isEmpty()) {
                return@withHandle null
            }

            mapDecisionWithRelations(rows)
        }
    }

    override fun update(id: Long, name: String): Decision? {
        return jdbi.withHandle<Decision?, Exception> { handle ->
            handle.createQuery(
                """
                UPDATE decisions
                SET name = :name
                WHERE id = :id
                RETURNING *
                """.trimIndent()
            )
                .bind("id", id)
                .bind("name", name)
                .map { rs, _ -> mapDecision(rs) }
                .findOne()
                .orElse(null)
        }
    }

    override fun delete(id: Long): Boolean {
        return jdbi.withHandle<Boolean, Exception> { handle ->
            val updated = handle.createUpdate(
                """
                DELETE FROM decisions
                WHERE id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .execute()
            updated > 0
        }
    }
}

fun mapDecision(rs: ResultSet): Decision {
    return Decision(
        id = rs.getLong("id"),
        name = rs.getString("name"),
    )
}

fun mapDecisionWithRelations(rows: List<Map<String, Any>>): Decision {
    if (rows.isEmpty()) throw IllegalArgumentException("Cannot map empty rows to Decision")

    val firstRow = rows.first()
    val decisionId = (firstRow["decision_id"] as Number).toLong()
    val decisionName = firstRow["decision_name"] as String

    val criteriaMap = mutableMapOf<Long, Criteria>()
    val optionsMap = mutableMapOf<Long, Option>()

    for (row in rows) {
        // Map criteria if present
        val criteriaId = (row["criteria_id"] as? Number)?.toLong()
        if (criteriaId != null) {
            criteriaMap[criteriaId] = Criteria(
                id = criteriaId,
                decisionId = decisionId,
                name = row["criteria_name"] as String,
                weight = (row["criteria_weight"] as Number).toInt()
            )
        }

        // Map options if present  
        val optionId = (row["option_id"] as? Number)?.toLong()
        if (optionId != null) {
            optionsMap[optionId] = Option(
                id = optionId,
                decisionId = decisionId,
                name = row["option_name"] as String
            )
        }
    }

    return Decision(
        id = decisionId,
        name = decisionName,
        criteria = criteriaMap.values.toList(),
        options = optionsMap.values.toList()
    )
}

