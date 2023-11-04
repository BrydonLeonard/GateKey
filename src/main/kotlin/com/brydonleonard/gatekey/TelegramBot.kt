package com.brydonleonard.gatekey

import com.brydonleonard.gatekey.auth.AuthHandler
import com.brydonleonard.gatekey.auth.PermissionBundle
import com.brydonleonard.gatekey.auth.Permissions
import com.brydonleonard.gatekey.conversation.CallbackManager
import com.brydonleonard.gatekey.conversation.ConversationHandler
import com.brydonleonard.gatekey.keys.KeyManager
import com.brydonleonard.gatekey.metrics.MetricPublisher
import com.brydonleonard.gatekey.persistence.model.ConversationStepModel
import com.brydonleonard.gatekey.persistence.model.ConversationStepType
import com.brydonleonard.gatekey.persistence.model.UserModel
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

// I trim single newlines and treat them as "editor-only". It makes writing long strings easier, but lets Telegram handle
// wrapping lines.
val HELP_TEXT = """
    This is *GateKey*, an app that'll create key codes for the complex gate 🏠. When visitors arrive at the complex, 
    instead of the intercom phoning your phone, they'll type in the codes, which will open the gate automatically.
    
    
    Tap /createKey and enter the name of the visitor that will use that key to create a key that will be valid for up to 30 days. 
    When a key is used for the first time, a 5 minute timer starts. The visitor can use the key as many times as they like in 
    those 5 minutes, just in case the gate doesn't work the first time. After the 5 minutes are up, the key will stop working 
    permanently.
    
    
    Tap /listKeys to get a list of all of your valid keys, the visitors that they're for, and their expiration dates.
    
    
    Tap /deleteKey to get a list of your valid keys and choose one to delete.
""".trimIndent().replace(Regex("(\n*)\n"), "$1")

