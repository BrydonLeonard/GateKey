package com.brydonleonard.gatekey.registration

import com.brydonleonard.gatekey.persistence.query.UserRegistrationStore
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UserRegistrationTokenSweeper(val userRegistrationStore: UserRegistrationStore) {
    @Scheduled(fixedRate = 60000, initialDelay = 90000)
    fun sweep() {
        val tokens = userRegistrationStore.getTokensWithExpiryBefore(Instant.now())

        userRegistrationStore.deleteTokens(tokens)
    }
}
