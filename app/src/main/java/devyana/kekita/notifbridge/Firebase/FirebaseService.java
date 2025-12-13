package devyana.kekita.notifbridge.Firebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import devyana.kekita.notifbridge.Helper.DatabaseHelper;
import devyana.kekita.notifbridge.R;

public class FirebaseService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "notif_bridge_channel";
    private DatabaseHelper dbHelper;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        dbHelper = new DatabaseHelper(this);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Cek apakah pesan mengandung payload notifikasi
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Payload: " + remoteMessage.getData());

            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            String role = remoteMessage.getData().get("role");

            if (title == null) {
                title = "Pesan Baru";
            }
            if (body == null) {
                body = "Anda menerima pesan baru.";
            }

            sendNotification(title, body, role);
        } else if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Notification Message Body: " + remoteMessage.getNotification().getBody());

            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            sendNotification(title, body, "");
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
     *
     * @param title       Judul notifikasi.
     * @param messageBody Isi pesan notifikasi.
     */
    private void sendNotification(String title, String messageBody, String role) {
        String channelId = getChannelId(role);
        createNotificationChannel(role);
//        createNotificationChannelDefault();

        // URL PWA tujuan
        String targetLink = dbHelper.getSetting("url").replace("api/", "pesanan/");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(targetLink));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Buat PendingIntent yang akan dieksekusi saat notifikasi diklik
        // Gunakan FLAG_IMMUTABLE untuk keamanan
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.twotone_circle_notifications)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS | NotificationCompat.DEFAULT_VIBRATE)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // ID notifikasi harus unik jika Anda ingin menampilkan beberapa notifikasi sekaligus
        // Jika ID sama, notifikasi yang ada akan diperbarui
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }

    private void createNotificationChannelDefault() {
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

    // NOTIFIKASI BERDASARKAN ROLE
    private String getChannelId(String role) {
        if (role.equals("kitchen")) return "channel_kitchen_v1";
        if (role.equals("bar")) return "channel_bar_v1";
        return "channel_default_v1";
    }

    /**
     * Membuat Notification Channel. Wajib untuk API 26+ (Android 8.0).
     */
    private void createNotificationChannel(String role) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            String channelId = getChannelId(role);
            NotificationManager nm = getSystemService(NotificationManager.class);

            if (nm.getNotificationChannel(channelId) != null) {
                return;
            }

            Uri soundUri;
            if (role.equals("kitchen")) {
                soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + R.raw.new_order_kitchen);
            } else if (role.equals("bar")) {
                soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + R.raw.new_order_bar);
            } else {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Notif " + role,
                    NotificationManager.IMPORTANCE_HIGH
            );

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            channel.setDescription("Notifikasi untuk " + role);
            channel.setSound(soundUri, audioAttributes);
            channel.enableVibration(true);

            nm.createNotificationChannel(channel);

            NotificationChannel ch = nm.getNotificationChannel(channelId);
            Log.d("CHAN_DEBUG", "sound=" + (ch != null ? ch.getSound() : "null"));
        }
    }

}
