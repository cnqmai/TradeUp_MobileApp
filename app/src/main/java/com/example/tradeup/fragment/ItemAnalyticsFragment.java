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
import java.util.Locale;
import java.util.Map;

public class ItemAnalyticsFragment extends Fragment {

    private static final String TAG = "ItemAnalyticsFragment";

    // UI Elements
    private ImageView ivBackButton;
    private ImageView ivItemThumbnail;
    private TextView tvItemTitle, tvItemPrice, tvItemStatus;
    private TextView tvViewsCount, tvChatsCount, tvOffersCount;

    private NavController navController;
    private FirebaseHelper firebaseHelper;
    private String itemId;

    public ItemAnalyticsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            itemId = getArguments().getString("itemId");
        }
        firebaseHelper = new FirebaseHelper(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_analytics, container, false);
        initViews(view);
        setupListeners();
        loadItemDetailsAndAnalytics();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
    }

    private void initViews(View view) {
        ivBackButton = view.findViewById(R.id.iv_back_button_item_analytics);
        ivItemThumbnail = view.findViewById(R.id.iv_item_thumbnail);
        tvItemTitle = view.findViewById(R.id.tv_item_title);
        tvItemPrice = view.findViewById(R.id.tv_item_price);
        tvItemStatus = view.findViewById(R.id.tv_item_status);
        tvViewsCount = view.findViewById(R.id.tv_views_count);
        tvChatsCount = view.findViewById(R.id.tv_chats_count);
        tvOffersCount = view.findViewById(R.id.tv_offers_count);
    }

    private void setupListeners() {
        ivBackButton.setOnClickListener(v -> {
            if (navController != null) {
                navController.navigateUp();
            }
        });
    }

    private void loadItemDetailsAndAnalytics() {
        if (itemId == null) {
            Toast.makeText(getContext(), "Không tìm thấy ID tin đăng.", Toast.LENGTH_SHORT).show();
            if (navController != null) {
                navController.navigateUp();
            }
            return;
        }

        // Load Item details
        firebaseHelper.getItem(itemId, new FirebaseHelper.DbReadCallback<Item>() {
            @Override
            public void onSuccess(Item item) {
                if (isAdded() && item != null) {
                    tvItemTitle.setText(item.getTitle());
                    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN")); // Vietnamese currency format
                    currencyFormatter.setMaximumFractionDigits(0);
                    tvItemPrice.setText("Giá: " + currencyFormatter.format(item.getPrice()) + " VNĐ");
                    tvItemStatus.setText("Trạng thái: " + item.getStatus());

                    if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                        Glide.with(requireContext())
                                .load(item.getPhotos().get(0)) // Load first photo as thumbnail
                                .placeholder(R.drawable.img_placeholder)
                                .error(R.drawable.img_error)
                                .into(ivItemThumbnail);
                    } else {
                        ivItemThumbnail.setImageResource(R.drawable.img_placeholder);
                    }
                } else if (isAdded()) {
                    Toast.makeText(getContext(), "Không tìm thấy chi tiết tin đăng.", Toast.LENGTH_SHORT).show();
                    if (navController != null) {
                        navController.navigateUp();
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load item details: " + errorMessage);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi khi tải chi tiết tin đăng: " + errorMessage, Toast.LENGTH_SHORT).show();
                    if (navController != null) {
                        navController.navigateUp();
                    }
                }
            }
        });

        // Load Item Analytics
        firebaseHelper.getItemAnalytics(itemId, new FirebaseHelper.DbReadCallback<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> analyticsData) {
                if (isAdded() && analyticsData != null) {
                    tvViewsCount.setText(String.valueOf(analyticsData.getOrDefault("views", 0)));
                    tvChatsCount.setText(String.valueOf(analyticsData.getOrDefault("chats_started", 0)));
                    tvOffersCount.setText(String.valueOf(analyticsData.getOrDefault("offers_made", 0)));
                } else if (isAdded()) {
                    // If no analytics data, display 0s (default values in XML already 0)
                    Log.d(TAG, "No analytics data found for item: " + itemId);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load item analytics: " + errorMessage);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi khi tải phân tích tin đăng: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
