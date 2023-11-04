package com.brydonleonard.gatekey

import com.brydonleonard.gatekey.conversation.ConversationHandler
import com.brydonleonard.gatekey.keys.KeyManager
import com.brydonleonard.gatekey.metrics.MetricPublisher
import com.brydonleonard.gatekey.persistence.model.KeyModel
import com.twilio.security.RequestValidator
import com.twilio.twiml.VoiceResponse
import com.twilio.twiml.voice.Gather
import com.twilio.twiml.voice.Play
import com.twilio.twiml.voice.Redirect
import com.twilio.twiml.voice.Reject
import com.twilio.twiml.voice.Say
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.InetAddress


private const val TWILIO_SIGNATURE_HEADER_NAME = "X-Twilio-Signature"

@RestController
class VoiceController(
        val keyManager: KeyManager,
        val conversationHandler: ConversationHandler,
        val telegramBot: TelegramBot,
        val config: Config,
        val metricPublisher: MetricPublisher
) {
    private val logger = KotlinLogging.logger(VoiceController::class.qualifiedName!!)

    private val twilioSignatureValidator = RequestValidator(config.twilioAuthToken)

    // TODO add a voice redirect and limit the total number of round trips so people don't just hang out on the phone forever

    @PostMapping(
            "/voice",
            consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE],
            produces = [MediaType.APPLICATION_XML_VALUE]
    )
    fun receiveVoice(@RequestParam requestBody: MultiValueMap<String, String>, request: HttpServletRequest): ResponseEntity<String> {
        val builder = VoiceResponse.Builder()

        // Basic API auth. Don't accept requests from unexpected sources
        if (!authorizeApiCaller(requestBody, request)) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }

        // Phone number auth. Don't accept calls from phone numbers that we don't expect.
        if (!authorizePhoneCaller(requestBody["Caller"]?.get(0) ?: "", request)) {
            return ok(builder.rejectCall())
        }

        val digits = requestBody["Digits"]?.get(0)

        if (digits != null) {
            logger.info { "Received digits from the gate: $digits" }

            if (!digits.matches("^[0-9]{6}$".toRegex())) {
                logger.warn { "The key didn't match the expected pattern" }
                metricPublisher.publish("useKey-badKey", 1.0)
                return ok(builder.invalidCode(digits).gatherKey())
            }
            val authorizedKey = authorizeKey(digits) ?: run {
                logger.warn { "The key was not authorized" }
                metricPublisher.publish("useKey-unauthorized", 1.0)
                return ok(builder.invalidCode(digits).gatherKey())
            }

            return ok(builder.openGate()).also {
                metricPublisher.publish("useKey-success", 1.0)
                logger.info { "Opening the gate for ${authorizedKey.assignee}" }

                // Notify all users that the gate has been opened
                conversationHandler.getAllChatsForHousehold(authorizedKey.household).forEach {
                    telegramBot.sendMessage(it, "Opening the gate for ${authorizedKey.assignee}")
                }
            }
        }

        return ok(builder.gatherKey())
    }

    private fun authorizeApiCaller(requestBody: MultiValueMap<String, String>, request: HttpServletRequest): Boolean {
        if (InetAddress.getByName(request.remoteAddr).isLoopbackAddress) {
            return true
        }

        logger.debug { "Rejecting an API call from ${request.remoteAddr}" }

        // application/x-www-form-urlencoded data can have multiple values per field, but Twilio doesn't do so,
        // so flatten each list into a single value.
        val params = requestBody.map {
            it.key to it.value.first()
        }.toMap()

        val twilioSignature = request.getHeader(TWILIO_SIGNATURE_HEADER_NAME)

        return twilioSignatureValidator.validate(request.requestURL.toString(), params, twilioSignature)
    }

    fun authorizePhoneCaller(callerNumber: String, request: HttpServletRequest): Boolean {
        if (InetAddress.getByName(request.remoteAddr).isLoopbackAddress) {
            return true
        }

        // Numbers can be configured with or without region codes and with or without the + ahead of the region code
        if (config.allowedCallers.intersect(validPhoneNumberForms(callerNumber)).isEmpty()) {
            logger.info { "Rejecting a call from $callerNumber" }
            return false
        }
        return true
    }

    //  TODO consider pre-materialising the list of valid numbers instead of transforming the numbers on each request
    private fun validPhoneNumberForms(number: String) = setOf(
            number,
            number.replace("+27", "27"),
            number.replace("+27", "0")
    )

    private fun authorizeKey(digits: String): KeyModel? {
        return keyManager.tryUseKey(digits)
    }

    private fun ok(voiceResponseBuilder: VoiceResponse.Builder): ResponseEntity<String> {
        return ResponseEntity(voiceResponseBuilder.build().toXml(), HttpStatus.OK)
    }

    private fun VoiceResponse.Builder.openGate(): VoiceResponse.Builder {
        return this.play(
                Play.Builder().digits("9999").build()
        )
    }

    private fun VoiceResponse.Builder.rejectCall(): VoiceResponse.Builder {
        return this.reject(
                Reject.Builder()
                        .reason(Reject.Reason.REJECTED)
                        .build()
        )
    }

    private fun VoiceResponse.Builder.invalidCode(code: String): VoiceResponse.Builder {
        return this.say(
                Say.Builder("That key is invalid. You typed $code").build()
        )
    }

    private fun VoiceResponse.Builder.gatherKey(): VoiceResponse.Builder {
        return this.gather(
                Gather.Builder()
                        .numDigits(7)
                        .say(Say.Builder("Enter your 6 digit key and press hash").build())
                        .build()
        ).redirect(Redirect.Builder("/voice").build())
    }
}
