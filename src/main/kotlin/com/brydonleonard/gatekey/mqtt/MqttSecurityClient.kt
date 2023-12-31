package com.brydonleonard.gatekey.mqtt

import com.brydonleonard.gatekey.Config
import org.springframework.stereotype.Component

/**
 * Wraps the MQTT client and provides tools to add user permissions.
 */
@Component
class MqttSecurityClient(
        val config: Config
) {
    // TODO: Actually implement user creation. For now, this just returns the admin user and password to everyone.
    fun addOrReplaceDeviceUser(userId: String, topic: String): MqttUser {
        // Assuming that these are non-null for now, since this won't be called unless the device registerer is active
        return MqttUser(config.mqttAdminUser!!, config.mqttAdminPassword!!)
    }

    fun revokeDeviceUsersFor(userId: String) {
        // Crickets
        // TODO: Get rid of the crickets
    }
}