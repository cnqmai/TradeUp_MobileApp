package com.example.tradeup.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query; // Import Query
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatDetailFragment extends Fragment {

    private static final String TAG = "ChatDetailFragment";

    private RecyclerView rvMessages;
    private EditText etMessageInput;
    private ImageView ivSendMessage;
    private ImageView ivAttachImage;
    private ImageView ivBackButton;
    private ImageView ivBlockChat, ivReportChat;
    private TextView tvOtherUserNameToolbar;
    private CircleImageView ivOtherUserProfileToolbar;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private DatabaseReference chatMessagesRef;
    private DatabaseReference chatsRef;
    private DatabaseReference usersRef;
    private StorageReference storageRef;

    private String currentUserId;
    private String chatId;
    private String otherUserId;
    private String otherUserName;
    private String user1IdInChat; // Lưu trữ user_1 của chat
    private String user2IdInChat; // Lưu trữ user_2 của chat


    private NavController navController;

    // ActivityResultLauncher cho việc chụp ảnh và chọn ảnh từ thư viện
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private Uri imageUri; // URI tạm thời để lưu ảnh chụp từ camera

    public ChatDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lấy các đối số từ navigation
        if (getArguments() != null) {
            chatId = getArguments().getString("chatId");
            otherUserId = getArguments().getString("otherUserId");
            otherUserName = getArguments().getString("otherUserName"); // Lấy tên người dùng khác
        }

        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        usersRef = FirebaseDatabase.getInstance().getReference("users");
        storageRef = FirebaseStorage.getInstance().getReference("chat_images");

        if (chatId == null) {
            // Nếu chatId chưa tồn tại, tìm hoặc tạo mới
            findOrCreateChat();
        } else {
            chatMessagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId);
            // Khi mở chat, đặt unreadCount của mình về 0
            // Chúng ta cần lấy thông tin user_1 và user_2 trước khi reset
            chatsRef.child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat != null) {
                        user1IdInChat = chat.getUser_1();
                        user2IdInChat = chat.getUser_2();
                        resetUnreadCountForCurrentUser();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load chat info for reset unread count: " + error.getMessage());
                }
            });

        }

        // Khởi tạo các ActivityResultLauncher
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
            // Nếu không có tên, lấy từ Firebase
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

        // Nếu đã có otherUserName từ Bundle, thử load ảnh ngay lập tức
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
        layoutManager.setStackFromEnd(true); // Để tin nhắn mới nhất hiển thị ở dưới cùng
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> navController.navigateUp());

        ivSendMessage.setOnClickListener(v -> sendMessage("text"));
        ivAttachImage.setOnClickListener(v -> showImagePickerDialog());

        ivBlockChat.setOnClickListener(v -> toggleBlockChat());
        ivReportChat.setOnClickListener(v -> reportChat());
    }

    private void findOrCreateChat() {
        // Logic để tìm chat ID giữa currentUserId và otherUserId
        // Nếu không tìm thấy, tạo chat mới
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
                        user1IdInChat = chat.getUser_1(); // Lưu user_1
                        user2IdInChat = chat.getUser_2(); // Lưu user_2
                        listenForMessages();
                        resetUnreadCountForCurrentUser();
                        break;
                    }
                }
                if (!chatFound) {
                    // Kiểm tra chiều ngược lại (otherUserId là user_1)
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
                                    user1IdInChat = chat.getUser_1(); // Lưu user_1
                                    user2IdInChat = chat.getUser_2(); // Lưu user_2
                                    listenForMessages();
                                    resetUnreadCountForCurrentUser();
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
        chatId = chatsRef.push().getKey(); // Tạo ID chat mới
        if (chatId == null) {
            Toast.makeText(requireContext(), "Không thể tạo ID cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sử dụng constructor mặc định và sau đó set các giá trị
        Chat newChat = new Chat();
        newChat.setChatId(chatId);
        newChat.setUser_1(currentUserId); // currentUserId là user_1
        newChat.setUser_2(otherUserId); // otherUserId là user_2
        newChat.setBlocked(false); // Mặc định là false
        newChat.setReported(false); // Mặc định là false
        newChat.setLastMessage("Hãy bắt đầu cuộc trò chuyện!");
        newChat.setLastMessageTimestamp(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
        newChat.setUser1UnreadCount(0); // Khởi tạo số tin nhắn chưa đọc cho user_1 (currentUserId)
        newChat.setUser2UnreadCount(0); // Khởi tạo số tin nhắn chưa đọc cho user_2 (otherUserId)

        // Lưu trữ ID của user1 và user2 trong biến cục bộ
        user1IdInChat = currentUserId;
        user2IdInChat = otherUserId;


        chatsRef.child(chatId).setValue(newChat)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New chat created: " + chatId);
                    chatMessagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId);
                    listenForMessages();
                    resetUnreadCountForCurrentUser();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create new chat: " + e.getMessage());
                    Toast.makeText(requireContext(), "Không thể tạo cuộc trò chuyện mới.", Toast.LENGTH_SHORT).show();
                });
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
                // Sắp xếp danh sách tin nhắn theo timestamp
                Collections.sort(messageList, new Comparator<Message>() {
                    @Override
                    public int compare(Message m1, Message m2) {
                        return m1.getTimestamp().compareTo(m2.getTimestamp());
                    }
                });

                messageAdapter.setMessages(messageList);
                rvMessages.scrollToPosition(messageList.size() - 1); // Cuộn xuống cuối
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
            message.setText(null);
            // imageUrl sẽ được set sau khi upload ảnh thành công
        }

        if (messageId != null) {
            chatMessagesRef.child(messageId).setValue(message)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Message sent: " + messageText);
                        etMessageInput.setText(""); // Xóa input sau khi gửi
                        updateChatLastMessage(messageText, timestamp); // Cập nhật tin nhắn cuối cùng cho chat
                        incrementUnreadCountForOtherUser(); // Cập nhật unread count cho người nhận
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send message: " + e.getMessage());
                        Toast.makeText(requireContext(), "Không thể gửi tin nhắn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateChatLastMessage(String lastMessage, String timestamp) {
        if (chatId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMessage", lastMessage);
            updates.put("lastMessageTimestamp", timestamp);
            chatsRef.child(chatId).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Chat last message updated."))
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


    // Logic xử lý gửi ảnh
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
            Toast.makeText(requireContext(), "Không thể tải ảnh.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), "Đang tải ảnh...", Toast.LENGTH_SHORT).show();

        StorageReference imageRef = storageRef.child(UUID.randomUUID().toString() + "." + getFileExtension(imageUri));
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String imageUrl = task.getResult().toString();
                        sendImageMessage(imageUrl);
                    } else {
                        Toast.makeText(requireContext(), "Lỗi lấy URL ảnh: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }))
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Tải ảnh thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendImageMessage(String imageUrl) {
        String messageId = chatMessagesRef.push().getKey();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

        Message message = new Message();
        message.setSender_id(currentUserId);
        message.setTimestamp(timestamp);
        message.setType("image");
        message.setImageUrl(imageUrl);
        message.setText(null); // Không có text cho tin nhắn ảnh

        if (messageId != null) {
            chatMessagesRef.child(messageId).setValue(message)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Image message sent: " + imageUrl);
                        etMessageInput.setText(""); // Xóa input
                        updateChatLastMessage("[Hình ảnh]", timestamp); // Cập nhật tin nhắn cuối cùng
                        incrementUnreadCountForOtherUser(); // Cập nhật unread count cho người nhận
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send image message: " + e.getMessage());
                        Toast.makeText(requireContext(), "Không thể gửi tin nhắn ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private String getFileExtension(Uri uri) {
        String extension;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(requireContext().getContentResolver().getType(uri));
        } else {
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new java.io.File(uri.getPath())).toString());
        }
        return extension;
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

    // FR-4.1.3: Chức năng chặn/báo cáo
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
                                // Cập nhật UI nếu cần
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

    private void reportChat() {
        if (chatId == null) {
            Toast.makeText(requireContext(), "Cuộc trò chuyện chưa được tạo.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Báo cáo cuộc trò chuyện")
                .setMessage("Bạn có chắc chắn muốn báo cáo cuộc trò chuyện này vì nội dung không phù hợp?")
                .setPositiveButton("Báo cáo", (dialog, which) -> {
                    chatsRef.child(chatId).child("reported").setValue(true)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(requireContext(), "Cuộc trò chuyện đã được báo cáo thành công.", Toast.LENGTH_SHORT).show();
                                // Có thể thêm logic ghi log vào admin_logs ở đây
                                recordAdminLog("reported_chat", chatId);
                            })
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Không thể báo cáo cuộc trò chuyện: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void recordAdminLog(String action, String targetId) {
        DatabaseReference adminLogsRef = FirebaseDatabase.getInstance().getReference("admin_logs");
        String logId = adminLogsRef.push().getKey();
        if (logId != null) {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("action", action);
            logEntry.put("admin_id", currentUserId); // Lưu user_id của người báo cáo
            logEntry.put("target_id", targetId);
            logEntry.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
            adminLogsRef.child(logId).setValue(logEntry)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Admin log recorded: " + action + " for " + targetId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to record admin log: " + e.getMessage()));
        }
    }
}