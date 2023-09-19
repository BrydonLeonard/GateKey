package com.brydonleonard.gatekey.persistence

import com.brydonleonard.gatekey.Config
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.DriverManager

@Component
class DbManager(
        val config: Config
) {
    final var ready = false
        private set

    @PostConstruct
    fun setupDb() {
        withConnection { connection ->
            val statement = connection.createStatement()

            statement.executeUpdate(
                    """
                create table if not exists gate_keys (
                    key text primary key, 
                    expiry long,
                    single_use integer,
                    assignee text,
                    first_use long
                )
                """.trimIndent()
            )

            statement.executeUpdate(
                    """
                create table if not exists users (
                    id text primary key,
                    name text not null,
                    permissions text not null
                )
                """.trimIndent()
            )

            statement.executeUpdate(
                    """
                create table if not exists user_registration_tokens (
                    token text primary key,
                    expiry long not null,
                    permissions text not null
                )
                """.trimIndent()
            )

            statement.executeUpdate(
                    """
                create table if not exists conversations (
                    chat_id text not null,
                    outbound_message_id text not null,
                    conversation_step_type text not null,
                    primary key(chat_id, outbound_message_id)
                )
                """.trimIndent()
            )
        }

        ready = true
    }

    fun <T> withConnection(function: (Connection) -> T): T {
        DriverManager.getConnection("jdbc:sqlite:${config.dbPath}").use { connection ->
            try {
                return function(connection)
            } catch (e: Exception) {
                logger.error(e) { "sad" }
                throw RuntimeException(e)
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger(DbManager::class.qualifiedName!!)
    }

    enum class KeyFields(val columnName: String) {
        KEY("key"),
        EXPIRY("expiry"),
        SINGLE_USE("single_use"),
        ASSIGNEE("assignee"),
        FIRST_USE("first_use")
    }

    enum class UserFields(val columnName: String) {
        ID("id"),
        NAME("name"),
        PERMISSIONS("permissions"),
        CHAT_ID("chat_id")
    }

    enum class UserRegistrationTokensFields(val columeName: String) {
        TOKEN("token"),
        EXPIRY("expiry"),
        PERMISSIONS("permissions")
    }

    enum class ConversationFields(val columnName: String) {
        CHAT_ID("chat_id"),
        OUTBOUND_MESSAGE_ID("outbound_message_id"),
        CONVERSATION_STEP_TYPE("conversation_step_type")
    }

    class DbReadyChecker(var dbReady: Boolean)
}
