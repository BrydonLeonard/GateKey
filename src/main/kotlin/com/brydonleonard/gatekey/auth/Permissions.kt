package com.brydonleonard.gatekey.auth

enum class Permissions {
    LIST_KEYS,
    CREATE_KEY,
    ADD_USER,
    UPDATE_PERMISSIONS
}

enum class PermissionBundle(val permissions: Set<Permissions>) {
    ADMIN(Permissions.values().toSet()),
    KEY_MANAGER(setOf(Permissions.LIST_KEYS, Permissions.CREATE_KEY))
}
