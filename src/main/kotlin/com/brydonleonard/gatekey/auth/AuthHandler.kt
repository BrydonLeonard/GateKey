package com.brydonleonard.gatekey.auth

import com.brydonleonard.gatekey.persistence.model.UserModel
import com.brydonleonard.gatekey.persistence.query.UserStore
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Check that the caller is in the DB and authorized to do whatever it is they're trying to do. Could just store
 * allowed actions as a list or something.
 *
 * Extensions:
 * - Allow callers to add new users. We should tie users back to the caller that added them to the list..
 * - Callers should only be allowed to manage one telephone number. We can deal with that later, though.
 */
@Component
class AuthHandler(val userStore: UserStore) {
    /**
     * Returns the user if authorized, or null if unauthorized
     */
    fun getAuthorizedUser(id: String, actions: Set<Permissions>): UserModel? {
        val user = userStore.getUser(id) ?: return null

        val missingPermissions = actions - user.permissions

        if (missingPermissions.isNotEmpty()) {
            logger.info {
                "Unauthorized user '${user.name}' attempted to take an action that requires $missingPermissions"
            }
        }
        if (missingPermissions.isEmpty()) {
            return user
        }
        return null
    }

    fun userExists(id: String): Boolean {
        return userStore.getUser(id) != null
    }

    fun getUser(id: String): UserModel? {
        return userStore.getUser(id)
    }

    companion object {
        private val logger = KotlinLogging.logger(AuthHandler::class.qualifiedName!!)
    }
}
