package com.brydonleonard.gatekey.notification

import com.brydonleonard.gatekey.persistence.model.KeyModel

interface Notifier {
    fun notify(authorizedKey: KeyModel)
}