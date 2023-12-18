package com.brydonleonard.gatekey.notification

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * Keeps the spring config close to home. It may be worth moving all config to a central location in future.
 */
@Configuration
class NotificationSpringConfig {
    @Bean
    fun getNotificationQueue(): BlockingQueue<NotificationSender.Notification> = LinkedBlockingQueue()
}