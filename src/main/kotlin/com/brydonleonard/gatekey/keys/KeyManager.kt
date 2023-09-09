package com.brydonleonard.gatekey.keys

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.KeyModel
import com.brydonleonard.gatekey.persistence.query.KeyQueries
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.times
import kotlin.time.toJavaDuration

val FIRST_USE_THRESHOLD = 5.minutes
val SINGLE_USE_KEY_VALIDITY = 30 * 24.hours

@Component
class KeyManager(val dbManager: DbManager) {
    fun generateKey(assignee: String? = null): KeyModel {
        val keyCode = Random.nextInt(999999).toString().padStart(6, '0')

        // TODO make expiry optional for single-use keys
        val key = KeyModel(
            keyCode,
            Instant.now().plus(SINGLE_USE_KEY_VALIDITY.toJavaDuration()).epochSecond,
            true,
            assignee
        )

        KeyQueries.addKey(dbManager, key)

        return key
    }

    fun getActiveKeys(): List<KeyModel> {
        return KeyQueries.getKeysWithExpiryAfter(dbManager, Instant.now().epochSecond).sortedBy { it.expiry }
    }

    /**
     * A key is valid
     */
    fun tryUseKey(keyCode: String): KeyModel? {
        val key = KeyQueries.getKey(dbManager, keyCode) ?: return null

        if (keyIsValid(key)) {
            updateFirstUse(key)
            return key
        }

        return null
    }

    fun keyIsValid(key: KeyModel): Boolean {
        val now = Instant.now().epochSecond

        // Just aliases to help make reading the conditionals easier
        val expired = key.expiry < now
        val singleUse = key.singleUse
        val withinThresholdOfFirstUse = key.firstUse != null &&
            (now - key.firstUse) < FIRST_USE_THRESHOLD.inWholeSeconds
        val used = key.firstUse != null

        if (singleUse && used) {
            return withinThresholdOfFirstUse
        }

        return !expired
    }

    private fun updateFirstUse(key: KeyModel) {
        if (key.firstUse == null) {
            KeyQueries.setFirstUse(dbManager, key, Instant.now().epochSecond)
        }
    }

    companion object {
        private val logger = KotlinLogging.logger(KeyManager::class.qualifiedName!!)
    }
}
