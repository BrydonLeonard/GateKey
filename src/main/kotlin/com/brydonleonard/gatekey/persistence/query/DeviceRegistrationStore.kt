package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.MqttDeviceModel
import org.springframework.stereotype.Component

@Component
class DeviceRegistrationStore(val dbManager: DbManager) {
    fun getDevice(deviceId: String): MqttDeviceModel? {
        return dbManager.notificationDeviceDao.queryForId(deviceId)
    }

    fun updateDevice(device: MqttDeviceModel) {
        dbManager.notificationDeviceDao.createOrUpdate(device)
    }
}