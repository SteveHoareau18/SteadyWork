package fr.steve.steadywork;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.Serializable;
import java.util.Optional;

import fr.steve.steadywork.service.SteadyService;

public class SteadyActivity extends Activity implements Serializable {

    public static final String PREFS_NAME = "SteadyWorkPrefs";
    public static Optional<View> layout;
    private transient Chronometer chronometer;
    private transient TextView inputHours, inputMinutes;
    private transient Button btnStart;

    public static void updateChronometer(SharedPreferences prefs) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {

            long totalTimeSeconds = prefs.getLong(KEY.CHRONO_ELAPSED.toString(), 0);

            int hours = (int) (totalTimeSeconds / 3600);
            int minutes = (int) ((totalTimeSeconds % 3600) / 60);
            int seconds = (int) (totalTimeSeconds % 60);

            @SuppressLint("DefaultLocale") String timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            layout.ifPresent(l -> {
                LinearLayout linearLayout = (LinearLayout) l;
                __blockInput();
                Chronometer childChrono = (Chronometer) linearLayout.getChildAt(4);
                childChrono.setVisibility(View.VISIBLE);
                childChrono.setText(timeFormatted);
            });
        }
    }

    private static void __blockInput() {
        layout.ifPresent(l -> {
            LinearLayout linearLayout = (LinearLayout) l;
            TextView inputHours = (TextView) linearLayout.getChildAt(1);
            TextView inputMinutes = (TextView) linearLayout.getChildAt(2);
            Button btnStart = (Button) linearLayout.getChildAt(3);
            inputHours.setText("");
            inputHours.setInputType(InputType.TYPE_NULL);
            inputMinutes.setText("");
            inputMinutes.setInputType(InputType.TYPE_NULL);
            btnStart.setText("ANNULER");
        });

    }

    private static void unBlockInput() {
        layout.ifPresent(l -> {
            LinearLayout linearLayout = (LinearLayout) l;
            EditText inputHours = (EditText) linearLayout.getChildAt(1);
            EditText inputMinutes = (EditText) linearLayout.getChildAt(2);
            Button btnStart = (Button) linearLayout.getChildAt(3);
            inputHours.setInputType(InputType.TYPE_CLASS_NUMBER);
            inputMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
            btnStart.setText("COMMENCER");
        });
    }

    public static boolean stop(SharedPreferences prefs) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            unBlockInput();
            layout.ifPresent(l -> {
                LinearLayout linearLayout = (LinearLayout) l;
                Chronometer child = (Chronometer) linearLayout.getChildAt(4);
                child.setText("");
            });
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY.START.toString(), false);
            editor.putLong(KEY.CHRONO_START.toString(), 0);
            editor.putLong(KEY.CHRONO_ELAPSED.toString(), 0);
            editor.apply();
            return true;
        }
        return false;
    }

    public static boolean isStart(SharedPreferences prefs) {
        return prefs.getBoolean(KEY.START.toString(), false);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputHours = findViewById(R.id.inputHours);
        inputMinutes = findViewById(R.id.inputMinutes);
        btnStart = findViewById(R.id.btnStart);
        chronometer = findViewById(R.id.chronometer);
        layout = Optional.of(findViewById(R.id.myLayout));

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_MULTI_PROCESS);
        if (isStart(prefs)) {
            updateChronometer(prefs);
        } else {
            clean(prefs);
        }

        btnStart.setOnClickListener(v -> {
            if (isStart(prefs)) {
                //stop the process
                stop(prefs);
                Intent serviceIntent = new Intent(this, SteadyService.class);
                stopService(serviceIntent);
            } else {
                //start the process
                createChronometer(prefs);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void createChronometer(SharedPreferences prefs) {
        //CHECK PERMISSIONS
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 1234);
            return;
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
            Toast.makeText(this, "Vous devez accepter de recevoir des notifications pour continuer...", Toast.LENGTH_LONG).show();
            return;
        }

        int hours = parseIntOrZero(inputHours.getText().toString());
        int minutes = parseIntOrZero(inputMinutes.getText().toString());

        if (hours == 0 && minutes == 0) {
            showToast();
            return;
        }

        long totalTimeMillis = calculateTotalTimeMillis(hours, minutes);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY.START.toString(), true);
        editor.putLong(KEY.CHRONO_START.toString(), totalTimeMillis);
        editor.apply();

        Intent serviceIntent = new Intent(this, SteadyService.class);
        serviceIntent.putExtra("activity", this);

        startForegroundService(serviceIntent);

        int seconds = (int) (totalTimeMillis / 1000) % 60;

        @SuppressLint("DefaultLocale") String timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        chronometer.setVisibility(View.VISIBLE);
        chronometer.setText(timeFormatted);
        blockInput();
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

    private void blockInput() {
        inputHours.setText("");
        inputHours.setInputType(InputType.TYPE_NULL);
        inputMinutes.setText("");
        inputMinutes.setInputType(InputType.TYPE_NULL);
        btnStart.setText("ANNULER");
    }

    private void clean(SharedPreferences prefs) {
        if (prefs.getLong(KEY.CHRONO_ELAPSED.toString(), 0) <= 0) {
            inputHours.setInputType(InputType.TYPE_CLASS_NUMBER);
            inputMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
            btnStart.setText("COMMENCER");
            chronometer.setText("");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY.START.toString(), false);
            editor.putLong(KEY.CHRONO_START.toString(), 0);
            editor.putLong(KEY.CHRONO_ELAPSED.toString(), 0);
            editor.apply();
        } else {
            blockInput();
        }
    }

    public enum LOG_TAG {
        DEBUG("debug");

        private final String tag;

        LOG_TAG(String _tag) {
            this.tag = "sw_" + _tag;
        }

        public String getTag() {
            return this.tag;
        }
    }

    public enum KEY {
        START,
        CHRONO_START,
        CHRONO_ELAPSED;
    }
}
