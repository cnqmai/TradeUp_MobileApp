package com.example.tradeup.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Message; // Cần import model Message
import com.example.tradeup.model.User; // Cần import model User
import com.google.firebase.auth.FirebaseAuth;
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

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int MSG_TYPE_SENT = 0;
    private static final int MSG_TYPE_RECEIVED = 1;

    private final Context context;
    private List<Message> messageList;
    private final String currentUserId;
    private final String otherUserId; // ID của người dùng đối diện trong chat
    private String otherUserProfilePictureUrl; // URL ảnh của người dùng đối diện

    public MessageAdapter(Context context, List<Message> messageList, String otherUserId) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        this.otherUserId = otherUserId;
        fetchOtherUserProfilePicture(); // Lấy ảnh đại diện của người dùng đối diện
    }

    public void setMessages(List<Message> newMessages) {
        this.messageList = newMessages;
        notifyDataSetChanged();
    }

    private void fetchOtherUserProfilePicture() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(otherUserId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getProfile_picture_url() != null) {
                    otherUserProfilePictureUrl = user.getProfile_picture_url();
                    notifyDataSetChanged(); // Cập nhật adapter để load ảnh
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MessageAdapter", "Failed to load other user profile picture: " + error.getMessage());
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).getSender_id().equals(currentUserId)) {
            return MSG_TYPE_SENT;
        } else {
            return MSG_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_SENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (holder.getItemViewType() == MSG_TYPE_SENT) {
            SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
            if (message.getType().equals("text")) {
                sentHolder.tvMessage.setText(message.getText());
                sentHolder.tvMessage.setVisibility(View.VISIBLE);
                sentHolder.ivImage.setVisibility(View.GONE);
            } else if (message.getType().equals("image") && message.getImageUrl() != null) {
                sentHolder.tvMessage.setVisibility(View.GONE);
                sentHolder.ivImage.setVisibility(View.VISIBLE);
                Glide.with(context).load(message.getImageUrl()).into(sentHolder.ivImage);
            }
            sentHolder.tvTimestamp.setText(formatTimestamp(message.getTimestamp()));
        } else {
            ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
            if (message.getType().equals("text")) {
                receivedHolder.tvMessage.setText(message.getText());
                receivedHolder.tvMessage.setVisibility(View.VISIBLE);
                receivedHolder.ivImage.setVisibility(View.GONE);
            } else if (message.getType().equals("image") && message.getImageUrl() != null) {
                receivedHolder.tvMessage.setVisibility(View.GONE);
                receivedHolder.ivImage.setVisibility(View.VISIBLE);
                Glide.with(context).load(message.getImageUrl()).into(receivedHolder.ivImage);
            }
            receivedHolder.tvTimestamp.setText(formatTimestamp(message.getTimestamp()));

            // Load ảnh đại diện người gửi
            if (otherUserProfilePictureUrl != null && !otherUserProfilePictureUrl.isEmpty()) {
                Glide.with(context).load(otherUserProfilePictureUrl).into(receivedHolder.ivSenderProfileImage);
            } else {
                receivedHolder.ivSenderProfileImage.setImageResource(R.drawable.img_profile_placeholder);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        sdf.setLenient(false);
        try {
            Date messageDate = sdf.parse(timestamp);
            SimpleDateFormat displaySdf = new SimpleDateFormat("HH:mm a", Locale.getDefault()); // Ví dụ: 10:30 AM/PM
            return displaySdf.format(messageDate);
        } catch (ParseException e) {
            Log.e("MessageAdapter", "Error parsing timestamp: " + timestamp, e);
            return timestamp;
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTimestamp;
        ImageView ivImage;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_sent);
            ivImage = itemView.findViewById(R.id.iv_image_sent);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp_sent);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivSenderProfileImage;
        TextView tvMessage, tvTimestamp;
        ImageView ivImage;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivSenderProfileImage = itemView.findViewById(R.id.iv_sender_profile_image);
            tvMessage = itemView.findViewById(R.id.tv_message_received);
            ivImage = itemView.findViewById(R.id.iv_image_received);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp_received);
        }
    }
}