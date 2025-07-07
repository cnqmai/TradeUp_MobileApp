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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tradeup.R;
import com.example.tradeup.adapter.ItemAdapter; // Sử dụng lại ItemAdapter
import com.example.tradeup.model.Item;
import com.example.tradeup.utils.FirebaseHelper;

import java.util.ArrayList;
import java.util.List;

public class CategoryItemListFragment extends Fragment {

    private static final String TAG = "CategoryItemListFrag";

    private TextView tvCategoryTitle;
    private RecyclerView rvCategoryItems;
    private TextView tvNoItemsInCategory;
    private ImageView ivBackButton;

    private ItemAdapter itemAdapter;
    private FirebaseHelper firebaseHelper;
    private String categoryName;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_item_list, container, false);

        tvCategoryTitle = view.findViewById(R.id.tv_category_title);
        rvCategoryItems = view.findViewById(R.id.rv_category_items);
        tvNoItemsInCategory = view.findViewById(R.id.tv_no_items_in_category);
        ivBackButton = view.findViewById(R.id.iv_back_button_category_items);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        firebaseHelper = new FirebaseHelper(requireContext());

        // Lấy tên category từ arguments
        if (getArguments() != null) {
            categoryName = getArguments().getString("categoryName");
            if (categoryName != null) {
                tvCategoryTitle.setText(categoryName);
                fetchItemsByCategory(categoryName);
            } else {
                tvCategoryTitle.setText("Category Items"); // Fallback title
                tvNoItemsInCategory.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "No category specified.", Toast.LENGTH_SHORT).show();
            }
        } else {
            tvCategoryTitle.setText("Category Items"); // Fallback title
            tvNoItemsInCategory.setVisibility(View.VISIBLE);
            Toast.makeText(getContext(), "No category specified.", Toast.LENGTH_SHORT).show();
        }

        setupRecyclerView();
        setupListeners();
    }

    private void setupRecyclerView() {
        // Use GridLayoutManager for grid display
        rvCategoryItems.setLayoutManager(new GridLayoutManager(getContext(), 2)); // 2 columns
        itemAdapter = new ItemAdapter(requireContext(), new ArrayList<>(), R.layout.item_list_vertical); // Fixed constructor parameters
        rvCategoryItems.setAdapter(itemAdapter);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            navController.navigateUp(); // Quay lại Fragment trước đó
        });
    }

    private void fetchItemsByCategory(String category) {
        firebaseHelper.getItemsByCategory(category, new FirebaseHelper.OnItemsFetchListener() {
            @Override
            public void onItemsFetched(List<Item> items) {
                if (isAdded()) {
                    if (items != null && !items.isEmpty()) {
                        itemAdapter.setItems(items);
                        rvCategoryItems.setVisibility(View.VISIBLE);
                        tvNoItemsInCategory.setVisibility(View.GONE);
                    } else {
                        itemAdapter.setItems(new ArrayList<>()); // Clear list
                        rvCategoryItems.setVisibility(View.GONE);
                        tvNoItemsInCategory.setVisibility(View.VISIBLE);
                    }
                    Log.d(TAG, "Items fetched for category " + category + ": " + (items != null ? items.size() : 0));
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Error fetching items for category " + category + ": " + errorMessage);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error loading items: " + errorMessage, Toast.LENGTH_SHORT).show();
                    rvCategoryItems.setVisibility(View.GONE);
                    tvNoItemsInCategory.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}