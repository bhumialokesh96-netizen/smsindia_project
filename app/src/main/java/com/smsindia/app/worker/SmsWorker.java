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
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.OneTimeWorkRequest;
import androidx.work.ExistingWorkPolicy;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class SmsWorker extends Worker {
    private static final String TAG = "SmsWorker";
    private final FirebaseFirestore db;

    public SmsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setForegroundAsync(createForegroundInfo("Sending SMS", "Processing messages..."));
        } catch (Exception e) {
            Log.w(TAG, "Unable to set foreground: " + e.getMessage());
        }

        String userId = getInputData().getString("userId");
        int subscriptionId = getInputData().getInt("subscriptionId", -1);
        int maxPerRun = getInputData().getInt("maxPerRun", 25);

        try {
            List<DocumentSnapshot> docs = Tasks.await(
                    db.collection("queue")
                            .whereEqualTo("sent", false)
                            .limit(maxPerRun)
                            .get()
            ).getDocuments();

            if (docs.isEmpty()) {
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
                Double price = data.get("price") instanceof Number
                        ? ((Number) data.get("price")).doubleValue()
                        : 0.16;
                String owner = (String) data.get("userId");

                if (target == null || message == null) {
                    d.getReference().update("sent", true);
                    continue;
                }

                boolean ok = sendSms(smsManager, target, message);
                if (ok) {
                    d.getReference().update(
                            "sent", true,
                            "sentAt", Timestamp.now()
                    );

                    if (owner != null) {
                        db.collection("users").document(owner).update(
                                "balance", com.google.firebase.firestore.FieldValue.increment(-price),
                                "dailySent", com.google.firebase.firestore.FieldValue.increment(1)
                        );
                    }
                    sentCount++;
                } else {
                    Log.w(TAG, "SMS failed to send to " + target);
                }
            }

            boolean shouldContinue = false;
            if (userId != null && !userId.isEmpty()) {
                DocumentSnapshot userSnap = Tasks.await(db.collection("users").document(userId).get());
                shouldContinue = userSnap.exists() && Boolean.TRUE.equals(userSnap.getBoolean("sendingRunning"));
            }

            if (shouldContinue) {
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