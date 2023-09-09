package com.brydonleonard.gatekey.registration

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.query.UserRegistrationTokenQueries
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UserRegistrationTokenSweeper(val db: DbManager) {
    @Scheduled(fixedRate = 60000, initialDelay = 90000)
    fun sweep() {
        val tokens = UserRegistrationTokenQueries.getTokensWithExpiryBefore(db, Instant.now())

        // TODO Do this in a batch
        tokens.forEach { token ->
            UserRegistrationTokenQueries.deleteToken(db, token)
        }
    }
}
