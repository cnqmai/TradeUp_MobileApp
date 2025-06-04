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

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView btnFilter = view.findViewById(R.id.btn_filter);
        btnFilter.setOnClickListener(v -> {
            FilterBottomSheet.newInstance().show(getChildFragmentManager(), "FilterBottomSheet");
        });

        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        categoryRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );

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

        Log.d("FIREBASE", "Bắt đầu truy cập Database...");

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("FIREBASE", "onDataChange: dữ liệu đã nhận");

                categoryList.clear(); // Clear trước khi add để tránh trùng
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Category category = dataSnapshot.getValue(Category.class);
                    if (category != null) {
                        categoryList.add(category);
                        Log.d("FIREBASE", "Thêm category: " + category.getName());
                    } else {
                        Log.w("FIREBASE", "Category null trong snapshot");
                    }
                }
                adapter.notifyDataSetChanged(); // Cập nhật RecyclerView
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FIREBASE", "Lỗi khi truy cập database: " + error.getMessage());
                Toast.makeText(requireContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Log.d("Banner", "Image URL: " + banner.getImageUrl());
                    Log.d("Banner", "Button Text: " + banner.getButtonText());
                    Log.d("Banner", "Action URL: " + banner.getActionUrl());

                    // Load ảnh
                    Glide.with(requireContext()).asGif().load(banner.getImageUrl()).into(bannerImage);

                    // Cập nhật các thành phần khác
                    bannerButton.setText(banner.getButtonText());

                    bannerButton.setOnClickListener(v -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(banner.getActionUrl()));
                        startActivity(browserIntent);
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Lỗi đọc dữ liệu", error.toException());
            }
        });
    }
}

