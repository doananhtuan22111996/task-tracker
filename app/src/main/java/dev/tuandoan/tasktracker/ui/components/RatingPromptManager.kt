package dev.tuandoan.tasktracker.ui.components

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

object RatingPromptManager {

    fun maybeRequestReview(activity: Activity) {
        try {
            val reviewManager = ReviewManagerFactory.create(activity)
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    reviewManager.launchReviewFlow(activity, task.result)
                }
            }
        } catch (_: Exception) {
            // Silently skip if Play Store is unavailable (e.g., non-Play builds)
        }
    }
}
