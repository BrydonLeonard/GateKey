package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.UserRegistrationTokenModel
import java.sql.ResultSet
import java.time.Instant

object UserRegistrationTokenQueries {
    fun createToken(dbManager: DbManager, token: UserRegistrationTokenModel) {
        dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(
                """
                insert into user_registration_tokens
                values (?, ?, ?)
                """.trimIndent()
            )

            preparedStatement.setString(1, token.token)
            preparedStatement.setLong(2, token.expiry)
            preparedStatement.setString(3, token.permissions.joinToString(",") { it.name })

            preparedStatement.executeUpdate()
        }
    }

    fun deleteToken(dbManager: DbManager, token: UserRegistrationTokenModel) {
        dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(
                """
                delete from user_registration_tokens 
                where token = ?
                """.trimIndent()
            )

            preparedStatement.setString(1, token.token)

            preparedStatement.executeUpdate()
        }
    }

    fun getToken(dbManager: DbManager, tokenString: String): UserRegistrationTokenModel? {
        return dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(
                """
                select * from user_registration_tokens where token = ?
                """.trimIndent()
            )

            preparedStatement.setString(1, tokenString)

            val rs = preparedStatement.executeQuery()
            if (rs.next()) {
                rs.toRegistrationToken()
            } else {
                null
            }
        }
    }

    fun getTokensWithExpiryBefore(dbManager: DbManager, timestamp: Instant): List<UserRegistrationTokenModel> {
        return dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(
                """
                select * from user_registration_tokens where expiry < ?
                """.trimIndent()
            )

            preparedStatement.setLong(1, timestamp.epochSecond)

            val rs = preparedStatement.executeQuery()
            val tokens = mutableListOf<UserRegistrationTokenModel>()

            while (rs.next()) {
                tokens.add(rs.toRegistrationToken())
            }

            tokens
        }
    }

    private fun ResultSet.toRegistrationToken() = UserRegistrationTokenModel(
        getString(com.brydonleonard.gatekey.persistence.DbManager.UserRegistrationTokensFields.TOKEN.name),
        getLong(com.brydonleonard.gatekey.persistence.DbManager.UserRegistrationTokensFields.EXPIRY.name),
        getString(com.brydonleonard.gatekey.persistence.DbManager.UserRegistrationTokensFields.PERMISSIONS.name)
            .split(",").map { com.brydonleonard.gatekey.auth.Permissions.valueOf(it) }.toSet()
    )
}
