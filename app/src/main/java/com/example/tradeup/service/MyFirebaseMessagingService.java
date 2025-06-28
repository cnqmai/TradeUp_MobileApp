package com.example.tradeup.service; // Đặt trong một package thích hợp, ví dụ: .service

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

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    /**
     * Được gọi khi nhận được tin nhắn FCM.
     * @param remoteMessage Đối tượng RemoteMessage chứa tin nhắn FCM.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Kiểm tra xem tin nhắn có chứa payload dữ liệu hay không.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            // Xử lý dữ liệu tại đây. Ví dụ, bạn có thể chuyển hướng người dùng đến một màn hình cụ thể
            // hoặc cập nhật UI mà không cần hiển thị thông báo.
            handleDataMessage(remoteMessage);
        }

        // Kiểm tra xem tin nhắn có chứa payload thông báo hay không.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
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

        // Gửi token này đến máy chủ của bạn (nếu có)
        // hoặc lưu trữ nó để sử dụng sau này (ví dụ, để gửi thông báo từ Cloud Functions).
        sendRegistrationToServer(token);
    }

    /**
     * Xử lý payload dữ liệu của tin nhắn FCM.
     * Bạn có thể tùy chỉnh logic này dựa trên cấu trúc dữ liệu của bạn.
     */
    private void handleDataMessage(RemoteMessage remoteMessage) {
        String title = remoteMessage.getData().get("title");
        String message = remoteMessage.getData().get("message");
        String chatId = remoteMessage.getData().get("chatId"); // Ví dụ: lấy ID chat
        String type = remoteMessage.getData().get("type"); // Ví dụ: "new_message", "new_offer"

        // Ở đây, bạn có thể quyết định hiển thị thông báo hoặc thực hiện hành động nền.
        // Ví dụ, nếu là tin nhắn mới, bạn vẫn có thể hiển thị thông báo.
        if (title != null && message != null) {
            sendNotification(title, message);
        }

        // Nếu là tin nhắn mới trong chat, bạn có thể điều hướng người dùng đến ChatDetailFragment
        if ("new_message".equals(type) && chatId != null) {
            // Đây là một ví dụ, bạn cần điều chỉnh để phù hợp với NavGraph của mình
            // Intent intent = new Intent(this, MainActivity.class);
            // intent.putExtra("chatId", chatId);
            // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            // PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            //         PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
            // sendNotification(title, message, pendingIntent); // Gửi thông báo có PendingIntent
        }
    }

    /**
     * Tạo và hiển thị thông báo đẩy.
     * @param title Tiêu đề thông báo.
     * @param body Nội dung thông báo.
     */
    private void sendNotification(String title, String body) {
        // Intent để mở Activity chính khi người dùng nhấn vào thông báo
        Intent intent = new Intent(this, MainActivity.class); // Thay thế MainActivity.class bằng Activity mà bạn muốn mở
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE); // FLAG_IMMUTABLE là bắt buộc từ Android S (API 31)

        String channelId = getString(R.string.default_notification_channel_id); // Định nghĩa trong strings.xml
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification) // Đặt icon cho thông báo
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Tạo NotificationChannel cho Android 8.0 (API level 26) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "TradeUp Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID thông báo */, notificationBuilder.build());
    }

    /**
     * Gửi token đăng ký lên máy chủ của bạn (tùy chọn, nếu bạn có máy chủ riêng).
     */
    private void sendRegistrationToServer(String token) {
        // TODO: Triển khai logic này để gửi token đến máy chủ của bạn.
        // Điều này là cần thiết nếu bạn muốn gửi thông báo từ máy chủ riêng của mình
        // thay vì trực tiếp từ Firebase Console hoặc Cloud Functions.
        Log.d(TAG, "Token to send to server: " + token);
    }
}