@Component
class TelegramBot(
        val keyManager: KeyManager,
        val authHandler: AuthHandler,
        val userRegistrationManager: UserRegistrationManager,
        val conversationHandler: ConversationHandler,
        val config: Config,
        val metricPublisher: MetricPublisher
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
                    authorized(setOf(Permissions.CREATE_KEY)) {
                        awaitResponse(
                                ConversationStepType.CREATE_SINGLE_USE_TOKEN,
                                "Who will the key be for?"
                        )
                    }
                    return@command
                }

                command("listKeys") {
                    authorized(setOf(Permissions.LIST_KEYS)) {
                        val user = authHandler.getUser(this.message.from!!.id.toString())
                        val keys = keyManager.getActiveKeys(user!!.household).joinToString("\n") {
                            "• \\[${it.formattedKey()}] for *${it.assignee}* expires ${it.formattedExpiry()}"
                        }

                        sendStandardMessage(keys, user)
                    }
                    return@command
                }

                command("deleteKey") {
                    authorized(setOf(Permissions.CREATE_KEY)) { user ->
                        bot.sendMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                text = "Which key would you like to delete?",
                                replyMarkup = InlineKeyboardMarkup.create(
                                        keyManager.getActiveKeys(user!!.household).chunked(2).map { chunk ->
                                            chunk.map {
                                                InlineKeyboardButton.CallbackData(
                                                        "${it.key}: ${it.assignee}",
                                                        CallbackManager.toCallbackDataWithId(CallbackManager.Callback.DELETE_KEY, it.key),
                                                )
                                            }
                                        }
                                )
                        )
                    }
                    return@command
                }

                command("addUser") {
                    authorized(setOf(Permissions.ADD_USER)) {
                        bot.sendMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                text = "What household should the user be added to?",
                                replyMarkup = InlineKeyboardMarkup.create(
                                        userRegistrationManager.getAllHouseholds().chunked(5).map { chunk ->
                                            chunk.map {
                                                InlineKeyboardButton.CallbackData(
                                                        it.id, CallbackManager.toCallbackDataWithId(CallbackManager.Callback.ADD_USER_HOUSEHOLD, it.id)
                                                )
                                            }
                                        }
                                )
                        )
                        metricPublisher.publish("addUser", 1.0)
                    }
                    return@command
                }

                command("addHousehold") {
                    authorized(setOf(Permissions.ADD_HOUSEHOLD)) {
                        awaitResponse(
                                ConversationStepType.CREATE_HOUSEHOLD,
                                "What should the household's ID be?"
                        )
                    }
                    return@command
                }

                command("help") {
                    val user = authHandler.getUser(this.message.from!!.id.toString())
                    if (user != null) {
                        sendStandardMessage(HELP_TEXT, user)
                    }
                    return@command
                }

                callbackQuery {
                    when (CallbackManager.callbackTypeForData(callbackQuery.data)) {
                        CallbackManager.Callback.DELETE_KEY -> handleDeleteKeyCallback()
                        CallbackManager.Callback.DELETE_KEY_YES -> handleDeleteKeyYes()
                        CallbackManager.Callback.DELETE_KEY_NO -> handleDeleteKeyNo()
                        CallbackManager.Callback.ADD_USER_HOUSEHOLD -> handleAddUserCallback()
                    }
                    return@callbackQuery
                }

                /**
                 * Catch-all that includes UUIDs for user registration
                 */
                command("start") {
                    logger.info { "Received a registration request from ${message.from!!.firstName} ${message.from!!.lastName}" }
                    val user = authHandler.getUser(this.message.from!!.id.toString())
                    if (user != null) {
                        sendStandardMessage("This account is already registered.", user)
                    } else {
                        try {
                            UUID.fromString(args[0])

                            val newUser = userRegistrationManager.createUserFromToken(
                                    args[0],
                                    this.message.from!!.id.toString(),
                                    this.message.from!!.let { "${it.firstName} ${it.lastName}" },
                                    this.message.chat.id.toString()
                            )

                            sendStandardMessage("Your account has been registered. Welcome to GateKey! " +
                                    "Create keys with the options in the menu below", newUser)
                        } catch (e: IllegalArgumentException) {
                            logger.error(e) { "Failed to register user" }
                            bot.sendMessage(
                                    chatId = ChatId.fromId(message.chat.id),
                                    text = "The token is invalid"
                            )
                        }
                    }
                    return@command
                }

                // https://t.me/LeonardHomeBot?start=UniqueStartMessage
                message(UUIDFilter.not() and Filter.Command.not()) {
                    val user = authHandler.getUser(this.message.from!!.id.toString())
                    if (user != null) {
                        val conversationStep = conversationHandler.checkForConversation(
                                this.message.chat.id,
                                this.message.replyToMessage?.messageId
                        )

                        if (!handleConversation(user, conversationStep)) {
                            logger.info { message.chat.id }

                            sendStandardMessage("Tap /help for more information on how to use the GateKey app.", user)
                        }
                    } else {
                        bot.sendMessage(
                                chatId = ChatId.fromId(message.chat.id),
                                text = "Please enter your registration token to continue."
                        )
                    }
                    return@message
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
    private fun MessageHandlerEnvironment.handleConversation(user: UserModel, conversationStep: ConversationStepModel?): Boolean {
        if (conversationStep == null) {
            return false
        }

        when (conversationStep.conversationStepType) {
            ConversationStepType.CREATE_SINGLE_USE_TOKEN -> handleCreateSingleUseTokenConversation(user, conversationStep)
            ConversationStepType.CREATE_HOUSEHOLD -> handleCreateHouseholdConversation(user, conversationStep)
        }

        return true
    }

    private fun MessageHandlerEnvironment.handleCreateSingleUseTokenConversation(user: UserModel, conversationStep: ConversationStepModel) {
        val user = authHandler.getUser(this.message.from!!.id.toString())
        val household = userRegistrationManager.getHousehold(user!!.household.id)
        val key = keyManager.generateKey(this.message.text, household)
        conversationHandler.stopAwaiting(conversationStep)

        sendStandardMessage("${key.assignee}'s key '${key.key}' will be valid until " +
                "${key.formattedExpiry()} for a single use. Here's a message that you " +
                "can forward straight to them:", user)
        sendStandardMessage("""
                    Hi, ${key.assignee}. Your L'Afrique keycode is
                    
                            *${key.formattedKey()}*
                    
                    When you arrive at L'Afrique Eco Village:
                    1. Dial 39📞 on the intercom keypad
                    2. Wait for the call to connect
                    3. Dial ${key.formattedKey()}#
                    4. The gate will open                          
                """.trimIndent(),
                user)

        metricPublisher.publish("createKey", 1.0)
    }

    private fun MessageHandlerEnvironment.handleCreateHouseholdConversation(user: UserModel, conversationStep: ConversationStepModel) {
        conversationHandler.stopAwaiting(conversationStep)

        val householdId = this.message.text!!
        userRegistrationManager.addHousehold(householdId)

        sendStandardMessage("New household '$householdId' has been added.", user)
    }

    private fun CallbackQueryHandlerEnvironment.handleDeleteKeyCallback() {
        authorized(setOf(Permissions.CREATE_KEY)) { user ->
            val chatId = this.callbackQuery.message!!.chat.id
            val keyId = CallbackManager.callbackIdForData(this.callbackQuery.data)
            val key = keyManager.getActiveKey(user.household, keyId)

            if (key == null) {
                sendStandardMessage("That key is already invalid!", user)
                return@authorized
            }

            bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = "Are you sure you want to delete key ${key.formattedKey()}? It's for ${key.assignee}" +
                            " and will expire at ${key.formattedExpiry()}.",
                    replyMarkup = InlineKeyboardMarkup.create(
                            listOf(
                                    InlineKeyboardButton.CallbackData(
                                            "Yes",
                                            CallbackManager.toCallbackDataWithId(CallbackManager.Callback.DELETE_KEY_YES, key.key)
                                    ),
                                    InlineKeyboardButton.CallbackData(
                                            "No",
                                            CallbackManager.toCallbackDataWithId(CallbackManager.Callback.DELETE_KEY_NO, key.key)
                                    )
                            )
                    )
            )

            clearCallbackKeyboard()
        }
    }

    private fun CallbackQueryHandlerEnvironment.handleDeleteKeyYes() {
        authorized(setOf(Permissions.CREATE_KEY)) { user ->
            val keyId = CallbackManager.callbackIdForData(this.callbackQuery.data)
            val key = keyManager.getActiveKey(user.household, keyId)

            if (key == null) {
                sendStandardMessage("That key is already invalid!", user)
                return@authorized
            }

            try {
                keyManager.expireKey(key)
                sendStandardMessage("${key.formattedKey()} has been deleted", user)
                metricPublisher.publish("deleteKey-success", 1.0)
            } catch (e: Exception) {
                sendStandardMessage("Something went wrong while deleting the key. It may already be deleted", user)
                metricPublisher.publish("deleteKey-success", 0.0)
                metricPublisher.publish("deleteKey-error", 1.0)
            }
            clearCallbackKeyboard()
        }
    }

    private fun CallbackQueryHandlerEnvironment.handleDeleteKeyNo() {
        authorized(setOf(Permissions.CREATE_KEY)) { user ->
            sendStandardMessage("Cancelled!", user)
            clearCallbackKeyboard()
            metricPublisher.publish("deleteKey-cancelled", 1.0)
        }
    }

    private fun CallbackQueryHandlerEnvironment.handleAddUserCallback() {
        authorized(setOf(Permissions.ADD_USER)) { user ->
            val household = userRegistrationManager.getHousehold(CallbackManager.callbackIdForData(this.callbackQuery.data))
            val token = userRegistrationManager.generateNewUserToken(PermissionBundle.RESIDENT, household)

            sendStandardMessage("A token has been created for a new user in household ${household.id}. Here's the link:", user)
            sendStandardMessage("""
                    Here's your Gatekey registration link!
                    ${UserRegistrationManager.tokenToLink(token.token)}
                    
                    GateKey runs inside an app called Telegram, so if the link above doesn't work, you'll need to install Telegram first:
                    - Android: https://play.google.com/store/apps/details?id=org.telegram.messenger
                    - iPhone: https://apps.apple.com/us/app/telegram-messenger/id686449807
                """.trimIndent(),
                    user
            )

            clearCallbackKeyboard()
        }
    }

    private fun CallbackQueryHandlerEnvironment.sendStandardMessage(message: String, user: UserModel) {
        val chatId = this.callbackQuery.message!!.chat.id

        sendStandardMessage(chatId, message, user)
    }

    private fun CommandHandlerEnvironment.sendStandardMessage(message: String, user: UserModel) {
        val chatId = this.message.chat.id

        sendStandardMessage(chatId, message, user)
    }

    private fun MessageHandlerEnvironment.sendStandardMessage(message: String, user: UserModel) {
        val chatId = this.message.chat.id

        sendStandardMessage(chatId, message, user)
    }

    private fun sendStandardMessage(chatId: Long, message: String, user: UserModel) {
        bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = message,
                replyMarkup = keyboard(user),
                parseMode = ParseMode.MARKDOWN,
        )
    }

    /**
     * Clears the inline keyboard of the message that triggered this callback
     */
    private fun CallbackQueryHandlerEnvironment.clearCallbackKeyboard() {
        val chatId = this.callbackQuery.message!!.chat.id
        bot.editMessageReplyMarkup(
                ChatId.fromId(chatId),
                callbackQuery.message!!.messageId,
                replyMarkup = InlineKeyboardMarkup.create(emptyList<List<InlineKeyboardButton>>())
        )
    }

    private fun keyboard(user: UserModel): KeyboardReplyMarkup {
        val lastRow = listOf<KeyboardButton>().let {
            if (user.permissions.contains(Permissions.ADD_HOUSEHOLD)) {
                it + KeyboardButton("/addHousehold")
            } else {
                it
            }
        }.let {
            if (user.permissions.contains(Permissions.ADD_HOUSEHOLD)) {
                it + KeyboardButton("/addUser")
            } else {
                it
            }
        }

        val buttons = listOf(
                listOf(KeyboardButton("/createKey")),
                listOf(KeyboardButton("/listKeys"), KeyboardButton("/deleteKey")),
                listOf(KeyboardButton("/help")),
                lastRow,
        )

        return KeyboardReplyMarkup(
                keyboard = buttons,
                resizeKeyboard = true
        )
    }

    private fun <T> CommandHandlerEnvironment.authorized(permissions: Set<Permissions>, action: (UserModel) -> T): T? {
        val authorizedUser: UserModel? = authHandler.getAuthorizedUser(this.message.from!!.id.toString(), permissions.toSet())

        if (authorizedUser == null) {
            bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Unauthorized"
            )

            metricPublisher.publish("Unauthorized", 1.0)
        }

        return authorizedUser?.let { action(it) }
    }

    private fun <T> CallbackQueryHandlerEnvironment.authorized(permissions: Set<Permissions>, action: (UserModel) -> T): T? {
        val authorizedUser = authHandler.getAuthorizedUser(callbackQuery.from.id.toString(), permissions.toSet())

        if (authorizedUser == null) {
            bot.sendMessage(
                    chatId = ChatId.fromId(callbackQuery.message!!.chat.id),
                    text = "Unauthorized"
            )

            metricPublisher.publish("Unauthorized", 1.0)
        }

        return authorizedUser?.let { action(it) }
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
