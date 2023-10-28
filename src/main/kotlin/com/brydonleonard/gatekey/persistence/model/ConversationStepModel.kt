package com.brydonleonard.gatekey.persistence.model

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

@DatabaseTable(tableName = "conversations")
class ConversationStepModel(
        @DatabaseField(id = true, columnName = "chat_id")
        var chatId: Long,
        @DatabaseField(canBeNull = false, columnName = "outbound_message_id")
        var outboundMessageId: Long,
        @DatabaseField(canBeNull = false, columnName = "conversation_step_type")
        var conversationStepType: ConversationStepType
) {
    /**
     * Necessary for ORMLite
     */
    constructor() : this(0L, 0L, ConversationStepType.CREATE_SINGLE_USE_TOKEN)

    enum class Fields(val columnName: String) {
        CHAT_ID("chat_id"),
        OUTBOUND_MESSAGE_ID("outbound_message_id"),
        CONVERSATION_STEP_TYPE("conversation_step_type")
    }
}

enum class ConversationStepType {
    CREATE_SINGLE_USE_TOKEN
}
