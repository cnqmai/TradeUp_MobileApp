package com.example.tradeup.fragment;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.adapter.MessageAdapter;
import com.example.tradeup.model.Chat;
import com.example.tradeup.model.Message;
import com.example.tradeup.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.apache.commons.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import android.app.ProgressDialog;

public class ChatDetailFragment extends Fragment {

    private static final String TAG = "ChatDetailFragment";

    private RecyclerView rvMessages;
    private EditText etMessageInput;
    private ImageView ivSendMessage;
    private ImageView ivAttachImage;
    private ImageView ivBackButton;
    private ImageView ivBlockChat, ivReportChat;
    private TextView tvOtherUserNameToolbar;
    private CircleImageView ivOtherUserProfileToolbar; // Ảnh đại diện người dùng khác

    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private DatabaseReference chatMessagesRef;
    private DatabaseReference chatsRef;
    private DatabaseReference usersRef;
    private ProgressDialog progressDialog;

    private String currentUserId;
    private String chatId;
    private String otherUserId;
    private String otherUserName;
    private String user1IdInChat; // Stores user_1 of the chat
    private String user2IdInChat; // Stores user_2 of the chat
    private Chat currentChat; // Declares variable to store current Chat object
    private NavController navController;

    // ActivityResultLauncher for taking photos and selecting images from the gallery
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private Uri imageUri; // Temporary URI to save images taken from the camera
    private Set<String> sensitiveWords;
    private OkHttpClient okHttpClient;

    public ChatDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get arguments from navigation
        if (getArguments() != null) {
            chatId = getArguments().getString("chatId");
            otherUserId = getArguments().getString("otherUserId");
            otherUserName = getArguments().getString("otherUserName"); // Get other user's name
        }

        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        okHttpClient = new OkHttpClient();

