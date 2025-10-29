package com.smsindia.app.worker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.CoroutineWorker;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ForegroundInfo;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.List;
import java.util.Map;

import kotlinx.coroutines.tasks.await;

public class SmsWorker extends CoroutineWorker {
    private static final String TAG = "SmsWorker";
    private final FirebaseFirestore db;

    public SmsWorker(@NonNull Context appContext, @NonNull WorkerParameters params) {
        super(appContext, params);
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = getInputData().getString("userId");
        int simId = getInputData().getInt("subscriptionId", -1);
        int maxPerRun = getInputData().getInt("maxPerRun", 10);

        try {
            setForeground(createForegroundInfo("SMSIndia", "Sending queued messages..."));

            List<DocumentSnapshot> docs = db.collection("queue")
                    .whereEqualTo("sent", false)
                    .whereEqualTo("userId", userId)
                    .limit(maxPerRun)
                    .get().await().getDocuments();

            if (docs.isEmpty()) {
                Log.i(TAG, "No pending messages found.");
                return Result.success();
            }

            SmsManager manager = (simId >= 0)
                    ? SmsManager.getSmsManagerForSubscriptionId(simId)
                    : SmsManager.getDefault();

            int sentCount = 0;
            for (DocumentSnapshot d : docs) {
                Map<String, Object> msg = d.getData();
                if (msg == null) continue;

                String target = (String) msg.get("target");
                String body = (String) msg.get("message");
                double price = msg.get("price") instanceof Number ? ((Number) msg.get("price")).doubleValue() : 0.16;

                if (target == null || body == null) continue;

                try {
                    manager.sendTextMessage(target, null, body, null, null);
                    Log.i(TAG, "✅ Sent SMS to " + target);

                    d.getReference().update(
                            "sent", true,
                            "sentAt", Timestamp.now()
                    );

                    db.collection("users").document(userId)
                            .update("balance", FieldValue.increment(-price),
                                    "dailySent", FieldValue.increment(1));

                    sentCount++;
                    Thread.sleep(1000); // delay between sends
                } catch (Exception e) {
                    Log.e(TAG, "SMS send fail: " + e.getMessage());
                }
            }

            if (sentCount > 0) {
                Data nextRun = new Data.Builder()
                        .putString("userId", userId)
                        .putInt("subscriptionId", simId)
                        .putInt("maxPerRun", maxPerRun)
                        .build();

                OneTimeWorkRequest next = new OneTimeWorkRequest.Builder(SmsWorker.class)
                        .setInputData(nextRun)
                        .build();

                WorkManager.getInstance(getApplicationContext())
                        .enqueueUniqueWork("sms_queue_worker_" + userId, ExistingWorkPolicy.APPEND_OR_REPLACE, next);
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Worker error", e);
            return Result.retry();
        }
    }

    private ForegroundInfo createForegroundInfo(String title, String text) {
        Context ctx = getApplicationContext();
        String channelId = "sms_worker_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(channelId, "SMS Worker", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(ch);
            }
        }
        Notification n = new NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .build();
        return new ForegroundInfo(100, n);
    }
}