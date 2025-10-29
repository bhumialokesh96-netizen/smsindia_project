package com.rupeedesk7.smsapp.worker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.CoroutineWorker;
import androidx.work.ForegroundInfo;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;
import androidx.work.OneTimeWorkRequest;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Data;
import androidx.work.ListenableWorker;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import kotlinx.coroutines.tasks.await;

import java.util.List;
import java.util.Map;

public class SmsWorker extends CoroutineWorker {
    private static final String TAG = "SmsWorker";
    private final FirebaseFirestore db;

    public SmsWorker(Context appContext, WorkerParameters params) {
        super(appContext, params);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public ForegroundInfo getForegroundInfoAsync() {
        return createForegroundInfo("Sending SMS", "Processing messages...");
    }

    @Override
    public Result doWork() {
        try {
            setForeground(createForegroundInfo("Sending SMS", "Processing messages..."));
        } catch (Exception e) {
            Log.w(TAG, "Unable to set foreground: " + e.getMessage());
        }

        // Input:
        // userId (String) - current user who started the sending
        // subscriptionId (int) - which SIM to use (-1 uses default)
        // maxPerRun (int) - how many messages to send in a single run (safety)
        String userId = getInputData().getString("userId");
        int subscriptionId = getInputData().getInt("subscriptionId", -1);
        int maxPerRun = getInputData().getInt("maxPerRun", 25);

        try {
            // Query queue: collection "queue" where sent == false and userId == userId (or all if null)
            List<DocumentSnapshot> docs = db.collection("queue")
                    .whereEqualTo("sent", false)
                    .limit(maxPerRun)
                    .get().await().getDocuments();

            if (docs.isEmpty()) {
                // Nothing to send — mark as success
                return Result.success();
            }

            SmsManager smsManager = (subscriptionId >= 0)
                ? getSmsManagerForSub(subscriptionId)
                : SmsManager.getDefault();

            int sentCount = 0;
            for (DocumentSnapshot d : docs) {
                Map<String, Object> data = d.getData();
                if (data == null) continue;
                String target = (String) data.get("target");
                String message = (String) data.get("message");
                Double price = data.get("price") instanceof Number ? ((Number) data.get("price")).doubleValue() : 0.16;
                String owner = (String) data.get("userId");
                if (target == null || message == null) {
                    d.getReference().update("sent", true).addOnFailureListener(e -> Log.w(TAG, "mark-sent-fail", e));
                    continue;
                }

                boolean ok = sendSms(smsManager, target, message);
                if (ok) {
                    // mark sent and set sentAt
                    d.getReference().update("sent", true, "sentAt", Timestamp.now()).addOnFailureListener(e -> Log.w(TAG, "mark-sent-fail", e));
                    // debit user's balance and increment dailySent
                    if (owner != null) {
                        db.collection("users").document(owner)
                            .update("balance", com.google.firebase.firestore.FieldValue.increment(-price),
                                    "dailySent", com.google.firebase.firestore.FieldValue.increment(1))
                            .addOnFailureListener(e -> Log.w(TAG, "user-update-fail", e));
                    }
                    sentCount++;
                } else {
                    Log.w(TAG, "SMS failed to send to " + target);
                }
            }

            // If we likely have more to send, and the user still wants running, schedule another run with slight delay
            // Check a document to see if user still running: users/{userId}.sendingRunning == true
            boolean shouldContinue = false;
            if (userId != null && !userId.isEmpty()) {
                DocumentSnapshot userSnap = db.collection("users").document(userId).get().await();
                shouldContinue = userSnap.exists() && Boolean.TRUE.equals(userSnap.getBoolean("sendingRunning"));
            }

            if (shouldContinue) {
                // reschedule another worker (append)
                Data input = new Data.Builder()
                        .putString("userId", userId)
                        .putInt("subscriptionId", subscriptionId)
                        .putInt("maxPerRun", maxPerRun)
                        .build();

                OneTimeWorkRequest w = new OneTimeWorkRequest.Builder(SmsWorker.class)
                        .setInputData(input)
                        .build();

                WorkManager.getInstance(getApplicationContext())
                        .enqueueUniqueWork("sms_queue_worker_" + userId, ExistingWorkPolicy.APPEND_OR_REPLACE, w);
            }

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Worker exception", e);
            // backoff and retry
            return Result.retry();
        }
    }

    private boolean sendSms(SmsManager manager, String target, String message) {
        try {
            manager.sendTextMessage(target, null, message, null, null);
            Log.i(TAG, "Sent to " + target);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "send fail", e);
            return false;
        }
    }

    private SmsManager getSmsManagerForSub(int subId) {
        try {
            return SmsManager.getSmsManagerForSubscriptionId(subId);
        } catch (Exception e) {
            Log.w(TAG, "getSmsManagerForSub failed, using default", e);
            return SmsManager.getDefault();
        }
    }

    private ForegroundInfo createForegroundInfo(String title, String text) {
        Context ctx = getApplicationContext();
        String channelId = "sms_worker_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(channelId) == null) {
                NotificationChannel ch = new NotificationChannel(channelId, "SMS Worker", NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(ch);
            }
        }
        Notification n = new NotificationCompat.Builder(ctx, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setOngoing(true)
            .build();
        return new ForegroundInfo(101, n);
    }
}