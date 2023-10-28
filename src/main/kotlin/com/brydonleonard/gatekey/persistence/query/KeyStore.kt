package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.KeyModel
import org.springframework.stereotype.Component

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

    fun getKey(keyCode: String): KeyModel? {
        return dbManager.keyDao.queryForId(keyCode)
    }

    fun getKeysWithExpiryAfter(minExpiry: Long): List<KeyModel> {
        return dbManager.keyDao.queryBuilder().where()
                .gt(KeyModel.Fields.EXPIRY.columnName, minExpiry)
                .query()
    }

    fun getKeysWithExpiryBefore(maxExpiry: Long): List<KeyModel> {
        return dbManager.keyDao.queryBuilder().where()
                .lt(KeyModel.Fields.EXPIRY.columnName, maxExpiry)
                .query()
    }
}
