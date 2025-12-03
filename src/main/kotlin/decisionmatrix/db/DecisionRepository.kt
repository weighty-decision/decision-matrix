package decisionmatrix.db

import decisionmatrix.Criteria
import decisionmatrix.Decision
import decisionmatrix.DecisionAggregate
import decisionmatrix.DecisionInput
import decisionmatrix.Option
import org.jdbi.v3.core.Jdbi
import java.sql.ResultSet

enum class TimeRange(val days: Int?) {
    ALL(null),
    LAST_7_DAYS(7),
    LAST_30_DAYS(30),
    LAST_90_DAYS(90),
    LAST_6_MONTHS(180);

    companion object {
        fun fromString(value: String?): TimeRange {
            return when (value?.uppercase()) {
                "ALL", "" -> ALL
                "7" -> LAST_7_DAYS
                "30" -> LAST_30_DAYS
                "90" -> LAST_90_DAYS
                "180" -> LAST_6_MONTHS
                else -> LAST_90_DAYS
            }
        }
    }
}

data class DecisionSearchFilters(
    val search: String? = null,
    val timeRange: TimeRange = TimeRange.LAST_90_DAYS,
    val involvedOnly: Boolean = false,
    val userId: String? = null
)

interface DecisionRepository {
    fun insert(decision: DecisionInput, createdBy: String = "unknown"): Decision = throw NotImplementedError()
    fun getDecision(id: Long): Decision? = throw NotImplementedError()
    fun getDecisionAggregate(id: Long): DecisionAggregate? = throw NotImplementedError()
    fun update(id: Long, name: String, minScore: Int, maxScore: Int, locked: Boolean): Decision? = throw NotImplementedError()
    fun delete(id: Long): Boolean = throw NotImplementedError()
    fun findDecisions(filters: DecisionSearchFilters): List<Decision> = throw NotImplementedError()
}

class DecisionRepositoryImpl(private val jdbi: Jdbi) : DecisionRepository {
    override fun insert(decision: DecisionInput, createdBy: String): Decision {
        return jdbi.withHandle<Decision, Exception> { handle ->
            handle.createQuery(
                """
                INSERT INTO decisions (name, min_score, max_score, locked, created_by, created_at)
                VALUES (:name, :minScore, :maxScore, :locked, :createdBy, CURRENT_TIMESTAMP)
                RETURNING *
                """.trimIndent()
            )
                .bind("name", decision.name)
                .bind("minScore", decision.minScore)
                .bind("maxScore", decision.maxScore)
                .bind("locked", decision.locked)
                .bind("createdBy", createdBy)
                .map { rs, _ -> mapDecision(rs) }
                .one()
        }
    }

