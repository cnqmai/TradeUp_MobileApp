package com.example.tradeup.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.tradeup.R; // Đảm bảo bạn có R.drawable.ic_notification nếu sử dụng
import com.example.tradeup.activity.MainActivity; // Thay thế bằng Activity chính của bạn
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.auth.FirebaseAuth; // Cần để lấy ID người dùng
import com.google.firebase.database.DatabaseReference; // Cần để tương tác với Realtime Database
import com.google.firebase.database.FirebaseDatabase; // Cần để tương tác với Realtime Database
import com.google.firebase.database.ServerValue; // Dùng để có timestamp từ server

import java.text.SimpleDateFormat; // Cần nếu bạn vẫn muốn lưu timestamp dạng String
import java.util.Date; // Cần nếu bạn vẫn muốn lưu timestamp dạng String
import java.util.HashMap;
import java.util.Locale; // Cần nếu bạn vẫn muốn lưu timestamp dạng String
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    // Constants cho Notification Channel (bắt buộc từ Android 8.0 Oreo)
    private static final String CHANNEL_ID = "tradeup_default_channel"; // ID duy nhất cho kênh thông báo
    private static final String CHANNEL_NAME = "TradeUp Notifications"; // Tên hiển thị cho người dùng
    private static final String CHANNEL_DESCRIPTION = "General notifications from TradeUp app"; // Mô tả kênh

    /**
     * Được gọi khi nhận được tin nhắn FCM.
     * @param remoteMessage Đối tượng RemoteMessage chứa tin nhắn FCM.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Kiểm tra xem tin nhắn có chứa payload dữ liệu hay không.
        // Hầu hết các thông báo bạn gửi từ server nên có data payload
        // để bạn có thể xử lý logic và điều hướng
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            // Xử lý dữ liệu và tạo thông báo hệ thống
            handleDataMessage(remoteMessage.getData());
        }

        // Kiểm tra xem tin nhắn có chứa payload thông báo hay không (notification payload)
        // Thông báo từ Firebase Console thường có cả notification và data payload.
        // Nếu chỉ có notification payload, bạn vẫn có thể hiển thị thông báo cơ bản.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            // Nếu notification payload có, bạn có thể sử dụng nó cho tiêu đề/nội dung
            // và sử dụng data payload để điều hướng
            sendNotification(
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    remoteMessage.getData() // Truyền data payload để xử lý click thông báo
            );
        }
    }

    /**
     * Được gọi khi một token mới được tạo hoặc một token hiện có được cập nhật.
     * @param token Token mới.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Gửi token này đến máy chủ của bạn hoặc lưu vào Firebase Realtime Database/Firestore
        // để bạn có thể gửi thông báo đến thiết bị cụ thể này sau.
        sendRegistrationToServer(token);
    }

    /**
     * Gửi token đăng ký lên máy chủ của bạn.
     * Trong trường hợp này, chúng ta sẽ lưu nó vào Firebase Realtime Database
     * dưới profile của người dùng hiện tại.
     */
    private void sendRegistrationToServer(String token) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            userRef.child("fcmToken").setValue(token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token saved to DB for user: " + userId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM Token to DB: " + e.getMessage()));
        } else {
            Log.w(TAG, "No user logged in, cannot save FCM token.");
            // Trường hợp này có thể xảy ra nếu token được tạo trước khi người dùng đăng nhập.
            // Bạn có thể cân nhắc lưu token tạm thời hoặc thử lại khi người dùng đăng nhập.
        }
    }

    /**
     * Xử lý payload dữ liệu của tin nhắn FCM.
     * Thêm logic để lưu thông báo vào Realtime Database.
     */
    private void handleDataMessage(Map<String, String> data) {
        // Lấy thông tin từ data payload
        String userId = data.get("user_id"); // ID của người nhận thông báo
        String type = data.get("type"); // Loại thông báo (new_message, new_offer, etc.)
        String title = data.get("title"); // Tiêu đề thông báo
        String body = data.get("body"); // Nội dung thông báo
        String relatedId = data.get("related_id"); // ID liên quan (chatId, offerId, itemId)

        // Kiểm tra xem có đủ thông tin để ghi vào DB không
        if (userId != null && type != null && title != null && body != null && relatedId != null) {
            DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
            String notificationId = notificationsRef.push().getKey();

            if (notificationId != null) {
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("user_id", userId);
                notificationData.put("type", type);
                notificationData.put("title", title);
                notificationData.put("body", body);
                notificationData.put("related_id", relatedId);
                notificationData.put("read", false); // Mặc định là chưa đọc

                // Sử dụng SimpleDateFormat để tạo timestamp dạng String
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC")); // Đảm bảo dùng UTC
                notificationData.put("timestamp", sdf.format(new Date()));


                notificationsRef.child(notificationId).setValue(notificationData)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification added to DB: " + notificationId))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to add notification to DB: " + e.getMessage()));
            }
        } else {
            Log.w(TAG, "Missing data for DB notification: " + data.toString());
        }

        // Đồng thời, hiển thị thông báo hệ thống nếu có đủ thông tin
        if (title != null && body != null) {
            sendNotification(title, body, data);
        }
    }

    /**
     * Tạo và hiển thị thông báo đẩy.
     * @param title Tiêu đề thông báo.
     * @param messageBody Nội dung thông báo.
     * @param data Dữ liệu payload từ FCM để điều hướng.
     */
    private void sendNotification(String title, String messageBody, Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // --- Bắt đầu: Thêm extras vào Intent để MainActivity điều hướng ---
        // Gửi type và related_id qua Intent
        if (data.containsKey("type")) {
            intent.putExtra("notification_type", data.get("type"));
        }
        if (data.containsKey("related_id")) {
            intent.putExtra("notification_related_id", data.get("related_id"));
        }
        // Bạn có thể thêm các dữ liệu khác nếu cần để điều hướng chính xác
        // --- Kết thúc: Thêm extras ---


        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE); // FLAG_IMMUTABLE là bắt buộc từ Android S (API 31)

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification) // Đặt icon cho thông báo của bạn
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT); // Đặt độ ưu tiên

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Tạo NotificationChannel cho Android 8.0 (API level 26) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT); // Độ quan trọng của kênh
            channel.setDescription(CHANNEL_DESCRIPTION);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID thông báo, có thể dùng ID duy nhất cho từng loại */, notificationBuilder.build());
    }
}
