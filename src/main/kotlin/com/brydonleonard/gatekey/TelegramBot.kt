package com.brydonleonard.gatekey

import com.brydonleonard.gatekey.auth.AuthHandler
import com.brydonleonard.gatekey.auth.PermissionBundle
import com.brydonleonard.gatekey.auth.Permissions
import com.brydonleonard.gatekey.conversation.ConversationHandler
import com.brydonleonard.gatekey.keys.KeyManager
import com.brydonleonard.gatekey.persistence.model.ConversationStepModel
import com.brydonleonard.gatekey.persistence.model.ConversationStepType
import com.brydonleonard.gatekey.registration.UserRegistrationManager
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ForceReplyMarkup
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.github.kotlintelegrambot.logging.LogLevel
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TelegramBot(
        val keyManager: KeyManager,
        val authHandler: AuthHandler,
        val userRegistrationManager: UserRegistrationManager,
        val conversationHandler: ConversationHandler,
        val config: Config
) {
    private val logger = KotlinLogging.logger(TelegramBot::class.qualifiedName!!)

    private lateinit var bot: Bot

    @PostConstruct
    fun start() {
        Thread { poll() }.start()
    }

    fun sendMessage(chatId: Long, message: String) {
        bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = message
        )
    }

    private fun poll() {
        bot = bot {
            logLevel = LogLevel.Error
            token = config.telegramBotToken

            dispatch {
                command("createKey") {
                    if (authorized(Permissions.CREATE_KEY)) {
                        val message = bot.sendMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                text = "Who will the key be for?",
                                replyMarkup = ForceReplyMarkup()
                        )

                        conversationHandler.awaitResponse(
                                ConversationStepModel(
                                        this.message.chat.id,
                                        message.get().messageId,
                                        ConversationStepType.CREATE_SINGLE_USE_TOKEN
                                )
                        )
                    }
                    return@command
                }

                command("listKeys") {
                    if (authorized(Permissions.LIST_KEYS)) {
                        val keys =
                                keyManager.getActiveKeys().joinToString("\n") {
                                    val assigneeSuffix = if (it.assignee != null) " (for ${it.assignee})" else ""
                                    "${it.key} expires at ${it.formattedExpiry()}$assigneeSuffix"
                                }

                        bot.sendMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                text = keys
                        )
                    }
                    return@command
                }

                command("addUser") {
                    if (authorized(Permissions.ADD_USER)) {
                        val token = userRegistrationManager.generateNewUserToken(PermissionBundle.ADMIN)

                        bot.sendMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                text = "A token has been created for the new user. Here's the link:"
                        )

                        bot.sendMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                text = UserRegistrationManager.tokenToLink(token.token)
                        )
                    }
                    return@command
                }

                /**
                 * Catch-all that includes UUIDs for user registration
                 */
                command("start") {
                    logger.info { "Received a registration request from ${message.from!!.firstName} ${message.from!!.lastName}" }
                    if (authHandler.userExists(this.message.from!!.id.toString())) {
                        bot.sendMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                text = "This account is already registered.",
                                replyMarkup = keyboard()
                        )
                    } else {
                        try {
                            UUID.fromString(args[0])

                            userRegistrationManager.createUserFromToken(
                                    args[0],
                                    this.message.from!!.id.toString(),
                                    this.message.from!!.let { "${it.firstName} ${it.lastName}" },
                                    this.message.chat.id.toString()
                            )

                            bot.sendMessage(
                                    chatId = ChatId.fromId(message.chat.id),
                                    text = "Your account has been registered. Welcome to GateKey! " +
                                            "Create keys with the options in the menu below",
                                    replyMarkup = keyboard()
                            )
                        } catch (e: IllegalArgumentException) {
                            logger.error(e) { "Failed to register user" }
                            bot.sendMessage(
                                    chatId = ChatId.fromId(message.chat.id),
                                    text = "The token is invalid"
                            )
                        }
                    }
                }

                // https://t.me/LeonardHomeBot?start=UniqueStartMessage
                message(UUIDFilter.not() and Filter.Command.not()) {
                    if (authHandler.userExists(this.message.from!!.id.toString())) {
                        val conversationStep = conversationHandler.checkForConversation(
                                this.message.chat.id,
                                this.message.replyToMessage?.messageId
                        )



                        if (conversationStep != null &&
                                conversationStep.conversationStepType == ConversationStepType.CREATE_SINGLE_USE_TOKEN
                        ) {
                            val key = keyManager.generateKey(this.message.text)
                            conversationHandler.stopAwaiting(conversationStep)

                            bot.sendMessage(
                                    chatId = ChatId.fromId(message.chat.id),
                                    text = "${key.assignee}'s key '${key.key}' will be valid until " +
                                            "${key.formattedExpiry()} for a single use. Here's a message that you " +
                                            "can forward straight to them:",
                                    replyMarkup = keyboard()
                            )
                            bot.sendMessage(
                                    chatId = ChatId.fromId(message.chat.id),
                                    text = """
                                        Hi, ${key.assignee}. Your L'Afrique keycode is
                                        
                                                *${key.formattedKey}*
                                        
                                        When you arrive at L'Afrique Eco Village:
                                        1. Dial 39📞 on the intercom keypad
                                        2. Wait for the call to connect
                                        3. Dial ${key.formattedKey}#
                                        4. The gate will open                          
                                    """.trimIndent(),
                                    replyMarkup = keyboard(),
                                    parseMode = ParseMode.MARKDOWN,
                            )
                        } else {
                            logger.info { message.chat.id }

                            bot.sendMessage(
                                    chatId = ChatId.fromId(message.chat.id),
                                    text = "Create keys with the options in the menu below.",
                                    replyMarkup = keyboard()
                            )
                        }
                    } else {
                        bot.sendMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                text = "Please enter your registration token to continue."
                        )
                    }
                }
            }
        }

        bot.startPolling()
    }

    private fun keyboard(): KeyboardReplyMarkup = KeyboardReplyMarkup(
            keyboard = listOf(listOf(KeyboardButton("/createKey"))) +
                    listOf(listOf(KeyboardButton("/listKeys"))) +
                    listOf(listOf(KeyboardButton("/addUser"))),
            resizeKeyboard = true
    )

    private fun CommandHandlerEnvironment.authorized(vararg permissions: Permissions): Boolean {
        return if (authHandler.authorize(this.message.from!!.id.toString(), permissions.toSet())) {
            true
        } else {
            bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Unauthorized"
            )
            false
        }
    }

    private object UUIDFilter : Filter {
        override fun Message.predicate(): Boolean {
            if (this.text == null) {
                return false
            }
            return try {
                UUID.fromString(this.text)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }
}
