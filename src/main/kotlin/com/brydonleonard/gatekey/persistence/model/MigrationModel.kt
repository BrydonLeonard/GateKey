package com.brydonleonard.gatekey.persistence.model

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "migrations")
class DbMigrationModel(
        @DatabaseField(id = true, columnName = "migration_id") var migrationId: Int
) {
    /**
     * Necessary for ORMLite
     */
    constructor() : this(0)
}