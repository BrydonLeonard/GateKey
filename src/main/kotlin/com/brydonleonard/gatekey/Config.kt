package com.brydonleonard.gatekey

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Config(
    @Value("\${GATE_KEY_ALLOWED_CALLERS:}")
    val allowedCallersRaw: String,
    @Value("\${GATE_KEY_DB_PATH:KeyDb.db}")
    val dbPath: String,
    @Value("\${GATE_KEY_TELEGRAM_BOT_TOKEN}")
    val telegramBotToken: String

) {
    val allowedCallers: Set<String> by lazy { allowedCallersRaw.split(",").toSet() }
}
