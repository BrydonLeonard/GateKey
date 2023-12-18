package com.brydonleonard.gatekey.notification

import com.brydonleonard.gatekey.persistence.model.HouseholdModel

/**
 * Notifiers are used to send messages to GateKey users when _things_ happen (such as a gate opening).
 */
interface Notifier {
    /**
     * For generic notifications.
     */
    fun notify(message: String, household: HouseholdModel)
}