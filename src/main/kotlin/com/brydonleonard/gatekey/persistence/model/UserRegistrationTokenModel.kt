package com.brydonleonard.gatekey.persistence.model

import com.brydonleonard.gatekey.auth.Permissions
import com.brydonleonard.gatekey.persistence.ext.PermissionsSetPersister
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "user_registration_tokens")
data class UserRegistrationTokenModel(
        @DatabaseField(id = true) var token: String,
        @DatabaseField(canBeNull = false) var expiry: Long,
        @DatabaseField(canBeNull = false, persisterClass = PermissionsSetPersister::class) var permissions: Set<Permissions>,
        @DatabaseField(canBeNull = false, foreign = true) var household: HouseholdModel
) {
    /**
     * Necessary for ORMLite
     */
    constructor() : this("", 0L, emptySet(), HouseholdModel())

    enum class Fields(val columeName: String) {
        TOKEN("token"),
        EXPIRY("expiry"),
        PERMISSIONS("permissions"),
        HOUSEHOLD("household_id")
    }
}
