package com.brydonleonard.gatekey.persistence

import com.brydonleonard.gatekey.Config
import com.brydonleonard.gatekey.persistence.ext.dbMigrations
import com.brydonleonard.gatekey.persistence.model.ConversationStepModel
import com.brydonleonard.gatekey.persistence.model.DbMigrationModel
import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import com.brydonleonard.gatekey.persistence.model.KeyModel
import com.brydonleonard.gatekey.persistence.model.UserModel
import com.brydonleonard.gatekey.persistence.model.UserRegistrationTokenModel
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource
import com.j256.ormlite.table.TableUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import org.sqlite.SQLiteException
import java.sql.DriverManager


/**
 * Responsible for configuring the DB as a whole and acts as a central source of DAOs, rather than all individual store
 * implementations having to pull in the individual ones that they use.
 *
 * Also allows other processes to check when the DB is ready for use.
 */
@Component
class DbManager(
        val config: Config,
        val connectionSource: JdbcPooledConnectionSource,
        val conversationDao: Dao<ConversationStepModel, String>,
        val keyDao: Dao<KeyModel, String>,
        val userDao: Dao<UserModel, String>,
        val userRegistrationTokenDao: Dao<UserRegistrationTokenModel, String>,
        val householdDao: Dao<HouseholdModel, String>,
        val migrationDao: Dao<DbMigrationModel, Int>
) {
    final var ready = false
        private set

    @PostConstruct
    fun setupDb() {
        TableUtils.createTableIfNotExists(connectionSource, DbMigrationModel::class.java)

        val nonMigrationDaos = listOf(
                conversationDao,
                keyDao,
                userDao,
                userRegistrationTokenDao,
                householdDao
        )

        val newTables = createTables(nonMigrationDaos)

        applyMigrations(nonMigrationDaos, newTables)

        ready = true
    }

    /**
     * Creates all tables and returns the names of those that were missing at the start of execution
     */
    fun createTables(otherDaos: List<Dao<*, *>>): List<String> {
        val missingTables = otherDaos.filter {
            !it.isTableExists
        }.map {
            it.tableName
        }

        TableUtils.createTableIfNotExists(connectionSource, ConversationStepModel::class.java)
        TableUtils.createTableIfNotExists(connectionSource, KeyModel::class.java)
        TableUtils.createTableIfNotExists(connectionSource, UserModel::class.java)
        TableUtils.createTableIfNotExists(connectionSource, UserRegistrationTokenModel::class.java)
        TableUtils.createTableIfNotExists(connectionSource, HouseholdModel::class.java)

        return missingTables
    }

    fun applyMigrations(nonMigrationDaos: List<Dao<*, *>>, newTables: List<String>) {

        // We use indexes as versions
        val latestVersion = dbMigrations.size - 1

        // If the current version is set, use that.
        // If not, if there are no tables in the DB at all, set it to the latest version. No need to migrate if we're setting up a fresh DB
        // Otherwise, set it to -1 so that all migrations are applied (or at least attempted)
        val currentVersion: Int = migrationDao.queryForAll().maxByOrNull { it.migrationId }?.migrationId
                ?: if (newTables.size == nonMigrationDaos.size) {
                    latestVersion
                } else {
                    -1
                }

        logger.info {
            "DB is at version $currentVersion. Latest version is $latestVersion. " +
                    "Created ${newTables.size} new tables of ${nonMigrationDaos.size} total tables"
        }

        if (currentVersion < latestVersion) {
            dbMigrations.subList(currentVersion + 1, latestVersion + 1).forEachIndexed { index, migration ->
                val version = index + currentVersion + 1
                logger.info { "Applying migration $version" }
                migration(this)

                migrationDao.create(DbMigrationModel(version))
            }
        }
    }

    /**
     * Executes raw SQL against the DB
     */
    fun executeRaw(sql: String) {
        DriverManager.getConnection("jdbc:sqlite:${config.dbPath}").use { connection ->
            try {
                val statement = connection.createStatement()
                sql.split(";").forEach {
                    statement.executeUpdate(it)
                }
            } catch (e: Exception) {
                logger.error(e) { "sad" }
                throw RuntimeException(e)
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger(DbManager::class.qualifiedName!!)
    }
}

