package com.brydonleonard.gatekey.keys

import com.brydonleonard.gatekey.persistence.query.KeyStore
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Wait this long before sweeping keys. It's an arbitrary period, but should prevent people from attempting to re-use keys.
 */
val KEY_SWEEP_OFFSET: Duration = Duration.ofDays(30)

@Component
class KeySweeper(val keyStore: KeyStore) {

    @Scheduled(fixedRate = 60000, initialDelay = 60000)
    fun reportCurrentTime() {
        val expiredKeys = keyStore.getKeysWithExpiryBefore(
                Instant.now().epochSecond - KEY_SWEEP_OFFSET.seconds
        )

        keyStore.deleteKeys(expiredKeys)
    }
}
