package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.ConversationStepModel
import com.brydonleonard.gatekey.persistence.model.ConversationStepType

object ConversationQueries {
    fun putConversationStep(dbManager: DbManager, conversationStep: ConversationStepModel) {
        dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(
                    """
                insert into conversations
                values (?, ?, ?)
                """.trimIndent()
            )

            preparedStatement.setLong(1, conversationStep.chatId)
            preparedStatement.setLong(2, conversationStep.outboundMessageId)
            preparedStatement.setString(3, conversationStep.conversationStepType.name)

            preparedStatement.executeUpdate()
        }
    }

    fun deleteConversationStep(dbManager: DbManager, conversationStep: ConversationStepModel) {
        dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(
                    """
                delete from conversations where chat_id = ? and outbound_message_id = ?
                """.trimIndent()
            )

            preparedStatement.setLong(1, conversationStep.chatId)
            preparedStatement.setLong(2, conversationStep.outboundMessageId)

            preparedStatement.executeUpdate()
        }
    }

    fun checkForConversation(dbManager: DbManager, chatId: Long, replyMessageId: Long): ConversationStepModel? {
        return dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(
                    """
                select * from conversations where chat_id = ? and outbound_message_id = ?
                """.trimIndent()
            )

            preparedStatement.setLong(1, chatId)
            preparedStatement.setLong(2, replyMessageId)

            val rs = preparedStatement.executeQuery()

            if (rs.next()) {
                ConversationStepModel(
                        rs.getLong(DbManager.ConversationFields.CHAT_ID.columnName),
                        rs.getLong(DbManager.ConversationFields.OUTBOUND_MESSAGE_ID.columnName),
                        ConversationStepType.valueOf(
                                rs.getString(DbManager.ConversationFields.CONVERSATION_STEP_TYPE.columnName)
                        )
                )
            } else {
                null
            }
        }
    }

    fun listAllChatIds(dbManager: DbManager): List<Long> {
        return dbManager.withConnection { connection ->
            val preparedStatement = connection.prepareStatement(
                    """
                select * from users
                """.trimIndent()
            )

            val rs = preparedStatement.executeQuery()

            val chatIds = mutableListOf<Long>()
            while (rs.next()) {
                try {
                    chatIds.add(rs.getLong(DbManager.UserFields.CHAT_ID.columnName))
                } catch (e: Exception) {
                    // Crickets
                }
            }

            chatIds
        }
    }
}
