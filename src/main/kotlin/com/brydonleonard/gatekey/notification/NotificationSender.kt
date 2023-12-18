package com.brydonleonard.gatekey.notification

import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue

/**
 * Pulls notifications from the queue and sends them via the configured [Notifier]s. This exists to break a circular
 * dependency that arises when attempting to send telegram notifications from the mqttDeviceRegisterer (and likely other
 * places in future).
 */
@Component
class NotificationSender(
        val queue: BlockingQueue<Notification>,
        val notifiers: List<Notifier>
) {
    fun send() {
        val notification = queue.take()

        notifiers.forEach {
            it.notify(notification.message, notification.household)
        }
    }

    @PostConstruct
    fun start() {
        Thread {
            while (true) {
                send();
                Thread.sleep(1);
            }
        }.start()
    }

    data class Notification(val message: String, val household: HouseholdModel)
}