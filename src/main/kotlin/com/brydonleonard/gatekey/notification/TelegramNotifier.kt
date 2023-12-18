package com.brydonleonard.gatekey.notification

import com.brydonleonard.gatekey.TelegramBot
import com.brydonleonard.gatekey.conversation.ConversationHandler
import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import com.brydonleonard.gatekey.persistence.model.KeyModel
import org.springframework.stereotype.Component

/**
 * A [Notifier] that sends messages to all users in the household via telegram.
 */
@Component
class TelegramNotifier(
        private val conversationHandler: ConversationHandler,
        private val telegramBot: TelegramBot
) : Notifier {

    override fun notify(message: String, household: HouseholdModel) {
        conversationHandler.getAllChatsForHousehold(household).forEach {
            telegramBot.sendMessage(it, message)
        }
    }
}