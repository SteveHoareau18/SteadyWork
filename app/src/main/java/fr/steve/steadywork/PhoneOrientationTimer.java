package fr.steve.steadywork;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class PhoneOrientationTimer {

    private final Context context;
    private Handler handler;
    private int secondsAll;
    private boolean isFlat;
    private Runnable notFlatTimer;

    public PhoneOrientationTimer(Context context) {
        this.context = context;
        this.secondsAll = 0;
        this.handler = new Handler(Looper.getMainLooper());
        this.isFlat = true;
    }

    public void onSensorChanged(float z) {
        if (Math.abs(z) < 8 || Math.abs(z) > 10) {
            handleNotFlatOrientation();
        } else {
            isFlat = true;
        }
    }

    private void handleNotFlatOrientation() {
        if (!isFlat) {
            return; // Already handling not flat orientation
        }

        isFlat = false;
        startNotFlatTimer();
        Toast.makeText(context, "Le téléphone n'est pas à plat", Toast.LENGTH_SHORT).show();
    }

    private void startNotFlatTimer() {
        if (handler == null || secondsAll > 0) {
            return;
        }

        notFlatTimer = new Runnable() {
            @Override
            public void run() {
                if (!isFlat) {
                    secondsAll++;
                    showAlertAndPlaySoundIfNeeded();
                }

                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(notFlatTimer, 1000);
    }

    private void showAlertAndPlaySoundIfNeeded() {
        Log.d("sw_debug", secondsAll+"s");
        if (secondsAll > 0 && secondsAll % 10 == 0) {
            playNotificationSound();
            Toast.makeText(context, "Le téléphone n'est pas à plat depuis plus d'une minute.", Toast.LENGTH_LONG).show();
        }
    }

    private void playNotificationSound() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone ringtone = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
        ringtone.play();
    }

    private void cancelNotFlatTimer() {
        if (handler != null) {
            handler.removeCallbacks(notFlatTimer);
            handler = null;
        }
        if(notFlatTimer != null){
            notFlatTimer = null;
        }
    }

    public void cancel() {
        cancelNotFlatTimer();
    }

    public int getSecondsAll() {
        return secondsAll;
    }
}
