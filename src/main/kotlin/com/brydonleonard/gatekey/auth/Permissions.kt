package com.brydonleonard.gatekey.auth

enum class Permissions {
    LIST_KEYS,
    CREATE_KEY,
    ADD_USER,
    UPDATE_PERMISSIONS,
    ADD_HOUSEHOLD,
}

enum class PermissionBundle(val permissions: Set<Permissions>) {
    ADMIN(Permissions.values().toSet()),
    RESIDENT(setOf(Permissions.LIST_KEYS, Permissions.CREATE_KEY))
}
