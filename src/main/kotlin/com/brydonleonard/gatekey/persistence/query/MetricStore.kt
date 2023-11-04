package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.MetricModel
import org.springframework.stereotype.Component

@Component
class MetricStore(private val dbManager: DbManager) {
    fun addMetric(metric: MetricModel) {
        dbManager.metricDao.create(metric)
    }
}