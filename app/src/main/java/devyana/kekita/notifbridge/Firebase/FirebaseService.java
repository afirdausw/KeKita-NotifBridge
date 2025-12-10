package devyana.kekita.notifbridge.Firebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import devyana.kekita.notifbridge.Activity.MainActivity;
import devyana.kekita.notifbridge.R;

public class FirebaseService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "notif_bridge_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Cek apakah pesan mengandung payload notifikasi
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Payload: " + remoteMessage.getData());

            String title    = remoteMessage.getData().get("title");
            String body     = remoteMessage.getData().get("body");

            if (title == null) {
                title = "Pesan Baru";
            }
            if (body == null) {
                body = "Anda menerima pesan baru.";
            }

            sendNotification(title, body);
        }
        else if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Notification Message Body: " + remoteMessage.getNotification().getBody());

            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            sendNotification(title, body);
        }
    }

    /**
     * Dipanggil saat token FCM baru dibuat.
     * Token ini adalah ID unik perangkat yang digunakan untuk mengirim notifikasi.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.e("FIREBASE", "Refreshed token: " + token);

        // Di sini Anda bisa mengirim token ini ke server Anda jika diperlukan,
        // agar server tahu harus mengirim notifikasi ke perangkat mana.
        // sendRegistrationToServer(token);
    }

    /**
     * Membuat dan menampilkan notifikasi sederhana.
     * @param title Judul notifikasi.
     * @param messageBody Isi pesan notifikasi.
     */
    private void sendNotification(String title, String messageBody) {
        // Buat channel notifikasi (wajib untuk Android 8.0 Oreo ke atas)
        createNotificationChannel();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Buat PendingIntent yang akan dieksekusi saat notifikasi diklik
        // Gunakan FLAG_IMMUTABLE untuk keamanan
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // ID notifikasi harus unik jika Anda ingin menampilkan beberapa notifikasi sekaligus
        // Jika ID sama, notifikasi yang ada akan diperbarui
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }

    /**
     * Membuat Notification Channel. Wajib untuk API 26+ (Android 8.0).
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "NotifBridge Channel";
            String description = "Channel untuk notifikasi dari NotifBridge";

            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
