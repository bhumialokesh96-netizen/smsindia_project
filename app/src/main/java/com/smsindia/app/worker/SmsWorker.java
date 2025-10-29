package com.smsindia.app.worker;

import android.content.Context;
import android.telephony.SmsManager;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.ForegroundInfo;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.smsindia.app.R;

import java.util.HashMap;
import java.util.Map;

public class SmsWorker extends Worker {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public SmsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            setForegroundAsync(createForegroundInfo("SMSIndia", "Sending messages..."));

            db.collection("smsQueue")
                    .whereEqualTo("status", "pending")
                    .get()
                    .addOnSuccessListener(query -> {
                        SmsManager smsManager = SmsManager.getDefault();
                        for (QueryDocumentSnapshot doc : query) {
                            String phone = doc.getString("phone");
                            String msg = doc.getString("message");

                            try {
                                smsManager.sendTextMessage(phone, null, msg, null, null);

                                // update status
                                Map<String, Object> update = new HashMap<>();
                                update.put("status", "sent");
                                db.collection("smsQueue").document(doc.getId()).update(update);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .addOnFailureListener(Throwable::printStackTrace);

            return Result.success();

        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }
    }

    private ForegroundInfo createForegroundInfo(String title, String message) {
        String channelId = "sms_worker_channel";

        NotificationCompat.Builder notification =
                new NotificationCompat.Builder(getApplicationContext(), channelId)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setSmallIcon(R.drawable.ic_send)
                        .setPriority(NotificationCompat.PRIORITY_LOW);

        return new ForegroundInfo(1, notification.build());
    }
}