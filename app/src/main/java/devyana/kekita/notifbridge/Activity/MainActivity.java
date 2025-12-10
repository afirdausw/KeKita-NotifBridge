package devyana.kekita.notifbridge.Activity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import devyana.kekita.notifbridge.Helper.DatabaseHelper;
import devyana.kekita.notifbridge.R;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "notifbridge_prefs";
    private static final String KEY_SELECTED_OPTION = "selected_option";
    private static final String KEY_SELECTED_TOPIC = "selected_topic";
    private static final String KEY_SERVICE_RUNNING = "service_running";

    private ImageView ivClientLogo;
    private TextView tvClientName, tvNotification;
    private MaterialButton btnPicker, btnStart, btnStop;

    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        if (dbHelper.getSetting("client") == null) {
            Intent wizardIntent = new Intent(MainActivity.this, WizardActivity.class);
            startActivity(wizardIntent);
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.setStatusBarColor(Color.WHITE);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        ivClientLogo = findViewById(R.id.iv_client_logo);
        tvClientName = findViewById(R.id.tv_client_name);
        tvNotification = findViewById(R.id.tv_notification);
        btnPicker = findViewById(R.id.btn_picker);
        btnStart = findViewById(R.id.btn_start_service);
        btnStop = findViewById(R.id.btn_stop_service);

        btnPicker.setOnClickListener(v -> {
            showNotificationDialog();
        });
        btnStart.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            String option = prefs.getString(KEY_SELECTED_OPTION, null);
            String topic = prefs.getString(KEY_SELECTED_TOPIC, null);

            if (topic == null) {
                Toast.makeText(this, "Pilih notifikasi dulu!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            prefs.edit().putBoolean(KEY_SERVICE_RUNNING, true).apply();
                            tvNotification.setText("Notif: " + option);
                        } else {
                            Toast.makeText(this, "Gagal START service!", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        btnStop.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            String topic = prefs.getString(KEY_SELECTED_TOPIC, null);

            if (topic == null) {
                Toast.makeText(this, "Tidak ada topic untuk dihentikan!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            prefs.edit().putBoolean(KEY_SERVICE_RUNNING, false).apply();
                            tvNotification.setText("Service Stopped");
                        } else {
                            Toast.makeText(this, "Gagal STOP service!", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2002);
            }
        }

        loadAndDisplaySettings();
        initFCM();
        loadSelectedNotif();
    }

    private void loadAndDisplaySettings() {
        Map<String, String> settings = dbHelper.getAllSettings();

        String clientName = settings.get("client");
        if (clientName != null) {
            tvClientName.setText(clientName);
        } else {
            tvClientName.setText("Nama Klien Tidak Ditemukan");
        }

        String logoPath = settings.get("logo");
        if (logoPath != null && !logoPath.isEmpty()) {
            File logoFile = new File(logoPath);
            if (logoFile.exists()) {
                Bitmap logoBitmap = BitmapFactory.decodeFile(logoFile.getAbsolutePath());
                ivClientLogo.setImageBitmap(logoBitmap);
            } else {
                ivClientLogo.setImageResource(R.mipmap.ic_launcher);
            }
        } else {
            ivClientLogo.setImageResource(R.mipmap.ic_launcher);
        }
    }

    private void initFCM() {
        // get token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("Main TAG", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.e("Main FIREBASE Token", token);

                    // sendTokenToServer(token);
                });
    }

    private void showNotificationDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        String clientName = dbHelper.getSetting("client");
        String[] options = clientOptions.get(clientName);

        if (options == null) {
            Toast.makeText(this, "Client tidak dikenal!", Toast.LENGTH_SHORT).show();
            return;
        }

        // cari posisi yang sebelumnya dipilih
        String lastSelected = prefs.getString(KEY_SELECTED_OPTION, "");
        int checkedItem = -1;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(lastSelected)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Pilih Notifikasi untuk " + clientName)
                .setSingleChoiceItems(options, checkedItem, null)
                .setPositiveButton("Simpan", (dialog, whichButton) -> {

                    int selectedIndex = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selectedIndex < 0) return;

                    String selectedOption = options[selectedIndex];
                    String topicName = makeTopicName(clientName, selectedOption);

                    // unsubscribe topic lama
                    String oldTopic = prefs.getString(KEY_SELECTED_TOPIC, null);
                    if (oldTopic != null && !oldTopic.equals(topicName)) {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(oldTopic);
                    }

                    // subscribe topic baru
                    FirebaseMessaging.getInstance().subscribeToTopic(topicName)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    tvNotification.setText("Notif: " + selectedOption);

                                    prefs.edit()
                                            .putString(KEY_SELECTED_OPTION, selectedOption)
                                            .putString(KEY_SELECTED_TOPIC, topicName)
                                            .apply();

                                    Log.e("Main FIREBASE Topic", topicName);
                                } else {
                                    Toast.makeText(this, "Gagal subscribe ke topic", Toast.LENGTH_SHORT).show();
                                }
                            });

                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void loadSelectedNotif() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String selected = prefs.getString(KEY_SELECTED_OPTION, "Belum diatur");
        tvNotification.setText("Notif: " + selected);
    }

    private Map<String, String[]> clientOptions = new HashMap<String, String[]>() {{
        put("Kusuma Kitchen", new String[]{"Kasir", "Gudang", "Central Bar", "Central Kitchen", "Kitchen Operation"});
        put("Biji Kopi", new String[]{"Kasir", "Bar", "Kitchen"});
    }};

    private String makeTopicName(String client, String option) {
        String c = client.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String o = option.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        return c + "_" + o;
    }

    public void onSettings(View view) {
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
    }

}