package com.brydonleonard.gatekey.notification

import com.brydonleonard.gatekey.persistence.model.KeyModel
import com.brydonleonard.gatekey.persistence.query.MqttTopicStore
import org.springframework.stereotype.Component

@Component
class MqttNotifier(
        val mqttClient: MqttClient,
        val topicStore: MqttTopicStore
) : Notifier {
    override fun notify(authorizedKey: KeyModel) {
        val topics = topicStore.getTopicsForHousehold(authorizedKey.household)

        topics.forEach {
            mqttClient.publish(it.topic, "Opening the gate for ${authorizedKey.assignee}.")
        }
    }
}