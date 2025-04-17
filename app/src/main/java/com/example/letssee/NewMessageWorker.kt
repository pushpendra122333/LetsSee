package com.example.letssee

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.concurrent.TimeUnit

class NewMessageWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private val firestore = FirebaseFirestore.getInstance()

    override fun doWork(): Result {
        val receiverId = inputData.getString("receiverId") ?: return Result.failure()
        val chatId = inputData.getString("chatId") ?: return Result.failure()

        val messagesRef = firestore.collection("chats")
            .document(chatId)
            .collection("messages")

        return try {
            val query = messagesRef
                .whereEqualTo("receiverId", receiverId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)

            val result = Tasks.await(query.get(), 10, TimeUnit.SECONDS)

            if (!result.isEmpty) {
                val message = result.documents[0]
                val messageText = message.getString("message") ?: "New message"
                sendNotification(messageText)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("NewMessageWorker", "Error checking new messages: ${e.message}")
            Result.retry()
        }
    }

    private fun sendNotification(messageText: String) {
        val notificationId = 1

        val notification = NotificationCompat.Builder(applicationContext, "default")
            .setContentTitle("New Message")
            .setContentText(messageText)
            .setSmallIcon(R.drawable.baseline_message_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(notificationId, notification)
        } else {
            Log.w("Notification", "POST_NOTIFICATIONS permission not granted")
        }
    }
}
