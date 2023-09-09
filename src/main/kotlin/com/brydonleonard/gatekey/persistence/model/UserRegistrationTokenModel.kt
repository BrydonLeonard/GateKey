package com.brydonleonard.gatekey.persistence.model

import com.brydonleonard.gatekey.auth.Permissions

data class UserRegistrationTokenModel(
    val token: String,
    val expiry: Long,
    val permissions: Set<Permissions>
)
