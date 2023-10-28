package com.brydonleonard.gatekey

import com.brydonleonard.gatekey.persistence.query.KeyStore
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class HealthCheckController(val keyStore: KeyStore) {
    @GetMapping("/ping")
    fun receiveVoice(): ResponseEntity<String> {
        // Just confirm that this doesn't fail
        keyStore.getKeysWithExpiryAfter(Instant.now().epochSecond)
        return ResponseEntity("healthy", HttpStatus.OK)
    }
}
