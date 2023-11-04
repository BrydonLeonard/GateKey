package com.brydonleonard.gatekey.persistence.model

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Date

@DatabaseTable(tableName = "keys")
data class KeyModel(
        @DatabaseField(id = true) var key: String,
        @DatabaseField(canBeNull = false) var expiry: Long,
        @DatabaseField(canBeNull = false, columnName = "single_use") var singleUse: Boolean,
        @DatabaseField(canBeNull = false, foreign = true) var household: HouseholdModel,
        @DatabaseField var assignee: String? = null,
        @DatabaseField(columnName = "first_use") var firstUse: Long? = null,
) {
    /**
     * Necessary for ORMLite
     */
    constructor() : this("", 0, false, HouseholdModel())

    // TODO: Don't assume UTC+2 when formatting these
    fun formattedExpiry(): String {
        val expiryDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(expiry), ZoneId.ofOffset("UTC", ZoneOffset.ofHours(2)))
        val now = LocalDateTime.now()
        val units = listOf(ChronoUnit.DAYS, ChronoUnit.HOURS, ChronoUnit.MINUTES)

        val deltaRepr = units.map {
            now.until(expiryDateTime, it) to it.name.lowercase()
        }.first {
            it.first > 0
        }

        val netDate = Date(expiry * 1000)
        return "in ${deltaRepr.first} ${deltaRepr.second} (${sdf.format(netDate)})"
    }

    fun formattedKey() = key.chunked(3).joinToString(" ")

    enum class Fields(val columnName: String) {
        KEY("key"),
        EXPIRY("expiry"),
        SINGLE_USE("single_use"),
        ASSIGNEE("assignee"),
        FIRST_USE("first_use"),
        HOUSEHOLD("household_id")
    }

    companion object {
        val sdf = SimpleDateFormat("d MMM HH:mm")
    }
}
