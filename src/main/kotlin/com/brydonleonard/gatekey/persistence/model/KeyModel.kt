package com.brydonleonard.gatekey.persistence.model

import java.text.SimpleDateFormat
import java.util.Date

data class KeyModel(
        val key: String,
        val expiry: Long,
        val singleUse: Boolean,
        val assignee: String? = null,
        val firstUse: Long? = null
) {
    fun formattedExpiry(): String {
        val netDate = Date(expiry * 1000)
        return sdf.format(netDate)
    }

    val formattedKey = key.chunked(3).joinToString(" ")

    companion object {
        val sdf = SimpleDateFormat("yyyy/dd/MM HH:mm")
    }
}
