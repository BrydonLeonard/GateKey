package com.brydonleonard.gatekey.persistence.model

import com.brydonleonard.gatekey.mqtt.MqttDeviceRegisterer
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "mqtt_devices")
class MqttDeviceModel(
        @DatabaseField(id = true, columnName = "device_id") var deviceId: String,
        @DatabaseField(canBeNull = true, foreign = true) var household: HouseholdModel?,
        @DatabaseField(canBeNull = false) var state: MqttDeviceRegisterer.DeviceRegistrationState
) {
    /**
     * Necessary for ORMLite
     */
    constructor() : this("", null, MqttDeviceRegisterer.DeviceRegistrationState.NONE)

    enum class Fields(val columnName: String) {
        DEVICE_ID("device_id"),
        HOUSEHOLD("household_id")
    }


}