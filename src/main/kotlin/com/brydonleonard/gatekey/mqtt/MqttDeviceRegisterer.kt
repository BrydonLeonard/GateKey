package com.brydonleonard.gatekey.mqtt

import com.brydonleonard.gatekey.mqtt.MqttDeviceRegisterer.DeviceRegistrationState.NEW_REGISTERED
import com.brydonleonard.gatekey.mqtt.MqttDeviceRegisterer.DeviceRegistrationState.NONE
import com.brydonleonard.gatekey.mqtt.MqttDeviceRegisterer.DeviceRegistrationState.POLLING
import com.brydonleonard.gatekey.mqtt.MqttDeviceRegisterer.DeviceRegistrationState.REGISTERED
import com.brydonleonard.gatekey.mqtt.MqttDeviceRegisterer.DeviceRegistrationState.REQUESTED
import com.brydonleonard.gatekey.notification.NotificationSender
import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import com.brydonleonard.gatekey.persistence.model.MqttDeviceModel
import com.brydonleonard.gatekey.persistence.model.MqttTopicModel
import com.brydonleonard.gatekey.persistence.query.DeviceRegistrationStore
import com.brydonleonard.gatekey.persistence.query.MqttTopicStore
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue

/**
 * Registers new MQTT devices with the system. Coincidentally, this is also the class that manages MQTT topics.
 * Households are only allocated topics when their first device registers, so topics are created at device registration
 * time. Each household can only have a single associated topic.
 *
 *                   ┌────┐
 *                   │NONE│
 *                   └┬──┬┘
 *         Poll       │  │       Add
 *     ┌────┐ ┌───────┘  └───────┐ ┌────┐
 *     │    │ │                  │ │    │
 *     │ ┌──▼─▼──┐          ┌────▼─▼──┐ │
 *     └─│POLLING│          │REQUESTED├─┘
 *   ┌──►└───┬───┘          └────┬────┘
 *   │      Add                  │
 *   │       │                   │
 *   │    ┌──▼───────────┐       │ Poll
 *   │ ┌──►NEW_REGISTERED│       │
 *   │ │  └┬─┬───────────┘       │
 *   │ │Add│ │ Poll (a)          │
 *   │ └───┘ └───────┐    ┌──────┘
 *   │               │    │ ┌────┐
 *   │ Poll (b)   ┌──▼────▼─▼┐   │ Add
 *   └────────────┤REGISTERED├───┘
 *                └──────────┘
 *
 *
 *
 *
 * (a) - Because the device still needs the creds at (a), it has a transition NEW_REGISTERED state, where creds are generated
 *  and returned.
 * (b) - If the device polls again after already being registered, something's gone wrong, so we deregister it and allow
 *  the user to re-add it.
 */
@Component
class MqttDeviceRegisterer(
        private val deviceRegistrationStore: DeviceRegistrationStore,
        private val mqttTopicStore: MqttTopicStore,
        private val securityClient: MqttSecurityClient,
        private val notificationQueue: BlockingQueue<NotificationSender.Notification>
) {
    /**
     * Handle a poll from a device to register itself with the system.
     */
    fun handlePollingDevice(deviceId: String): MqttDeviceRegistrationResponse {
        val device = deviceRegistrationStore.getDevice(deviceId)
                ?: return addPendingDevice(deviceId, POLLING);

        return when (device.state) {
            NONE -> return addPendingDevice(deviceId, POLLING)
            REQUESTED, NEW_REGISTERED -> registerDevice(device, device.household!!, false)
            POLLING -> MqttDeviceRegistrationResponse(MqttDeviceRegistrationResponse.Outcomes.ACCEPTED)
            REGISTERED -> deregisterDevice(device)
        }
    }

    /**
     * Handle a user request to add a device to their household.
     */
    fun handleRequestedDevice(deviceId: String, household: HouseholdModel): MqttDeviceRegistrationResponse {
        val device = deviceRegistrationStore.getDevice(deviceId)
                ?: return addPendingDevice(deviceId, REQUESTED, household)

        return when (device.state) {
            NONE -> addPendingDevice(deviceId, REQUESTED, household)
            POLLING -> registerDevice(device, household, true)
            REGISTERED, REQUESTED, NEW_REGISTERED -> MqttDeviceRegistrationResponse(MqttDeviceRegistrationResponse.Outcomes.ACCEPTED)
        }
    }

    /**
     * Add a device in one of the two intermediate states
     */
    private fun addPendingDevice(deviceId: String, state: DeviceRegistrationState, household: HouseholdModel? = null): MqttDeviceRegistrationResponse {
        val newDevice = MqttDeviceModel(deviceId, household, state)
        deviceRegistrationStore.updateDevice(newDevice)

        return MqttDeviceRegistrationResponse(MqttDeviceRegistrationResponse.Outcomes.ACCEPTED);
    }

    private fun registerDevice(device: MqttDeviceModel, household: HouseholdModel, shouldAllowSubsequentPoll: Boolean): MqttDeviceRegistrationResponse {
        val topic: MqttTopicModel = mqttTopicStore.getTopicForHousehold(household)
                ?: MqttTopicModel(householdToTopic(household), household)

        val newState = if (shouldAllowSubsequentPoll) NEW_REGISTERED else REGISTERED
        val newDevice = MqttDeviceModel(device.deviceId, household, newState)

        deviceRegistrationStore.updateDevice(newDevice)
        mqttTopicStore.setTopic(household, topic.topic)

        return MqttDeviceRegistrationResponse(
                topic,
                securityClient.addOrReplaceDeviceUser(newDevice.deviceId, topic.topic)
        )
    }

    private fun deregisterDevice(device: MqttDeviceModel): MqttDeviceRegistrationResponse {
        // Reset to pending and revoke any active credentials
        val response = addPendingDevice(device.deviceId, POLLING)
        securityClient.revokeDeviceUsersFor(device.deviceId)

        notificationQueue.put(
                NotificationSender.Notification(
                        "Device '${device.deviceId}' has been reset and is ready to reconnect. " +
                                "If you would like to allow it to reconnect, tap /addDevice and enter its ID.",
                        device.household!!
                )
        )

        return response
    }

    private fun householdToTopic(household: HouseholdModel): String {
        return "GateKey/household/${household.id}"
    }

    enum class DeviceRegistrationState {
        NONE, // Just here to act as a default
        POLLING, // The device has connected to the internet and is awaiting a household
        REQUESTED, // A user has attempted to register the device and is waiting for it to poll
        NEW_REGISTERED, // The device has been registered, but not yet issued with credentials
        REGISTERED // The device has been registered and issued with credentials
    }
}