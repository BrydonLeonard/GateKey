package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.ConversationStepModel
import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import com.brydonleonard.gatekey.persistence.model.UserModel
import org.springframework.stereotype.Component

@Component
class ConversationStore(private val dbManager: DbManager) {
    fun putConversationStep(conversationStep: ConversationStepModel) {
        // We can only have one ongoing conversation per chat, so clear out any old ones
        dbManager.conversationDao.deleteById(conversationStep.chatId)
        dbManager.conversationDao.create(conversationStep)
    }

    fun deleteConversationStep(conversationStep: ConversationStepModel) {
        dbManager.conversationDao.delete(conversationStep)
    }

    fun checkForConversation(chatId: Long, replyMessageId: Long): ConversationStepModel? {
        return dbManager.conversationDao.queryBuilder().where()
                .eq(ConversationStepModel.Fields.CHAT_ID.columnName, chatId)
                .and()
                .eq(ConversationStepModel.Fields.OUTBOUND_MESSAGE_ID.columnName, replyMessageId)
                .query()
                .first()
    }

    fun getAllChatsForHousehold(household: HouseholdModel): List<Long> {
        return dbManager.userDao.queryBuilder().where()
                .eq(UserModel.Fields.HOUSEHOLD.columnName, household)
                .query()
                .map { it.chatId.toLong() }
    }
}
