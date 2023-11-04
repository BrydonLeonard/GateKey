package com.brydonleonard.gatekey.conversation

object CallbackManager {
    fun toCallbackDataWithId(callback: Callback, id: String) = "$callback-$id"

    fun callbackTypeForData(callbackString: String) = Callback.valueOf(callbackString.split("-").first())

    fun callbackIdForData(callbackString: String) = callbackString.split("-").last()

    enum class Callback {
        DELETE_KEY,
        DELETE_KEY_YES,
        DELETE_KEY_NO,
        ADD_USER_HOUSEHOLD,
    }
}