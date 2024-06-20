package fr.steve.steadywork;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class PhoneOrientationService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Handler handler;
    private boolean isFlat;
    private int secondsAll;
    private Runnable notFlatTimer;
    private Ringtone ringtone;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        isFlat = true;
        secondsAll = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        cancelNotFlatTimer();
        Log.d("sw_debug","onDestroyService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float z = event.values[2];
        if (Math.abs(z) < 8 || Math.abs(z) > 10) {
            handleNotFlatOrientation();
        } else {
            handleFlatOrientation();
        }
    }

    private void handleNotFlatOrientation() {
        if (isFlat) {
            isFlat = false;
            showToast("Le téléphone n'est pas à plat");
            startNotFlatTimer();
        }
    }

    private void handleFlatOrientation() {
        if (!isFlat) {
            isFlat = true;
            cancelNotFlatTimer();
        }
    }

    private void startNotFlatTimer() {
        if (notFlatTimer == null) {
            notFlatTimer = new Runnable() {
                @Override
                public void run() {
                    if (!isFlat) {
                        secondsAll++;
                        showAlertAndPlaySoundIfNeeded();
                        handler.postDelayed(this, 1000);
                    }
                }
            };
            handler.postDelayed(notFlatTimer, 1000);
        }
    }

    private void showAlertAndPlaySoundIfNeeded() {
        Log.d("sw_debug", secondsAll + "s");
        if (secondsAll > 0 && secondsAll % 60 == 0) {
            playNotificationSound();
            showToast("Le téléphone n'est pas à plat depuis plus d'une minute.");
        }
    }

    private void playNotificationSound() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        ringtone.play();
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }

    private void cancelNotFlatTimer() {
        if (notFlatTimer != null) {
            handler.removeCallbacks(notFlatTimer);
            notFlatTimer = null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
