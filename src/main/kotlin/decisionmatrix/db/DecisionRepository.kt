package decisionmatrix.db

import decisionmatrix.DecisionInput
import decisionmatrix.Criteria
import decisionmatrix.Decision
import decisionmatrix.Option
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.ZoneOffset

interface DecisionRepository {
    fun insert(decision: DecisionInput, createdBy: String = "unknown"): Decision = throw NotImplementedError()
    fun findById(id: Long): Decision? = throw NotImplementedError()
    fun update(id: Long, name: String): Decision? = throw NotImplementedError()
    fun update(id: Long, name: String, minScore: Int, maxScore: Int): Decision? = throw NotImplementedError()
    fun delete(id: Long): Boolean = throw NotImplementedError()
    fun findAllInvolvedDecisions(userId: String): List<Decision> = throw NotImplementedError()
    fun findAllRecentDecisions(): List<Decision> = throw NotImplementedError()
}

class DecisionRepositoryImpl(private val jdbi: Jdbi) : DecisionRepository {
    override fun insert(decision: DecisionInput, createdBy: String): Decision {
        return jdbi.withHandle<Decision, Exception> { handle ->
            handle.createQuery(
                """
                INSERT INTO decisions (name, min_score, max_score, created_by) 
                VALUES (:name, :minScore, :maxScore, :createdBy)
                RETURNING *
                """.trimIndent()
            )
                .bind("name", decision.name)
                .bind("minScore", decision.minScore)
                .bind("maxScore", decision.maxScore)
                .bind("createdBy", createdBy)
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
                    d.min_score as decision_min_score,
                    d.max_score as decision_max_score,
                    d.created_by as decision_created_by,
                    d.created_at as decision_created_at,
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

    override fun update(id: Long, name: String, minScore: Int, maxScore: Int): Decision? {
        return jdbi.withHandle<Decision?, Exception> { handle ->
            handle.createQuery(
                """
                UPDATE decisions
                SET name = :name, min_score = :minScore, max_score = :maxScore
                WHERE id = :id
                RETURNING *
                """.trimIndent()
            )
                .bind("id", id)
                .bind("name", name)
                .bind("minScore", minScore)
                .bind("maxScore", maxScore)
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

    override fun findAllInvolvedDecisions(userId: String): List<Decision> {
        return jdbi.withHandle<List<Decision>, Exception> { handle ->
            val rows = handle.createQuery(
                """
                SELECT 
                    d.id as decision_id, 
                    d.name as decision_name,
                    d.min_score as decision_min_score,
                    d.max_score as decision_max_score,
                    d.created_by as decision_created_by,
                    d.created_at as decision_created_at,
                    c.id as criteria_id,
                    c.name as criteria_name,
                    c.weight as criteria_weight,
                    o.id as option_id,
                    o.name as option_name
                FROM decisions d
                LEFT JOIN criteria c ON d.id = c.decision_id
                LEFT JOIN options o ON d.id = o.decision_id
                WHERE d.created_by = :userId 
                   OR d.id IN (
                       SELECT DISTINCT decision_id 
                       FROM user_scores 
                       WHERE scored_by = :userId
                   )
                ORDER BY d.created_at DESC
                """.trimIndent()
            )
                .bind("userId", userId)
                .mapToMap()
                .list()

            if (rows.isEmpty()) {
                return@withHandle emptyList()
            }

            // Group rows by decision_id
            val decisionGroups = rows.groupBy { (it["decision_id"] as Number).toLong() }
            
            decisionGroups.map { (_, decisionRows) ->
                mapDecisionWithRelations(decisionRows)
            }
        }
    }

    override fun findAllRecentDecisions(): List<Decision> {
        return jdbi.withHandle<List<Decision>, Exception> { handle ->
            handle.createQuery(
                """
                SELECT *
                FROM decisions 
                WHERE created_at >= datetime('now', '-3 months')
                ORDER BY created_at DESC
                """.trimIndent()
            )
                .map { rs, _ -> mapDecision(rs) }
                .list()
        }
    }
}

fun mapDecision(rs: ResultSet): Decision {
    return Decision(
        id = rs.getLong("id"),
        name = rs.getString("name"),
        minScore = rs.getInt("min_score"),
        maxScore = rs.getInt("max_score"),
        createdBy = rs.getString("created_by"),
        createdAt = rs.getString("created_at")?.let { 
            LocalDateTime.parse(it.replace(" ", "T")).atOffset(ZoneOffset.UTC).toInstant()
        }
    )
}

fun mapDecisionWithRelations(rows: List<Map<String, Any>>): Decision {
    if (rows.isEmpty()) throw IllegalArgumentException("Cannot map empty rows to Decision")

    val firstRow = rows.first()
    val decisionId = (firstRow["decision_id"] as Number).toLong()
    val decisionName = firstRow["decision_name"] as String
    val decisionMinScore = (firstRow["decision_min_score"] as Number).toInt()
    val decisionMaxScore = (firstRow["decision_max_score"] as Number).toInt()
    val decisionCreatedBy = firstRow["decision_created_by"] as? String
    val decisionCreatedAt = (firstRow["decision_created_at"] as? String)?.let { 
        LocalDateTime.parse(it.replace(" ", "T")).atOffset(ZoneOffset.UTC).toInstant()
    }

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
        minScore = decisionMinScore,
        maxScore = decisionMaxScore,
        createdBy = decisionCreatedBy,
        createdAt = decisionCreatedAt,
        criteria = criteriaMap.values.toList(),
        options = optionsMap.values.toList()
    )
}

