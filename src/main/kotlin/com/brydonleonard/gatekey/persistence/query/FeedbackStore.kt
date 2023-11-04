package com.brydonleonard.gatekey.persistence.query

import com.brydonleonard.gatekey.persistence.DbManager
import com.brydonleonard.gatekey.persistence.model.FeedbackModel
import com.brydonleonard.gatekey.persistence.model.UserModel
import org.springframework.stereotype.Component

@Component
class FeedbackStore(val dbManager: DbManager) {
    fun putFeedback(feedbackMessage: String, user: UserModel) {
        val feedback = FeedbackModel();
        feedback.user = user
        feedback.message = feedbackMessage

        dbManager.feedbackDao.create(feedback)
    }
}