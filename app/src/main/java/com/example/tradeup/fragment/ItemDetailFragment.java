package com.example.tradeup.fragment;

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
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.tradeup.R;
import com.example.tradeup.model.Item;
import com.example.tradeup.model.User;
import com.example.tradeup.utils.FirebaseHelper;
import com.google.firebase.auth.FirebaseAuth; // Cần import này để lấy User ID
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase; // Cần import này để truy cập DatabaseReference
import com.google.firebase.database.ServerValue; // Cần import này cho ServerValue.TIMESTAMP
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ItemDetailFragment extends Fragment {

    private static final String TAG = "ItemDetailFragment";

    private TextView tvTitle, tvPrice, tvDescription, tvCategory, tvCondition, tvLocation,
            tvItemBehavior, tvTags, tvStatus, tvSellerName, tvSellerRating, tvViewsCount;

    // >>> THÊM CÁC TRƯỜNG TEXTVIEW MỚI CHO RATING CỦA SẢN PHẨM <<<
    private TextView tvItemAverageRating;
    private TextView tvItemRatingCount;

    private Button btnChatSeller, btnMakeOffer, btnAddToFavorites;
    private ViewPager2 vpItemImages;

    private String itemId;
    private FirebaseHelper firebaseHelper;

    private String sellerId;
    private String currentUserId; // Để lưu trữ User ID hiện tại

    public ItemDetailFragment() {
        // Required empty public constructor
    }

    public static ItemDetailFragment newInstance(String itemId) {
        ItemDetailFragment fragment = new ItemDetailFragment();
        Bundle args = new Bundle();
        args.putString("itemId", itemId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called.");
        firebaseHelper = new FirebaseHelper();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (getArguments() != null) {
            itemId = getArguments().getString("itemId");
            Log.d(TAG, "itemId received in onCreate: " + itemId);
        } else {
            Log.w(TAG, "No arguments or itemId found in onCreate.");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called.");
        View view = inflater.inflate(R.layout.fragment_item_detail, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called. Current itemId: " + itemId);
        if (itemId != null) {
            loadItemDetails(itemId);
            updateItemViews(itemId);
        } else {
            Toast.makeText(getContext(), "Không tìm thấy ID tin đăng.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onResume: itemId is null, popping back stack.");
            if (getParentFragmentManager() != null && getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        }
    }

    private void initViews(View view) {
        tvTitle = view.findViewById(R.id.tv_detail_title);
        tvPrice = view.findViewById(R.id.tv_detail_price);
        tvDescription = view.findViewById(R.id.tv_detail_description);
        tvCategory = view.findViewById(R.id.tv_detail_category);
        tvCondition = view.findViewById(R.id.tv_detail_condition);
        tvLocation = view.findViewById(R.id.tv_detail_location);
        tvItemBehavior = view.findViewById(R.id.tv_detail_item_behavior);
        tvTags = view.findViewById(R.id.tv_detail_tags);
        tvStatus = view.findViewById(R.id.tv_detail_status);
        tvSellerName = view.findViewById(R.id.tv_detail_seller_name);
        tvSellerRating = view.findViewById(R.id.tv_detail_seller_rating);
        tvViewsCount = view.findViewById(R.id.tv_detail_views_count);

        // >>> ÁNH XẠ CÁC TEXTVIEW MỚI CHO RATING CỦA SẢN PHẨM <<<
        tvItemAverageRating = view.findViewById(R.id.tv_detail_item_average_rating);
        tvItemRatingCount = view.findViewById(R.id.tv_detail_item_rating_count);


        btnChatSeller = view.findViewById(R.id.btn_detail_chat_seller);
        btnMakeOffer = view.findViewById(R.id.btn_detail_make_offer);
        btnAddToFavorites = view.findViewById(R.id.btn_detail_add_to_favorites);
        vpItemImages = view.findViewById(R.id.vp_item_images);

        btnChatSeller.setOnClickListener(v -> Toast.makeText(getContext(), "Chức năng chat đang phát triển!", Toast.LENGTH_SHORT).show());
        btnMakeOffer.setOnClickListener(v -> Toast.makeText(getContext(), "Chức năng đề nghị đang phát triển!", Toast.LENGTH_SHORT).show());
        btnAddToFavorites.setOnClickListener(v -> Toast.makeText(getContext(), "Chức năng yêu thích đang phát triển!", Toast.LENGTH_SHORT).show());
    }

    private void loadItemDetails(String id) {
        Log.d(TAG, "loadItemDetails called for itemId: " + id);
        firebaseHelper.getItem(id, new FirebaseHelper.DbReadCallback<Item>() {
            @Override
            public void onSuccess(Item item) {
                if (item != null) {
                    Log.d(TAG, "Item data loaded successfully for itemId: " + id + ", title: " + item.getTitle());
                    tvTitle.setText(item.getTitle());
                    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                    currencyFormatter.setMaximumFractionDigits(0);
                    tvPrice.setText("Giá: " + currencyFormatter.format(item.getPrice()));
                    tvDescription.setText(item.getDescription());
                    tvCategory.setText("Danh mục: " + item.getCategory());
                    tvCondition.setText("Tình trạng: " + item.getCondition());

                    if (item.getLocation() != null) {
                        tvLocation.setText("Vị trí: " + item.getLocation().getManual_address());
                    } else {
                        tvLocation.setText("Vị trí: Không xác định");
                    }

                    tvItemBehavior.setText("Cách thức giao dịch: " + (item.getItem_behavior() != null ? item.getItem_behavior() : "Chưa xác định"));
                    if (item.getTags() != null && !item.getTags().isEmpty()) {
                        tvTags.setText("Tags: " + android.text.TextUtils.join(", ", item.getTags()));
                    } else {
                        tvTags.setText("Tags: Không có");
                    }
                    tvStatus.setText("Trạng thái: " + item.getStatus());

                    // >>> HIỂN THỊ RATING CỦA SẢN PHẨM <<<
                    if (item.getAverage_rating() != null && item.getRating_count() != null) {
                        tvItemAverageRating.setText(String.format(Locale.getDefault(), "Đánh giá SP: %.1f/5.0", item.getAverage_rating()));
                        tvItemRatingCount.setText(String.format(Locale.getDefault(), "(%d lượt)", item.getRating_count()));
                    } else {
                        tvItemAverageRating.setText("Đánh giá SP: N/A");
                        tvItemRatingCount.setText("(0 lượt)");
                    }


                    sellerId = item.getUser_id();
                    loadSellerInfo(sellerId);

                    if (item.getPhotos() != null && !item.getPhotos().isEmpty()) {
                        Log.d(TAG, "Loading " + item.getPhotos().size() + " images.");
                        ImageSliderAdapter adapter = new ImageSliderAdapter(item.getPhotos());
                        vpItemImages.setAdapter(adapter);
                    } else {
                        Log.d(TAG, "No images found, using placeholder.");
                        List<String> placeholders = new ArrayList<>();
                        placeholders.add("android.resource://" + requireContext().getPackageName() + "/" + R.drawable.img_placeholder);
                        ImageSliderAdapter adapter = new ImageSliderAdapter(placeholders);
                        vpItemImages.setAdapter(adapter);
                    }

                    // >>> GHI NHẬN HOẠT ĐỘNG DUYỆT WEB CỦA NGƯỜI DÙNG <<<
                    if (currentUserId != null && item.getCategory() != null) {
                        recordUserView(currentUserId, id, item.getCategory());
                    } else {
                        Log.w(TAG, "Cannot record user view: currentUserId or item category is null.");
                    }

                } else {
                    Log.w(TAG, "Item data is null for itemId: " + id);
                    Toast.makeText(getContext(), "Không thể tải chi tiết tin đăng: Dữ liệu trống.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(getContext(), "Lỗi tải chi tiết tin đăng: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading item details: " + errorMessage);
            }
        });
    }

    private void loadSellerInfo(String userId) {
        Log.d(TAG, "loadSellerInfo called for userId: " + userId);
        firebaseHelper.getUserProfile(userId, new FirebaseHelper.DbReadCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    Log.d(TAG, "Seller info loaded: " + user.getDisplay_name());
                    tvSellerName.setText("Người bán: " + user.getDisplay_name());
                    Double rating = user.getRating();
                    if (rating != null) {
                        tvSellerRating.setText(String.format(Locale.getDefault(), "Đánh giá: %.1f/5.0", rating));
                    } else {
                        tvSellerRating.setText("Đánh giá: N/A");
                    }
                } else {
                    Log.w(TAG, "Seller data is null for userId: " + userId);
                    tvSellerName.setText("Người bán: Không xác định");
                    tvSellerRating.setText("Đánh giá: N/A");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load seller info: " + errorMessage);
                tvSellerName.setText("Người bán: Lỗi tải");
                tvSellerRating.setText("Đánh giá: Lỗi tải");
            }
        });
    }

    private void updateItemViews(String id) {
        Log.d(TAG, "updateItemViews called for itemId: " + id);
        firebaseHelper.incrementItemView(id, new FirebaseHelper.DbWriteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "View count incremented successfully for item: " + id);
                firebaseHelper.getItemAnalytics(id, new FirebaseHelper.DbReadCallback<Map<String, Object>>() {
                    @Override
                    public void onSuccess(Map<String, Object> analyticsData) {
                        if (analyticsData != null && analyticsData.containsKey("views")) {
                            long views = 0;
                            Object viewsObj = analyticsData.get("views");
                            if (viewsObj instanceof Long) {
                                views = (Long) viewsObj;
                            } else if (viewsObj instanceof Integer) {
                                views = ((Integer) viewsObj).longValue();
                            }
                            Log.d(TAG, "Updated views fetched: " + views);
                            tvViewsCount.setText("Lượt xem: " + views);
                        } else {
                            Log.d(TAG, "Analytics data or views not found for item: " + id);
                            tvViewsCount.setText("Lượt xem: 0");
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to fetch updated views for item " + id + ": " + errorMessage);
                        tvViewsCount.setText("Lượt xem: Lỗi tải");
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to increment view count for item " + id + ": " + errorMessage);
            }
        });
    }

    /**
     * Ghi nhận lượt xem danh mục của người dùng vào node user_activity.
     * Phương thức này sẽ được gọi mỗi khi người dùng xem chi tiết sản phẩm.
     * @param userId ID của người dùng hiện tại.
     * @param itemId ID của sản phẩm đang được xem.
     * @param category Danh mục của sản phẩm đang được xem.
     */
    private void recordUserView(String userId, String itemId, String category) {
        if (userId == null || itemId == null || category == null || category.isEmpty()) {
            Log.w(TAG, "Cannot record user view: missing userId, itemId or category.");
            return;
        }

        // Tùy chọn: Bạn có thể di chuyển logic này vào FirebaseHelper để quản lý tập trung hơn
        // Ví dụ: firebaseHelper.recordUserCategoryView(userId, category, new FirebaseHelper.DbWriteCallback() {...});

        FirebaseDatabase.getInstance().getReference("user_activity")
                .child(userId)
                .child("viewed_categories")
                .child(category)
                .child("last_viewed")
                .setValue(ServerValue.TIMESTAMP) // Ghi lại thời gian xem gần nhất
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User category view recorded for: " + category))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to record user category view: " + e.getMessage()));

        // Tùy chọn: Ghi nhận cả từng item đã xem nếu cần chi tiết hơn
        // FirebaseDatabase.getInstance().getReference("user_activity")
        //         .child(userId)
        //         .child("viewed_items")
        //         .child(itemId)
        //         .setValue(Map.of("timestamp", ServerValue.TIMESTAMP, "category", category))
        //         .addOnSuccessListener(aVoid -> Log.d(TAG, "User item view recorded for: " + itemId))
        //         .addOnFailureListener(e -> Log.e(TAG, "Failed to record user item view: " + e.getMessage()));
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called.");

        tvTitle = null;
        tvPrice = null;
        tvDescription = null;
        tvCategory = null;
        tvCondition = null;
        tvLocation = null;
        tvItemBehavior = null;
        tvTags = null;
        tvStatus = null;
        tvSellerName = null;
        tvSellerRating = null;
        tvViewsCount = null;
        tvItemAverageRating = null; // Giải phóng TextView mới
        tvItemRatingCount = null;   // Giải phóng TextView mới
        btnChatSeller = null;
        btnMakeOffer = null;
        btnAddToFavorites = null;
        vpItemImages = null;
    }

    private class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {
        private List<String> imageUrls;

        public ImageSliderAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_slider_item, parent, false);
            return new SliderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrls.get(position))
                    .placeholder(R.drawable.img_placeholder)
                    .error(R.drawable.img_error)
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        class SliderViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public SliderViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_slider_image);
            }
        }
    }
}