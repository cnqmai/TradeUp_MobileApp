package com.example.tradeup.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.activity.LoginActivity;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.User; // NEW: Import User model to get display name
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot; // NEW: For Firebase data fetching
import com.google.firebase.database.DatabaseError; // NEW: For Firebase data fetching
import com.google.firebase.database.FirebaseDatabase; // NEW: For Firebase data fetching
import com.google.firebase.database.ValueEventListener; // NEW: For Firebase data fetching
import com.google.firebase.database.Query; // NEW: For Firebase query

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class UserListingsFragment extends Fragment {

    private static final String TAG = "UserListingsFragment";

    private RecyclerView recyclerView;
    private UserItemsAdapter adapter; // Đổi tên adapter để rõ ràng hơn
    private TextView textNoItems;
    private TextView tvUserNameListings; // NEW: TextView để hiển thị tên người dùng
    private ImageView ivBackButton; // NEW: Nút quay lại
    private FirebaseHelper firebaseHelper;
    private List<Item> items = new ArrayList<>();
    private NavController navController;
    private String targetUserId; // ID của người dùng mà chúng ta đang xem tin đăng của họ
    private String currentUserId; // ID của người dùng hiện tại (để ẩn nút chỉnh sửa nếu xem tin đăng của mình)

    // Interface để Adapter/ViewHolder có thể gọi lại Fragment
    public interface OnItemActionCallback {
        void onItemClick(Item item);
        // Không cần onEditClick hay onItemDeleted ở đây vì đây là tin đăng của người khác
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_user_listings, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_user_items); // Cập nhật ID
        textNoItems = view.findViewById(R.id.text_no_items_user_listings); // Cập nhật ID
        tvUserNameListings = view.findViewById(R.id.tv_user_name_listings); // Cập nhật ID
        ivBackButton = view.findViewById(R.id.iv_back_button_user_listings); // Cập nhật ID

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        firebaseHelper = new FirebaseHelper();
        Log.d(TAG, "FirebaseHelper initialized.");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            currentUserId = null; // Người dùng chưa đăng nhập
        }

        // Lấy targetUserId từ arguments
        if (getArguments() != null) {
            targetUserId = getArguments().getString("userId");
            Log.d(TAG, "Received targetUserId: " + targetUserId);
        } else {
            Log.e(TAG, "No userId received in arguments for UserListingsFragment!");
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID người dùng.", Toast.LENGTH_SHORT).show();
            // Điều hướng quay lại nếu không có userId
            if (navController != null) { // Kiểm tra navController trước khi sử dụng
                navController.navigateUp();
            }
            return view;
        }

        // Khởi tạo adapter và gán cho RecyclerView ngay lập tức
        // Truyền currentUserId vào adapter để nó biết có nên hiển thị các nút chỉnh sửa/xóa hay không
        adapter = new UserItemsAdapter(items, firebaseHelper, currentUserId, new OnItemActionCallback() {
            @Override
            public void onItemClick(Item item) {
                if (navController != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("itemId", item.getId());
                    navController.navigate(R.id.action_userListingsFragment_to_itemDetailFragment, bundle);
                } else {
                    Log.e(TAG, "NavController is null on item click.");
                }
            }
        });
        recyclerView.setAdapter(adapter);
        Log.d(TAG, "RecyclerView adapter initialized and set in onCreateView.");

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");
        if (navController == null) {
            navController = Navigation.findNavController(view);
            Log.d(TAG, "NavController initialized in onViewCreated.");
        }

        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
            }
        });

        // Tải tên người dùng và các mặt hàng
        loadUserNameAndItems(targetUserId);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called. Reloading items to ensure fresh data.");
        // Không cần loadMyItems() ở đây, loadUserItems() sẽ được gọi trong loadUserNameAndItems()
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called. Clearing view references.");

        recyclerView = null;
        textNoItems = null;
        tvUserNameListings = null;
        ivBackButton = null;
        adapter = null;
        navController = null;
    }

    private void loadUserNameAndItems(String userId) {
        Log.d(TAG, "loadUserNameAndItems() called for user: " + userId);
        if (userId == null) {
            Log.w(TAG, "Target User ID is null, cannot load user name and items.");
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID người dùng.", Toast.LENGTH_SHORT).show();
            if (navController != null) {
                navController.navigateUp();
            }
            return;
        }

        // Tải tên người dùng
        FirebaseDatabase.getInstance().getReference("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null && user.getDisplay_name() != null) {
                                tvUserNameListings.setText("Tin đăng của " + user.getDisplay_name());
                            } else {
                                tvUserNameListings.setText("Tin đăng của Người dùng");
                            }
                            // Sau khi tải tên, tải các mặt hàng
                            loadUserItems(userId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded()) {
                            Log.e(TAG, "Failed to load user name: " + error.getMessage());
                            tvUserNameListings.setText("Tin đăng của Người dùng");
                            Toast.makeText(getContext(), "Lỗi khi tải tên người dùng.", Toast.LENGTH_SHORT).show();
                            loadUserItems(userId); // Vẫn cố gắng tải mặt hàng ngay cả khi tên lỗi
                        }
                    }
                });
    }


    private void loadUserItems(String userId) {
        Log.d(TAG, "loadUserItems() called for user ID: " + userId);
        if (userId == null) {
            Log.w(TAG, "User ID is null, cannot load items.");
            Toast.makeText(getContext(), "Không tìm thấy ID người dùng để tải tin đăng.", Toast.LENGTH_SHORT).show();
            textNoItems.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        firebaseHelper.getUserItems(userId, new FirebaseHelper.DbReadCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> fetchedItems) {
                if (!isAdded()) return; // Đảm bảo fragment vẫn được attach

                Log.d(TAG, "getUserItems onSuccess. Fetched " + (fetchedItems != null ? fetchedItems.size() : 0) + " items.");
                items.clear();
                if (fetchedItems != null) {
                    items.addAll(fetchedItems);
                }
                adapter.notifyDataSetChanged();

                if (items.isEmpty()) {
                    Log.d(TAG, "Items list is empty. Showing no items message.");
                    textNoItems.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    Log.d(TAG, "Items list is NOT empty. Showing RecyclerView.");
                    textNoItems.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return; // Đảm bảo fragment vẫn được attach
                Log.e(TAG, "getUserItems onFailure: " + errorMessage);
                Toast.makeText(getContext(), "Lỗi khi tải tin đăng của người dùng: " + errorMessage, Toast.LENGTH_SHORT).show();
                textNoItems.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    // --- Static Inner Adapter Class (Adapted from MyItemsAdapter) ---
    // Đổi tên từ MyItemsAdapter sang UserItemsAdapter để rõ ràng hơn
    private static class UserItemsAdapter extends RecyclerView.Adapter<UserItemsAdapter.ItemViewHolder> {
        private List<Item> items;
        private FirebaseHelper firebaseHelper;
        private OnItemActionCallback callback;
        private String currentUserId; // Để biết người dùng hiện tại có phải chủ sở hữu item không

        public UserItemsAdapter(List<Item> items, FirebaseHelper firebaseHelper, String currentUserId, OnItemActionCallback callback) {
            this.items = items;
            this.firebaseHelper = firebaseHelper;
            this.currentUserId = currentUserId;
            this.callback = callback;
            Log.d(TAG, "UserItemsAdapter initialized with " + items.size() + " items.");
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.d(TAG, "onCreateViewHolder called.");
            // Sử dụng layout item_card_view của bạn
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_view, parent, false);
            return new ItemViewHolder(view, firebaseHelper, callback, currentUserId);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
            Item item = items.get(position);
            Log.d(TAG, "onBindViewHolder called for position: " + position + ", item ID: " + item.getId());
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        // --- Static ViewHolder Class (Adapted from MyItemsAdapter.ItemViewHolder) ---
        static class ItemViewHolder extends RecyclerView.ViewHolder {
            ImageView ivItemImage;
            TextView tvItemTitle, tvItemPrice, tvItemStatus, tvItemAnalytics;
            // Removed btnEditItem, btnDeleteItem, btnChangeStatus as they are not needed for other users' listings
            // Button btnEditItem, btnDeleteItem, btnChangeStatus; // Removed

            private FirebaseHelper firebaseHelper;
            private OnItemActionCallback callback;
            private String currentUserId; // Để so sánh với ownerId của item

            public ItemViewHolder(@NonNull View itemView, FirebaseHelper firebaseHelper, OnItemActionCallback callback, String currentUserId) {
                super(itemView);
                this.firebaseHelper = firebaseHelper;
                this.callback = callback;
                this.currentUserId = currentUserId; // Nhận currentUserId
                Log.d(TAG, "ItemViewHolder initialized.");

                ivItemImage = itemView.findViewById(R.id.iv_item_image);
                tvItemTitle = itemView.findViewById(R.id.tv_item_title);
                tvItemPrice = itemView.findViewById(R.id.tv_item_price);
                tvItemStatus = itemView.findViewById(R.id.tv_item_status);
                tvItemAnalytics = itemView.findViewById(R.id.tv_item_analytics);

                // Ẩn các nút chỉnh sửa/xóa/thay đổi trạng thái nếu chúng tồn tại trong item_card_view
                // và người dùng hiện tại không phải là chủ sở hữu của tin đăng.
                // Nếu các nút này không có trong item_card_view, bạn có thể bỏ qua phần này.
                Button btnEditItem = itemView.findViewById(R.id.btn_edit_item);
                Button btnDeleteItem = itemView.findViewById(R.id.btn_delete_item);
                Button btnChangeStatus = itemView.findViewById(R.id.btn_change_status);

                if (btnEditItem != null) btnEditItem.setVisibility(View.GONE);
                if (btnDeleteItem != null) btnDeleteItem.setVisibility(View.GONE);
                if (btnChangeStatus != null) btnChangeStatus.setVisibility(View.GONE);
            }

            public void bind(Item item) {
                Log.d(TAG, "ItemViewHolder bind called for item: " + item.getId());
                tvItemTitle.setText(item.getTitle());

                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                currencyFormatter.setMaximumFractionDigits(0);
                tvItemPrice.setText("Giá: " + currencyFormatter.format(item.getPrice()));

                tvItemStatus.setText("Trạng thái: " + item.getStatus());

                if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                    Object firstPhoto = item.getPhotos().get(0);
                    if (firstPhoto instanceof String) {
                        Log.d(TAG, "Loading image for item " + item.getId() + ": " + (String) firstPhoto);
                        Glide.with(itemView.getContext())
                                .load((String) firstPhoto)
                                .placeholder(R.drawable.img_placeholder)
                                .error(R.drawable.img_error)
                                .into(ivItemImage);
                    } else {
                        Log.e(TAG, "Photo URL is not a String for item: " + item.getId() + ". Type: " + (firstPhoto != null ? firstPhoto.getClass().getName() : "null"));
                        Glide.with(itemView.getContext())
                                .load(R.drawable.img_placeholder)
                                .into(ivItemImage);
                    }
                } else {
                    Log.d(TAG, "No photos found for item: " + item.getId() + ". Loading placeholder.");
                    Glide.with(itemView.getContext())
                            .load(R.drawable.img_placeholder)
                            .into(ivItemImage);
                }

                Log.d(TAG, "Fetching analytics for item: " + item.getId());
                firebaseHelper.getItemAnalytics(item.getId(), new FirebaseHelper.DbReadCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> analyticsData) {
                        Log.d(TAG, "getItemAnalytics onSuccess for item: " + item.getId() + ". Data: " + analyticsData);
                        long views = 0;
                        if (analyticsData.containsKey("views") && analyticsData.get("views") instanceof Long) {
                            views = (Long) analyticsData.get("views");
                        } else if (analyticsData.containsKey("views") && analyticsData.get("views") instanceof Number) {
                            views = ((Number) analyticsData.get("views")).longValue();
                        }

                        long chatsStarted = 0;
                        if (analyticsData.containsKey("chats_started") && analyticsData.get("chats_started") instanceof Long) {
                            chatsStarted = (Long) analyticsData.get("chats_started");
                        } else if (analyticsData.containsKey("chats_started") && analyticsData.get("chats_started") instanceof Number) {
                            chatsStarted = ((Number) analyticsData.get("chats_started")).longValue();
                        }

                        long offersMade = 0;
                        if (analyticsData.containsKey("offers_made") && analyticsData.get("offers_made") instanceof Long) {
                            offersMade = (Long) analyticsData.get("offers_made");
                        } else if (analyticsData.containsKey("offers_made") && analyticsData.get("offers_made") instanceof Number) {
                            offersMade = ((Number) analyticsData.get("offers_made")).longValue();
                        }

                        tvItemAnalytics.setText(String.format("Lượt xem: %d | Chats: %d | Đề nghị: %d", views, chatsStarted, offersMade));
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "getItemAnalytics onFailure for item " + item.getId() + ": " + errorMessage);
                        tvItemAnalytics.setText("Lượt xem: N/A | Tương tác: N/A");
                    }
                });

                // Chỉ thiết lập OnClickListener cho itemView, không phải các nút chỉnh sửa/xóa
                itemView.setOnClickListener(v -> {
                    Log.d(TAG, "ItemView clicked for item: " + item.getId());
                    if (callback != null) {
                        callback.onItemClick(item);
                    }
                });
            }
            // Các phương thức showDeleteConfirmationDialog, performDeleteItem, showChangeStatusDialog, updateItemStatus
            // không cần thiết ở đây vì chúng liên quan đến chỉnh sửa/xóa tin đăng của chính mình.
            // Nếu bạn muốn giữ chúng cho mục đích khác (ví dụ: admin có thể chỉnh sửa tin đăng của người khác),
            // bạn sẽ cần logic kiểm tra quyền. Tuy nhiên, với mục đích hiển thị tin đăng của người khác, chúng ta sẽ loại bỏ chúng.
        }
    }
}
