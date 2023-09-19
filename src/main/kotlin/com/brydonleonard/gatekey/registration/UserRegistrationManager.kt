package com.brydonleonard.gatekey.registration

import com.brydonleonard.gatekey.auth.PermissionBundle
import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.UserModel
import com.brydonleonard.gatekey.persistence.model.UserRegistrationTokenModel
import com.brydonleonard.gatekey.persistence.query.UserQueries
import com.brydonleonard.gatekey.persistence.query.UserRegistrationTokenQueries
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.lang.Thread.sleep
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val REGISTRATION_TOKEN_VALIDITY = Duration.ofDays(1)

@Component
class UserRegistrationManager(val dbManager: DbManager) {
    private val logger = KotlinLogging.logger(UserRegistrationManager::class.qualifiedName!!)

    @PostConstruct
    fun generateInitialToken() {
        while (!dbManager.ready) {
            sleep(500)
        }

        if (UserQueries.noUsers(dbManager)) {
            val newUserToken = generateNewUserToken(PermissionBundle.ADMIN)
            logger.info { "The DB is empty. Generated a first-time new user token: ${tokenToLink(newUserToken.token)}" }
        }
    }

    fun generateNewUserToken(permissionBundle: PermissionBundle): UserRegistrationTokenModel {
        val token = UserRegistrationTokenModel(
                UUID.randomUUID().toString(),
                Instant.now().plus(REGISTRATION_TOKEN_VALIDITY).epochSecond,
                permissionBundle.permissions
        )

        UserRegistrationTokenQueries.createToken(dbManager, token)

        return token
    }

    // TODO all of this should really be done transactionally, but I don't feel like it right now
    fun createUserFromToken(tokenString: String, userId: String, userName: String, chatId: String) {
        val token = UserRegistrationTokenQueries.getToken(dbManager, tokenString)
                ?: throw IllegalArgumentException("The token is invalid")

        if (token.expiry < Instant.now().epochSecond) {
            throw IllegalArgumentException("The token is expired")
        }

        val user = UserModel(
                userId,
                userName,
                token.permissions,
                chatId
        )

        UserQueries.addUser(dbManager, user)

        UserRegistrationTokenQueries.deleteToken(dbManager, token)
    }

    companion object {
        fun tokenToLink(token: String): String = "https://t.me/LeonardHomeBot?start=${token}"
    }
}