        if (chatId == null) {
            // If chatId does not exist, find or create new
            findOrCreateChat();
        } else {
            // If chatId exists, set up chatMessagesRef and load chat details
            chatMessagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId);
            // When opening chat, reset current user's unreadCount to 0
            // We need to get user_1 and user_2 information before resetting
            // And also need to listen for chat details to update currentChat
            loadChatDetails(); // Call this function to set up listener for chat details
        }

        // Initialize ActivityResultLaunchers
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
            if (result) {
                if (imageUri != null) {
                    uploadImageAndSendMessage(imageUri);
                }
            } else {
                Toast.makeText(requireContext(), "Chụp ảnh bị hủy hoặc lỗi.", Toast.LENGTH_SHORT).show();
            }
        });

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                uploadImageAndSendMessage(uri);
            } else {
                Toast.makeText(requireContext(), "Chọn ảnh bị hủy.", Toast.LENGTH_SHORT).show();
            }
        });

        initializeSensitiveWords();
    }

    // New method to initialize the list of sensitive words
    private void initializeSensitiveWords() {
        sensitiveWords = new HashSet<>();
        // Add your sensitive words here (should be lowercase for case-insensitive checking)
        sensitiveWords.add("địt");
        sensitiveWords.add("mày");
        sensitiveWords.add("đồ ngu");
        sensitiveWords.add("buồi");
        sensitiveWords.add("lồn");
        sensitiveWords.add("cặc");
        sensitiveWords.add("phò");
        sensitiveWords.add("đĩ");
        sensitiveWords.add("sex");
        sensitiveWords.add("lol");
        sensitiveWords.add("đcm");
        sensitiveWords.add("clm");
        // Add more words according to your application's regulations
        // In a real application, you might load this list from Firebase Remote Config
        // or a remote source for easy updates without app updates.
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang xử lý..."); // Default message
        progressDialog.setCancelable(false);

        initViews(view);
        setupToolbar();
        setupRecyclerView();
        setupListeners();

        if (chatMessagesRef != null) {
            listenForMessages();
        }
    }

    private void initViews(View view) {
        rvMessages = view.findViewById(R.id.rv_messages);
        etMessageInput = view.findViewById(R.id.et_message_input);
        ivSendMessage = view.findViewById(R.id.iv_send_message);
        ivAttachImage = view.findViewById(R.id.iv_attach_image);
        ivBackButton = view.findViewById(R.id.iv_back_button_chat);
        ivBlockChat = view.findViewById(R.id.iv_block_chat);
        ivReportChat = view.findViewById(R.id.iv_report_chat);
        tvOtherUserNameToolbar = view.findViewById(R.id.tv_other_user_name_toolbar);
        ivOtherUserProfileToolbar = view.findViewById(R.id.iv_other_user_profile_toolbar);
    }

    private void setupToolbar() {
        if (otherUserName != null && !otherUserName.isEmpty()) {
            tvOtherUserNameToolbar.setText(otherUserName);
        } else {
            // If no name, get from Firebase
            usersRef.child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && user.getDisplay_name() != null) {
                        tvOtherUserNameToolbar.setText(user.getDisplay_name());
                    } else {
                        tvOtherUserNameToolbar.setText("Người dùng khác");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load other user info for toolbar: " + error.getMessage());
                    tvOtherUserNameToolbar.setText("Lỗi");
                }
            });
        }

        // If otherUserName is already available from Bundle, try loading the image immediately
        if (otherUserId != null) {
            usersRef.child(otherUserId).child("profile_picture_url").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String profilePicUrl = snapshot.getValue(String.class);
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Glide.with(requireContext()).load(profilePicUrl).into(ivOtherUserProfileToolbar);
                    } else {
                        ivOtherUserProfileToolbar.setImageResource(R.drawable.img_profile_placeholder);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load other user profile picture for toolbar: " + error.getMessage());
                    ivOtherUserProfileToolbar.setImageResource(R.drawable.img_profile_placeholder);
                }
            });
        }
    }


    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(requireContext(), messageList, otherUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true); // To display the latest message at the bottom
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> navController.navigateUp());

        ivSendMessage.setOnClickListener(v -> sendMessage("text"));
        ivAttachImage.setOnClickListener(v -> showImagePickerDialog());

        ivBlockChat.setOnClickListener(v -> toggleBlockChat());
        ivReportChat.setOnClickListener(v -> toggleReportChat());

        // NEW: Click listener for other user's profile picture in toolbar
        ivOtherUserProfileToolbar.setOnClickListener(v -> {
            if (otherUserId != null && !otherUserId.isEmpty() && navController != null) {
                // Prevent navigating to own profile if it somehow gets clicked
                if (currentUserId != null && currentUserId.equals(otherUserId)) {
                    Toast.makeText(getContext(), "Đây là hồ sơ của bạn.", Toast.LENGTH_SHORT).show();
                    // Optionally, navigate to the main ProfileFragment if you have one for own profile
                    // navController.navigate(R.id.action_chatDetailFragment_to_profileFragment);
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putString("userId", otherUserId);
                    // Assuming you have an action in nav_graph.xml from chatDetailFragment to userProfileFragment
                    navController.navigate(R.id.action_chatDetailFragment_to_userProfileFragment, bundle);
                }
            } else {
                Toast.makeText(getContext(), "Không thể xem hồ sơ người dùng.", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Cannot navigate to user profile: otherUserId is null or navController is null.");
            }
        });
    }

    private void findOrCreateChat() {
        // Logic to find chat ID between currentUserId and otherUserId
        // If not found, create new chat
        Query query1 = chatsRef.orderByChild("user_1").equalTo(currentUserId);
        query1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean chatFound = false;
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    Chat chat = chatSnapshot.getValue(Chat.class);
                    if (chat != null && chat.getUser_2().equals(otherUserId)) {
                        chatId = chatSnapshot.getKey();
                        chatFound = true;
                        chatMessagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId);
                        user1IdInChat = chat.getUser_1(); // Store user_1
                        user2IdInChat = chat.getUser_2(); // Store user_2
                        loadChatDetails(); // Load chat details and set listener
                        listenForMessages(); // Start listening for messages
                        break;
                    }
                }
                if (!chatFound) {
                    // Check the other way around (otherUserId is user_1)
                    Query query2 = chatsRef.orderByChild("user_1").equalTo(otherUserId);
                    query2.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean chatFoundReverse = false;
                            for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                                Chat chat = chatSnapshot.getValue(Chat.class);
                                if (chat != null && chat.getUser_2().equals(currentUserId)) {
                                    chatId = chatSnapshot.getKey();
                                    chatFoundReverse = true;
                                    chatMessagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId);
                                    user1IdInChat = chat.getUser_1(); // Store user_1
                                    user2IdInChat = chat.getUser_2(); // Store user_2
                                    loadChatDetails(); // Load chat details and set listener
                                    listenForMessages(); // Start listening for messages
                                    break;
                                }
                            }
                            if (!chatFoundReverse) {
                                createNewChat();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to find chat (reverse): " + error.getMessage());
                            Toast.makeText(requireContext(), "Lỗi khi tìm cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to find chat: " + error.getMessage());
                Toast.makeText(requireContext(), "Lỗi khi tìm cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createNewChat() {
        chatId = chatsRef.push().getKey(); // Create new chat ID
        if (chatId == null) {
            Toast.makeText(requireContext(), "Không thể tạo ID cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use default constructor and then set values
        Chat newChat = new Chat();
        newChat.setChatId(chatId);
        newChat.setUser_1(currentUserId); // currentUserId is user_1
        newChat.setUser_2(otherUserId); // otherUserId is user_2
        newChat.setBlocked(false); // Default to false
        newChat.setReported(false); // Default to false
        newChat.setLastMessage("Hãy bắt đầu cuộc trò chuyện!");
        newChat.setLastMessageTimestamp(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
        newChat.setUser1UnreadCount(0); // Initialize unread message count for user_1 (currentUserId)
        newChat.setUser2UnreadCount(0); // Initialize unread message count for user_2 (otherUserId)

        // Store user1 and user2 IDs in local variables
        user1IdInChat = currentUserId;
        user2IdInChat = otherUserId;


        chatsRef.child(chatId).setValue(newChat)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New chat created: " + chatId);
                    chatMessagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId);
                    loadChatDetails(); // Load chat details and set listener
                    listenForMessages();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create new chat: " + e.getMessage());
                    Toast.makeText(requireContext(), "Không thể tạo cuộc trò chuyện mới.", Toast.LENGTH_SHORT).show();
                });
    }

    // Method to listen for chat details (including block status)
    private void loadChatDetails() {
        if (chatId == null) {
            Log.w(TAG, "chatId is null, cannot load chat details.");
            return;
        }
        chatsRef.child(chatId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentChat = snapshot.getValue(Chat.class); // Update currentChat variable
                if (currentChat != null) {
                    user1IdInChat = currentChat.getUser_1();
                    user2IdInChat = currentChat.getUser_2();
                    resetUnreadCountForCurrentUser(); // Reset when opening chat
                    updateChatHeader(currentChat); // Update UI toolbar (e.g., block icon)
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load chat details: " + error.getMessage());
                Toast.makeText(requireContext(), "Lỗi khi tải chi tiết cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // New method to check if a message contains sensitive words
    private boolean isMessageSensitive(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        // Convert message to lowercase for case-insensitive checking
        String lowerCaseMessage = message.toLowerCase(Locale.getDefault());
        // Split message into words. Use regex to handle spaces and punctuation.
        // Example: "đồ.ngu" will be split into "đồ", "ngu"
        String[] words = lowerCaseMessage.split("\\s+|\\p{Punct}");

        for (String word : words) {
            // Remove leading/trailing whitespace from each word after splitting
            String cleanedWord = word.trim();
            if (sensitiveWords.contains(cleanedWord)) {
                return true; // Sensitive word found
            }
        }
        return false; // No sensitive words found
    }

    // New method to display sensitive content warning dialog
    private void showSensitiveContentWarning(String messageText, String type) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cảnh báo nội dung nhạy cảm")
                .setMessage("Tin nhắn của bạn có thể chứa từ ngữ không phù hợp. Bạn có muốn:")
                .setPositiveButton("Gửi dù sao", (dialog, which) -> {
                    // User chooses to send anyway, call the actual send message method
                    actuallySendMessage(messageText, type);
                })
                .setNeutralButton("Chỉnh sửa", (dialog, which) -> {
                    // User chooses to edit, keep the text in EditText
                    dialog.dismiss(); // Dismiss dialog
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    // User chooses to cancel, clear the text in EditText
                    etMessageInput.setText("");
                    dialog.dismiss(); // Dismiss dialog
                })
                .show();
    }
    private void updateChatHeader(Chat chat) {
        // Update block status UI (keep as is or already exists)
        if (chat.getBlocked() != null && chat.getBlocked()) {
            ivBlockChat.setImageResource(R.drawable.ic_lock);
            etMessageInput.setEnabled(false);
            ivSendMessage.setEnabled(false);
            ivAttachImage.setEnabled(false);
            Toast.makeText(requireContext(), "Cuộc trò chuyện này đang bị chặn.", Toast.LENGTH_SHORT).show();
        } else {
            ivBlockChat.setImageResource(R.drawable.ic_unlock);
            etMessageInput.setEnabled(true);
            ivSendMessage.setEnabled(true);
            ivAttachImage.setEnabled(true);
        }

        // Update report status UI (New part to add)
        // Make sure you have corresponding drawables: ic_report and ic_report_off (or your chosen icon)
        if (chat.getReported() != null && chat.getReported()) {
            ivReportChat.setImageResource(R.drawable.ic_flag_filled); // Assuming reported icon
            // Can add Toast if you want to display when chat is reported
        } else {
            ivReportChat.setImageResource(R.drawable.ic_flag_outline); // Assuming not reported / unreport icon
        }
    }

    private void listenForMessages() {
        chatMessagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message message = messageSnapshot.getValue(Message.class);
                    if (message != null) {
                        messageList.add(message);
                    }
                }
                // Sort the message list by timestamp
                Collections.sort(messageList, new Comparator<Message>() {
                    @Override
                    public int compare(Message m1, Message m2) {
                        // Compare timestamps. Assume timestamp is a String in ISO 8601 format
                        // If timestamp is Long (ServerValue.TIMESTAMP), compare directly
                        if (m1.getTimestamp() == null || m2.getTimestamp() == null) {
                            return 0; // Handle null cases
                        }
                        // If timestamp is String, you need to parse or ensure comparable format
                        return m1.getTimestamp().compareTo(m2.getTimestamp());
                    }
                });

                messageAdapter.setMessages(messageList);
                rvMessages.scrollToPosition(messageList.size() - 1); // Scroll to the end
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load messages: " + error.getMessage());
                Toast.makeText(requireContext(), "Lỗi khi tải tin nhắn: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String type) {
        String messageText = etMessageInput.getText().toString().trim();
        if (messageText.isEmpty() && !"image".equals(type)) {
            Toast.makeText(requireContext(), "Vui lòng nhập tin nhắn hoặc chọn ảnh.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chatId == null || currentChat == null) {
            Toast.makeText(requireContext(), "Chưa thể gửi tin nhắn. Đang tải cuộc trò chuyện...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check block status first
        if (currentChat.getBlocked() != null && currentChat.getBlocked()) {
            Toast.makeText(requireContext(), "Cuộc trò chuyện này đã bị chặn. Bạn không thể gửi tin nhắn.", Toast.LENGTH_LONG).show();
            return;
        }

        // Only check sensitive words for text messages
        if ("text".equals(type)) {
            if (isMessageSensitive(messageText)) {
                // If message is sensitive, show warning and stop
                showSensitiveContentWarning(messageText, type);
                return; // Important: stop here to wait for user decision
            }
        }

        // If not a text message, or it's text but not sensitive, or user chose "Send anyway"
        // then proceed to send the message
        actuallySendMessage(messageText, type);
    }

    // New method containing the actual message sending logic
    private void actuallySendMessage(String messageText, String type) {
        String messageId = chatMessagesRef.push().getKey();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        Message message = new Message();
        message.setSender_id(currentUserId);
        message.setTimestamp(timestamp);
        message.setType(type);

        if ("text".equals(type)) {
            message.setText(messageText);
            message.setImageUrl(null);
        } else if ("image".equals(type)) {
            // Note: Image upload and image message sending logic is handled separately by uploadImageAndSendMessage
            // This function (actuallySendMessage) will be called by sendImageMessage(String imageUrl) after the image is uploaded.
            // Therefore, the messageText variable here will be imageUrl if type is "image".
            // Make sure you pass the correct imageUrl here if this function is called from sendImageMessage.
            // However, with the current structure, sendMessage(String type) only handles type="text"
            // and uploadImageAndSendMessage handles type="image".
            // So, the if ("image".equals(type)) { ... } block might not be necessary here if this function is only called for text.
            // If you want this function to handle images as well, you need to pass imageUrl and set imageUrl for the message.
            // With the current code, sendMessage("text") will call this function. sendMessage("image") calls uploadImageAndSendMessage.
            // For now, let's assume `type` will be "text" when this function is called after sensitivity check.
            // The image message flow is handled by `sendImageMessage`.
            message.setText(null); // Assuming type "image" is not handled directly here
            message.setImageUrl(messageText); // In this case, messageText holds the imageUrl
        }

        if (messageId != null) {
            chatMessagesRef.child(messageId).setValue(message)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Message sent: " + messageText);
                        etMessageInput.setText("");
                        updateChatLastMessage(messageText); // Update last message in chat list
                        incrementUnreadCountForOtherUser(); // Increment unread message count for the other user
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send message: " + e.getMessage());
                        Toast.makeText(requireContext(), "Không thể gửi tin nhắn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateChatLastMessage(String lastMessage) { // Removed timestampString parameter
        if (chatId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMessage", lastMessage);
            // Always use SimpleDateFormat for real-time
            updates.put("lastMessageTimestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
            chatsRef.child(chatId).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Chat last message and timestamp updated."))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update chat last message: " + e.getMessage()));
        }
    }

    private void resetUnreadCountForCurrentUser() {
        if (chatId == null || user1IdInChat == null || user2IdInChat == null) {
            Log.w(TAG, "Cannot reset unread count: chatId or user IDs are null.");
            return;
        }

        chatsRef.child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Chat chat = snapshot.getValue(Chat.class);
                if (chat != null) {
                    if (currentUserId.equals(user1IdInChat)) {
                        // Current user is user_1, reset user1UnreadCount
                        chatsRef.child(chatId).child("user1UnreadCount").setValue(0)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "user1UnreadCount reset to 0 for current user."))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to reset user1UnreadCount: " + e.getMessage()));
                    } else if (currentUserId.equals(user2IdInChat)) {
                        // Current user is user_2, reset user2UnreadCount
                        chatsRef.child(chatId).child("user2UnreadCount").setValue(0)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "user2UnreadCount reset to 0 for current user."))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to reset user2UnreadCount: " + e.getMessage()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read chat status for resetting unread count: " + error.getMessage());
            }
        });
    }


    private void incrementUnreadCountForOtherUser() {
        if (chatId == null || user1IdInChat == null || user2IdInChat == null) {
            Log.w(TAG, "Cannot increment unread count: chatId or user IDs are null.");
            return;
        }

        chatsRef.child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Chat chat = snapshot.getValue(Chat.class);
                if (chat != null) {
                    if (currentUserId.equals(user1IdInChat)) {
                        // Current user is user_1, so other user is user_2. Increment user2UnreadCount
                        Integer currentUnreadCount = chat.getUser2UnreadCount() != null ? chat.getUser2UnreadCount() : 0;
                        chatsRef.child(chatId).child("user2UnreadCount").setValue(currentUnreadCount + 1)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "user2UnreadCount incremented."))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to increment user2UnreadCount: " + e.getMessage()));
                    } else if (currentUserId.equals(user2IdInChat)) {
                        // Current user is user_2, so other user is user_1. Increment user1UnreadCount
                        Integer currentUnreadCount = chat.getUser1UnreadCount() != null ? chat.getUser1UnreadCount() : 0;
                        chatsRef.child(chatId).child("user1UnreadCount").setValue(currentUnreadCount + 1)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "user1UnreadCount incremented."))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to increment user1UnreadCount: " + e.getMessage()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read chat status for incrementing unread count: " + error.getMessage());
            }
        });
    }


    // Image sending logic
    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Chọn ảnh");
        builder.setItems(new CharSequence[]{"Chụp ảnh", "Chọn từ thư viện"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    checkCameraPermissionAndTakePicture();
                    break;
                case 1:
                    checkGalleryPermissionAndPickImage();
                    break;
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndTakePicture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePicture();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void checkGalleryPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            } else {
                galleryPermissionLauncherTiramisu.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            } else {
                galleryPermissionLauncherLegacy.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void takePicture() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        imageUri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        takePictureLauncher.launch(imageUri);
    }

    private void pickImage() {
        pickImageLauncher.launch("image/*");
    }

    private void uploadImageAndSendMessage(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(requireContext(), "Không có ảnh để tải lên.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if chatId and currentChat are initialized
        if (chatId == null || currentChat == null) {
            Toast.makeText(requireContext(), "Chưa thể gửi tin nhắn. Đang tải cuộc trò chuyện...", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- START BLOCK CHECK LOGIC ---
        if (currentChat.getBlocked() != null && currentChat.getBlocked()) {
            Toast.makeText(requireContext(), "Cuộc trò chuyện này đã bị chặn. Bạn không thể gửi tin nhắn.", Toast.LENGTH_LONG).show();
            return; // PREVENT SENDING MESSAGE
        }
        // --- END BLOCK CHECK LOGIC ---


        progressDialog.setMessage("Đang tải ảnh...");
        progressDialog.show();

        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Không thể mở luồng đầu vào từ URI ảnh.", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] imageBytes = IOUtils.toByteArray(inputStream);
            inputStream.close();

            String mimeType = requireContext().getContentResolver().getType(imageUri);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            // Use the new getFileExtensionFromMimeType method (you'll add it in step 5)
            String fileExtension = getFileExtensionFromMimeType(mimeType);
            String fileName = "chat_image_" + UUID.randomUUID().toString() + fileExtension;

            String cloudName = "dp6tzdsyt"; // Your Cloudinary Cloud Name
            String uploadPreset = "TradeUp"; // Your Cloudinary Unsigned Upload Preset

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName,
                            RequestBody.create(imageBytes, MediaType.parse(mimeType)))
                    .addFormDataPart("upload_preset", uploadPreset)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload")
                    .post(requestBody)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Tải ảnh thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Cloudinary upload failed", e);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    // Ensure dialog is dismissed before processing response
                    progressDialog.dismiss();
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Lỗi tải ảnh: " + response.code() + " " + response.message() + " - " + errorBody, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Cloudinary upload error: " + response.code() + " " + response.message() + " - " + errorBody);
                        });
                        return;
                    }

                    String json = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        String imageUrl = jsonObject.getString("secure_url");
                        requireActivity().runOnUiThread(() -> sendImageMessage(imageUrl)); // Send message after getting image URL

                    } catch (Exception e) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Lỗi xử lý phản hồi Cloudinary: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        Log.e(TAG, "Error parsing Cloudinary response", e);
                    } finally {
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                }
            });

        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Lỗi đọc URI ảnh: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Lỗi đọc ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    // Helper method to get file extension from MIME type
    private String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return ".bin";
        }
        switch (mimeType) {
            case "image/jpeg":
                return ".jpeg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/webp":
                return ".webp";
            default:
                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                return extension != null ? "." + extension : ".bin";
        }
    }

    private void sendImageMessage(String imageUrl) {
        // Check if chatId and currentChat are initialized
        if (chatId == null || currentChat == null) {
            Toast.makeText(requireContext(), "Chưa thể gửi tin nhắn. Đang tải cuộc trò chuyện...", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- START BLOCK CHECK LOGIC ---
        if (currentChat.getBlocked() != null && currentChat.getBlocked()) {
            Toast.makeText(requireContext(), "Cuộc trò chuyện này đã bị chặn. Bạn không thể gửi tin nhắn.", Toast.LENGTH_LONG).show();
            return; // PREVENT SENDING MESSAGE
        }
        // --- END BLOCK CHECK LOGIC ---

        String messageId = chatMessagesRef.push().getKey();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()); // Use SimpleDateFormat

        Message message = new Message();
        message.setSender_id(currentUserId);
        message.setTimestamp(timestamp);
        message.setType("image");
        message.setImageUrl(imageUrl);
        message.setText(null); // No text for image message

        if (messageId != null) {
            chatMessagesRef.child(messageId).setValue(message)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Image message sent: " + imageUrl);
                        etMessageInput.setText(""); // Clear input
                        updateChatLastMessage("[Hình ảnh]"); // Update last message
                        incrementUnreadCountForOtherUser(); // Update unread count for recipient
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send image message: " + e.getMessage());
                        Toast.makeText(requireContext(), "Không thể gửi tin nhắn ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    //region Permission Launchers
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    takePicture();
                } else {
                    Toast.makeText(requireContext(), "Quyền máy ảnh bị từ chối.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> galleryPermissionLauncherTiramisu =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    pickImage();
                } else {
                    Toast.makeText(requireContext(), "Quyền đọc ảnh bị từ chối.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> galleryPermissionLauncherLegacy =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    pickImage();
                } else {
                    Toast.makeText(requireContext(), "Quyền đọc bộ nhớ bị từ chối.", Toast.LENGTH_SHORT).show();
                }
            });
    //endregion

    // FR-4.1.3: Block/Report function
    private void toggleBlockChat() {
        if (chatId == null) {
            Toast.makeText(requireContext(), "Cuộc trò chuyện chưa được tạo.", Toast.LENGTH_SHORT).show();
            return;
        }

        chatsRef.child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Chat chat = snapshot.getValue(Chat.class);
                if (chat != null) {
                    boolean currentBlockedStatus = chat.getBlocked() != null ? chat.getBlocked() : false;
                    chatsRef.child(chatId).child("blocked").setValue(!currentBlockedStatus)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(requireContext(), !currentBlockedStatus ? "Cuộc trò chuyện đã bị chặn." : "Cuộc trò chuyện đã được bỏ chặn.", Toast.LENGTH_SHORT).show();
                                // Update UI immediately after blocking/unblocking
                                // This will trigger the listener in loadChatDetails and update currentChat
                                // and then updateChatHeader will be called.
                            })
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Không thể cập nhật trạng thái chặn: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read chat status for blocking: " + error.getMessage());
                Toast.makeText(requireContext(), "Lỗi khi kiểm tra trạng thái chặn.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // New method to handle reporting and unreporting
    private void toggleReportChat() {
        if (chatId == null || currentChat == null) {
            Toast.makeText(requireContext(), "Cuộc trò chuyện chưa được tạo hoặc đang tải.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean currentReportedStatus = currentChat.getReported() != null ? currentChat.getReported() : false;

        if (currentReportedStatus) {
            // If already reported, unreport (no confirmation needed)
            updateReportStatus(false, null); // Pass null for reason when unreporting
            recordAdminLog("unreported_chat", chatId, null); // Log unreport action
        } else {
            // If not reported, ask for confirmation before reporting with reasons
            final String[] reportReasons = {"Lừa đảo/gian lận", "Nội dung không phù hợp", "Spam"};
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Báo cáo cuộc trò chuyện vì:");
            builder.setItems(reportReasons, (dialog, which) -> {
                String selectedReason = reportReasons[which];
                updateReportStatus(true, selectedReason); // Update status with selected reason
                recordAdminLog("reported_chat", chatId, selectedReason); // Log report action with reason
            });
            builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
            builder.show();
        }
    }

    private void updateReportStatus(boolean newStatus, @Nullable String reason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("reported", newStatus);
        if (newStatus) { // Only add reason if reporting
            updates.put("reportReason", reason);
            updates.put("reportTimestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
        } else { // Clear reason and timestamp if unreporting
            updates.put("reportReason", null);
            updates.put("reportTimestamp", null);
        }

        chatsRef.child(chatId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), newStatus ? "Cuộc trò chuyện đã được báo cáo." : "Cuộc trò chuyện đã được bỏ báo cáo.", Toast.LENGTH_SHORT).show();
                    // UI will be updated automatically via loadChatDetails listener
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Không thể cập nhật trạng thái báo cáo: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void recordAdminLog(String action, String targetId, @Nullable String reason) {
        DatabaseReference adminLogsRef = FirebaseDatabase.getInstance().getReference("admin_logs");
        String logId = adminLogsRef.push().getKey();
        if (logId != null) {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("action", action);
            logEntry.put("admin_id", currentUserId); // Store reporter's user_id
            logEntry.put("target_id", targetId);
            if (reason != null) {
                logEntry.put("reason", reason); // Store report reason
            }
            // Use SimpleDateFormat for String timestamp
            logEntry.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
            adminLogsRef.child(logId).setValue(logEntry)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Admin log recorded: " + action + " for " + targetId + (reason != null ? " (Reason: " + reason + ")" : "")))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to record admin log: " + e.getMessage()));
        }
    }
}
