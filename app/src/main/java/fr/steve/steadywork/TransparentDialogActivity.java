package fr.steve.steadywork;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import fr.steve.steadywork.listener.SteadyServiceReceiver;

public class TransparentDialogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transparent_dialog);

        // Récupérer les vues
        TextView titleTextView = findViewById(R.id.dialog_title);
        TextView messageTextView = findViewById(R.id.dialog_message);
        Button button = findViewById(R.id.dialog_button);

        // Récupérer les données passées par l'intent (si nécessaire)
        String title = getIntent().getStringExtra("title");
        String message = getIntent().getStringExtra("message");

        // Mettre à jour les vues avec les données
        titleTextView.setText(title);
        messageTextView.setText(message);

        button.setOnClickListener(this::onDialogButtonClick);
    }

    public void onDialogButtonClick(View view) {
        finish();
        SteadyServiceReceiver.stopRingtone();
    }
}
