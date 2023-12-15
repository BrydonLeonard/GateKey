package com.brydonleonard.gatekey.persistence.model

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "mqtt_topics")
class MqttTopicModel(
        @DatabaseField(id = true) var topic: String,
        @DatabaseField(canBeNull = false, foreign = true) var household: HouseholdModel
) {
    /**
     * Necessary for ORMLite
     */
    constructor() : this("", HouseholdModel())

    enum class Fields(val columnName: String) {
        TOPIC("topic"),
        HOUSEHOLD("household_id")
    }
}