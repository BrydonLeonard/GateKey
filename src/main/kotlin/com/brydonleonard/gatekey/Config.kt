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
        val telegramBotToken: String,
        @Value("\${GATE_KEY_TWILIO_TOKEN}")
        val twilioAuthToken: String,
        @Value("\${GATE_KEY_DEFAULT_HOUSEHOLD:default}")
        val defaultHouseholdId: String,
        @Value("\${GATE_KEY_MQTT_ENDPOINT}")
        val mqttBrokerEndpoint: String?,
        @Value("\${GATE_KEY_MQTT_PORT:1883}")
        val mqttPort: String?,
        @Value("\${GATE_KEY_MQTT_ADMIN_USER}")
        val mqttAdminUser: String?,
        @Value("\${GATE_KEY_MQTT_ADMIN_PASSWORD}")
        val mqttAdminPassword: String?,
) {
    val allowedCallers: Set<String> by lazy { allowedCallersRaw.split(",").toSet() }
}
