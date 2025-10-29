package com.smsindia.app.worker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SmsStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int result = getResultCode();
        if (result == android.app.Activity.RESULT_OK) {
            Log.i("SmsStatusReceiver", "SMS sent ok");
        } else {
            Log.w("SmsStatusReceiver", "SMS not sent: " + result);
        }
    }
}
