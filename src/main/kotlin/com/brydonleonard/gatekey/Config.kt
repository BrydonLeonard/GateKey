package com.brydonleonard.gatekey

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class Config(
    @Value("\${ALLOWED_CALLERS:}")
    val allowedCallersRaw: String,
    @Value("\${DB_PATH:KeyDb.db}")
    val dbPath: String,
    @Value("\${TELEGRAM_BOT_TOKEN}")
    val telegramBotToken: String

) {
    val allowedCallers: Set<String> by lazy { allowedCallersRaw.split(",").toSet() }
}
