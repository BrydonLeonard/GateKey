package com.brydonleonard.gatekey.notification

import com.brydonleonard.gatekey.mqtt.MqttClient
import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import com.brydonleonard.gatekey.persistence.model.KeyModel
import com.brydonleonard.gatekey.persistence.query.MqttTopicStore
import org.springframework.stereotype.Component

/**
 * A [Notifier] that sends messages via an MQTT topic.
 */
@Component
class MqttNotifier(
        val mqttClient: MqttClient,
        val topicStore: MqttTopicStore
) : Notifier {

    override fun notify(message: String, household: HouseholdModel) {
        val topic = topicStore.getTopicForHousehold(household) ?: return

        mqttClient.publish(topic.topic, message)
    }
}