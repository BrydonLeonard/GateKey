package com.brydonleonard.gatekey

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.query.KeyQueries
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class HealthCheckController(val dbManager: DbManager) {
    @GetMapping("/ping")
    fun receiveVoice(): ResponseEntity<String> {
        // Just confirm that this doesn't fail
        KeyQueries.getKeysWithExpiryAfter(dbManager, Instant.now().epochSecond)
        return ResponseEntity("healthy", HttpStatus.OK)
    }
}
