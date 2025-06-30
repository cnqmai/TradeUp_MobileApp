package com.example.tradeup.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.model.Notification;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private List<Notification> notificationList;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(Context context, List<Notification> notificationList, OnNotificationClickListener listener) {
        this.context = context;
        this.notificationList = notificationList;
        this.listener = listener;
    }

    public void setNotifications(List<Notification> newNotifications) {
        this.notificationList = newNotifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.tvTitle.setText(notification.getTitle());
        holder.tvBody.setText(notification.getBody() != null ? notification.getBody() : notification.getTitle()); // Dùng body nếu có, không thì dùng title
        holder.tvTime.setText(formatTimestamp(notification.getTimestamp()));

        // Thay đổi màu sắc/kiểu chữ nếu thông báo chưa đọc
        if (notification.getRead() != null && !notification.getRead()) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.unread_notification_background)); // Định nghĩa màu này
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            holder.tvBody.setTypeface(null, Typeface.BOLD);
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            holder.tvBody.setTypeface(null, Typeface.NORMAL);
        }

        // Đặt icon dựa trên loại thông báo (tùy chọn)
        int iconRes = getIconForNotificationType(notification.getType());
        holder.ivIcon.setImageResource(iconRes);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "";

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC")); // Chuẩn để parse đúng ISO 'Z'

            Date notificationDate = inputFormat.parse(timestamp);
            Date now = new Date();

            long diffMillis = now.getTime() - notificationDate.getTime();

            long seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
            long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
            long days = TimeUnit.MILLISECONDS.toDays(diffMillis);

            if (seconds < 60) {
                return "Vừa xong";
            } else if (minutes < 60) {
                return minutes + " phút trước";
            } else if (hours < 24) {
                return hours + " giờ trước";
            } else if (days < 7) {
                return days + " ngày trước";
            } else {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return outputFormat.format(notificationDate);
            }
        } catch (ParseException e) {
            Log.e("NotificationAdapter", "Lỗi định dạng timestamp: " + timestamp, e);
            return timestamp;
        }
    }



    private int getIconForNotificationType(String type) {
        if (type == null) return R.drawable.ic_notification; // Icon mặc định

        switch (type) {
            case "new_message":
                return R.drawable.ic_message;
            case "new_offer":
                return R.drawable.ic_attach_money; // Cần icon tiền
            case "promotion":
                return R.drawable.ic_promotion; // Cần icon sao
            case "reported_chat":
                return R.drawable.ic_report; // Cần icon báo cáo
            case "offer_accepted":
                return R.drawable.ic_check_circle; // Cần icon check
            // Thêm các loại thông báo khác nếu có
            default:
                return R.drawable.ic_notification;
        }
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivIcon;
        TextView tvTitle, tvBody, tvTime;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_notification);
            ivIcon = itemView.findViewById(R.id.iv_notification_icon);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvBody = itemView.findViewById(R.id.tv_notification_body);
            tvTime = itemView.findViewById(R.id.tv_notification_time);
        }
    }
}