package com.brydonleonard.gatekey.metrics

import com.brydonleonard.gatekey.persistence.model.MetricModel
import com.brydonleonard.gatekey.persistence.query.MetricStore
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MetricPublisher(val metricStore: MetricStore) {
    fun publish(metricName: String, value: Double) {
        metricStore.addMetric(
                MetricModel(
                        Instant.now().epochSecond,
                        metricName,
                        value
                )
        )
    }
}