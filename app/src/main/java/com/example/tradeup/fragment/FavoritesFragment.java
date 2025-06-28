// Tạo file mới: com.example.tradeup.fragment.FavoritesFragment.java

package com.example.tradeup.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
// import com.example.tradeup.adapter.ItemListAdapter; // Bạn sẽ cần adapter riêng cho danh sách sản phẩm
// import com.example.tradeup.model.Item; // Cần model Item của bạn
import com.example.tradeup.model.Item;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FavoritesFragment extends Fragment {

    private static final String TAG = "FavoritesFragment";

    private RecyclerView recyclerViewFavorites;
    private TextView tvNoFavorites;
    // private ItemListAdapter favoritesAdapter; // Adapter để hiển thị danh sách Item
    private List<String> favoriteItemIds; // Danh sách các itemId yêu thích

    private String currentUserId;
    private DatabaseReference favoritesRef;
    private DatabaseReference itemsRef;

    public FavoritesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        favoritesRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId).child("favorites");
        itemsRef = FirebaseDatabase.getInstance().getReference("items"); // Tham chiếu đến node items
        favoriteItemIds = new ArrayList<>();
        // favoritesAdapter = new ItemListAdapter(getContext(), new ArrayList<>()); // Khởi tạo adapter trống
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);
        recyclerViewFavorites = view.findViewById(R.id.recycler_view_favorites);
        tvNoFavorites = view.findViewById(R.id.tv_no_favorites);

        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        // recyclerViewFavorites.setAdapter(favoritesAdapter); // Đặt adapter vào RecyclerView

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadFavoriteItemIds();
    }

    private void loadFavoriteItemIds() {
        favoritesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoriteItemIds.clear();
                List<Item> favoriteItems = new ArrayList<>(); // Danh sách các Item đầy đủ

                if (snapshot.exists() && snapshot.hasChildren()) {
                    for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                        String itemId = itemSnapshot.getKey(); // Lấy itemId từ key
                        if (itemId != null) {
                            favoriteItemIds.add(itemId);
                        }
                    }
                    if (favoriteItemIds.isEmpty()) {
                        tvNoFavorites.setVisibility(View.VISIBLE);
                        recyclerViewFavorites.setVisibility(View.GONE);
                        // favoritesAdapter.updateItems(favoriteItems); // Cập nhật adapter rỗng
                    } else {
                        tvNoFavorites.setVisibility(View.GONE);
                        recyclerViewFavorites.setVisibility(View.VISIBLE);
                        fetchFavoriteItems(favoriteItemIds); // Lấy chi tiết các Item
                    }
                } else {
                    tvNoFavorites.setVisibility(View.VISIBLE);
                    recyclerViewFavorites.setVisibility(View.GONE);
                    // favoritesAdapter.updateItems(favoriteItems); // Cập nhật adapter rỗng
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load favorite item IDs: " + error.getMessage());
                Toast.makeText(getContext(), "Lỗi tải mục yêu thích: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                tvNoFavorites.setText("Lỗi tải mục yêu thích.");
                tvNoFavorites.setVisibility(View.VISIBLE);
                recyclerViewFavorites.setVisibility(View.GONE);
            }
        });
    }

    private void fetchFavoriteItems(List<String> itemIds) {
        List<Item> itemsToDisplay = new ArrayList<>();
        // Đây là cách đơn giản để lấy từng item, có thể không tối ưu cho số lượng lớn
        for (String itemId : itemIds) {
            itemsRef.child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Item item = snapshot.getValue(Item.class); // Cần model Item của bạn
                    // if (item != null) {
                    //     itemsToDisplay.add(item);
                    //     // Khi tất cả item đã được tải, cập nhật adapter
                    //     if (itemsToDisplay.size() == itemIds.size()) {
                    //         favoritesAdapter.updateItems(itemsToDisplay);
                    //     }
                    // }
                    // Tạm thời, chỉ ghi log để thấy dữ liệu được tải
                    Log.d(TAG, "Loaded favorite item: " + (snapshot.child("title").getValue(String.class)));
                    // Nếu bạn có ItemListAdapter và Item model, bỏ comment dòng trên và dòng dưới:
                    // favoritesAdapter.updateItems(itemsToDisplay); // Cập nhật ngay khi có item, hoặc sau khi tải hết
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load favorite item " + itemId + ": " + error.getMessage());
                }
            });
        }
    }
}