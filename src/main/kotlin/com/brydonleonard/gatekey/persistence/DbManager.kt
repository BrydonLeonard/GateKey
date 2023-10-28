package com.brydonleonard.gatekey.persistence

import com.brydonleonard.gatekey.persistence.model.ConversationStepModel
import com.brydonleonard.gatekey.persistence.model.KeyModel
import com.brydonleonard.gatekey.persistence.model.UserModel
import com.brydonleonard.gatekey.persistence.model.UserRegistrationTokenModel
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource
import com.j256.ormlite.table.TableUtils
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component


/**
 * Responsible for configuring the DB as a whole and acts as a central source of DAOs, rather than all individual store
 * implementations having to pull in the individual ones that they use.
 *
 * Also allows other processes to check when the DB is ready for use.
 */
@Component
class DbManager(
        val connectionSource: JdbcPooledConnectionSource,
        val conversationDao: Dao<ConversationStepModel, String>,
        val keyDao: Dao<KeyModel, String>,
        val userDao: Dao<UserModel, String>,
        val userRegistrationTokenDao: Dao<UserRegistrationTokenModel, String>
) {
    final var ready = false
        private set

    @PostConstruct
    fun setupDb() {
        TableUtils.createTableIfNotExists(connectionSource, ConversationStepModel::class.java)
        TableUtils.createTableIfNotExists(connectionSource, KeyModel::class.java)
        TableUtils.createTableIfNotExists(connectionSource, UserModel::class.java)
        TableUtils.createTableIfNotExists(connectionSource, UserRegistrationTokenModel::class.java)

        ready = true
    }
}

