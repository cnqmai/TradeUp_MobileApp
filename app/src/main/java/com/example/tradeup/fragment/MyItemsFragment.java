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
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MyItemsFragment extends Fragment {

    private static final String TAG = "MyItemsFragment";

    private RecyclerView recyclerView;
    private MyItemsAdapter adapter;
    private TextView textNoItems;
    private Button btnAddNewItemEmptyState;
    private FirebaseHelper firebaseHelper;
    private List<Item> items = new ArrayList<>();
    private NavController navController;
    private String currentUserId;

    // Interface để Adapter/ViewHolder có thể gọi lại Fragment
    // cho các hành động cần NavController hoặc tải lại dữ liệu.
    public interface OnItemActionCallback {
        void onItemClick(Item item);
        void onEditClick(Item item);
        void onItemDeleted();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_my_items, container, false);

        recyclerView = view.findViewById(R.id.recycler_my_items);
        textNoItems = view.findViewById(R.id.text_no_items);
        btnAddNewItemEmptyState = view.findViewById(R.id.btn_add_new_item_empty_state);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        firebaseHelper = new FirebaseHelper();
        Log.d(TAG, "FirebaseHelper initialized.");

        // Lấy ID người dùng ngay khi Fragment được tạo view
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            Log.d(TAG, "Current user ID obtained: " + currentUserId);
        } else {
            Log.w(TAG, "No current user logged in in onCreateView.");
            Toast.makeText(getContext(), "Bạn cần đăng nhập để xem tin đăng của mình.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            // Có thể thêm requireActivity().finish(); nếu bạn muốn đóng Activity cha
        }

        // Khởi tạo adapter và gán cho RecyclerView ngay lập tức
        adapter = new MyItemsAdapter(items, firebaseHelper, new OnItemActionCallback() {
            @Override
            public void onItemClick(Item item) {
                if (navController != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("itemId", item.getId());
                    navController.navigate(R.id.action_myItemsFragment_to_itemDetailFragment, bundle);
                } else {
                    Log.e(TAG, "NavController is null on item click.");
                }
            }

            @Override
            public void onEditClick(Item item) {
                if (navController != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("itemId", item.getId());
                    navController.navigate(R.id.action_myItemsFragment_to_editItemFragment, bundle);
                } else {
                    Log.e(TAG, "NavController is null on edit click.");
                }
            }

            @Override
            public void onItemDeleted() {
                Log.d(TAG, "onItemDeleted callback received. Reloading items.");
                loadMyItems();
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

        btnAddNewItemEmptyState.setOnClickListener(v -> {
            Log.d(TAG, "btnAddNewItemEmptyState clicked. Navigating to AddItemFragment.");
            if (navController != null) {
                navController.navigate(MyItemsFragmentDirections.actionMyItemsFragmentToAddItemFragment());
            } else {
                Log.e(TAG, "NavController is null on Add New Item click.");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called. Reloading items to ensure fresh data.");
        loadMyItems();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called. Clearing view references.");

        recyclerView = null;
        textNoItems = null;
        btnAddNewItemEmptyState = null;
        adapter = null;
        navController = null;
        // firebaseHelper không cần đặt null ở đây vì nó không phải là View hay Adapter
    }

    private void loadMyItems() {
        Log.d(TAG, "loadMyItems() called.");
        if (currentUserId == null) {
            // Nếu currentUserId vẫn null (có thể do người dùng chưa đăng nhập hoặc bị đăng xuất)
            Log.w(TAG, "currentUserId is null, cannot load items. Redirecting to LoginActivity.");
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập. Vui lòng đăng nhập để xem tin đăng của bạn.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            return;
        }

        Log.d(TAG, "Fetching items for current user ID: " + currentUserId);

        firebaseHelper.getUserItems(currentUserId, new FirebaseHelper.DbReadCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> fetchedItems) {
                Log.d(TAG, "getUserItems onSuccess. Fetched " + (fetchedItems != null ? fetchedItems.size() : 0) + " items.");
                items.clear();
                if (fetchedItems != null) {
                    items.addAll(fetchedItems);
                }
                adapter.notifyDataSetChanged();

                if (items.isEmpty()) {
                    Log.d(TAG, "Items list is empty. Showing no items message and add button.");
                    textNoItems.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    Log.d(TAG, "Items list is NOT empty. Showing RecyclerView.");
                    textNoItems.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
                // Luôn hiển thị nút "Đăng tin mới", bất kể danh sách có rỗng hay không
                btnAddNewItemEmptyState.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "getUserItems onFailure: " + errorMessage);
                Toast.makeText(getContext(), "Lỗi khi tải tin đăng của bạn: " + errorMessage, Toast.LENGTH_SHORT).show();
                textNoItems.setVisibility(View.VISIBLE);
                btnAddNewItemEmptyState.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    // --- Static Inner Adapter Class ---
    private static class MyItemsAdapter extends RecyclerView.Adapter<MyItemsAdapter.ItemViewHolder> {
        private List<Item> items;
        private FirebaseHelper firebaseHelper;
        private OnItemActionCallback callback; // Callback cho các hành động của Fragment

        public MyItemsAdapter(List<Item> items, FirebaseHelper firebaseHelper, OnItemActionCallback callback) {
            this.items = items;
            this.firebaseHelper = firebaseHelper;
            this.callback = callback;
            Log.d(TAG, "MyItemsAdapter initialized with " + items.size() + " items.");
        }

        @NonNull
        @Override
        public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.d(TAG, "onCreateViewHolder called.");
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card_view, parent, false);
            return new ItemViewHolder(view, firebaseHelper, callback, items);
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

        // --- Static ViewHolder Class ---
        static class ItemViewHolder extends RecyclerView.ViewHolder {
            ImageView ivItemImage;
            TextView tvItemTitle, tvItemPrice, tvItemStatus, tvItemAnalytics;
            Button btnEditItem, btnDeleteItem, btnChangeStatus;

            private FirebaseHelper firebaseHelper;
            private OnItemActionCallback callback;
            private List<Item> itemsList; // Tham chiếu đến danh sách items của adapter

            public ItemViewHolder(@NonNull View itemView, FirebaseHelper firebaseHelper, OnItemActionCallback callback, List<Item> itemsList) {
                super(itemView);
                this.firebaseHelper = firebaseHelper;
                this.callback = callback;
                this.itemsList = itemsList;

                ivItemImage = itemView.findViewById(R.id.iv_item_image);
                tvItemTitle = itemView.findViewById(R.id.tv_item_title);
                tvItemPrice = itemView.findViewById(R.id.tv_item_price);
                tvItemStatus = itemView.findViewById(R.id.tv_item_status);
                tvItemAnalytics = itemView.findViewById(R.id.tv_item_analytics);
                btnEditItem = itemView.findViewById(R.id.btn_edit_item);
                btnDeleteItem = itemView.findViewById(R.id.btn_delete_item);
                btnChangeStatus = itemView.findViewById(R.id.btn_change_status);
                Log.d(TAG, "ItemViewHolder initialized.");
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

                btnEditItem.setOnClickListener(v -> {
                    Log.d(TAG, "Edit button clicked for item: " + item.getId());
                    if (callback != null) {
                        callback.onEditClick(item);
                    }
                });

                btnDeleteItem.setOnClickListener(v -> {
                    Log.d(TAG, "Delete button clicked for item: " + item.getId());
                    showDeleteConfirmationDialog(item);
                });

                btnChangeStatus.setOnClickListener(v -> {
                    Log.d(TAG, "Change Status button clicked for item: " + item.getId());
                    showChangeStatusDialog(item);
                });

                itemView.setOnClickListener(v -> {
                    Log.d(TAG, "ItemView clicked for item: " + item.getId());
                    if (callback != null) {
                        callback.onItemClick(item);
                    }
                });
            }

            private void showDeleteConfirmationDialog(Item item) {
                Log.d(TAG, "Showing delete confirmation dialog for item: " + item.getId());
                new AlertDialog.Builder(itemView.getContext()) // Sử dụng itemView.getContext()
                        .setTitle("Xóa Tin Đăng")
                        .setMessage("Bạn có chắc muốn xóa tin đăng này không? Hành động này không thể hoàn tác.")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            Log.d(TAG, "Delete confirmed for item: " + item.getId());
                            performDeleteItem(item);
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> Log.d(TAG, "Delete cancelled for item: " + item.getId()))
                        .show();
            }

            private void performDeleteItem(Item item) {
                String itemId = item.getId();
                Log.d(TAG, "performDeleteItem called for item ID: " + itemId);
                if (itemId == null) {
                    Log.e(TAG, "Item ID is null for deletion.");
                    Toast.makeText(itemView.getContext(), "Không tìm thấy ID tin đăng để xóa.", Toast.LENGTH_SHORT).show();
                    return;
                }

                firebaseHelper.deleteItem(itemId, new FirebaseHelper.DbWriteCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Item deleted successfully: " + itemId);
                        Toast.makeText(itemView.getContext(), "Tin đăng đã được xóa thành công.", Toast.LENGTH_SHORT).show();
                        if (callback != null) {
                            callback.onItemDeleted(); // Gọi callback để Fragment tải lại dữ liệu
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error deleting item " + itemId + ": " + errorMessage);
                        Toast.makeText(itemView.getContext(), "Không thể xóa tin đăng: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            private void showChangeStatusDialog(Item item) {
                Log.d(TAG, "Showing change status dialog for item: " + item.getId());
                // Lưu ý: getResources() chỉ có thể được gọi từ Context hoặc Fragment.
                // Ở đây, ItemViewHolder cần Context để truy cập tài nguyên.
                final String[] statuses = itemView.getContext().getResources().getStringArray(R.array.item_statuses_array);
                int currentStatusIndex = -1;
                for (int i = 0; i < statuses.length; i++) {
                    if (statuses[i].equals(item.getStatus())) {
                        currentStatusIndex = i;
                        break;
                    }
                }
                Log.d(TAG, "Current status for item " + item.getId() + " is: " + item.getStatus() + ", index: " + currentStatusIndex);

                new AlertDialog.Builder(itemView.getContext()) // Sử dụng itemView.getContext()
                        .setTitle("Đổi Trạng Thái Tin Đăng")
                        .setSingleChoiceItems(statuses, currentStatusIndex, (dialog, which) -> {
                            String newStatus = statuses[which];
                            Log.d(TAG, "Selected new status for item " + item.getId() + ": " + newStatus);
                            if (!newStatus.equals(item.getStatus())) {
                                updateItemStatus(item, newStatus);
                            } else {
                                Log.d(TAG, "New status is same as current status. No update needed.");
                            }
                            dialog.dismiss();
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> Log.d(TAG, "Change status cancelled for item: " + item.getId()))
                        .show();
            }

            private void updateItemStatus(Item item, String newStatus) {
                String itemId = item.getId();
                Log.d(TAG, "updateItemStatus called for item ID: " + itemId + ", new status: " + newStatus);
                if (itemId == null) {
                    Log.e(TAG, "Item ID is null for status update.");
                    Toast.makeText(itemView.getContext(), "Không tìm thấy ID tin đăng để cập nhật trạng thái.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("status", newStatus);
                String updatedAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());
                updates.put("updated_at", updatedAt);
                Log.d(TAG, "Updating item " + itemId + " with status: " + newStatus + " and updated_at: " + updatedAt);

                firebaseHelper.updateItem(itemId, updates, new FirebaseHelper.DbWriteCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Status updated successfully for item " + itemId + " to " + newStatus);
                        Toast.makeText(itemView.getContext(), "Trạng thái tin đăng đã cập nhật thành " + newStatus, Toast.LENGTH_SHORT).show();
                        item.setStatus(newStatus);
                        // Cập nhật item cụ thể trong adapter thay vì tải lại toàn bộ list để hiệu suất tốt hơn
                        // Cần lấy adapter từ RecyclerView hoặc thông báo cho Fragment để gọi notifyItemChanged trên adapter của nó.
                        // Cách tốt nhất là truyền danh sách 'items' và gọi notifyItemChanged trực tiếp trên nó.
                        int position = itemsList.indexOf(item);
                        if (position != -1) {
                            //itemsList.set(position, item); // Đã cập nhật trạng thái của item trực tiếp
                            // Gọi notifyDataSetChanged() hoặc notifyItemChanged(position) trên adapter chính.
                            // Do adapter là private trong Fragment, ta cần một cách để gọi từ đây.
                            // Callback là cách tốt nhất, nhưng ở đây có thể cập nhật trực tiếp item trong list
                            // và sau đó gọi notifyItemChanged từ adapter nếu cần.
                            // Tuy nhiên, việc truyền itemsList vào ItemViewHolder có thể gây rò rỉ bộ nhớ nếu không cẩn thận.
                            // Cách đơn giản hơn là reload toàn bộ danh sách qua callback như delete.
                            // Để hiệu suất, bạn có thể truyền lại Adapter instance hoặc sử dụng LiveData/ViewModel.
                            // Hiện tại, tôi sẽ không gọi notifyItemChanged trực tiếp từ đây để tránh phức tạp và rò rỉ.
                            // Việc gọi loadMyItems() sau khi cập nhật trạng thái là an toàn nhưng kém hiệu quả hơn.
                            // Một giải pháp khác là đảm bảo item trong list của adapter được cập nhật.
                            // Vì item là tham chiếu, khi item.setStatus(newStatus) được gọi, item trong list cũng được cập nhật.
                            // Chỉ cần thông báo cho adapter biết rằng một item đã thay đổi.
                            // getAdapterPosition() chỉ hoạt động nếu ViewHolder không phải static và có thể truy cập adapter.
                            // Vì ItemViewHolder là static, nó không thể gọi getAdapterPosition() trên adapter của nó.
                            // Cần một cơ chế callback để Fragment xử lý.

                            // Cách hiệu quả hơn: cập nhật item trong list và gọi notifyItemChanged từ Fragment
                            // Nhưng để đơn giản và an toàn với cấu trúc static hiện tại, chúng ta sẽ reload toàn bộ list.
                            // (Nếu hiệu suất là vấn đề, cần tái cấu trúc mạnh hơn với LiveData/ViewModel.)
                            if (callback != null) {
                                callback.onItemDeleted(); // Kích hoạt tải lại toàn bộ danh sách, coi như item đã thay đổi
                            }
                        } else {
                            Log.e(TAG, "Item not found in list for status update.");
                            if (callback != null) {
                                callback.onItemDeleted(); // Tải lại toàn bộ danh sách nếu không tìm thấy item
                            }
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Error updating status for item " + itemId + ": " + errorMessage);
                        Toast.makeText(itemView.getContext(), "Không thể cập nhật trạng thái: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}