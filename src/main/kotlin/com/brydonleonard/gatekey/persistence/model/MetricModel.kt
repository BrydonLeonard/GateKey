package com.brydonleonard.gatekey.persistence.model

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "metrics")
data class MetricModel(
        @DatabaseField(generatedId = true) var id: Long,
        @DatabaseField(canBeNull = false) var timestamp: Long,
        @DatabaseField(canBeNull = false) var name: String,
        @DatabaseField(canBeNull = false) var value: Double
) {
    constructor() : this(0L, 0L, "", 0.0)

    constructor(timestamp: Long, name: String, value: Double) : this(0L, timestamp, name, value)
}
