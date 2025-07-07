package com.example.tradeup.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.adapter.NotificationAdapter;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.Notification;
import com.example.tradeup.model.Transaction;
import com.google.android.material.tabs.TabLayout;
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
import java.util.List;
import java.util.Objects;
import java.util.Date;
import java.util.Locale;

public class NotificationFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private static final String TAG = "NotificationFragment";

    private RecyclerView rvNotifications;
    private TextView tvNoNotifications;
    private ImageView ivSettingsButton; // Renamed from ivBackButton
    private TabLayout tabLayout; // Added tabLayout reference

    private NotificationAdapter notificationAdapter;
    private List<Notification> allNotifications; // Store all fetched notifications
    private List<Notification> currentFilteredNotifications; // Store currently displayed notifications

    private DatabaseReference notificationsRef;
    private String currentUserId;
    private NavController navController;

    private String currentFilterTab = "all"; // Default filter

    public NotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");

        allNotifications = new ArrayList<>();
        currentFilteredNotifications = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        initViews(view);
        setupRecyclerView();
        setupTabLayout(); // Setup tabs before fetching notifications
        setupListeners();
        fetchNotifications();
    }

    private void initViews(View view) {
        rvNotifications = view.findViewById(R.id.rv_notifications);
        tvNoNotifications = view.findViewById(R.id.tv_no_notifications);
        ivSettingsButton = view.findViewById(R.id.iv_settings_button_notifications); // Corrected ID
        tabLayout = view.findViewById(R.id.tab_layout_notifications); // Initialize tabLayout
    }

    private void setupRecyclerView() {
        notificationAdapter = new NotificationAdapter(requireContext(), currentFilteredNotifications, this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(notificationAdapter);
    }

    private void setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_all)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_messages)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_offers)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.tab_updates)));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentFilterTab = "all";
                        break;
                    case 1:
                        currentFilterTab = "new_message"; // Matches notification type
                        break;
                    case 2:
                        currentFilterTab = "new_offer"; // Matches notification type
                        break;
                    case 3:
                        currentFilterTab = "listing_update"; // Custom type for listing updates
                        break;
                }
                filterNotificationsByTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Do nothing
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Do nothing
            }
        });
    }

    private void setupListeners() {
        // ivBackButton is removed based on UI image, so its listener is removed
        ivSettingsButton.setOnClickListener(v -> {
            // Handle settings button click, e.g., navigate to settings fragment
            Toast.makeText(requireContext(), getString(R.string.settings_button_clicked), Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchNotifications() {
        Query query = notificationsRef.orderByChild("user_id").equalTo(currentUserId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment not added, skipping UI update.");
                    return;
                }
                allNotifications.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                        Notification notification = notificationSnapshot.getValue(Notification.class);
                        if (notification != null) {
                            notification.setId(notificationSnapshot.getKey());
                            allNotifications.add(notification);
                        }
                    }
                }

                // Sort all notifications by timestamp (newest first)
                Collections.sort(allNotifications, (n1, n2) -> {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));

                        Date date1 = sdf.parse(n1.getTimestamp());
                        Date date2 = sdf.parse(n2.getTimestamp());

                        return date2.compareTo(date1); // Newest first
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing timestamp for sorting: " + e.getMessage());
                        return 0; // If error, keep original order
                    }
                });

                filterNotificationsByTab(); // Apply filter after fetching and sorting all
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) {
                    Log.w(TAG, "Fragment not added, skipping error message.");
                    return;
                }
                Log.e(TAG, "Failed to load notifications: " + error.getMessage());
                Toast.makeText(requireContext(), getString(R.string.toast_error_loading_notifications, error.getMessage()), Toast.LENGTH_SHORT).show();
                tvNoNotifications.setText(getString(R.string.no_notifications_message));
                tvNoNotifications.setVisibility(View.VISIBLE);
                rvNotifications.setVisibility(View.GONE);
                notificationAdapter.setNotifications(new ArrayList<>()); // Clear adapter on error
            }
        });
    }

    private void filterNotificationsByTab() {
        currentFilteredNotifications.clear();
        if (currentFilterTab.equals("all")) {
            currentFilteredNotifications.addAll(allNotifications);
        } else {
            for (Notification notification : allNotifications) {
                // Special handling for "Offers" tab to include multiple offer-related types
                if (currentFilterTab.equals("new_offer") && (notification.getType().equals("new_offer") ||
                        notification.getType().equals("offer_accepted") ||
                        notification.getType().equals("counter_offer") ||
                        notification.getType().equals("buyer_responded_offer"))) {
                    currentFilteredNotifications.add(notification);
                } else if (notification.getType() != null && notification.getType().equals(currentFilterTab)) {
                    currentFilteredNotifications.add(notification);
                }
            }
        }

        if (currentFilteredNotifications.isEmpty()) {
            tvNoNotifications.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            tvNoNotifications.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
        notificationAdapter.setNotifications(currentFilteredNotifications);
    }


    @Override
    public void onNotificationClick(Notification notification) {
        // Handle when a user clicks on a notification
        // Mark notification as read
        if (notification.getId() != null) {
            notificationsRef.child(notification.getId()).child("read").setValue(true)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Notification marked as read: " + notification.getId());
                        // Update the UI immediately without re-fetching all data
                        notification.setRead(true);
                        notificationAdapter.notifyDataSetChanged(); // Notify adapter to re-bind item
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to mark notification as read: " + e.getMessage()));
        }

        // Navigate user based on notification type (FR-4.2.2)
        if (notification.getType() != null && notification.getRelated_id() != null) {
            Bundle bundle = new Bundle();
            switch (notification.getType()) {
                case "new_message":
                    bundle.putString("chatId", notification.getRelated_id());
                    // You need a mechanism to get otherUserId from chatId or pass it from the beginning
                    // For now, let's just pass chatId. OtherUserId will be fetched inside ChatDetailFragment
                    bundle.putString("otherUserId", "unknown"); // Needs improvement
                    navController.navigate(R.id.action_notificationFragment_to_chatDetailFragment, bundle);
                    break;
                case "new_offer":
                case "offer_accepted":
                case "counter_offer":
                case "buyer_responded_offer":
                    String offerIdFromNotification = notification.getRelated_id();
                    if (offerIdFromNotification != null) {
                        Bundle offerDetailBundle = new Bundle();
                        offerDetailBundle.putString("offerId", offerIdFromNotification);
                        navController.navigate(R.id.action_notificationFragment_to_offerDetailFragment, offerDetailBundle);
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.toast_offer_id_not_found), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Related ID (offerId) is null in notification for offer type.");
                    }
                    break;
                case "payment_required": // <-- ĐÂY LÀ TRƯỜNG HỢP CẦN KIỂM TRA
                    String transactionId = notification.getRelated_id(); // Lấy transactionId từ related_id
                    if (transactionId != null) {
                        // Cần fetch thông tin giao dịch từ Firebase để có đủ dữ liệu cho PaymentFragment
                        FirebaseDatabase.getInstance().getReference("transactions").child(transactionId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot transactionSnapshot) {
                                        Transaction transaction = transactionSnapshot.getValue(Transaction.class);
                                        if (transaction != null) {
                                            // Kiểm tra xem người dùng hiện tại có phải là người mua của giao dịch này không
                                            if (currentUserId.equals(transaction.getBuyer_id())) {
                                                // *** THÊM LOGIC KIỂM TRA TRẠNG THÁI GIAO DỊCH ĐÃ HOÀN TẤT CHƯA ***
                                                if (transaction.getCompletion_timestamp() != null) {
                                                    // Giao dịch đã hoàn tất, không cho phép thanh toán lại
                                                    Toast.makeText(requireContext(), getString(R.string.toast_transaction_already_completed), Toast.LENGTH_LONG).show();
                                                    // Tùy chọn: Điều hướng đến TransactionDetailFragment để xem chi tiết
                                                    // Bundle completedTransactionBundle = new Bundle();
                                                    // completedTransactionBundle.putString("transactionId", transactionId);
                                                    // navController.navigate(R.id.action_notificationFragment_to_transactionDetailFragment, completedTransactionBundle);
                                                } else {
                                                    // Giao dịch CHƯA hoàn tất, tiến hành thanh toán
                                                    // Bây giờ, fetch thông tin Item bằng itemId từ transaction
                                                    String itemId = transaction.getItem_id();
                                                    if (itemId != null) {
                                                        FirebaseDatabase.getInstance().getReference("items").child(itemId)
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot itemSnapshot) {
                                                                        Item item = itemSnapshot.getValue(Item.class);
                                                                        if (item != null) {
                                                                            // Chuẩn bị các đối số cho PaymentFragment
                                                                            // Bundle bundle = new Bundle(); // Không khởi tạo lại bundle ở đây
                                                                            bundle.putString("itemId", transaction.getItem_id());
                                                                            bundle.putString("transactionId", transaction.getTransaction_id());
                                                                            bundle.putString("sellerId", transaction.getSeller_id());
                                                                            bundle.putString("buyerId", transaction.getBuyer_id());

                                                                            // Lấy URL của hình ảnh đầu tiên từ danh sách
                                                                            if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                                                                                bundle.putString("itemImageUrl", item.getPhotos().get(0));
                                                                                Log.d(TAG, "Fetched itemImageUrl from Item: " + item.getPhotos().get(0));
                                                                            } else {
                                                                                bundle.putString("itemImageUrl", null);
                                                                                Log.d(TAG, "Item has no image URLs.");
                                                                            }

                                                                            Long priceFromFirebase = transaction.getFinal_price();
                                                                            Log.d(TAG, "Fetched final_price from Firebase: " + (priceFromFirebase != null ? priceFromFirebase : "null"));
                                                                            bundle.putLong("finalPrice", priceFromFirebase != null ? priceFromFirebase : 0L);
                                                                            bundle.putString("itemTitle", transaction.getItem_title()); // Giả sử Transaction model có item_title hoặc lấy từ item.getName()

                                                                            // Điều hướng trực tiếp đến PaymentFragment
                                                                            navController.navigate(R.id.action_notificationFragment_to_paymentFragment, bundle);
                                                                        } else {
                                                                            Toast.makeText(requireContext(), getString(R.string.item_not_found), Toast.LENGTH_SHORT).show();
                                                                            Log.e(TAG, "Item not found for transaction: " + transactionId + " with itemId: " + itemId);
                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                                        Log.e(TAG, "Failed to load item for payment notification: " + error.getMessage());
                                                                        Toast.makeText(requireContext(), getString(R.string.error_loading_item), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                    } else {
                                                        Toast.makeText(requireContext(), getString(R.string.toast_item_id_missing_in_transaction), Toast.LENGTH_SHORT).show();
                                                        Log.e(TAG, "Item ID is null in transaction: " + transactionId);
                                                    }
                                                }
                                            } else {
                                                // Nếu người dùng hiện tại không phải người mua (ví dụ: người bán click thông báo này)
                                                Toast.makeText(requireContext(), getString(R.string.toast_payment_notification_seller_info), Toast.LENGTH_LONG).show();
                                            }
                                        } else {
                                            Toast.makeText(requireContext(), getString(R.string.toast_transaction_not_found), Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Failed to load transaction for payment notification: " + error.getMessage());
                                        Toast.makeText(requireContext(), getString(R.string.toast_error_loading_transaction_for_payment), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.toast_transaction_id_missing), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Related ID (transactionId) is null for payment required notification.");
                    }
                    break;
                case "promotion":
                    Toast.makeText(requireContext(), getString(R.string.toast_promotion_notification, notification.getRelated_id()), Toast.LENGTH_SHORT).show();
                    break;
                case "reported_chat":
                    Toast.makeText(requireContext(), getString(R.string.toast_reported_chat_notification, notification.getRelated_id()), Toast.LENGTH_SHORT).show();
                    break;
                case "listing_update": // New type for listing updates
                    // Assuming related_id for listing_update is itemId
                    bundle.putString("itemId", notification.getRelated_id());
                    navController.navigate(R.id.action_notificationFragment_to_itemDetailFragment, bundle);
                    break;
                default:
                    Toast.makeText(requireContext(), getString(R.string.toast_general_notification, notification.getTitle()), Toast.LENGTH_SHORT).show();
                    break;
            }
        } else {
            Toast.makeText(requireContext(), getString(R.string.toast_general_notification, notification.getTitle()), Toast.LENGTH_SHORT).show();
        }
    }
}
