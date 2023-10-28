package com.brydonleonard.gatekey.persistence.model

import com.brydonleonard.gatekey.Config
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "households")
class HouseholdModel(
        @DatabaseField(id = true) var id: String
) {
    constructor() : this("")

    fun toCallbackQueryString() = "household-$id"

    companion object {
        fun default(config: Config) = HouseholdModel(config.defaultHouseholdId)

        fun callbackQueryStringToId(s: String) = s.split("-").last()
    }
}