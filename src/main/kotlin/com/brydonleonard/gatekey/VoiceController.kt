package com.brydonleonard.gatekey

import com.brydonleonard.gatekey.conversation.ConversationHandler
import com.brydonleonard.gatekey.keys.KeyManager
import com.brydonleonard.gatekey.persistence.model.KeyModel
import com.twilio.twiml.VoiceResponse
import com.twilio.twiml.voice.Dial
import com.twilio.twiml.voice.Gather
import com.twilio.twiml.voice.Number
import com.twilio.twiml.voice.Redirect
import com.twilio.twiml.voice.Reject
import com.twilio.twiml.voice.Say
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class VoiceController(
        val keyManager: KeyManager,
        val messageHandler: ConversationHandler,
        val telegramBot: TelegramBot,
        val config: Config
) {
    private val logger = KotlinLogging.logger(VoiceController::class.qualifiedName!!)

    // TODO add a voice redirect and limit the total number of round trips so people don't just hang out on the phone forever

    @PostMapping(
            "/voice",
            consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
            produces = [MediaType.APPLICATION_XML_VALUE]
    )
    fun receiveVoice(@RequestParam requestBody: MultiValueMap<String, String>): ResponseEntity<String> {
        val builder = VoiceResponse.Builder()

        /*if (!config.allowedCallers.contains(requestBody["Caller"]?.get(0) ?: "")) {
            logger.info { "Rejecting a call from ${requestBody["Caller"]?.get(0)}" }
            return ok(builder.rejectCall())
        }*/

        val digits = requestBody["Digits"]?.get(0)

        logger.info { "Received digits from the gate: $digits" }

        if (digits != null) {
            if (!digits.matches("^[0-9]{6}$".toRegex())) {
                logger.warn { "The key didn't match the expected pattern" }
                return ok(builder.invalidCode(digits).gatherKey())
            }
            val authorizedKey = authorizeCaller(digits) ?: run {
                logger.warn { "The key was not authorized" }
                return ok(builder.invalidCode(digits).gatherKey())
            }

            return ok(builder.openGate()).also {
                logger.info { "Opening the gate for ${authorizedKey.assignee}" }
                messageHandler.getAllChatIds().forEach {
                    telegramBot.sendMessage(it, "Opening the gate for ${authorizedKey.assignee}")
                }
            }
        }

        return ok(builder.gatherKey())
    }

    fun authorizeCaller(digits: String): KeyModel? {
        return keyManager.tryUseKey(digits)
    }

    fun ok(voiceResponseBuilder: VoiceResponse.Builder): ResponseEntity<String> {
        return ResponseEntity(voiceResponseBuilder.build().toXml(), HttpStatus.OK)
    }

    fun VoiceResponse.Builder.openGate(): VoiceResponse.Builder {
        return this.dial(
                Dial.Builder().number(
                        Number.Builder("9").build()
                ).build()
        )
    }

    fun VoiceResponse.Builder.rejectCall(): VoiceResponse.Builder {
        return this.reject(
                Reject.Builder()
                        .reason(Reject.Reason.REJECTED)
                        .build()
        )
    }

    fun VoiceResponse.Builder.invalidCode(code: String): VoiceResponse.Builder {
        return this.say(
                Say.Builder("That key is invalid. You typed $code").build()
        )
    }

    fun VoiceResponse.Builder.gatherKey(): VoiceResponse.Builder {
        return this.gather(
                Gather.Builder()
                        .numDigits(7)
                        .say(Say.Builder("Enter your 6 digit key and press hash").build())
                        .build()
        ).redirect(Redirect.Builder("/voice").build())
    }
}
