package com.example.tradeup.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Thêm import cho Button
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController; // Thêm import cho NavController
import androidx.navigation.Navigation; // Thêm import cho Navigation
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.adapter.ImageSliderAdapter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PreviewItemFragment extends Fragment {

    private TextView tvTitle, tvPrice, tvDescription, tvCategory, tvCondition, tvLocation,
            tvItemBehavior, tvTags;
    private ViewPager2 vpItemImages;
    private ImageSliderAdapter imageSliderAdapter;
    private Button btnBackToEdit; // Khai báo nút
    private NavController navController; // Khai báo NavController

    public PreviewItemFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_preview_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        displayItemDetails();

        // Khởi tạo NavController
        navController = Navigation.findNavController(view);

        // Thiết lập OnClickListener cho nút "Quay lại chỉnh sửa"
        btnBackToEdit.setOnClickListener(v -> {
            if (navController != null) {
                navController.popBackStack(); // Quay trở lại Fragment trước đó
            }
        });
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tv_preview_title);
        tvPrice = view.findViewById(R.id.tv_preview_price);
        tvDescription = view.findViewById(R.id.tv_preview_description);
        tvCategory = view.findViewById(R.id.tv_preview_category);
        tvCondition = view.findViewById(R.id.tv_preview_condition);
        tvLocation = view.findViewById(R.id.tv_preview_location);
        tvItemBehavior = view.findViewById(R.id.tv_preview_item_behavior);
        tvTags = view.findViewById(R.id.tv_preview_tags);
        vpItemImages = view.findViewById(R.id.vp_preview_item_images);
        btnBackToEdit = view.findViewById(R.id.btn_back_to_edit); // Ánh xạ nút
    }

    private void displayItemDetails() {
        if (getArguments() != null) {
            String title = PreviewItemFragmentArgs.fromBundle(getArguments()).getTitle();
            long price = PreviewItemFragmentArgs.fromBundle(getArguments()).getPrice();
            String description = PreviewItemFragmentArgs.fromBundle(getArguments()).getDescription();
            String category = PreviewItemFragmentArgs.fromBundle(getArguments()).getCategory();
            String condition = PreviewItemFragmentArgs.fromBundle(getArguments()).getCondition();
            String location = PreviewItemFragmentArgs.fromBundle(getArguments()).getLocation();
            String itemBehavior = PreviewItemFragmentArgs.fromBundle(getArguments()).getItemBehavior();
            String[] tagsArray = PreviewItemFragmentArgs.fromBundle(getArguments()).getTags();
            String[] imageUrlsArray = PreviewItemFragmentArgs.fromBundle(getArguments()).getImageUrls();

            tvTitle.setText(title);
            tvPrice.setText(NumberFormat.getCurrencyInstance(new Locale("vi", "VN")).format(price));
            tvDescription.setText(description);
            tvCategory.setText(category);
            tvCondition.setText(condition);
            tvLocation.setText(location);
            tvItemBehavior.setText(itemBehavior.isEmpty() ? getString(R.string.no_item_behavior) : itemBehavior);

            if (tagsArray != null && tagsArray.length > 0) {
                StringBuilder tagsSb = new StringBuilder();
                for (String tag : tagsArray) {
                    tagsSb.append("#").append(tag).append(" ");
                }
                tvTags.setText(tagsSb.toString().trim());
            } else {
                tvTags.setText(R.string.no_tags);
            }

            // Setup Image Slider
            List<String> imageUrls = new ArrayList<>();
            if (imageUrlsArray != null) {
                for (String url : imageUrlsArray) {
                    imageUrls.add(url);
                }
            }
            if (!imageUrls.isEmpty()) {
                imageSliderAdapter = new ImageSliderAdapter(imageUrls);
                vpItemImages.setAdapter(imageSliderAdapter);
            } else {
                vpItemImages.setVisibility(View.GONE);
            }
        }
    }
}