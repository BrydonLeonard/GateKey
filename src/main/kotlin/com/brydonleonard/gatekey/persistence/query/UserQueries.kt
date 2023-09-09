package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.auth.Permissions
import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.UserModel
import java.sql.ResultSet

object UserQueries {
    fun getUser(dbManager: DbManager, id: String): UserModel? {
        return dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(
                """
                select * from users where id = ?
                """.trimIndent()
            )

            preparedStatement.setString(1, id)

            val rs = preparedStatement.executeQuery()
            if (rs.next()) {
                rs.toUser()
            } else {
                null
            }
        }
    }

    fun addUser(dbManager: DbManager, user: UserModel) {
        dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(
                """
                insert into users values (?, ?, ?, ?)
                """.trimIndent()
            )

            preparedStatement.setString(1, user.id)
            preparedStatement.setString(2, user.name)
            preparedStatement.setString(3, user.permissions.joinToString(",") { it.name })
            preparedStatement.setLong(4, user.chatId)

            preparedStatement.executeUpdate()
        }
    }

    private fun ResultSet.toUser() = UserModel(
        getString(DbManager.UserFields.ID.columnName),
        getString(DbManager.UserFields.NAME.columnName),
        getString(DbManager.UserFields.PERMISSIONS.columnName)
            .split(",")
            .map { Permissions.valueOf(it) }
            .toSet(),
        getLong(DbManager.UserFields.CHAT_ID.columnName)
    )
}
