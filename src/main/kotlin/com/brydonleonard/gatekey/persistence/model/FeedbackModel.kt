package com.brydonleonard.gatekey.persistence.model

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "feedback")
class FeedbackModel(
        @DatabaseField(generatedId = true) var id: Int,
        @DatabaseField(canBeNull = false, foreign = true) var user: UserModel,
        @DatabaseField(canBeNull = false) var message: String
) {
    constructor() : this(0, UserModel(), "")
}