package com.brydonleonard.gatekey.persistence.model

class ConversationStepModel(
    val chatId: Long,
    val outboundMessageId: Long,
    val conversationStepType: ConversationStepType
)

enum class ConversationStepType {
    CREATE_SINGLE_USE_TOKEN
}
