package fr.steve.steadywork.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import fr.steve.steadywork.R;
import fr.steve.steadywork.SteadyActivity;
import fr.steve.steadywork.SteadyRunnable;
import fr.steve.steadywork.listener.SteadyServiceReceiver;

public class SteadyService extends Service implements SensorEventListener {

    private static final String CHANNEL_ID = "SteadyChannel";
    private static final int NOTIFICATION_ID = 1;
    private Handler handler;
    private SteadyRunnable runnable;
    private boolean isFlat;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());
        Log.d(SteadyActivity.LOG_TAG.DEBUG.getTag(), "Service created");
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = buildForegroundNotification();
        startForeground(NOTIFICATION_ID, notification);
        long startMillis = getSharedPreferences(SteadyActivity.PREFS_NAME, MODE_MULTI_PROCESS).getLong(SteadyActivity.KEY.CHRONO_START.toString(), 0);
        runnable = new SteadyRunnable() {
            private long count;
            private long usedInSecondsCount;

            @Override
            public void run() {
                if (!isBegin) {
                    isBegin = true;
                    isFlat = true;
                    count = startMillis / 1000;
                    usedInSecondsCount = 0;
                }
                if (count == 0) {
                    SharedPreferences prefs = getSharedPreferences(SteadyActivity.PREFS_NAME, MODE_MULTI_PROCESS);

                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction("TIME_ELAPSED");
                    broadcastIntent.setPackage(getPackageName());
                    broadcastIntent.putExtra("usedSeconds", usedInSecondsCount);
                    sendBroadcast(broadcastIntent);
                    Log.d(SteadyActivity.LOG_TAG.DEBUG.getTag(), "usedSeconds: " + usedInSecondsCount);
                    Notification buildFinishNotification = buildFinishNotification(usedInSecondsCount);
                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(NOTIFICATION_ID, buildFinishNotification);

                    if (!SteadyActivity.stop(prefs)) {
                        onDestroy();
                    }
                    return;
                }
                continueRunnable();
                if (!isFlat) {
                    Log.d(SteadyActivity.LOG_TAG.DEBUG.getTag(), "notFlat");
                    if (usedInSecondsCount % 20 == 0) {
                        Notification buildOnUseCellphoneNotification = buildOnUseCellphoneNotification();
                        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        notificationManager.notify(NOTIFICATION_ID, buildOnUseCellphoneNotification);
                    }
                    usedInSecondsCount += 1;
                }
            }

            private void continueRunnable() {
                Log.d(SteadyActivity.LOG_TAG.DEBUG.getTag(), count + "s");
                SharedPreferences.Editor editor = getSharedPreferences(SteadyActivity.PREFS_NAME, MODE_MULTI_PROCESS).edit();
                editor.putLong(SteadyActivity.KEY.CHRONO_ELAPSED.toString(), count);
                editor.apply();
                SharedPreferences prefs = getSharedPreferences(SteadyActivity.PREFS_NAME, MODE_MULTI_PROCESS);
                SteadyActivity.updateChronometer(prefs);
                count -= 1;
                handler.postDelayed(this, 1000);
            }
        };

        handler.post(runnable);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float z = Math.abs(event.values[2]);
        isFlat = z > 8 && z < 10;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        stopSelf();
        Log.d(SteadyActivity.LOG_TAG.DEBUG.getTag(), "Service destroyed");
    }

    private void createNotificationChannel() {
        CharSequence name = "My Channel";
        String description = "Channel for MyService notifications";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 500, 100, 500}); // Same pattern as above

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        Log.d(SteadyActivity.LOG_TAG.DEBUG.getTag(), "Notification channel created");
    }

    private Notification buildForegroundNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SteadyWork")
                .setContentText("Vous avez un chronomètre pour travailler serieusement en cours d'execution !")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        return builder.build();
    }

    private Notification buildOnUseCellphoneNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SteadyWork")
                .setContentText("Vous ne devez pas utiliser le téléphone pendant votre temps de travail !")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        return builder.build();
    }

    private Notification buildFinishNotification(long seconds) {
        String[] titleMessage = SteadyServiceReceiver.getTitleMessage(seconds);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SteadyWork | " + titleMessage[0])
                .setContentText(titleMessage[1])
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        return builder.build();
    }
}
