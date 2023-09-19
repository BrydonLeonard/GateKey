package com.brydonleonard.gatekey.keys

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.query.KeyQueries
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * Wait this long before sweeping keys. It's an arbitrary period, but should prevent people from attempting to re-use keys.
 */
val KEY_SWEEP_OFFSET: Duration = Duration.ofDays(30)

@Component
class KeySweeper(val dbManager: DbManager) {

    @Scheduled(fixedRate = 60000, initialDelay = 60000)
    fun reportCurrentTime() {
        val expiredKeys = KeyQueries.getKeysWithExpiryBefore(
                dbManager,
                Instant.now().epochSecond - KEY_SWEEP_OFFSET.seconds
        )

        KeyQueries.deleteKeys(dbManager, expiredKeys)
    }
}
