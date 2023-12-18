package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import com.brydonleonard.gatekey.persistence.model.MqttTopicModel
import org.springframework.stereotype.Component

@Component
class MqttTopicStore(private val dbManager: DbManager) {
    fun setTopic(household: HouseholdModel, topic: String) {
        // We only want one topic per household (for now at least)
        val deleteBuilder = dbManager.mqttTopicDao.deleteBuilder()
        deleteBuilder.where()
                .eq(MqttTopicModel.Fields.HOUSEHOLD.columnName, household.id)
        deleteBuilder.delete()

        dbManager.mqttTopicDao.create(
                MqttTopicModel(topic, household)
        )
    }

    fun getTopicForHousehold(household: HouseholdModel): MqttTopicModel? {
        return dbManager.mqttTopicDao.queryBuilder().where()
                .eq(MqttTopicModel.Fields.HOUSEHOLD.columnName, household)
                .query()
                .firstOrNull()
    }
}