    override fun getDecision(id: Long): Decision? {

        return jdbi.withHandle<Decision?, Exception> { handle ->
            handle.createQuery(
                """
                SELECT id, name, min_score, max_score, locked, created_by, created_at
                FROM decisions d
                WHERE d.id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .map { rs, _ -> mapDecision(rs) }
                .findOne()
                .orElse(null)
        }
    }

    override fun getDecisionAggregate(id: Long): DecisionAggregate? {
        return jdbi.withHandle<DecisionAggregate?, Exception> { handle ->
            val rows = handle.createQuery(
                """
                SELECT
                    d.id as decision_id,
                    d.name as decision_name,
                    d.min_score as decision_min_score,
                    d.max_score as decision_max_score,
                    d.locked as decision_locked,
                    d.created_by as decision_created_by,
                    d.created_at as decision_created_at,
                    c.id as criteria_id,
                    c.name as criteria_name,
                    c.weight as criteria_weight,
                    o.id as option_id,
                    o.name as option_name,
                    t.id as tag_id,
                    t.name as tag_name
                FROM decisions d
                LEFT JOIN criteria c ON d.id = c.decision_id
                LEFT JOIN options o ON d.id = o.decision_id
                LEFT JOIN decision_tags dt ON d.id = dt.decision_id
                LEFT JOIN tags t ON dt.tag_id = t.id
                WHERE d.id = :id
                """.trimIndent()
            )
                .bind("id", id)
                .mapToMap()
                .list()

            if (rows.isEmpty()) {
                return@withHandle null
            }

            mapDecisionAggregate(rows)
        }
    }

    override fun update(id: Long, name: String, minScore: Int, maxScore: Int, locked: Boolean): Decision? {
        return jdbi.withHandle<Decision?, Exception> { handle ->
            handle.createQuery(
                """
                UPDATE decisions
                SET name = :name, min_score = :minScore, max_score = :maxScore, locked = :locked
                WHERE id = :id
                RETURNING *
                """.trimIndent()
            )
                .bind("id", id)
                .bind("name", name)
                .bind("minScore", minScore)
                .bind("maxScore", maxScore)
                .bind("locked", locked)
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

    override fun findDecisions(filters: DecisionSearchFilters): List<Decision> {
        return jdbi.withHandle<List<Decision>, Exception> { handle ->
            val conditions = mutableListOf<String>()
            val parameters = mutableMapOf<String, Any>()
            val joins = mutableListOf<String>()

            // Search by decision name or tag
            filters.search?.let { searchTerm ->
                if (searchTerm.startsWith("@")) {
                    // Tag search
                    val tagName = searchTerm.substring(1).lowercase()
                    joins.add("INNER JOIN decision_tags dt ON d.id = dt.decision_id")
                    joins.add("INNER JOIN tags t ON dt.tag_id = t.id")
                    conditions.add("t.name = :tagName")
                    parameters["tagName"] = tagName
                } else {
                    // Name search
                    conditions.add("d.name ILIKE :search")
                    parameters["search"] = "%$searchTerm%"
                }
            }

            // Time range filter
            filters.timeRange.days?.let { days ->
                conditions.add("(d.created_at >= CURRENT_TIMESTAMP - INTERVAL '$days days' " +
                        "OR EXISTS (SELECT 1 FROM user_scores us WHERE us.decision_id = d.id AND us.created_at >= CURRENT_TIMESTAMP - INTERVAL '$days days'))")
            }

            // Involvement filter
            if (filters.involvedOnly && filters.userId != null) {
                conditions.add("(d.created_by = :userId OR d.id IN (SELECT DISTINCT decision_id FROM user_scores WHERE scored_by = :userId))")
                parameters["userId"] = filters.userId
            }

            val joinClause = if (joins.isEmpty()) "" else joins.joinToString(" ")
            val whereClause = if (conditions.isEmpty()) "" else "WHERE ${conditions.joinToString(" AND ")}"

            val query = """
                SELECT DISTINCT d.id, d.name, d.min_score, d.max_score, d.locked, d.created_by, d.created_at
                FROM decisions d
                $joinClause
                $whereClause
                ORDER BY d.created_at DESC
                """.trimIndent()

            var queryBuilder = handle.createQuery(query)
            for ((key, value) in parameters) {
                queryBuilder = queryBuilder.bind(key, value)
            }

            queryBuilder.map { rs, _ -> mapDecision(rs) }.list()
        }
    }
}

fun mapDecision(rs: ResultSet): Decision {
    return Decision(
        id = rs.getLong("id"),
        name = rs.getString("name"),
        minScore = rs.getInt("min_score"),
        maxScore = rs.getInt("max_score"),
        locked = rs.getBoolean("locked"),
        createdBy = rs.getString("created_by"),
        createdAt = rs.getTimestamp("created_at").toInstant()
    )
}

fun mapDecisionAggregate(rows: List<Map<String, Any>>): DecisionAggregate {
    require(rows.isNotEmpty()) { "Cannot map empty rows to Decision" }

    val firstRow = rows.first()
    val decisionId = (firstRow["decision_id"] as Number).toLong()
    val decisionName = firstRow["decision_name"] as String
    val decisionMinScore = (firstRow["decision_min_score"] as Number).toInt()
    val decisionMaxScore = (firstRow["decision_max_score"] as Number).toInt()
    val decisionLocked = firstRow["decision_locked"] as Boolean
    val decisionCreatedBy = firstRow["decision_created_by"] as String
    val decisionCreatedAt = (firstRow["decision_created_at"] as java.sql.Timestamp).toInstant()

    val criteriaMap = mutableMapOf<Long, Criteria>()
    val optionsMap = mutableMapOf<Long, Option>()
    val tagsMap = mutableMapOf<Long, decisionmatrix.Tag>()

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

        // Map tags if present
        val tagId = (row["tag_id"] as? Number)?.toLong()
        if (tagId != null) {
            tagsMap[tagId] = decisionmatrix.Tag(
                id = tagId,
                name = row["tag_name"] as String
            )
        }
    }

    return DecisionAggregate(
        decision = Decision(
            id = decisionId,
            name = decisionName,
            minScore = decisionMinScore,
            maxScore = decisionMaxScore,
            locked = decisionLocked,
            createdBy = decisionCreatedBy,
            createdAt = decisionCreatedAt,
        ),
        criteria = criteriaMap.values.toList(),
        options = optionsMap.values.toList(),
        tags = tagsMap.values.toList()
    )
}

