package com.brydonleonard.gatekey.notification

import com.brydonleonard.gatekey.TelegramBot
import com.brydonleonard.gatekey.conversation.ConversationHandler
import com.brydonleonard.gatekey.persistence.model.KeyModel
import org.springframework.stereotype.Component

@Component
class TelegramNotifier(
        private val conversationHandler: ConversationHandler,
        private val telegramBot: TelegramBot
) : Notifier {
    override fun notify(authorizedKey: KeyModel) {
        conversationHandler.getAllChatsForHousehold(authorizedKey.household).forEach {
            telegramBot.sendMessage(it, "Opening the gate for ${authorizedKey.assignee}")
        }
    }
}