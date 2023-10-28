package com.brydonleonard.gatekey.persistence.ext

import com.brydonleonard.gatekey.persistence.DbManager
import java.sql.DriverManager

/**
 * Schema migrations for the DB. These can make use of the [DbManager.migrationDao] to apply migrations.
 *
 * **New tables are always created before applying migrations.**
 */
val dbMigrations = listOf<(DbManager) -> Unit>(
        {
            it.executeRaw(
                    """
                        CREATE TABLE users_new (id VARCHAR , name VARCHAR NOT NULL , permissions VARCHAR NOT NULL , chat_id VARCHAR NOT NULL , household_id VARCHAR NOT NULL , PRIMARY KEY (id) );
                        INSERT INTO users_new(id, name, permissions, chat_id, household_id) SELECT id, name, permissions, chat_id, '${it.config.defaultHouseholdId}' FROM users;
                        DROP TABLE users;
                        ALTER TABLE users_new RENAME TO users;
                        
                        CREATE TABLE keys_new (key VARCHAR , expiry BIGINT NOT NULL , single_use BOOLEAN NOT NULL , household_id VARCHAR NOT NULL , assignee VARCHAR , first_use BIGINT , PRIMARY KEY (key) );
                        INSERT INTO keys_new (key, expiry, single_use, household_id, assignee, first_use) SELECT key, expiry, single_use, '${it.config.defaultHouseholdId}', assignee, first_use FROM keys;
                        DROP TABLE keys;
                        ALTER TABLE keys_new RENAME TO keys;
                        
                        CREATE TABLE user_registration_tokens_new (token VARCHAR , expiry BIGINT NOT NULL , permissions VARCHAR NOT NULL , household_id VARCHAR NOT NULL , PRIMARY KEY (token) );
                        INSERT INTO user_registration_tokens_new (token, expiry, permissions, household_id) SELECT token, expiry, permissions, '${it.config.defaultHouseholdId}' FROM user_registration_tokens;
                        DROP TABLE user_registration_tokens;
                        ALTER TABLE user_registration_tokens_new RENAME TO user_registration_tokens;
                    """.trimIndent()
            )
        }
)