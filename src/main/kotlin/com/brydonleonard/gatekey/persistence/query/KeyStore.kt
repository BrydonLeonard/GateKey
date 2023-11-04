package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.KeyModel
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration

// When a user "deletes" a key, we instead set the expiry this amount of time in the past to tombstone it.
val INSTANT_EXPIRE_NEGATIVE_TIME = 24.hours.toJavaDuration()

@Component
class KeyStore(private val dbManager: DbManager) {
    fun addKey(key: KeyModel) {
        dbManager.keyDao.create(key)
    }

    fun setFirstUse(key: KeyModel, firstUse: Long) {
        val cloneKey = key.copy(firstUse = firstUse)
        dbManager.keyDao.update(cloneKey)
    }

    fun deleteKeys(keys: Collection<KeyModel>) {
        dbManager.keyDao.delete(keys)
    }

    fun expireKey(key: KeyModel) {
        key.expiry = Instant.now().minus(INSTANT_EXPIRE_NEGATIVE_TIME).epochSecond
        dbManager.keyDao.update(key)
    }

    fun getKey(keyCode: String): KeyModel? {
        return dbManager.keyDao.queryForId(keyCode)
    }

    fun getKeysWithExpiryAfter(minExpiry: Long, householdId: String? = null): List<KeyModel> {
        val query = dbManager.keyDao.queryBuilder().where()
                .gt(KeyModel.Fields.EXPIRY.columnName, minExpiry)

        if (householdId != null) {
            query.and()
                    .eq(KeyModel.Fields.HOUSEHOLD.columnName, householdId)
        }

        return query.query()
    }

    fun getKeysWithExpiryBefore(maxExpiry: Long): List<KeyModel> {
        return dbManager.keyDao.queryBuilder().where()
                .lt(KeyModel.Fields.EXPIRY.columnName, maxExpiry)
                .query()
    }
}
