package com.brydonleonard.gatekey.persistence.model

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.text.SimpleDateFormat
import java.util.Date


@DatabaseTable(tableName = "keys")
data class KeyModel(
        @DatabaseField(id = true) var key: String,
        @DatabaseField(canBeNull = false) var expiry: Long,
        @DatabaseField(canBeNull = false, columnName = "singleUse") var singleUse: Boolean,
        @DatabaseField var assignee: String? = null,
        @DatabaseField(columnName = "first_use") var firstUse: Long? = null
) {
    /**
     * Necessary for ORMLite
     */
    constructor() : this("", 0, false)

    fun formattedExpiry(): String {
        val netDate = Date(expiry * 1000)
        return sdf.format(netDate)
    }

    val formattedKey = key.chunked(3).joinToString(" ")

    enum class Fields(val columnName: String) {
        KEY("key"),
        EXPIRY("expiry"),
        SINGLE_USE("single_use"),
        ASSIGNEE("assignee"),
        FIRST_USE("first_use")
    }

    companion object {
        val sdf = SimpleDateFormat("yyyy/dd/MM HH:mm")
    }
}
