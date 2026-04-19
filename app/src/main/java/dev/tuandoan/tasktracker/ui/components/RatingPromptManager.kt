package dev.tuandoan.tasktracker.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewManagerFactory

object RatingPromptManager {

    fun maybeRequestReview(activity: Activity) {
        try {
            val reviewManager = ReviewManagerFactory.create(activity)
            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    reviewManager.launchReviewFlow(activity, task.result)
                } else {
                    openPlayStore(activity)
                }
            }
        } catch (_: Exception) {
            openPlayStore(activity)
        }
    }

    private fun openPlayStore(activity: Activity) {
        val uri = Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")
        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
