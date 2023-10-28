package com.brydonleonard.gatekey

import com.brydonleonard.gatekey.auth.AuthHandler
import com.brydonleonard.gatekey.auth.PermissionBundle
import com.brydonleonard.gatekey.auth.Permissions
import com.brydonleonard.gatekey.conversation.ConversationHandler
import com.brydonleonard.gatekey.keys.KeyManager
import com.brydonleonard.gatekey.persistence.model.ConversationStepModel
import com.brydonleonard.gatekey.persistence.model.ConversationStepType
import com.brydonleonard.gatekey.persistence.model.HouseholdModel
import com.brydonleonard.gatekey.registration.UserRegistrationManager
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.handlers.CallbackQueryHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.CommandHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.handlers.MessageHandlerEnvironment
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ForceReplyMarkup
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
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
                        awaitResponse(
                                ConversationStepType.CREATE_SINGLE_USE_TOKEN,
                                "Who will the key be for?"
                        )
                    }
                    return@command
                }

                command("listKeys") {
                    if (authorized(Permissions.LIST_KEYS)) {
                        val user = authHandler.getUser(this.message.from!!.id.toString())
                        val keys = keyManager.getActiveKeys(user!!.household).joinToString("\n") {
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
                        bot.sendMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                text = "What household should the user be added to?",
                                replyMarkup = InlineKeyboardMarkup.create(
                                        userRegistrationManager.getAllHouseholds().chunked(5).map { chunk ->
                                            chunk.map {
                                                InlineKeyboardButton.CallbackData(it.id, it.toCallbackQueryString())
                                            }
                                        }
                                )
                        )
                    }
                    return@command
                }

                command("addHousehold") {
                    if (authorized(Permissions.ADD_HOUSEHOLD)) {
                        awaitResponse(
                                ConversationStepType.CREATE_HOUSEHOLD,
                                "What should the household's ID be?"
                        )
                    }
                    return@command
                }

                callbackQuery {
                    if (authorized(Permissions.ADD_USER)) {
                        handleAddUserCallback()
                    }
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

                        if (!handleConversation(conversationStep)) {
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

    private fun CommandHandlerEnvironment.awaitResponse(stepType: ConversationStepType, messageText: String, replyMarkup: ReplyMarkup? = null) {
        val message = bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = messageText,
                replyMarkup = replyMarkup ?: ForceReplyMarkup()
        )

        conversationHandler.awaitResponse(
                ConversationStepModel(
                        this.message.chat.id,
                        message.get().messageId,
                        stepType
                )
        )
    }

    /**
     * Handles awaited conversations. Returns true if there was a conversation and it was handled.
     */
    private fun MessageHandlerEnvironment.handleConversation(conversationStep: ConversationStepModel?): Boolean {
        if (conversationStep == null) {
            return false
        }

        when (conversationStep.conversationStepType) {
            ConversationStepType.CREATE_SINGLE_USE_TOKEN -> handleCreateSingleUseTokenConversation(conversationStep)
            ConversationStepType.CREATE_HOUSEHOLD -> handleCreateHouseholdConversation(conversationStep)
        }

        return true
    }

    private fun MessageHandlerEnvironment.handleCreateSingleUseTokenConversation(conversationStep: ConversationStepModel) {
        val user = authHandler.getUser(this.message.from!!.id.toString())
        val household = userRegistrationManager.getHousehold(user!!.household.id)
        val key = keyManager.generateKey(this.message.text, household)
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
    }

    private fun MessageHandlerEnvironment.handleCreateHouseholdConversation(conversationStep: ConversationStepModel) {
        conversationHandler.stopAwaiting(conversationStep)

        val householdId = this.message.text!!
        userRegistrationManager.addHousehold(householdId)

        bot.sendMessage(
                chatId = ChatId.fromId(message.chat.id),
                text = "New household '$householdId' has been added.",
                replyMarkup = keyboard()
        )
    }

    private fun CallbackQueryHandlerEnvironment.handleAddUserCallback() {
        val household = userRegistrationManager.getHousehold(HouseholdModel.callbackQueryStringToId(this.callbackQuery.data))
        val token = userRegistrationManager.generateNewUserToken(PermissionBundle.RESIDENT, household)
        val chatId = this.callbackQuery.message!!.chat.id

        bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = "A token has been created for a new user in household ${household.id}. Here's the link:"
        )

        bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = UserRegistrationManager.tokenToLink(token.token)
        )

        bot.editMessageReplyMarkup(
                ChatId.fromId(chatId),
                callbackQuery.message!!.messageId,
                replyMarkup = InlineKeyboardMarkup.create(emptyList<List<InlineKeyboardButton>>())
        )
    }

    private fun keyboard(): KeyboardReplyMarkup = KeyboardReplyMarkup(
            keyboard = listOf(listOf(KeyboardButton("/createKey"))) +
                    listOf(listOf(KeyboardButton("/listKeys"))) +
                    listOf(listOf(KeyboardButton("/addHousehold"), KeyboardButton("/addUser"))),
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

    private fun CallbackQueryHandlerEnvironment.authorized(vararg permissions: Permissions): Boolean {
        return if (authHandler.authorize(callbackQuery.from.id.toString(), permissions.toSet())) {
            true
        } else {
            bot.sendMessage(
                    chatId = ChatId.fromId(callbackQuery.message!!.chat.id),
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
