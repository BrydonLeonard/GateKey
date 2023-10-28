package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.UserModel
import org.springframework.stereotype.Component

@Component
class UserStore(private val dbManager: DbManager) {
    fun getUser(id: String): UserModel? {
        return dbManager.userDao.queryForId(id)
    }

    fun addUser(user: UserModel) {
        dbManager.userDao.create(user)
    }

    @Suppress("ReplaceSizeZeroCheckWithIsEmpty")
    fun noUsers(): Boolean {
        return dbManager.userDao.count() <= 0
    }
}
