package com.brydonleonard.gatekey.persistence.model

import com.brydonleonard.gatekey.auth.Permissions
import com.brydonleonard.gatekey.persistence.ext.PermissionsSetPersister
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "users")
data class UserModel(
        @DatabaseField(id = true) val id: String,
        @DatabaseField(canBeNull = false) var name: String,
        @DatabaseField(canBeNull = false, persisterClass = PermissionsSetPersister::class)
        var permissions: Set<Permissions>,
        @DatabaseField(canBeNull = false, columnName = "chat_id") var chatId: String
) {
    /**
     * Necessary for ORMLite
     */
    constructor() : this("", "", emptySet(), "")

    enum class Fields(val columnName: String) {
        ID("id"),
        NAME("name"),
        PERMISSIONS("permissions"),
        CHAT_ID("chat_id")
    }
}
