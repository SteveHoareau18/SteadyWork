package fr.steve.steadywork;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {

    private EditText inputHours, inputMinutes;
    private Chronometer chronometer;
    private CountDownTimer countDownTimer;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private PhoneOrientationTimer phoneOrientationTimer;
    private Ringtone ringtone;
    private Button btnStart;

    private boolean isRunning = false;
    private Intent intent;
    private HelloService helloService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputHours = findViewById(R.id.inputHours);
        inputMinutes = findViewById(R.id.inputMinutes);
        btnStart = findViewById(R.id.btnStart);
        chronometer = findViewById(R.id.chronometer);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        phoneOrientationTimer = new PhoneOrientationTimer(this);

        btnStart.setOnClickListener(v -> {
            if (isRunning) {
                manualStop();
            } else {
                startChronometer();
            }
        });
        intent = new Intent(this, HelloService.class);
        if(!isBound) {
            startService(intent);
        }else{
            if (helloService != null) {
                Log.d("MainActivity", "Received message from service: Hello");
            }
        }
        updateUIState();
    }

    private void startChronometer() {
        int hours = parseIntOrZero(inputHours.getText().toString());
        int minutes = parseIntOrZero(inputMinutes.getText().toString());

        if (hours == 0 && minutes == 0) {
            showToast();
            manualStop();
            return;
        }

        long totalTimeMillis = calculateTotalTimeMillis(hours, minutes);

        chronometer.setBase(SystemClock.elapsedRealtime() + totalTimeMillis);
        inputHours.setText("");
        inputHours.setInputType(InputType.TYPE_NULL);
        inputMinutes.setText("");
        inputMinutes.setInputType(InputType.TYPE_NULL);
        chronometer.start();

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        cancelExistingTimer();

        countDownTimer = createCountDownTimer(totalTimeMillis);
        countDownTimer.start();
        phoneOrientationTimer = new PhoneOrientationTimer(this);

        isRunning = true;
        updateUIState();
    }

    private int parseIntOrZero(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long calculateTotalTimeMillis(int hours, int minutes) {
        return (hours * 3600L + minutes * 60L) * 1000;
    }

    private void showToast() {
        Toast.makeText(this, "Il faut rentrer au moins 1 valeur", Toast.LENGTH_LONG).show();
    }

    private CountDownTimer createCountDownTimer(long totalTimeMillis) {
        return new CountDownTimer(totalTimeMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Optional: Add tick logic if needed
            }

            @Override
            public void onFinish() {
                showAlert();
                manualStop();
            }
        };
    }

    private void showAlert() {
        int secondsAll = phoneOrientationTimer.getSecondsAll();
        String secondsOrMinutes = (secondsAll / 60 >= 1) ?
                (secondsAll / 60) + " minute(s)" :
                secondsAll + " seconde(s)";

        new AlertDialog.Builder(this)
                .setTitle("Attention")
                .setMessage("Le temps est écoulé ! Vous avez passé au moins " + secondsOrMinutes + " sur votre téléphone au lieu de travailler !")
                .setPositiveButton("OK", (dialog, which) -> {
                    manualStop();
                    stopRingtone();
                })
                .show();

        playRingtone();
    }

    private void playRingtone() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        ringtone = RingtoneManager.getRingtone(this.getApplicationContext(), notification);
        ringtone.play();
    }

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void updateUIState() {
        btnStart.setText(isRunning ? "ARRÊTER" : "COMMENCER");
    }

    private void manualStop() {
        chronometer.stop();
        chronometer.setText("");
        sensorManager.unregisterListener(this);
        inputHours.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);

        cancelExistingTimer();

        isRunning = false;
        updateUIState();
    }

    private void cancelExistingTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if(phoneOrientationTimer != null){
            phoneOrientationTimer.cancel();
            phoneOrientationTimer = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float z = event.values[2];
        phoneOrientationTimer.onSensorChanged(z);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Optional: Handle accuracy changes if needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
    }

    private boolean isBound;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            HelloService.LocalBinder binder = (HelloService.LocalBinder) service;
            helloService = binder.getService();
            isBound = true;
            Log.d("sw_debug", "Service connected");

            Log.d("sw_debug", "Received message from service: Hello");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            Log.d("sw_debug", "Service disconnected");
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Lier l'activité au service si nécessaire
        if (!isBound) {
            Intent serviceIntent = new Intent(this, HelloService.class);
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Délier l'activité du service si elle était liée
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
