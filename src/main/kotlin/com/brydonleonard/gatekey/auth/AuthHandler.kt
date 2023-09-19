package com.brydonleonard.gatekey.auth

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.query.UserQueries
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
class AuthHandler(
        private val db: DbManager
) {
    /**
     * Returns true if the given user has permission to execute all of the given actions.
     */
    fun authorize(id: String, actions: Set<Permissions>): Boolean {
        val user = UserQueries.getUser(db, id) ?: return false

        val missingPermissions = actions - user.permissions

        if (missingPermissions.isNotEmpty()) {
            logger.info {
                "Unauthorized user '${user.name}' attempted to take an action that requires $missingPermissions"
            }
        }
        return missingPermissions.isEmpty()
    }

    fun userExists(id: String): Boolean {
        return UserQueries.getUser(db, id) != null
    }

    companion object {
        private val logger = KotlinLogging.logger(AuthHandler::class.qualifiedName!!)
    }
}
