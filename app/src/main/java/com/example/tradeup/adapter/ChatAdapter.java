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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Chat;
import com.example.tradeup.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final Context context;
    private List<Chat> chatList;
    private final String currentUserId;
    private OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(Chat chat, String otherUserId, String otherUserName);
    }

    public ChatAdapter(Context context, List<Chat> chatList, String currentUserId, OnChatClickListener listener) {
        this.context = context;
        this.chatList = chatList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setChats(List<Chat> newChats) {
        this.chatList = newChats;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_preview, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        // Xác định ID của người dùng khác
        String otherUserId;
        if (chat.getUser_1().equals(currentUserId)) {
            otherUserId = chat.getUser_2();
        } else {
            otherUserId = chat.getUser_1();
        }

        // Lấy thông tin người dùng khác từ Firebase
        DatabaseReference otherUserRef = FirebaseDatabase.getInstance().getReference("users").child(otherUserId);
        otherUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User otherUser = snapshot.getValue(User.class);
                if (otherUser != null) {
                    holder.tvOtherUserName.setText(otherUser.getDisplay_name());
                    // Load ảnh đại diện
                    if (otherUser.getProfile_picture_url() != null && !otherUser.getProfile_picture_url().isEmpty()) {
                        Glide.with(context).load(otherUser.getProfile_picture_url()).into(holder.ivOtherUserProfileImage);
                    } else {
                        holder.ivOtherUserProfileImage.setImageResource(R.drawable.img_profile_placeholder);
                    }
                    // Thiết lập listener sau khi có đủ thông tin
                    holder.itemView.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onChatClick(chat, otherUserId, otherUser.getDisplay_name());
                        }
                    });
                } else {
                    holder.tvOtherUserName.setText("Người dùng không tồn tại");
                    holder.ivOtherUserProfileImage.setImageResource(R.drawable.img_profile_placeholder);
                    holder.itemView.setOnClickListener(null); // Vô hiệu hóa click nếu người dùng không tồn tại
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatAdapter", "Failed to load other user info: " + error.getMessage());
                holder.tvOtherUserName.setText("Lỗi tải thông tin");
                holder.ivOtherUserProfileImage.setImageResource(R.drawable.img_profile_placeholder);
                holder.itemView.setOnClickListener(null);
            }
        });

        // Hiển thị tin nhắn cuối cùng và thời gian
        holder.tvLastMessage.setText(chat.getLastMessage() != null ? chat.getLastMessage() : "Không có tin nhắn");
        holder.tvLastMessageTime.setText(formatTimestamp(chat.getLastMessageTimestamp()));

        // --- Cập nhật logic đánh dấu tin nhắn chưa đọc ---
        int unreadCount = 0;
        if (currentUserId.equals(chat.getUser_1())) {
            // Nếu người dùng hiện tại là user_1, kiểm tra unreadCount của user_1
            unreadCount = chat.getUser1UnreadCount() != null ? chat.getUser1UnreadCount() : 0;
        } else if (currentUserId.equals(chat.getUser_2())) {
            // Nếu người dùng hiện tại là user_2, kiểm tra unreadCount của user_2
            unreadCount = chat.getUser2UnreadCount() != null ? chat.getUser2UnreadCount() : 0;
        }

        if (unreadCount > 0) {
            holder.ivUnreadIndicator.setVisibility(View.VISIBLE);
            holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
            holder.tvOtherUserName.setTypeface(null, Typeface.BOLD);
            // Có thể hiển thị số lượng tin nhắn chưa đọc nếu muốn
            // holder.tvUnreadCount.setText(String.valueOf(unreadCount));
            // holder.tvUnreadCount.setVisibility(View.VISIBLE);
        } else {
            holder.ivUnreadIndicator.setVisibility(View.GONE);
            holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
            holder.tvOtherUserName.setTypeface(null, Typeface.NORMAL);
            // holder.tvUnreadCount.setVisibility(View.GONE);
        }
        // --- Kết thúc logic cập nhật ---
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        sdf.setLenient(false);
        try {
            Date messageDate = sdf.parse(timestamp);
            Date now = new Date();

            long diffMillis = now.getTime() - messageDate.getTime();

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
                SimpleDateFormat displaySdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return displaySdf.format(messageDate);
            }
        } catch (ParseException e) {
            Log.e("ChatAdapter", "Error parsing timestamp: " + timestamp, e);
            return timestamp;
        }
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        CircleImageView ivOtherUserProfileImage;
        TextView tvOtherUserName, tvLastMessage, tvLastMessageTime;
        ImageView ivUnreadIndicator;
        // Nếu bạn muốn hiển thị số lượng tin nhắn chưa đọc, thêm TextView này:
        // TextView tvUnreadCount;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_chat_preview);
            ivOtherUserProfileImage = itemView.findViewById(R.id.iv_other_user_profile_image);
            tvOtherUserName = itemView.findViewById(R.id.tv_other_user_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvLastMessageTime = itemView.findViewById(R.id.tv_last_message_time);
            ivUnreadIndicator = itemView.findViewById(R.id.iv_unread_indicator);
            // Khởi tạo nếu bạn thêm tvUnreadCount vào layout item_chat_preview.xml
            // tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
        }
    }
}