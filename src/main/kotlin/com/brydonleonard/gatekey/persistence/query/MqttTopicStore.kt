package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import com.brydonleonard.gatekey.persistence.model.MqttTopicModel
import org.springframework.stereotype.Component

@Component
class MqttTopicStore(private val dbManager: DbManager) {
    fun addTopic(household: HouseholdModel, topic: String) {
        dbManager.mqttTopicDao.create(
                MqttTopicModel(topic, household)
        )
    }

    fun getTopicsForHousehold(household: HouseholdModel): List<MqttTopicModel> {
        return dbManager.mqttTopicDao.queryBuilder().where()
                .eq(MqttTopicModel.Fields.HOUSEHOLD.columnName, household)
                .query()
    }
}