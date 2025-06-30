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
import java.util.Locale;

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
    private CircleImageView ivOtherUserProfileToolbar;

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
    private String user1IdInChat; // Lưu trữ user_1 của chat
    private String user2IdInChat; // Lưu trữ user_2 của chat
    private Chat currentChat; // Khai báo biến để lưu trữ đối tượng Chat hiện tại
    private NavController navController;

    // ActivityResultLauncher cho việc chụp ảnh và chọn ảnh từ thư viện
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private Uri imageUri; // URI tạm thời để lưu ảnh chụp từ camera
    private Set<String> sensitiveWords;
    private OkHttpClient okHttpClient;

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
        okHttpClient = new OkHttpClient();

        if (chatId == null) {
            // Nếu chatId chưa tồn tại, tìm hoặc tạo mới
            findOrCreateChat();
        } else {
            // Đã có chatId, thiết lập chatMessagesRef và load chi tiết chat
            chatMessagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId);
            // Khi mở chat, đặt unreadCount của mình về 0
            // Chúng ta cần lấy thông tin user_1 và user_2 trước khi reset
            // Và cũng cần lắng nghe chi tiết chat để cập nhật currentChat
            loadChatDetails(); // Gọi hàm này để thiết lập listener cho chat details
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

        initializeSensitiveWords();
    }

    // Phương thức mới để khởi tạo danh sách từ nhạy cảm
    private void initializeSensitiveWords() {
        sensitiveWords = new HashSet<>();
        // Thêm các từ nhạy cảm của bạn vào đây (nên viết thường để kiểm tra không phân biệt chữ hoa/thường)
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
        // Thêm nhiều từ khác tùy theo quy định ứng dụng của bạn
        // Trong một ứng dụng thực tế, bạn có thể tải danh sách này từ Firebase Remote Config
        // hoặc một nguồn từ xa để dễ dàng cập nhật mà không cần cập nhật ứng dụng.
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
        progressDialog.setMessage("Đang xử lý..."); // Tin nhắn mặc định
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
        ivReportChat.setOnClickListener(v -> toggleReportChat());
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
                        loadChatDetails(); // Load chi tiết chat và đặt listener
                        listenForMessages(); // Bắt đầu lắng nghe tin nhắn
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
                                    loadChatDetails(); // Load chi tiết chat và đặt listener
                                    listenForMessages(); // Bắt đầu lắng nghe tin nhắn
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
                    loadChatDetails(); // Load chi tiết chat và đặt listener
                    listenForMessages();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create new chat: " + e.getMessage());
                    Toast.makeText(requireContext(), "Không thể tạo cuộc trò chuyện mới.", Toast.LENGTH_SHORT).show();
                });
    }

    // Phương thức để lắng nghe chi tiết chat (bao gồm trạng thái chặn)
    private void loadChatDetails() {
        if (chatId == null) {
            Log.w(TAG, "chatId is null, cannot load chat details.");
            return;
        }
        chatsRef.child(chatId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentChat = snapshot.getValue(Chat.class); // Cập nhật biến currentChat
                if (currentChat != null) {
                    user1IdInChat = currentChat.getUser_1();
                    user2IdInChat = currentChat.getUser_2();
                    resetUnreadCountForCurrentUser(); // Reset khi mở chat
                    updateChatHeader(currentChat); // Cập nhật UI toolbar (ví dụ icon chặn)
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load chat details: " + error.getMessage());
                Toast.makeText(requireContext(), "Lỗi khi tải chi tiết cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Phương thức mới để kiểm tra tin nhắn có chứa từ nhạy cảm hay không
    private boolean isMessageSensitive(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        // Chuyển tin nhắn về chữ thường để kiểm tra không phân biệt chữ hoa/thường
        String lowerCaseMessage = message.toLowerCase(Locale.getDefault());
        // Tách tin nhắn thành các từ. Sử dụng regex để xử lý khoảng trắng và dấu câu.
        // Ví dụ: "đồ.ngu" sẽ được tách thành "đồ", "ngu"
        String[] words = lowerCaseMessage.split("\\s+|\\p{Punct}");

        for (String word : words) {
            // Loại bỏ khoảng trắng thừa ở đầu/cuối mỗi từ sau khi tách
            String cleanedWord = word.trim();
            if (sensitiveWords.contains(cleanedWord)) {
                return true; // Tìm thấy từ nhạy cảm
            }
        }
        return false; // Không tìm thấy từ nhạy cảm nào
    }

    // Phương thức mới để hiển thị hộp thoại cảnh báo nội dung nhạy cảm
    private void showSensitiveContentWarning(String messageText, String type) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cảnh báo nội dung nhạy cảm")
                .setMessage("Tin nhắn của bạn có thể chứa từ ngữ không phù hợp. Bạn có muốn:")
                .setPositiveButton("Gửi dù sao", (dialog, which) -> {
                    // Người dùng chọn vẫn gửi, gọi phương thức gửi tin nhắn thực sự
                    actuallySendMessage(messageText, type);
                })
                .setNeutralButton("Chỉnh sửa", (dialog, which) -> {
                    // Người dùng chọn chỉnh sửa, giữ nguyên văn bản trong EditText
                    dialog.dismiss(); // Đóng hộp thoại
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    // Người dùng chọn hủy, xóa văn bản trong EditText
                    etMessageInput.setText("");
                    dialog.dismiss(); // Đóng hộp thoại
                })
                .show();
    }
    private void updateChatHeader(Chat chat) {
        // Update block status UI (giữ nguyên hoặc đã có)
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

        // Update report status UI (Phần mới cần thêm)
        // Đảm bảo rằng bạn có các drawable tương ứng: ic_report và ic_report_off (hoặc icon bạn chọn)
        if (chat.getReported() != null && chat.getReported()) {
            ivReportChat.setImageResource(R.drawable.ic_flag_filled); // Giả sử icon đã báo cáo
            // Có thể thêm Toast nếu muốn hiển thị khi chat đã được báo cáo
        } else {
            ivReportChat.setImageResource(R.drawable.ic_flag_outline); // Giả sử icon chưa báo cáo / bỏ báo cáo
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
                // Sắp xếp danh sách tin nhắn theo timestamp
                Collections.sort(messageList, new Comparator<Message>() {
                    @Override
                    public int compare(Message m1, Message m2) {
                        // So sánh timestamp. Giả sử timestamp là String theo định dạng ISO 8601
                        // Nếu timestamp là Long (ServerValue.TIMESTAMP), thì so sánh trực tiếp
                        if (m1.getTimestamp() == null || m2.getTimestamp() == null) {
                            return 0; // Xử lý trường hợp null
                        }
                        // Nếu timestamp là String, bạn cần parse hoặc đảm bảo định dạng so sánh được
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

        if (chatId == null || currentChat == null) {
            Toast.makeText(requireContext(), "Chưa thể gửi tin nhắn. Đang tải cuộc trò chuyện...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra trạng thái chặn trước tiên
        if (currentChat.getBlocked() != null && currentChat.getBlocked()) {
            Toast.makeText(requireContext(), "Cuộc trò chuyện này đã bị chặn. Bạn không thể gửi tin nhắn.", Toast.LENGTH_LONG).show();
            return;
        }

        // Chỉ kiểm tra từ nhạy cảm đối với tin nhắn dạng văn bản
        if ("text".equals(type)) {
            if (isMessageSensitive(messageText)) {
                // Nếu tin nhắn nhạy cảm, hiển thị cảnh báo và dừng lại
                showSensitiveContentWarning(messageText, type);
                return; // Quan trọng: dừng lại ở đây để đợi người dùng quyết định
            }
        }

        // Nếu không phải là tin nhắn văn bản, hoặc là văn bản nhưng không nhạy cảm, hoặc người dùng đã chọn "Gửi dù sao"
        // thì tiếp tục gửi tin nhắn
        actuallySendMessage(messageText, type);
    }

    // Phương thức mới chứa logic gửi tin nhắn thực sự
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
            // Lưu ý: Logic tải ảnh và gửi tin nhắn ảnh được xử lý riêng bởi uploadImageAndSendMessage
            // Hàm này (actuallySendMessage) sẽ được gọi bởi sendImageMessage(String imageUrl) sau khi ảnh được tải lên.
            // Do đó, biến messageText ở đây sẽ là imageUrl nếu type là "image".
            // Đảm bảo rằng bạn truyền đúng imageUrl vào đây nếu hàm này được gọi từ sendImageMessage.
            // Tuy nhiên, với cấu trúc hiện tại, sendMessage(String type) chỉ xử lý type="text"
            // và uploadImageAndSendMessage xử lý type="image".
            // Vậy, đoạn if ("image".equals(type)) { ... } có thể không cần thiết ở đây nếu hàm này chỉ gọi cho text.
            // Nếu bạn muốn hàm này xử lý cả ảnh, bạn cần truyền imageUrl vào và đặt imageUrl cho message.
            // Với code hiện tại, sendMessage("text") sẽ gọi hàm này. sendMessage("image") thì gọi uploadImageAndSendMessage.
            // Để đơn giản, hãy giả định actuallySendMessage chỉ được gọi cho tin nhắn TEXT.
            // Nếu bạn cần kiểm tra cả ảnh, logic phức tạp hơn (ví dụ, kiểm tra metadata ảnh hoặc dùng AI)
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
                        updateChatLastMessage(messageText); // Cập nhật tin nhắn cuối cùng trong chat list
                        incrementUnreadCountForOtherUser(); // Tăng số tin nhắn chưa đọc cho người kia
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send message: " + e.getMessage());
                        Toast.makeText(requireContext(), "Không thể gửi tin nhắn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateChatLastMessage(String lastMessage) { // Bỏ tham số timestampString
        if (chatId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("lastMessage", lastMessage);
            // Luôn dùng SimpleDateFormat cho thời gian thực
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
            Toast.makeText(requireContext(), "Không có ảnh để tải lên.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra xem chatId và currentChat đã được khởi tạo chưa
        if (chatId == null || currentChat == null) {
            Toast.makeText(requireContext(), "Chưa thể gửi tin nhắn. Đang tải cuộc trò chuyện...", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- BẮT ĐẦU LOGIC KIỂM TRA CHẶN ---
        if (currentChat.getBlocked() != null && currentChat.getBlocked()) {
            Toast.makeText(requireContext(), "Cuộc trò chuyện này đã bị chặn. Bạn không thể gửi tin nhắn.", Toast.LENGTH_LONG).show();
            return; // NGĂN CHẶN GỬI TIN NHẮN
        }
        // --- KẾT THÚC LOGIC KIỂM TRA CHẶN ---


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

            // Sử dụng phương thức getFileExtensionFromMimeType mới (bạn sẽ thêm nó ở bước 5)
            String fileExtension = getFileExtensionFromMimeType(mimeType);
            String fileName = "chat_image_" + UUID.randomUUID().toString() + fileExtension;

            String cloudName = "dp6tzdsyt"; // Tên Cloudinary Cloud Name của bạn
            String uploadPreset = "TradeUp"; // Tên Cloudinary Unsigned Upload Preset của bạn

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
                    // Đảm bảo dismiss dialog trước khi xử lý phản hồi
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
                        requireActivity().runOnUiThread(() -> sendImageMessage(imageUrl)); // Gửi tin nhắn sau khi có URL ảnh

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
    // Phương thức hỗ trợ để lấy phần mở rộng tệp từ kiểu MIME
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
        // Kiểm tra xem chatId và currentChat đã được khởi tạo chưa
        if (chatId == null || currentChat == null) {
            Toast.makeText(requireContext(), "Chưa thể gửi tin nhắn. Đang tải cuộc trò chuyện...", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- BẮT ĐẦU LOGIC KIỂM TRA CHẶN ---
        if (currentChat.getBlocked() != null && currentChat.getBlocked()) {
            Toast.makeText(requireContext(), "Cuộc trò chuyện này đã bị chặn. Bạn không thể gửi tin nhắn.", Toast.LENGTH_LONG).show();
            return; // NGĂN CHẶN GỬI TIN NHẮN
        }
        // --- KẾT THÚC LOGIC KIỂM TRA CHẶN ---

        String messageId = chatMessagesRef.push().getKey();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()); // Sử dụng SimpleDateFormat

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
                        updateChatLastMessage("[Hình ảnh]"); // Cập nhật tin nhắn cuối cùng
                        incrementUnreadCountForOtherUser(); // Cập nhật unread count cho người nhận
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
                                // Cập nhật UI ngay lập tức sau khi chặn/bỏ chặn
                                // Điều này sẽ kích hoạt listener trong loadChatDetails và cập nhật currentChat
                                // và sau đó updateChatHeader sẽ được gọi.
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

    // Phương thức mới để xử lý báo cáo và bỏ báo cáo
    private void toggleReportChat() {
        if (chatId == null || currentChat == null) {
            Toast.makeText(requireContext(), "Cuộc trò chuyện chưa được tạo hoặc đang tải.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean currentReportedStatus = currentChat.getReported() != null ? currentChat.getReported() : false;

        if (currentReportedStatus) {
            // Nếu đã báo cáo, thì bỏ báo cáo (không cần xác nhận)
            updateReportStatus(false);
            recordAdminLog("unreported_chat", chatId); // Ghi log hành động bỏ báo cáo
        } else {
            // Nếu chưa báo cáo, hỏi xác nhận trước khi báo cáo
            new AlertDialog.Builder(requireContext())
                    .setTitle("Báo cáo cuộc trò chuyện")
                    .setMessage("Bạn có chắc chắn muốn báo cáo cuộc trò chuyện này vì nội dung không phù hợp?")
                    .setPositiveButton("Báo cáo", (dialog, which) -> {
                        updateReportStatus(true);
                        recordAdminLog("reported_chat", chatId); // Ghi log hành động báo cáo
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    private void updateReportStatus(boolean newStatus) {
        chatsRef.child(chatId).child("reported").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), newStatus ? "Cuộc trò chuyện đã được báo cáo." : "Cuộc trò chuyện đã được bỏ báo cáo.", Toast.LENGTH_SHORT).show();
                    // UI sẽ được cập nhật tự động qua loadChatDetails listener
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Không thể cập nhật trạng thái báo cáo: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void recordAdminLog(String action, String targetId) {
        DatabaseReference adminLogsRef = FirebaseDatabase.getInstance().getReference("admin_logs");
        String logId = adminLogsRef.push().getKey();
        if (logId != null) {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("action", action);
            logEntry.put("admin_id", currentUserId); // Lưu user_id của người báo cáo
            logEntry.put("target_id", targetId);
            // Sử dụng SimpleDateFormat cho timestamp kiểu String
            logEntry.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
            adminLogsRef.child(logId).setValue(logEntry)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Admin log recorded: " + action + " for " + targetId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to record admin log: " + e.getMessage()));
        }
    }
}