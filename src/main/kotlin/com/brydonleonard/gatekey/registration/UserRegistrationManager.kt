package com.brydonleonard.gatekey.registration

import com.brydonleonard.gatekey.Config
import com.brydonleonard.gatekey.auth.PermissionBundle
import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import com.brydonleonard.gatekey.persistence.model.UserModel
import com.brydonleonard.gatekey.persistence.model.UserRegistrationTokenModel
import com.brydonleonard.gatekey.persistence.query.HouseholdStore
import com.brydonleonard.gatekey.persistence.query.UserRegistrationStore
import com.brydonleonard.gatekey.persistence.query.UserStore
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.lang.Thread.sleep
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val REGISTRATION_TOKEN_VALIDITY = Duration.ofDays(1)

@Component
class UserRegistrationManager(val config: Config, val dbManager: DbManager, val userStore: UserStore, val userRegistrationStore: UserRegistrationStore, val householdStore: HouseholdStore) {
    private val logger = KotlinLogging.logger(UserRegistrationManager::class.qualifiedName!!)

    @PostConstruct
    fun generateInitialToken() {
        while (!dbManager.ready) {
            sleep(500)
        }

        if (householdStore.noHouseholds()) {
            householdStore.addHousehold(config.defaultHouseholdId)
            logger.info { "The households table is empty. Inserted the default household (${config.defaultHouseholdId})" }
        }

        if (userStore.noUsers()) {
            val household = householdStore.getHousehold(config.defaultHouseholdId)
            val newUserToken = generateNewUserToken(PermissionBundle.ADMIN, household)
            logger.info { "The users table is empty. Generated a first-time new user token: ${tokenToLink(newUserToken.token)}" }
        }
    }

    fun generateNewUserToken(permissionBundle: PermissionBundle, household: HouseholdModel): UserRegistrationTokenModel {
        val token = UserRegistrationTokenModel(
                UUID.randomUUID().toString(),
                Instant.now().plus(REGISTRATION_TOKEN_VALIDITY).epochSecond,
                permissionBundle.permissions,
                household
        )

        userRegistrationStore.createToken(token)

        return token
    }

    // TODO all of this should really be done transactionally, but I don't feel like it right now
    fun createUserFromToken(tokenString: String, userId: String, userName: String, chatId: String) {
        val token = userRegistrationStore.getToken(tokenString)
                ?: throw IllegalArgumentException("The token is invalid")

        if (token.expiry < Instant.now().epochSecond) {
            throw IllegalArgumentException("The token is expired")
        }

        val user = UserModel(
                userId,
                userName,
                token.permissions,
                chatId,
                token.household
        )

        userStore.addUser(user)

        userRegistrationStore.deleteToken(token)
    }

    fun addHousehold(householdId: String) {
        householdStore.addHousehold(householdId)
    }

    fun getAllHouseholds(): List<HouseholdModel> {
        return householdStore.listHouseholds();
    }

    fun getHousehold(id: String): HouseholdModel {
        return householdStore.getHousehold(id)
    }

    companion object {
        fun tokenToLink(token: String): String = "https://t.me/LeonardHomeBot?start=${token}"
    }
}
