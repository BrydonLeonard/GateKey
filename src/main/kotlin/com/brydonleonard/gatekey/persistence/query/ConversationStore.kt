package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.ConversationStepModel
import org.springframework.stereotype.Component

@Component
class ConversationStore(private val dbManager: DbManager) {
    fun putConversationStep(conversationStep: ConversationStepModel) {
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

    fun listAllChatIds(): List<Long> {
        return dbManager.userDao.queryForAll().map { it.chatId.toLong() }
    }
}
