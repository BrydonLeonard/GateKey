package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import org.springframework.stereotype.Component

@Component
class HouseholdStore(private val dbManager: DbManager) {
    fun addHousehold(householdId: String) {
        dbManager.householdDao.create(HouseholdModel(householdId))
    }

    fun noHouseholds(): Boolean {
        return dbManager.householdDao.queryForAll().isEmpty()
    }

    fun listHouseholds(): List<HouseholdModel> {
        return dbManager.householdDao.queryForAll()
    }

    fun getHousehold(id: String): HouseholdModel {
        return dbManager.householdDao.queryForId(id)
    }
}