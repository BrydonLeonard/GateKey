package com.brydonleonard.gatekey.keys

import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import com.brydonleonard.gatekey.persistence.model.KeyModel
import com.brydonleonard.gatekey.persistence.query.KeyStore
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
class KeyManager(val keyStore: KeyStore) {
    fun generateKey(assignee: String? = null, household: HouseholdModel): KeyModel {
        // Not very idiomatic Kotlin, I know. It gets the job done.
        var keyExists = true
        var keyCode: String? = null

        while (keyExists) {
            keyCode = Random.nextInt(999999).toString().padStart(6, '0')
            keyExists = keyStore.getKey(keyCode) != null
        }

        val key = KeyModel(
                keyCode!!,
                Instant.now().plus(SINGLE_USE_KEY_VALIDITY.toJavaDuration()).epochSecond,
                true,
                household,
                assignee
        )

        keyStore.addKey(key)

        return key
    }

    fun getActiveKeys(household: HouseholdModel): List<KeyModel> {
        return keyStore.getKeysWithExpiryAfter(Instant.now().epochSecond, household.id)
                .filter { keyIsValid(it) }
                .sortedBy { it.expiry }
    }

    /**
     * A key is valid
     */
    fun tryUseKey(keyCode: String): KeyModel? {
        val key = keyStore.getKey(keyCode) ?: return null

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
                (now - key.firstUse!!) < FIRST_USE_THRESHOLD.inWholeSeconds
        val used = key.firstUse != null

        if (singleUse && used) {
            return withinThresholdOfFirstUse
        }

        return !expired
    }

    private fun updateFirstUse(key: KeyModel) {
        if (key.firstUse == null) {
            keyStore.setFirstUse(key, Instant.now().epochSecond)
        }
    }
}
