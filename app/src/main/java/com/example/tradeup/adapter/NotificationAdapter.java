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
import java.util.ArrayList;
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
        // FIX: Add null check for newNotifications
        if (newNotifications != null) {
            this.notificationList = newNotifications;
            notifyDataSetChanged();
        } else {
            Log.w("NotificationAdapter", "Attempted to set null notification list.");
            this.notificationList = new ArrayList<>(); // Set to empty list to avoid NPE
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        // FIX: Add null check for notificationList before accessing elements
        if (notificationList == null || position >= notificationList.size()) {
            Log.e("NotificationAdapter", "notificationList is null or position is out of bounds in onBindViewHolder.");
            return;
        }

        Notification notification = notificationList.get(position);

        holder.tvTitle.setText(notification.getTitle());
        holder.tvBody.setText(notification.getBody() != null ? notification.getBody() : notification.getTitle());
        holder.tvTime.setText(formatTimestamp(notification.getTimestamp()));

        // Change color/font style if notification is unread
        if (notification.getRead() != null && !notification.getRead()) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.unread_notification_background));
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            holder.tvBody.setTypeface(null, Typeface.BOLD);
            holder.unreadDot.setVisibility(View.VISIBLE); // Show the unread dot
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            holder.tvBody.setTypeface(null, Typeface.NORMAL);
            holder.unreadDot.setVisibility(View.GONE); // Hide the unread dot
        }

        // Set icon based on notification type
        int iconRes = getIconForNotificationType(notification.getType());
        holder.ivIcon.setImageResource(iconRes);
        // Set tint color for the icon background circle
        holder.ivIcon.setBackgroundResource(R.drawable.bg_circle_icon_light); // Ensure this drawable exists
        holder.ivIcon.setColorFilter(ContextCompat.getColor(context, getIconTintColor(notification.getType())));


        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        // FIX: Ensure notificationList is not null before calling size()
        return notificationList != null ? notificationList.size() : 0;
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return "";

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

            Date notificationDate = inputFormat.parse(timestamp);
            Date now = new Date();

            long diffMillis = now.getTime() - notificationDate.getTime();

            long seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
            long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
            long days = TimeUnit.MILLISECONDS.toDays(diffMillis);

            if (seconds < 60) {
                return context.getString(R.string.time_just_now);
            } else if (minutes < 60) {
                return context.getString(R.string.time_minutes_ago, minutes);
            } else if (hours < 24) {
                return context.getString(R.string.time_hours_ago, hours);
            } else if (days < 7) {
                return context.getString(R.string.time_days_ago, days);
            } else {
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return outputFormat.format(notificationDate);
            }
        } catch (ParseException e) {
            Log.e("NotificationAdapter", "Error formatting timestamp: " + timestamp, e);
            return timestamp;
        }
    }

    private int getIconForNotificationType(String type) {
        if (type == null) return R.drawable.ic_notification; // Default icon

        switch (type) {
            case "new_message":
                return R.drawable.ic_message;
            case "new_offer":
            case "offer_accepted":
            case "counter_offer":
            case "buyer_responded_offer":
                return R.drawable.ic_attach_money; // Icon for money/offers
            case "promotion":
                return R.drawable.ic_promotion; // Icon for promotion
            case "reported_chat":
                return R.drawable.ic_report; // Icon for report
            case "listing_update": // New type for listing updates
                return R.drawable.ic_update; // Icon for updates (e.g., refresh icon)
            default:
                return R.drawable.ic_notification; // Default icon
        }
    }

    private int getIconTintColor(String type) {
        if (type == null) return R.color.gray; // Default tint color

        switch (type) {
            case "new_message":
                return R.color.green_bold; // Green for messages
            case "new_offer":
            case "offer_accepted":
            case "counter_offer":
            case "buyer_responded_offer":
                return R.color.orange_bold; // Orange for offers
            case "promotion":
                return R.color.yellow_bold; // Yellow for promotions
            case "reported_chat":
                return R.color.red_bold; // Red for reports
            case "listing_update":
                return R.color.blue_bold; // Blue for updates
            default:
                return R.color.gray;
        }
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivIcon;
        TextView tvTitle, tvBody, tvTime;
        View unreadDot; // Added unread dot

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_notification);
            ivIcon = itemView.findViewById(R.id.iv_notification_icon);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvBody = itemView.findViewById(R.id.tv_notification_body);
            tvTime = itemView.findViewById(R.id.tv_notification_time);
            unreadDot = itemView.findViewById(R.id.unread_dot); // Initialize unread dot
        }
    }
}
