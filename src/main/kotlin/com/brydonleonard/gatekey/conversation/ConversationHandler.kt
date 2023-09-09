package com.brydonleonard.gatekey.conversation

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.ConversationStepModel
import com.brydonleonard.gatekey.persistence.query.ConversationQueries
import org.springframework.stereotype.Component

// TODO add start time to conversations so we can sweep them
@Component
class ConversationHandler(val dbManager: DbManager) {
    fun awaitResponse(conversationStep: ConversationStepModel) {
        ConversationQueries.putConversationStep(dbManager, conversationStep)
    }

    fun stopAwaiting(conversationStep: ConversationStepModel) {
        ConversationQueries.deleteConversationStep(dbManager, conversationStep)
    }

    fun checkForConversation(chatId: Long, replyMessageId: Long?): ConversationStepModel? {
        if (replyMessageId == null) {
            return null
        }

        return ConversationQueries.checkForConversation(dbManager, chatId, replyMessageId)
    }

    fun getAllChatIds(): List<Long> {
        return ConversationQueries.listAllChatIds(dbManager)
    }
}
