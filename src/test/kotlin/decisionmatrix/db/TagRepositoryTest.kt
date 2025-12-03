package decisionmatrix.db

import decisionmatrix.Tag
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TagRepositoryTest {

    val jdbi = getTestJdbi()

    @Test fun `findOrCreate creates new tag and normalizes to lowercase`() {
        cleanTestDatabase()
        val tagRepository = TagRepositoryImpl(jdbi)

        val tag = tagRepository.findOrCreate("Vacations")

        tag.name shouldBe "vacations"
        tag.id shouldBe tag.id // has an ID
    }

    @Test fun `findOrCreate returns existing tag when name already exists`() {
        cleanTestDatabase()
        val tagRepository = TagRepositoryImpl(jdbi)

        val tag1 = tagRepository.findOrCreate("vacations")
        val tag2 = tagRepository.findOrCreate("Vacations")
        val tag3 = tagRepository.findOrCreate("VACATIONS")

        tag1.id shouldBe tag2.id
        tag2.id shouldBe tag3.id
        tag1.name shouldBe "vacations"
    }

    @Test fun `findByPrefix returns matching tags`() {
        cleanTestDatabase()
        val tagRepository = TagRepositoryImpl(jdbi)

        tagRepository.findOrCreate("vacations")
        tagRepository.findOrCreate("vacation-planning")
        tagRepository.findOrCreate("frontend-working-group")
        tagRepository.findOrCreate("backend-api")

        val results = tagRepository.findByPrefix("vaca")

        results shouldHaveSize 2
        results.map { it.name } shouldContainExactlyInAnyOrder listOf("vacations", "vacation-planning")
    }

    @Test fun `findByPrefix returns empty list when no matches`() {
        cleanTestDatabase()
        val tagRepository = TagRepositoryImpl(jdbi)

        tagRepository.findOrCreate("vacations")

        val results = tagRepository.findByPrefix("frontend")

        results.shouldBeEmpty()
    }

    @Test fun `findByPrefix limits results`() {
        cleanTestDatabase()
        val tagRepository = TagRepositoryImpl(jdbi)

        // Create 15 tags starting with "tag"
        repeat(15) { i ->
            tagRepository.findOrCreate("tag-$i")
        }

        val results = tagRepository.findByPrefix("tag", limit = 5)

        results shouldHaveSize 5
    }

    @Test fun `findByDecisionId returns all tags for a decision`() {
        cleanTestDatabase()
        val tagRepository = TagRepositoryImpl(jdbi)
        val decisionRepository = DecisionRepositoryImpl(jdbi)

        val decision = decisionRepository.insert(decisionmatrix.DecisionInput(name = "My Decision"))

        val tag1 = tagRepository.findOrCreate("vacations")
        val tag2 = tagRepository.findOrCreate("urgent")
        val tag3 = tagRepository.findOrCreate("other-decision-tag")

        // Associate tags with decision
        tagRepository.addTagToDecision(decisionId = decision.id, tagId = tag1.id)
        tagRepository.addTagToDecision(decisionId = decision.id, tagId = tag2.id)

        val tags = tagRepository.findByDecisionId(decision.id)

        tags shouldHaveSize 2
        tags.map { it.name } shouldContainExactlyInAnyOrder listOf("vacations", "urgent")
    }

    @Test fun `removeTagFromDecision removes association`() {
        cleanTestDatabase()
        val tagRepository = TagRepositoryImpl(jdbi)
        val decisionRepository = DecisionRepositoryImpl(jdbi)

        val decision = decisionRepository.insert(decisionmatrix.DecisionInput(name = "My Decision"))
        val tag = tagRepository.findOrCreate("vacations")

        tagRepository.addTagToDecision(decisionId = decision.id, tagId = tag.id)
        tagRepository.findByDecisionId(decision.id) shouldHaveSize 1

        tagRepository.removeTagFromDecision(decisionId = decision.id, tagId = tag.id)

        tagRepository.findByDecisionId(decision.id).shouldBeEmpty()
    }

    @Test fun `adding same tag twice does not duplicate`() {
        cleanTestDatabase()
        val tagRepository = TagRepositoryImpl(jdbi)
        val decisionRepository = DecisionRepositoryImpl(jdbi)

        val decision = decisionRepository.insert(decisionmatrix.DecisionInput(name = "My Decision"))
        val tag = tagRepository.findOrCreate("vacations")

        tagRepository.addTagToDecision(decisionId = decision.id, tagId = tag.id)
        tagRepository.addTagToDecision(decisionId = decision.id, tagId = tag.id)

        val tags = tagRepository.findByDecisionId(decision.id)
        tags shouldHaveSize 1
    }
}
