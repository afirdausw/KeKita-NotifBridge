package devyana.kekita.notifbridge.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import devyana.kekita.notifbridge.Helper.DatabaseHelper;
import devyana.kekita.notifbridge.R;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS = "notifbridge_prefs";
    private static final String KEY_SELECTED_OPTION = "selected_option";
    private static final String KEY_SELECTED_TOPIC = "selected_topic";
    private static final String KEY_SERVICE_RUNNING = "service_running";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_settings);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.setStatusBarColor(Color.parseColor("#F4F4F4"));
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    public void onBack(View view) {
        finish();
    }

    public void onLogout(View view) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_SELECTED_OPTION)
                .remove(KEY_SELECTED_TOPIC)
                .remove(KEY_SERVICE_RUNNING)
                .apply();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.clearAllSettings();

        finish();

        Intent intent = new Intent(this, WizardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
