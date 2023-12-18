package com.brydonleonard.gatekey.mqtt

import com.brydonleonard.gatekey.Config
import io.github.oshai.kotlinlogging.KotlinLogging
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.springframework.stereotype.Component

private const val CLIENT_ID = "GateKeyServer"
private const val QOS_EXACTLY_ONCE = 2

@Component
class MqttClient(private val config: Config) {
    val enabled = config.mqttBrokerEndpoint != null &&
            config.mqttPort != null &&
            config.mqttAdminUser != null &&
            config.mqttAdminPassword != null
    private val logger = KotlinLogging.logger(MqttClient::class.qualifiedName!!)
    private val client = MqttClient(
            "${config.mqttBrokerEndpoint}:${config.mqttPort}",
            CLIENT_ID,
            MemoryPersistence()
    )

    // TODO secure client. This is sending plaintext
    fun publish(topic: String, message: String) {
        if (!enabled) {
            return
        }

        logger.debug { "Publishing $message to $topic" }
        withRetries(4) {
            try {
                val connOpts = MqttConnectOptions()
                connOpts.isCleanSession = true
                connOpts.userName = config.mqttAdminUser
                connOpts.password = config.mqttAdminPassword!!.toCharArray()
                client.connect(connOpts)

                val mqttMessage = MqttMessage(message.toByteArray())
                mqttMessage.qos = QOS_EXACTLY_ONCE

                client.publish(topic, mqttMessage)
            } finally {
                if (client.isConnected) {
                    client.disconnect()
                }
            }
        }
    }

    private fun withRetries(maxAttempts: Int, op: () -> Unit) {
        var attempts = 0
        var success = false
        while (attempts < maxAttempts && !success) {
            try {
                attempts++
                op()
                success = true
            } catch (me: MqttException) {
                logger.error(me) { "Failed to publish. ${maxAttempts - attempts} attempts remaining." }
            }
        }
    }
}