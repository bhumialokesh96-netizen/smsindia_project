package com.smsindia.app.worker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.telephony.SmsManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.smsindia.app.R;

import java.util.concurrent.TimeUnit;

public class SmsWorker extends Worker {
    private static final String CHANNEL_ID = "smsindia_channel";
    private FirebaseFirestore db;

    public SmsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setForegroundAsync(createForegroundInfo("SMS sending active..."));

            db.collection("sms_inventory")
                    .whereEqualTo("sent", false)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String number = doc.getString("phone");
                            String message = doc.getString("message");

                            try {
                                SmsManager smsManager = SmsManager.getDefault();
                                smsManager.sendTextMessage(number, null, message, null, null);
                                db.collection("sms_inventory")
                                        .document(doc.getId())
                                        .update("sent", true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

            // Wait 1 second and requeue itself
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            return Result.retry(); // makes it loop every second

        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }

    private ForegroundInfo createForegroundInfo(String message) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("SMSIndia")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_sms)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();

        return new ForegroundInfo(1, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "SMS Sending",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}