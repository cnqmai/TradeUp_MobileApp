package com.example.tradeup.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Item; // Đảm bảo import Item model
import com.example.tradeup.model.Offer;
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ItemDetailFragment extends Fragment {

    private static final String TAG = "ItemDetailFragment";

    private TextView tvTitle, tvPrice, tvDescription, tvCategory, tvCondition, tvLocation,
            tvItemBehavior, tvTags, tvStatus, tvSellerName, tvSellerRating, tvViewsCount;

    // >>> THÊM CÁC TRƯỜNG TEXTVIEW MỚI CHO RATING CỦA SẢN PHẨM <<<
    private TextView tvItemAverageRating;
    private TextView tvItemRatingCount;

    private Button btnChatSeller, btnMakeOffer, btnAddToFavorites;
    private ViewPager2 vpItemImages;

    private String itemId;
    private FirebaseHelper firebaseHelper;

    private String sellerId;
    private String currentUserId; // Để lưu trữ User ID hiện tại
    private Item currentItem; // Biến mới để lưu trữ thông tin Item hiện tại

    public ItemDetailFragment() {
        // Required empty public constructor
    }

    public static ItemDetailFragment newInstance(String itemId) {
        ItemDetailFragment fragment = new ItemDetailFragment();
        Bundle args = new Bundle();
        args.putString("itemId", itemId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called.");
        firebaseHelper = new FirebaseHelper();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (getArguments() != null) {
            itemId = getArguments().getString("itemId");
            Log.d(TAG, "itemId received in onCreate: " + itemId);
        } else {
            Log.w(TAG, "No arguments or itemId found in onCreate.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called.");
        View view = inflater.inflate(R.layout.fragment_item_detail, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called. Current itemId: " + itemId);
        if (itemId != null) {
            loadItemDetails(itemId);
            updateItemViews(itemId);
        } else {
            Toast.makeText(getContext(), "Không tìm thấy ID tin đăng.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onResume: itemId is null, popping back stack.");
            if (getParentFragmentManager() != null && getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        }
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tv_detail_title);
        tvPrice = view.findViewById(R.id.tv_detail_price);
        tvDescription = view.findViewById(R.id.tv_detail_description);
        tvCategory = view.findViewById(R.id.tv_detail_category);
        tvCondition = view.findViewById(R.id.tv_detail_condition);
        tvLocation = view.findViewById(R.id.tv_detail_location);
        tvItemBehavior = view.findViewById(R.id.tv_detail_item_behavior);
        tvTags = view.findViewById(R.id.tv_detail_tags);
        tvStatus = view.findViewById(R.id.tv_detail_status);
        tvSellerName = view.findViewById(R.id.tv_detail_seller_name);
        tvSellerRating = view.findViewById(R.id.tv_detail_seller_rating);
        tvViewsCount = view.findViewById(R.id.tv_detail_views_count);

        // >>> ÁNH XẠ CÁC TEXTVIEW MỚI CHO RATING CỦA SẢN PHẨM <<<
        tvItemAverageRating = view.findViewById(R.id.tv_detail_item_average_rating);
        tvItemRatingCount = view.findViewById(R.id.tv_detail_item_rating_count);


        btnChatSeller = view.findViewById(R.id.btn_detail_chat_seller);
        btnMakeOffer = view.findViewById(R.id.btn_detail_make_offer);
        btnAddToFavorites = view.findViewById(R.id.btn_detail_add_to_favorites);
        vpItemImages = view.findViewById(R.id.vp_item_images);

        btnChatSeller.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để trò chuyện.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (sellerId != null && !sellerId.isEmpty()) {
                startChatWithSeller(sellerId);
            } else {
                Toast.makeText(getContext(), "Không thể tìm thấy thông tin người bán.", Toast.LENGTH_SHORT).show();
            }
        });

        btnMakeOffer.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để gửi đề nghị.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (itemId == null || sellerId == null || sellerId.isEmpty()) {
                Toast.makeText(getContext(), "Không thể gửi đề nghị: Thiếu thông tin tin đăng hoặc người bán.", Toast.LENGTH_SHORT).show();
                return;
            }
            showMakeOfferDialog();
        });

        btnAddToFavorites.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(getContext(), "Vui lòng đăng nhập để thêm vào yêu thích.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (itemId != null && !itemId.isEmpty()) {
                toggleFavorite(itemId);
            } else {
                Toast.makeText(getContext(), "Không thể thêm vào yêu thích: Thiếu ID tin đăng.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startChatWithSeller(String otherUserId) {
        if (currentUserId == null || otherUserId == null || currentUserId.equals(otherUserId)) {
            Log.e(TAG, "Invalid chat participants: currentUserId or otherUserId is null/same.");
            Toast.makeText(getContext(), "Không thể bắt đầu cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        // Tìm kiếm cuộc trò chuyện hiện có
        // Query cả hai trường hợp user_1 = currentUserId AND user_2 = otherUserId
        // Hoặc user_1 = otherUserId AND user_2 = currentUserId
        Query query1 = chatsRef.orderByChild("user_1").equalTo(currentUserId);
        Query query2 = chatsRef.orderByChild("user_1").equalTo(otherUserId);

        query1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String chatId = findExistingChat(dataSnapshot, otherUserId, "user_2"); // Kiểm tra user_2

                if (chatId != null) {
                    navigateToChatDetail(chatId, otherUserId);
                } else {
                    // Nếu không tìm thấy, thử với query2 (swap user_1 và user_2)
                    query2.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {
                            String chatId2 = findExistingChat(dataSnapshot2, otherUserId, "user_2"); // Kiểm tra user_2
                            if (chatId2 != null) {
                                navigateToChatDetail(chatId2, otherUserId);
                            } else {
                                // Nếu vẫn không tìm thấy, tạo cuộc trò chuyện mới
                                createNewChat(otherUserId);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "Error checking chat (query2): " + databaseError.getMessage());
                            Toast.makeText(getContext(), "Lỗi khi kiểm tra cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error checking chat (query1): " + databaseError.getMessage());
                Toast.makeText(getContext(), "Lỗi khi kiểm tra cuộc trò chuyện.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String findExistingChat(DataSnapshot dataSnapshot, String targetUserId, String otherUserField) {
        for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
            String user1 = chatSnapshot.child("user_1").getValue(String.class);
            String user2 = chatSnapshot.child("user_2").getValue(String.class);
            if ((Objects.equals(user1, currentUserId) && Objects.equals(user2, targetUserId)) ||
                    (Objects.equals(user1, targetUserId) && Objects.equals(user2, currentUserId))) {
                return chatSnapshot.getKey();
            }
        }
        return null;
    }

    private void createNewChat(String otherUserId) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        String newChatId = chatsRef.push().getKey();

        if (newChatId == null) {
            Toast.makeText(getContext(), "Lỗi: Không thể tạo ID đề nghị.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo một bản ghi chat cơ bản
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("user_1", currentUserId);
        chatData.put("user_2", otherUserId);
        chatData.put("lastMessage", "Cuộc trò chuyện mới"); // Tin nhắn mặc định
        chatData.put("lastMessageTimestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));
        chatData.put("blocked", false);
        chatData.put("reported", false);
        // Không cần thêm "messages": {} ở đây, Firebase tự động tạo khi có tin nhắn đầu tiên
        // Nhưng bạn có thể thêm nó nếu muốn đảm bảo node con tồn tại ngay từ đầu:
        // chatData.put("messages", new HashMap<>()); // Thêm node 'messages' trống ban đầu

        chatsRef.child(newChatId).setValue(chatData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "New chat created: " + newChatId);
                    navigateToChatDetail(newChatId, otherUserId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create new chat: " + e.getMessage());
                    Toast.makeText(getContext(), "Lỗi khi tạo cuộc trò chuyện mới.", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToChatDetail(String chatId, String otherUserId) {
        Bundle bundle = new Bundle();
        bundle.putString("chatId", chatId);
        bundle.putString("otherUserId", otherUserId); // Truyền otherUserId để ChatDetailFragment có thể hiển thị tên người dùng
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_itemDetailFragment_to_chatDetailFragment, bundle);
    }

    private void showMakeOfferDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Gửi Đề nghị Giá");

        final EditText input = new EditText(getContext());
        input.setHint("Nhập giá đề nghị của bạn (ví dụ: 1200000)");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Gửi Đề nghị", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String offerPriceStr = input.getText().toString().trim();
                if (offerPriceStr.isEmpty()) {
                    Toast.makeText(getContext(), "Vui lòng nhập giá đề nghị.", Toast.LENGTH_SHORT).show();
                    return;
                }
                long offerPrice = Long.parseLong(offerPriceStr);
                createOffer(offerPrice);
            }
        });
        builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void createOffer(long offerPrice) {
        DatabaseReference offersRef = FirebaseDatabase.getInstance().getReference("offers");
        String offerId = offersRef.push().getKey(); // Tạo ID duy nhất cho đề nghị

        if (offerId == null) {
            Toast.makeText(getContext(), "Lỗi: Không thể tạo ID đề nghị.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        Offer offer = new Offer(
                offerId,
                itemId,
                currentUserId, // buyer_id
                sellerId,      // seller_id
                offerPrice,
                "pending",     // Trạng thái ban đầu
                null,          // counter_price ban đầu là null
                timestamp,
                timestamp      // updated_at ban đầu giống created_at
        );

        offersRef.child(offerId).setValue(offer)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Đề nghị đã được gửi thành công!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Offer created: " + offerId);

                    // >>> BẮT ĐẦU PHẦN THÊM MỚI: TẠO THÔNG BÁO CHO NGƯỜI BÁN <<<
                    if (sellerId != null && !sellerId.isEmpty() && currentItem != null) {
                        DatabaseReference notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
                        String notificationId = notificationsRef.push().getKey();

                        if (notificationId != null) {
                            Map<String, Object> notificationContent = new HashMap<>();
                            notificationContent.put("user_id", sellerId); // ID của người nhận thông báo (người bán)
                            notificationContent.put("title", "Đề nghị mới!");
                            notificationContent.put("body", "Có đề nghị mới cho sản phẩm \"" + currentItem.getTitle() + "\" của bạn.");
                            notificationContent.put("type", "new_offer");
                            notificationContent.put("related_id", offerId); // ID của offer mới tạo
                            String isoTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());
                            notificationContent.put("timestamp", isoTimestamp);
                            // Timestamp từ server
                            notificationContent.put("read", false); // Mặc định là chưa đọc

                            notificationsRef.child(notificationId).setValue(notificationContent)
                                    .addOnSuccessListener(aVoid1 -> Log.d(TAG, "Notification created for seller: " + sellerId))
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to create notification for seller: " + e.getMessage()));
                        }
                    } else {
                        Log.w(TAG, "Cannot create notification: Seller ID or Item data is missing.");
                    }
                    // >>> KẾT THÚC PHẦN THÊM MỚI <<<

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi khi gửi đề nghị: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to create offer: " + e.getMessage());
                });
    }

    private void toggleFavorite(String itemId) {
        DatabaseReference favoritesRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUserId)
                .child("favorites")
                .child(itemId);

        favoritesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Nếu đã có trong yêu thích, xóa đi
                    favoritesRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Đã xóa khỏi mục yêu thích.", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Item " + itemId + " removed from favorites.");
                                // Cập nhật UI của nút để thể hiện trạng thái (ví dụ: đổi icon)
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Lỗi khi xóa khỏi yêu thích: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Failed to remove item " + itemId + " from favorites: " + e.getMessage());
                            });
                } else {
                    // Nếu chưa có, thêm vào
                    favoritesRef.setValue(true) // Ghi đơn giản là true để đánh dấu có mặt
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Đã thêm vào mục yêu thích!", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Item " + itemId + " added to favorites.");
                                // Cập nhật UI của nút để thể hiện trạng thái (ví dụ: đổi icon)
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Lỗi khi thêm vào yêu thích: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Failed to add item " + itemId + " to favorites: " + e.getMessage());
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi kiểm tra yêu thích: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to check favorite status: " + error.getMessage());
            }
        });
    }

    private void loadItemDetails(String id) {
        Log.d(TAG, "loadItemDetails called for itemId: " + id);
        firebaseHelper.getItem(id, new FirebaseHelper.DbReadCallback<Item>() {
            @Override
            public void onSuccess(Item item) {
                if (item != null) {
                    currentItem = item; // Lưu item vào biến toàn cục mới
                    Log.d(TAG, "Item data loaded successfully for itemId: " + id + ", title: " + item.getTitle());
                    tvTitle.setText(item.getTitle());
                    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                    currencyFormatter.setMaximumFractionDigits(0);
                    tvPrice.setText("Giá: " + currencyFormatter.format(item.getPrice()));
                    tvDescription.setText(item.getDescription());
                    tvCategory.setText("Danh mục: " + item.getCategory());
                    tvCondition.setText("Tình trạng: " + item.getCondition());

                    if (item.getLocation() != null) {
                        tvLocation.setText("Vị trí: " + item.getLocation().getManual_address());
                    } else {
                        tvLocation.setText("Vị trí: Không xác định");
                    }

                    tvItemBehavior.setText("Cách thức giao dịch: " + (item.getItem_behavior() != null ? item.getItem_behavior() : "Chưa xác định"));
                    if (item.getTags() != null && !item.getTags().isEmpty()) {
                        tvTags.setText("Tags: " + android.text.TextUtils.join(", ", item.getTags()));
                    } else {
                        tvTags.setText("Tags: Không có");
                    }
                    tvStatus.setText("Trạng thái: " + item.getStatus());

                    // >>> THAY ĐỔI Ở ĐÂY: average_rating và rating_count <<<
                    if (item.getAverage_rating() != null && item.getRating_count() != null) {
                        tvItemAverageRating.setText(String.format(Locale.getDefault(), "Đánh giá SP: %.1f/5.0", item.getAverage_rating()));
                        tvItemRatingCount.setText(String.format(Locale.getDefault(), "(%d lượt)", item.getRating_count()));
                    } else {
                        tvItemAverageRating.setText("Đánh giá SP: N/A");
                        tvItemRatingCount.setText("(0 lượt)");
                    }

                    sellerId = item.getUser_id(); // Seller ID được gán ở đây
                    loadSellerInfo(sellerId);

                    if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                        Log.d(TAG, "Loading " + item.getPhotos().size() + " images.");
                        ImageSliderAdapter adapter = new ImageSliderAdapter(item.getPhotos());
                        vpItemImages.setAdapter(adapter);
                    } else {
                        Log.d(TAG, "No images found, using placeholder.");
                        List<String> placeholders = new ArrayList<>();
                        placeholders.add("android.resource://" + requireContext().getPackageName() + "/" + R.drawable.img_placeholder);
                        ImageSliderAdapter adapter = new ImageSliderAdapter(placeholders);
                        vpItemImages.setAdapter(adapter);
                    }

                    if (currentUserId != null && item.getCategory() != null) {
                        recordUserView(currentUserId, id, item.getCategory());
                    } else {
                        Log.w(TAG, "Cannot record user view: currentUserId or item category is null.");
                    }

                    // --- BỔ SUNG LOGIC HIỂN THỊ NÚT Ở ĐÂY ---
                    if (currentUserId != null && currentUserId.equals(sellerId)) {
                        // Nếu người dùng hiện tại là người bán của tin đăng này
                        btnChatSeller.setVisibility(View.GONE); // Ẩn nút Chat
                        btnMakeOffer.setVisibility(View.GONE);  // Ẩn nút Make Offer
                        btnAddToFavorites.setVisibility(View.GONE); // Người bán không thêm tin của mình vào yêu thích
                        Log.d(TAG, "Current user is seller. Hiding chat, offer, and favorite buttons.");
                    } else {
                        // Nếu không phải người bán, đảm bảo các nút hiển thị
                        btnChatSeller.setVisibility(View.VISIBLE);
                        btnMakeOffer.setVisibility(View.VISIBLE);
                        btnAddToFavorites.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Current user is buyer. Showing chat, offer, and favorite buttons.");
                    }
                    // --- KẾT THÚC BỔ SUNG ---

                } else {
                    Log.w(TAG, "Item data is null for itemId: " + id);
                    Toast.makeText(getContext(), "Không thể tải chi tiết tin đăng: Dữ liệu trống.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(getContext(), "Lỗi tải chi tiết tin đăng: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading item details: " + errorMessage);
            }
        });
    }

    private void loadSellerInfo(String userId) {
        Log.d(TAG, "loadSellerInfo called for userId: " + userId);
        firebaseHelper.getUserProfile(userId, new FirebaseHelper.DbReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    Log.d(TAG, "Seller info loaded: " + user.getDisplay_name());
                    tvSellerName.setText("Người bán: " + user.getDisplay_name());
                    // THAY ĐỔI TẠI ĐÂY: Dùng getAverage_rating() thay vì getRating()
                    Double averageRating = user.getAverage_rating();
                    if (averageRating != null) {
                        tvSellerRating.setText(String.format(Locale.getDefault(), "Đánh giá: %.1f/5.0", averageRating));
                    } else {
                        tvSellerRating.setText("Đánh giá: N/A");
                    }
                } else {
                    Log.w(TAG, "Seller data is null for userId: " + userId);
                    tvSellerName.setText("Người bán: Không xác định");
                    tvSellerRating.setText("Đánh giá: N/A");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load seller info: " + errorMessage);
                tvSellerName.setText("Người bán: Lỗi tải");
                tvSellerRating.setText("Đánh giá: Lỗi tải");
            }
        });
    }

    private void updateItemViews(String id) {
        Log.d(TAG, "updateItemViews called for itemId: " + id);
        firebaseHelper.incrementItemView(id, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "View count incremented successfully for item: " + id);
                firebaseHelper.getItemAnalytics(id, new FirebaseHelper.DbReadCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> analyticsData) {
                        if (analyticsData != null && analyticsData.containsKey("views")) {
                            long views = 0;
                            Object viewsObj = analyticsData.get("views");
                            if (viewsObj instanceof Long) {
                                views = (Long) viewsObj;
                            } else if (viewsObj instanceof Integer) {
                                views = ((Integer) viewsObj).longValue();
                            }
                            Log.d(TAG, "Updated views fetched: " + views);
                            tvViewsCount.setText("Lượt xem: " + views);
                        } else {
                            Log.d(TAG, "Analytics data or views not found for item: " + id);
                            tvViewsCount.setText("Lượt xem: 0");
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to fetch updated views for item " + id + ": " + errorMessage);
                        tvViewsCount.setText("Lượt xem: Lỗi tải");
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to increment view count for item " + id + ": " + errorMessage);
            }
        });
    }

    /**
     * Ghi nhận lượt xem danh mục của người dùng vào node user_activity.
     * Phương thức này sẽ được gọi mỗi khi người dùng xem chi tiết sản phẩm.
     * @param userId ID của người dùng hiện tại.
     * @param itemId ID của sản phẩm đang được xem.
     * @param category Danh mục của sản phẩm đang được xem.
     */
    private void recordUserView(String userId, String itemId, String category) {
        if (userId == null || itemId == null || category == null || category.isEmpty()) {
            Log.w(TAG, "Cannot record user view: missing userId, itemId or category.");
            return;
        }

        // Tùy chọn: Bạn có thể di chuyển logic này vào FirebaseHelper để quản lý tập trung hơn
        // Ví dụ: firebaseHelper.recordUserCategoryView(userId, category, new FirebaseHelper.DbWriteCallback() {...});

        FirebaseDatabase.getInstance().getReference("user_activity")
                .child(userId)
                .child("viewed_categories")
                .child(category)
                .child("last_viewed")
                .setValue(ServerValue.TIMESTAMP) // Ghi lại thời gian xem gần nhất
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User category view recorded for: " + category))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to record user category view: " + e.getMessage()));

        // Tùy chọn: Ghi nhận cả từng item đã xem nếu cần chi tiết hơn
        // FirebaseDatabase.getInstance().getReference("user_activity")
        //         .child(userId)
        //         .child("viewed_items")
        //         .child(itemId)
        //         .setValue(Map.of("timestamp", ServerValue.TIMESTAMP, "category", category))
        //         .addOnSuccessListener(aVoid -> Log.d(TAG, "User item view recorded for: " + itemId))
        //         .addOnFailureListener(e -> Log.e(TAG, "Failed to record user item view: " + e.getMessage()));
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called.");

        tvTitle = null;
        tvPrice = null;
        tvDescription = null;
        tvCategory = null;
        tvCondition = null;
        tvLocation = null;
        tvItemBehavior = null;
        tvTags = null;
        tvStatus = null;
        tvSellerName = null;
        tvSellerRating = null;
        tvViewsCount = null;
        tvItemAverageRating = null; // Giải phóng TextView mới
        tvItemRatingCount = null;   // Giải phóng TextView mới
        btnChatSeller = null;
        btnMakeOffer = null;
        btnAddToFavorites = null;
        vpItemImages = null;
        currentItem = null; // Giải phóng currentItem
    }

    private class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {
        private List<String> imageUrls;

        public ImageSliderAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_slider_item, parent, false);
            return new SliderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
            String imageUrl = imageUrls.get(position);
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.img_placeholder) // Placeholder image
                    .error(R.drawable.img_error) // Error image
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        public class SliderViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public SliderViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_slider_image); // Đảm bảo ID này chính xác trong image_slider_item.xml
            }
        }
    }
}
