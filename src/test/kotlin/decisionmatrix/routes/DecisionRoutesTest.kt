package decisionmatrix.routes

import decisionmatrix.Decision
import decisionmatrix.DecisionInput
import decisionmatrix.db.DecisionRepository
import decisionmatrix.json
import io.kotest.matchers.shouldBe
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class DecisionRoutesTest {

    private class FakeDecisionRepository : DecisionRepository {
        private val store = mutableMapOf<Long, Decision>()
        private var nextId = 1L

        override fun insert(decision: DecisionInput): Decision {
            val created = Decision(id = nextId++, name = decision.name, minScore = decision.minScore, maxScore = decision.maxScore)
            store[created.id] = created
            return created
        }

        override fun findById(id: Long): Decision? = store[id]

        override fun update(id: Long, name: String): Decision? {
            val existing = store[id] ?: return null
            val updated = existing.copy(name = name)
            store[id] = updated
            return updated
        }

        override fun update(id: Long, name: String, minScore: Int, maxScore: Int): Decision? {
            val existing = store[id] ?: return null
            val updated = existing.copy(name = name, minScore = minScore, maxScore = maxScore)
            store[id] = updated
            return updated
        }

        override fun delete(id: Long): Boolean {
            return store.remove(id) != null
        }
    }

    @Test fun `update decision - success`() {
        val repo = FakeDecisionRepository()
        val routes = DecisionRoutes(repo).routes
        val created = repo.insert(DecisionInput(name = "Original"))

        val body = json.encodeToString(DecisionInput(name = "Updated"))
        val request = Request(Method.PUT, "/api/decisions/${created.id}")
            .body(body)

        val response = routes(request)
        response.status shouldBe Status.OK

        val updated = json.decodeFromString<Decision>(response.bodyString())
        updated.id shouldBe created.id
        updated.name shouldBe "Updated"
    }

    @Test fun `update decision - not found`() {
        val repo = FakeDecisionRepository()
        val routes = DecisionRoutes(repo).routes

        val body = json.encodeToString(DecisionInput(name = "Updated"))
        val response = routes(Request(Method.PUT, "/api/decisions/999").body(body))

        response.status shouldBe Status.NOT_FOUND
    }

    @Test fun `delete decision - success`() {
        val repo = FakeDecisionRepository()
        val routes = DecisionRoutes(repo).routes
        val created = repo.insert(DecisionInput(name = "ToDelete"))

        val response = routes(Request(Method.DELETE, "/api/decisions/${created.id}"))
        response.status shouldBe Status.NO_CONTENT

        val after = repo.findById(created.id)
        after shouldBe null
    }

    @Test fun `delete decision - not found`() {
        val repo = FakeDecisionRepository()
        val routes = DecisionRoutes(repo).routes

        val response = routes(Request(Method.DELETE, "/api/decisions/12345"))
        response.status shouldBe Status.NOT_FOUND
    }
}
