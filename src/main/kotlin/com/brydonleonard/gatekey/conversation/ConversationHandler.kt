package com.brydonleonard.gatekey.conversation

import com.brydonleonard.gatekey.persistence.model.ConversationStepModel
import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import com.brydonleonard.gatekey.persistence.query.ConversationStore
import org.springframework.stereotype.Component

// TODO add start time to conversations so we can sweep them
@Component
class ConversationHandler(val conversationStore: ConversationStore) {
    fun awaitResponse(conversationStep: ConversationStepModel) {
        conversationStore.putConversationStep(conversationStep)
    }

    fun stopAwaiting(conversationStep: ConversationStepModel) {
        conversationStore.deleteConversationStep(conversationStep)
    }

    fun checkForConversation(chatId: Long, replyMessageId: Long?): ConversationStepModel? {
        if (replyMessageId == null) {
            return null
        }

        return conversationStore.checkForConversation(chatId, replyMessageId)
    }

    fun getAllChatsForHousehold(household: HouseholdModel): List<Long> {
        return conversationStore.getAllChatsForHousehold(household)
    }
}
