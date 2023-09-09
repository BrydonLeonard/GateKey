package com.brydonleonard.gatekey

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class GateKeyApplication

fun main(args: Array<String>) {
    runApplication<GateKeyApplication>(*args)
}
