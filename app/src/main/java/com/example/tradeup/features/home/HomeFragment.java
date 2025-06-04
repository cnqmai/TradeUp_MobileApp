package com.example.tradeup.features.home;

import android.content.Intent;
import android.net.Uri;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.features.banner.BannerItem;
import com.example.tradeup.features.category.Category;
import com.example.tradeup.features.category.CategoryAdapter;
import com.example.tradeup.features.filter.FilterBottomSheet;
import com.example.tradeup.features.product.Product;
import com.example.tradeup.features.product.ProductAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView categoryRecyclerView;
    private CategoryAdapter adapter;
    private List<Category> categoryList = new ArrayList<>();

    private RecyclerView productRecyclerView;
    private ProductAdapter productAdapter;      // Adapter cho products
    private List<Product> productList = new ArrayList<>();  // Danh sách products

    public HomeFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_filter).setOnClickListener(v ->
                FilterBottomSheet.newInstance().show(getChildFragmentManager(), "FilterBottomSheet"));

        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false));

        adapter = new CategoryAdapter(requireContext(), categoryList);
        categoryRecyclerView.setAdapter(adapter);

        loadCategoriesFromFirebase();

        ImageView bannerImage = view.findViewById(R.id.bannerImage);
        Button bannerButton = view.findViewById(R.id.bannerButton);

        loadBannerFromFirebase(bannerImage, bannerButton);
    }

    private void loadCategoriesFromFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://tradeup-a83a0-default-rtdb.asia-southeast1.firebasedatabase.app/");
        DatabaseReference databaseRef = database.getReference("categories");

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Category category = dataSnapshot.getValue(Category.class);
                    if (category != null) {
                        categoryList.add(category);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Lỗi tải danh mục: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBannerFromFirebase(ImageView bannerImage, Button bannerButton) {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://tradeup-a83a0-default-rtdb.asia-southeast1.firebasedatabase.app/");
        DatabaseReference bannerRef = database.getReference("banners/banner1");

        bannerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                BannerItem banner = snapshot.getValue(BannerItem.class);
                if (banner != null) {
                    String imageUrl = banner.getImageUrl();

                    if (imageUrl != null) {
                        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                            Glide.with(requireContext()).load(imageUrl)
                                    .placeholder(R.drawable.placeholder_image)
                                    .into(bannerImage);
                        } else {
                            int resId = getResources().getIdentifier(imageUrl, "drawable", requireContext().getPackageName());
                            if (resId != 0) {
                                bannerImage.setImageResource(resId);
                            } else {
                                bannerImage.setImageResource(R.drawable.placeholder_image);
                            }
                        }
                    } else {
                        bannerImage.setImageResource(R.drawable.placeholder_image);
                    }

                    bannerButton.setText(banner.getButtonText() != null ? banner.getButtonText() : "Xem thêm");

                    bannerButton.setOnClickListener(v -> {
                        String actionUrl = banner.getActionUrl();
                        if (actionUrl != null && !actionUrl.trim().isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl));
                            startActivity(intent);
                        } else {
                            Toast.makeText(requireContext(), "Không có đường dẫn hành động", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Lỗi tải banner: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProductsFromFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://tradeup-a83a0-default-rtdb.asia-southeast1.firebasedatabase.app/");
        DatabaseReference productRef = database.getReference("products");

        productRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Product product = dataSnapshot.getValue(Product.class);
                    if (product != null) {
                        productList.add(product);
                    }
                }

                // 👉 Cá nhân hóa:
                // 1️⃣ Sắp xếp theo độ phổ biến
                productList.sort((p1, p2) -> Float.compare(p2.getPopularity(), p1.getPopularity()));

                // 2️⃣ Giả sử: nếu có Location (hoặc sau khi lấy được), có thể sắp xếp thêm theo vị trí
                // ví dụ:
                // Location userLocation = ...;
                // for (Product p : productList) {
                //     p.setDistanceToUser(TÍNH khoảng cách ở đây);
                // }
                // Collections.sort(productList, Comparator.comparing(Product::getDistanceToUser));

                productAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Lỗi tải sản phẩm: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
