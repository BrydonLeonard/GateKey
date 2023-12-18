package com.brydonleonard.gatekey.mqtt

import com.brydonleonard.gatekey.persistence.model.MqttTopicModel
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
class MqttDeviceRegistrationResponse(
        val outcome: Outcomes = Outcomes.ACCEPTED,
        val topic: String? = null,
        val user: MqttUser? = null
) {
    constructor(
            topic: MqttTopicModel,
            user: MqttUser,
    ) : this(Outcomes.REGISTERED, topic.topic, user);

    enum class Outcomes {
        ERROR, // Something went wrong
        ACCEPTED, // Poll has been accepted and device is now waiting
        REGISTERED, // The device has been registered and MQTT credentials created for it

    }
}

class MqttUser(
        val user: String,
        val password: String
)