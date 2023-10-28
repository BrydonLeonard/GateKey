package com.brydonleonard.gatekey.persistence.ext

import com.brydonleonard.gatekey.persistence.DbManager

/**
 * Schema migrations for the DB. These can make use of the [DbManager.migrationDao] to apply migrations.
 *
 * **New tables are always created before applying migrations.**
 */
val dbMigrations = listOf<(DbManager) -> Unit>()