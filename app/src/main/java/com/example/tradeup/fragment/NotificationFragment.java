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
import com.example.tradeup.model.Notification;
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
    private ImageView ivBackButton;

    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;

    private DatabaseReference notificationsRef;
    private String currentUserId;
    private NavController navController;

    public NotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        notificationsRef = FirebaseDatabase.getInstance().getReference("notifications");
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
        setupListeners();
        fetchNotifications();
    }

    private void initViews(View view) {
        rvNotifications = view.findViewById(R.id.rv_notifications);
        tvNoNotifications = view.findViewById(R.id.tv_no_notifications);
        ivBackButton = view.findViewById(R.id.iv_back_button_notifications);
    }

    private void setupRecyclerView() {
        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(requireContext(), notificationList, this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(notificationAdapter);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> navController.navigateUp());
    }

    private void fetchNotifications() {
        // Lấy thông báo cho người dùng hiện tại, sắp xếp theo thời gian giảm dần
        Query query = notificationsRef.orderByChild("user_id").equalTo(currentUserId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                        Notification notification = notificationSnapshot.getValue(Notification.class);
                        if (notification != null) {
                            // Firebase không tự set ID khi getValue, cần set thủ công
                            notification.setId(notificationSnapshot.getKey());
                            notificationList.add(notification);
                        }
                    }
                }

                // Sắp xếp lại theo timestamp giảm dần (mới nhất lên đầu)
                Collections.sort(notificationList, (n1, n2) -> {
                    // Cần parse timestamp string để so sánh
                    // Sử dụng cùng định dạng "yyyy-MM-dd'T'HH:mm:ss'Z'"
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                        Date date1 = sdf.parse(n1.getTimestamp());
                        Date date2 = sdf.parse(n2.getTimestamp());
                        return date2.compareTo(date1); // Mới nhất lên đầu
                    } catch (java.text.ParseException e) {
                        Log.e(TAG, "Error parsing timestamp for sorting: " + e.getMessage());
                        return 0; // Giữ nguyên thứ tự nếu lỗi parse
                    }
                });

                if (notificationList.isEmpty()) {
                    tvNoNotifications.setVisibility(View.VISIBLE);
                    rvNotifications.setVisibility(View.GONE);
                } else {
                    tvNoNotifications.setVisibility(View.GONE);
                    rvNotifications.setVisibility(View.VISIBLE);
                }
                notificationAdapter.setNotifications(notificationList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load notifications: " + error.getMessage());
                Toast.makeText(requireContext(), "Lỗi khi tải thông báo: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onNotificationClick(Notification notification) {
        // Xử lý khi người dùng nhấn vào một thông báo
        // Đánh dấu thông báo là đã đọc
        if (notification.getId() != null) {
            notificationsRef.child(notification.getId()).child("read").setValue(true)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification marked as read: " + notification.getId()))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to mark notification as read: " + e.getMessage()));
        }

        // Điều hướng người dùng dựa trên loại thông báo (FR-4.2.2)
        // Ví dụ:
        if (notification.getType() != null && notification.getRelated_id() != null) {
            Bundle bundle = new Bundle();
            switch (notification.getType()) {
                case "new_message":
                    bundle.putString("chatId", notification.getRelated_id());
                    // Bạn cần một cơ chế để lấy otherUserId từ chatId hoặc pass nó từ đầu
                    // Tạm thời, bạn có thể lấy otherUserId từ trong ChatDetailFragment hoặc pass null
                    // Hoặc tốt hơn, lưu cả user_1 và user_2 vào notification hoặc chỉ cần related_id
                    // For now, let's just pass chatId. OtherUserId will be fetched inside ChatDetailFragment
                    bundle.putString("otherUserId", "unknown"); // Cần cải thiện
                    navController.navigate(R.id.action_notificationFragment_to_chatDetailFragment, bundle);
                    break;
                case "new_offer":
                case "offer_accepted":
                    bundle.putString("itemId", notification.getRelated_id()); // related_id là item_id hoặc offer_id
                    // Nếu là offer_id, bạn cần tra cứu item_id từ offer_id
                    // Giả sử related_id là item_id:
                    navController.navigate(R.id.action_notificationFragment_to_itemDetailFragment, bundle); // Cần hành động này
                    break;
                case "promotion":
                    // Có thể điều hướng đến một danh sách sản phẩm theo category
                    // hoặc chỉ hiển thị thông báo.
                    // bundle.putString("categoryId", notification.getRelated_id());
                    // navController.navigate(R.id.action_notificationFragment_to_itemListFragment, bundle);
                    Toast.makeText(requireContext(), "Đây là thông báo khuyến mãi về: " + notification.getRelated_id(), Toast.LENGTH_SHORT).show();
                    break;
                case "reported_chat":
                    // Đây thường là thông báo cho admin, không phải người dùng thông thường
                    // Người dùng có thể chỉ xem thông báo
                    Toast.makeText(requireContext(), "Cuộc trò chuyện đã được báo cáo: " + notification.getRelated_id(), Toast.LENGTH_SHORT).show();
                    break;
                // Thêm các trường hợp khác
                default:
                    Toast.makeText(requireContext(), "Thông báo: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
                    break;
            }
        } else {
            Toast.makeText(requireContext(), "Thông báo: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }
}