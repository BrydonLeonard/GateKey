package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.UserRegistrationTokenModel
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UserRegistrationStore(private val dbManager: DbManager) {
    fun createToken(token: UserRegistrationTokenModel) {
        dbManager.userRegistrationTokenDao.create(token)
    }

    fun deleteToken(token: UserRegistrationTokenModel) {
        dbManager.userRegistrationTokenDao.delete(token)
    }

    fun deleteTokens(tokens: List<UserRegistrationTokenModel>) {
        dbManager.userRegistrationTokenDao.delete(tokens)
    }

    fun getToken(tokenString: String): UserRegistrationTokenModel? {
        return dbManager.userRegistrationTokenDao.queryForId(tokenString)
    }

    fun getTokensWithExpiryBefore(timestamp: Instant): List<UserRegistrationTokenModel> {
        return dbManager.userRegistrationTokenDao.queryBuilder().where()
                .lt(UserRegistrationTokenModel.Fields.EXPIRY.columeName, timestamp.epochSecond)
                .query()
    }
}
