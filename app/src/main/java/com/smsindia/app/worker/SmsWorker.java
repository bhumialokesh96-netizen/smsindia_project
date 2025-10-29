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

public class SmsWorker extends Worker {

    private static final String TAG = "SmsWorker";
    private static final String CHANNEL_ID = "smsindia_channel";
    private final FirebaseFirestore db;

    public SmsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setForegroundAsync(createForegroundInfo());

            CountDownLatch latch = new CountDownLatch(1);

            db.collection("sms_inventory")
                    .whereEqualTo("sent", false)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots.isEmpty()) {
                            Log.d(TAG, "No unsent SMS available.");
                        }
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String number = doc.getString("phone");
                            String message = doc.getString("message");
                            if (number == null || message == null) continue;

                            try {
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(number, null, message, null, null);

                                db.collection("sms_inventory")
                                        .document(doc.getId())
                                        .update("sent", true);

                                Log.d(TAG, "✅ SMS sent to " + number);
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

            latch.await(3, TimeUnit.SECONDS);

            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
            return Result.retry();

        } catch (Exception e) {
            Log.e(TAG, "Worker crashed: " + e.getMessage());
            return Result.failure();
        }
    }

    private ForegroundInfo createForegroundInfo() {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("SMSIndia")
                .setContentText("🟠 SMS sending service active")
                .setSmallIcon(R.drawable.ic_message) // use ic_message instead of ic_sms
                .setColor(0xFFFF9800)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        return new ForegroundInfo(1, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMSIndia Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager =
                    getApplicationContext().getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}