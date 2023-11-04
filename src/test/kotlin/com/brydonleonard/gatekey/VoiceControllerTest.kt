package com.brydonleonard.gatekey

import com.brydonleonard.gatekey.conversation.ConversationHandler
import com.brydonleonard.gatekey.keys.KeyManager
import com.brydonleonard.gatekey.metrics.MetricPublisher
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations.openMocks

class VoiceControllerTest {
    @Mock
    lateinit var keyManager: KeyManager

    @Mock
    lateinit var conversationHandler: ConversationHandler

    @Mock
    lateinit var telegramBot: TelegramBot

    @Mock
    lateinit var request: HttpServletRequest

    @Mock
    lateinit var metricPublisher: MetricPublisher

    @BeforeEach
    fun setup() {
        openMocks(this)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "0123334444",
        "+27123334444",
        "27123334444",
        "0123334444,+27123335555,27123336666",
        "0123335555,+27123334444,27123336666",
        "0123335555,+27123336666,27123334444",
    ])
    fun authorizePhoneCaller(allowedCallers: String) {
        val config = getConfig(allowedCallers)
        val subject = VoiceController(keyManager, conversationHandler, telegramBot, config, metricPublisher)
        `when`(request.remoteAddr).thenReturn("192.168.100.100")

        assertThat(subject.authorizePhoneCaller("+27123334444", request)).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "0123334444",
        "+27123334444",
        "27123334444",
        "0123334444,+27123335555,27123336666",
        "0123335555,+27123334444,27123336666",
        "0123335555,+27123336666,27123334444",
    ])
    fun rejectPhoneCaller(allowedCallers: String) {
        val config = getConfig(allowedCallers)
        val subject = VoiceController(keyManager, conversationHandler, telegramBot, config, metricPublisher)
        `when`(request.remoteAddr).thenReturn("192.168.100.100")

        assertThat(subject.authorizePhoneCaller("+27123332222", request)).isFalse()
    }

    private fun getConfig(allowedCallers: String) = Config(
            allowedCallers,
            "./foo.db",
            "123",
            "123",
            "default"
    )
}