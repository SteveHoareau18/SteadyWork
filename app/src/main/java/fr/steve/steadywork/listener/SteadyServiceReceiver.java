package fr.steve.steadywork.listener;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import fr.steve.steadywork.SteadyActivity;
import fr.steve.steadywork.TransparentDialogActivity;

//when service is finish
public class SteadyServiceReceiver extends BroadcastReceiver {

    private static Ringtone ringtone;
    private long usedSeconds;
    private Handler handler;

    public static void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
            ringtone = null;
        }
    }

    @SuppressLint("DefaultLocale")
    public static String[] getTitleMessage(long seconds) {
        String title, message;
        if (seconds < 20) {
            message = "Bien joué, vous n'avez pas passé de temps sur votre téléphone !";
            title = "Super !";
        } else {
            String unit;
            title = "Attention !";
            if (seconds < 60) {
                unit = "seconde";
            } else if (seconds < 3600) {
                unit = "minute";
                seconds /= 60; // Convertit en minutes
            } else {
                unit = "heure";
                seconds /= 3600; // Convertit en heures
            }
            message = String.format("Vous avez passé %d %s%s sur votre téléphone au lieu de travailler !", seconds, unit, (seconds > 1 ? "s" : ""));
        }
        return new String[]{title, message};
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("TIME_ELAPSED".equals(intent.getAction())) {
            this.usedSeconds = intent.getLongExtra("usedSeconds", -1);
            showAlert(context);
        }
    }

    @SuppressLint("DefaultLocale")
    private void showAlert(Context context) {
        Intent intentTransparent = new Intent(context, TransparentDialogActivity.class);
        intentTransparent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String[] titleMessage = getTitleMessage(this.usedSeconds);
        intentTransparent.putExtra("title", titleMessage[0]);
        intentTransparent.putExtra("message", titleMessage[1]);
        context.startActivity(intentTransparent);

        playRingtone(context);
    }

    private void playRingtone(Context context) {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        ringtone = RingtoneManager.getRingtone(context, notification);
        ringtone.play();
        handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            private long count;
            private boolean isBegin = false;

            @Override
            public void run() {
                if (!isBegin) {
                    isBegin = true;
                    count = 0;
                }
                if (count == 7) {
                    stopRingtone();
                    handler.removeCallbacks(this);
                } else {
                    handler.postDelayed(this, 1000);
                    Log.d(SteadyActivity.LOG_TAG.DEBUG.getTag(), count + "s");
                    count += 1;
                }
            }
        };
        handler.post(runnable);
    }
}
