package decisionmatrix.audit

import org.junit.jupiter.api.Test
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.shouldBe
import org.slf4j.LoggerFactory

class AuditLoggerTest {

    @Test fun `auditLog uses fluent API to log structured events`() {
        val logger = LoggerFactory.getLogger("Audit") as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)

        // Log an audit event using the fluent API
        auditLog.atInfo()
            .setMessage("Decision created")
            .addKeyValue("event", "decision.created")
            .addKeyValue("user_id", "alice@example.com")
            .addKeyValue("decision_id", 123)
            .addKeyValue("decision_name", "Test Decision")
            .log()

        appender.list.size shouldBe 1
        val event = appender.list[0]
        event.level shouldBe Level.INFO
        event.loggerName shouldBe "Audit"
        event.message shouldBe "Decision created"
    }
}
