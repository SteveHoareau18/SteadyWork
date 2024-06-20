package fr.steve.steadywork;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class HelloService extends Service {

    private static final String TAG = "HelloService";
    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        // Si votre service doit être redémarré par le système, retournez START_STICKY
        // Sinon, retournez START_NOT_STICKY
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service onBind");
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
    }

    public class LocalBinder extends Binder {
        public HelloService getService() {
            return HelloService.this;
        }
    }

    // Méthodes publiques accessibles depuis l'extérieur pour le traitement de données, etc.
    public void performTask() {
        Log.d(TAG, "Performing task...");
    }
}
