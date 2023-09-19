package com.brydonleonard.gatekey.persistence.model

import com.brydonleonard.gatekey.auth.Permissions

data class UserModel(
        val id: String,
        val name: String,
        val permissions: Set<Permissions>,
        val chatId: String
)
