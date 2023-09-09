package com.brydonleonard.gatekey.keys

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.KeyModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mock
import org.mockito.Mockito.atMostOnce
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.any
import java.time.Instant
import java.util.stream.Stream
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeyManagerTest {
    @Mock
    lateinit var dbManager: DbManager

    @BeforeEach
    fun setup() {
        openMocks(this)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `tryUseKey only sets first use if is is unset`(shouldSet: Boolean) {
        val subject = KeyManager(dbManager)
        `when`(dbManager.withConnection<List<KeyModel>>(any()))
            .thenReturn(
                listOf(
                    KeyModel(
                        "123456",
                        Instant.now().plus(5.minutes.toJavaDuration()).epochSecond,
                        true,
                        "test",
                        if (shouldSet) Instant.now().minus(1.minutes.toJavaDuration()).epochSecond else null
                    )
                )
            )

        val result = subject.tryUseKey("123456")

        assertEquals("123456", result!!.key)

        if (shouldSet) {
            verify(dbManager, atMostOnce()).withConnection<Int>(any())
        }
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("keyIsValidTestInput")
    fun `keyIsValid tests`(
        deltaMinutesExpiry: Int,
        deltaMinutesFirstUse: Int?,
        shouldBeValid: Boolean,
        testName: String
    ) {
        val subject = KeyManager(dbManager)
        val now = Instant.now()
        val key = KeyModel(
            "123456",
            now.plus(deltaMinutesExpiry.minutes.toJavaDuration()).epochSecond,
            true,
            "test",
            deltaMinutesFirstUse?.let { now.plus(it.minutes.toJavaDuration()).epochSecond }
        )

        assertEquals(shouldBeValid, subject.keyIsValid(key))
    }

    companion object {
        @JvmStatic
        fun keyIsValidTestInput(): Stream<Arguments> = Stream.of(
            Arguments.of(60, null, true, "Keys are valid prior to expiry"),
            Arguments.of(60, -2, true, "Keys are valid for five minutes after their first use"),
            Arguments.of(-10, -2, true, "Keys are valid for five minutes after their first use, even if they expire"),
            Arguments.of(-10, null, false, "Expired keys are invalid"),
            Arguments.of(-10, -10, false, "Expired keys that have been used are invalid")
        )
    }
}
