package com.brydonleonard.gatekey

import com.brydonleonard.gatekey.mqtt.MqttDeviceRegistrationResponse
import com.brydonleonard.gatekey.mqtt.MqttDeviceRegisterer
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Receives requests from MQTT devices that would like to register with the system. GateKey uses Telegram to perform
 * passwordless auth for new devices joining a household.
 */
@RestController
class DeviceRegistrationController(
        private val mqttDeviceRegisterer: MqttDeviceRegisterer
) {
    @PostMapping(
            "/register_device",
            consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
            produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun receiveRegistrationRequest(@RequestParam requestBody: MultiValueMap<String, String>, request: HttpServletRequest): MqttDeviceRegistrationResponse {
        val deviceId = requestBody["device_id"]?.firstOrNull()
                ?: throw IllegalArgumentException("device_id must be provided")

        return mqttDeviceRegisterer.handlePollingDevice(deviceId)
    }
}