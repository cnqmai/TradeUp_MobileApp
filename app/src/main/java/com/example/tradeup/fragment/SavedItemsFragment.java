package com.example.tradeup.fragment;

import android.app.AlertDialog;
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

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Item;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SavedItemsFragment extends Fragment {

    private static final String TAG = "SavedItemsFragment";

    private RecyclerView recyclerView;
    private SavedItemsAdapter adapter;
    private TextView textNoSavedItems;
    private ImageView ivBackButton;

    private NavController navController;
    private FirebaseHelper firebaseHelper;
    private List<Item> savedItemsList; // List to hold saved Item objects

    public SavedItemsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseHelper = new FirebaseHelper(requireContext());
        savedItemsList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_items, container, false);
        initViews(view);
        setupListeners();
        setupRecyclerView();
        loadSavedItems();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_saved_items);
        textNoSavedItems = view.findViewById(R.id.text_no_saved_items);
        ivBackButton = view.findViewById(R.id.iv_back_button_saved_items);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new SavedItemsAdapter(savedItemsList, new SavedItemsAdapter.OnSavedItemClickListener() {
            @Override
            public void onSavedItemClick(Item item) {
                // Navigate to ItemDetailFragment
                Toast.makeText(getContext(), "Clicked on saved item: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                if (navController != null && item.getId() != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString("itemId", item.getId());
                    // Assuming action_savedItemsFragment_to_itemDetailFragment exists in nav_graph
                    navController.navigate(R.id.action_savedItemsFragment_to_itemDetailFragment, bundle);
                }
            }

            @Override
            public void onRemoveClick(Item item) {
                // Handle removing item from saved list
                showRemoveConfirmationDialog(item);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadSavedItems() {
        String currentUserId = firebaseHelper.getCurrentUserId();
        if (currentUserId == null) {
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập.", Toast.LENGTH_SHORT).show();
            updateEmptyState();
            return;
        }

        // 1. Get item IDs from saved_items for current user
        DatabaseReference savedItemsRef = FirebaseDatabase.getInstance().getReference("saved_items").child(currentUserId);
        savedItemsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isAdded()) {
                    savedItemsList.clear();
                    List<String> itemIds = new ArrayList<>();
                    for (DataSnapshot idSnapshot : snapshot.getChildren()) {
                        if (Boolean.TRUE.equals(idSnapshot.getValue(Boolean.class))) { // Ensure it's marked as true
                            itemIds.add(idSnapshot.getKey()); // Get item ID from key
                        }
                    }
                    Log.d(TAG, "Found " + itemIds.size() + " saved item IDs.");

                    if (itemIds.isEmpty()) {
                        updateEmptyState();
                        return;
                    }

                    // 2. Fetch details for each item ID from 'items' node
                    DatabaseReference itemsRef = FirebaseDatabase.getInstance().getReference("items");
                    for (String itemId : itemIds) {
                        itemsRef.child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot itemSnapshot) {
                                if (isAdded()) {
                                    Item item = itemSnapshot.getValue(Item.class);
                                    if (item != null) {
                                        item.setId(itemSnapshot.getKey()); // Set ID from key
                                        savedItemsList.add(item);
                                        adapter.notifyDataSetChanged(); // Notify after each item is added (can be optimized)
                                    } else {
                                        // Item might have been deleted, remove from saved_items
                                        removeSavedItemFromFirebase(currentUserId, itemSnapshot.getKey());
                                    }
                                    updateEmptyState();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Failed to load item details for ID " + itemId + ": " + error.getMessage());
                                if (isAdded()) {
                                    Toast.makeText(getContext(), "Lỗi khi tải chi tiết mặt hàng đã lưu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    updateEmptyState();
                                }
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load saved item IDs: " + error.getMessage());
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi khi tải danh sách mặt hàng đã lưu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                }
            }
        });
    }

    private void showRemoveConfirmationDialog(Item item) {
        if (!isAdded()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa mặt hàng đã lưu")
                .setMessage("Bạn có chắc chắn muốn xóa \"" + item.getTitle() + "\" khỏi danh sách đã lưu?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    removeSavedItem(item);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void removeSavedItem(Item item) {
        String currentUserId = firebaseHelper.getCurrentUserId();
        if (currentUserId == null || item.getId() == null) {
            Toast.makeText(getContext(), "Không thể xóa mặt hàng đã lưu.", Toast.LENGTH_SHORT).show();
            return;
        }
        removeSavedItemFromFirebase(currentUserId, item.getId());
    }

    private void removeSavedItemFromFirebase(String userId, String itemId) {
        DatabaseReference savedItemRef = FirebaseDatabase.getInstance().getReference("saved_items").child(userId).child(itemId);
        savedItemRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Đã xóa mặt hàng khỏi danh sách đã lưu.", Toast.LENGTH_SHORT).show();
                        // Refresh list by re-loading or removing from local list
                        // For simplicity, we'll let the ValueEventListener handle the refresh
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Lỗi khi xóa mặt hàng đã lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmptyState() {
        if (isAdded()) {
            if (savedItemsList.isEmpty()) {
                textNoSavedItems.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                textNoSavedItems.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    // Adapter for the RecyclerView
    private static class SavedItemsAdapter extends RecyclerView.Adapter<SavedItemsAdapter.SavedItemViewHolder> {

        private List<Item> items;
        private OnSavedItemClickListener listener;

        public interface OnSavedItemClickListener {
            void onSavedItemClick(Item item);
            void onRemoveClick(Item item);
        }

        public SavedItemsAdapter(List<Item> items, OnSavedItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public SavedItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_item, parent, false);
            return new SavedItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SavedItemViewHolder holder, int position) {
            Item item = items.get(position);
            holder.bind(item, listener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class SavedItemViewHolder extends RecyclerView.ViewHolder {
            ImageView ivSavedItemThumbnail;
            TextView tvSavedItemTitle, tvSavedItemPrice, tvSavedItemStatus;
            ImageView ivRemoveSavedItem;

            public SavedItemViewHolder(@NonNull View itemView) {
                super(itemView);
                ivSavedItemThumbnail = itemView.findViewById(R.id.iv_saved_item_thumbnail);
                tvSavedItemTitle = itemView.findViewById(R.id.tv_saved_item_title);
                tvSavedItemPrice = itemView.findViewById(R.id.tv_saved_item_price);
                tvSavedItemStatus = itemView.findViewById(R.id.tv_saved_item_status);
                ivRemoveSavedItem = itemView.findViewById(R.id.iv_remove_saved_item);
            }

            public void bind(final Item item, final OnSavedItemClickListener listener) {
                tvSavedItemTitle.setText(item.getTitle());
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                currencyFormatter.setMaximumFractionDigits(0);
                tvSavedItemPrice.setText("Giá: " + currencyFormatter.format(item.getPrice()) + " VNĐ");
                tvSavedItemStatus.setText("Trạng thái: " + item.getStatus());

                if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(item.getPhotos().get(0))
                            .placeholder(R.drawable.img_placeholder)
                            .error(R.drawable.img_error)
                            .into(ivSavedItemThumbnail);
                } else {
                    ivSavedItemThumbnail.setImageResource(R.drawable.img_placeholder);
                }

                itemView.setOnClickListener(v -> listener.onSavedItemClick(item));
                ivRemoveSavedItem.setOnClickListener(v -> listener.onRemoveClick(item));
            }
        }
    }
}
