package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.KeyModel
import java.sql.ResultSet
import java.sql.Types

object KeyQueries {
    fun addKey(dbManager: DbManager, key: KeyModel) {
        dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement("insert into gate_keys values (?, ?, ?, ?, ?)")

            preparedStatement.setString(1, key.key)
            preparedStatement.setLong(2, key.expiry)
            preparedStatement.setLong(3, if (key.singleUse) 1 else 0)
            key.assignee?.also { assignee ->
                preparedStatement.setString(4, assignee)
            } ?: {
                preparedStatement.setNull(4, Types.VARCHAR)
            }
            key.firstUse?.also { firstUse ->
                preparedStatement.setLong(5, firstUse)
            } ?: {
                preparedStatement.setNull(5, Types.INTEGER)
            }

            preparedStatement.executeUpdate()
        }
    }

    fun setFirstUse(dbManager: DbManager, key: KeyModel, firstUse: Long) {
        dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(
                    """
                    update gate_keys
                    set first_use = ?
                    where key = ? and first_use is null
                """.trimIndent()
            )

            preparedStatement.setLong(1, firstUse)
            preparedStatement.setString(2, key.key)

            preparedStatement.executeUpdate()
        }
    }

    fun deleteKeys(dbManager: DbManager, keys: Collection<KeyModel>) {
        dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(
                    "delete from gate_keys where key in (${List(keys.size) { "?" }.joinToString(",")})"
            )

            keys.forEachIndexed { index, key ->
                preparedStatement.setString(index + 1, key.key)
            }

            preparedStatement.executeUpdate()
        }
    }

    fun getKey(dbManager: DbManager, keyCode: String): KeyModel? {
        val queryResults = listKeyQuery(dbManager, "select * from gate_keys where key = ?", keyCode)

        if (queryResults.isEmpty()) {
            return null
        }

        return queryResults[0]
    }

    fun getKeysWithExpiryAfter(dbManager: DbManager, minExpiry: Long): List<KeyModel> =
            listKeyQuery(dbManager, "select * from gate_keys where expiry > ?", minExpiry)

    fun getKeysWithExpiryBefore(dbManager: DbManager, maxExpiry: Long): List<KeyModel> =
            listKeyQuery(dbManager, "select * from gate_keys where expiry < ?", maxExpiry)

    private fun listKeyQuery(dbManager: DbManager, query: String, vararg params: Any): List<KeyModel> {
        return dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(query)

            params.forEachIndexed { index, param ->
                when (param) {
                    is String -> preparedStatement.setString(index + 1, param)
                    is Long -> preparedStatement.setLong(index + 1, param)
                    is Int -> preparedStatement.setInt(index + 1, param)
                    is Double -> preparedStatement.setDouble(index + 1, param)
                }
            }

            val rs = preparedStatement.executeQuery()

            val keys = mutableListOf<KeyModel>()
            while (rs.next()) {
                keys.add(rs.toKey())
            }
            keys
        }
    }

    private fun ResultSet.toKey() = KeyModel(
            getString(DbManager.KeyFields.KEY.columnName),
            getLong(DbManager.KeyFields.EXPIRY.columnName),
            getInt(DbManager.KeyFields.SINGLE_USE.columnName) == 1,
            getString(DbManager.KeyFields.ASSIGNEE.columnName),
            getLong(DbManager.KeyFields.FIRST_USE.columnName).let { if (it == 0L) null else it }
    )
}
