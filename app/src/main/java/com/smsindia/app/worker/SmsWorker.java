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
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.smsindia.app.R;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Background worker that fetches unsent SMS tasks from Firestore
 * and sends them securely. Runs as a foreground task for Android O+.
 */
public class SmsWorker extends Worker {

    private static final String TAG = "SmsWorker";
    private static final String CHANNEL_ID = "smsindia_channel";
    private final FirebaseFirestore db;

    public SmsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Ensure Foreground service notification (required on Android O+)
            setForegroundAsync(createForegroundInfo());

            Log.d(TAG, "🚀 SmsWorker started");

            CountDownLatch latch = new CountDownLatch(1);

            db.collection("sms_inventory")
                    .whereEqualTo("sent", false)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots.isEmpty()) {
                            Log.d(TAG, "No unsent SMS found");
                        }

                        for (QueryDocumentSnapshot doc : snapshots) {
                            String number = doc.getString("phone");
                            String message = doc.getString("message");

                            if (number == null || message == null || number.trim().isEmpty()) {
                                Log.w(TAG, "Invalid SMS entry skipped");
                                continue;
                            }

                            try {
                                SmsManager sms = SmsManager.getDefault();
                                sms.sendTextMessage(number, null, message, null, null);

                                db.collection("sms_inventory")
                                        .document(doc.getId())
                                        .update("sent", true);

                                Log.i(TAG, "✅ Sent to " + number);
                            } catch (Exception e) {
                                Log.e(TAG, "❌ Send failed: " + e.getMessage());
                            }
                        }

                        latch.countDown();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firestore error: " + e.getMessage());
                        latch.countDown();
                    });

            // Wait a few seconds for Firebase callback
            latch.await(4, TimeUnit.SECONDS);

            // Add short delay to prevent hammering
            Thread.sleep(2000);

            // Retry periodically for new tasks
            return Result.retry();

        } catch (Exception e) {
            Log.e(TAG, "Worker crashed: " + e.getMessage(), e);
            return Result.failure();
        }
    }

    /** Creates persistent foreground notification for the worker */
    private ForegroundInfo createForegroundInfo() {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("SMS India Service")
                .setContentText("📡 Sending SMS in background")
                .setSmallIcon(R.drawable.ic_message)
                .setColor(0xFFFF9800)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        return new ForegroundInfo(1, notification);
    }

    /** Builds notification channel on O+ */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS India Background",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = getApplicationContext().getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }
}